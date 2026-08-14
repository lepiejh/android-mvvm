package com.ved.framework.base;

import android.content.Intent;
import android.os.Bundle;

import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.LifecycleOwner;

import com.blankj.swipepanel.SwipePanel;
import com.ved.framework.R;
import com.ved.framework.base.helper.DialogHelper;
import com.ved.framework.base.helper.NavigatorHelper;
import com.ved.framework.base.helper.PermissionHelper;
import com.ved.framework.bus.Messenger;
import com.ved.framework.bus.event.eventbus.EventBusUtil;
import com.ved.framework.entity.ParameterField;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.DpiUtils;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.SoftKeyboardUtil;

import org.greenrobot.eventbus.EventBus;

class BaseView<V extends ViewDataBinding, VM extends BaseViewModel> {
    protected V binding;
    protected VM viewModel;
    private final IBaseView<V, VM> viewDelegate;

    // 职责助手（单一职责 + 委托模式）：对话框 / 导航 / 权限系统能力
    private final DialogHelper<V, VM> dialogHelper;
    private final NavigatorHelper<V, VM> navigatorHelper;
    private final PermissionHelper<V, VM> permissionHelper;

    // EventBus 注册状态跟踪
    private boolean isEventBusRegistered = false;

    protected BaseView(IBaseView<V, VM> viewDelegate) {
        this.viewDelegate = viewDelegate;
        this.dialogHelper = new DialogHelper<>(viewDelegate);
        this.navigatorHelper = new NavigatorHelper<>(viewDelegate);
        this.permissionHelper = new PermissionHelper<>(viewDelegate);
    }

    protected final void initialize(Bundle savedInstanceState) {
        initViewDataBinding(savedInstanceState);
        registerUIChangeLiveDataCallBack();
    }

    protected void initViewDataBinding(Bundle savedInstanceState) {
        binding = viewDelegate.getBinding(savedInstanceState);
        viewModel = viewDelegate.ensureViewModelCreated();
        if (binding != null && viewModel != null) {
            binding.setVariable(Constant.variableId, viewModel);
            binding.setLifecycleOwner(viewDelegate.getLifecycleOwner());
            viewDelegate.getViewLifecycle().addObserver(viewModel);
            viewModel.injectLifecycleProvider(viewDelegate.getLifecycleProvider());
        } else {
            KLog.e("Critical: Binding or ViewModel is null");
        }
    }

    private void registerUIChangeLiveDataCallBack() {
        if (null == viewModel){
            viewModel = viewDelegate.ensureViewModelCreated();
        }
        if (viewModel != null) {
            setupViewModelObservers();
        }
    }

    private void setupViewModelObservers() {
        LifecycleOwner owner = viewDelegate.getLifecycleOwner();

        // 对话框相关
        viewModel.getUC().getShowDialogEvent().observe(owner, this::showDialog);
        viewModel.getUC().getDismissDialogEvent().observe(owner, v -> dismissDialog());

        // 权限相关
        viewModel.getUC().getRequestPermissionEvent().observe(owner, params -> {
            IPermission iPermission = (IPermission) params.get(Constant.PERMISSION);
            String[] permissions = (String[]) params.get(Constant.PERMISSION_NAME);
            requestPermission(iPermission, permissions);
        });

        // 电话相关
        viewModel.getUC().getRequestCallPhoneEvent().observe(owner, params -> {
            String phoneNumber = (String) params.get(Constant.PHONE_NUMBER);
            permissionHelper.callPhone(phoneNumber);
        });

        viewModel.getUC().getRequestWifiRssiEvent().observe(owner, o -> permissionHelper.getWifiRssi());

        // 活动跳转相关
        viewModel.getUC().getStartActivityEvent().observe(owner, params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startActivity(clz, bundle);
        });

        viewModel.getUC().getStartActivityForResultEvent().observe(owner, params -> {
            Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            int requestCode = (int) params.get(ParameterField.REQUEST_CODE);
            startActivityForResult(clz, requestCode, bundle);
        });

        viewModel.getUC().getStartContainerActivityEvent().observe(owner, params -> {
            String canonicalName = (String) params.get(ParameterField.CANONICAL_NAME);
            Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
            startContainerActivity(canonicalName, bundle);
        });

        // 生命周期相关
        viewModel.getUC().getFinishEvent().observe(owner, v -> finishActivity());
        viewModel.getUC().getOnBackPressedEvent().observe(owner, v -> viewDelegate.FragmentActivity().onBackPressed());

        // 初始化相关
        viewModel.getUC().getOnLoadEvent().observe(owner, o -> handleOnLoadEvent());

        // 广播相关
        viewModel.getUC().getReceiverEvent().observe(owner, o -> sendReceiver());

        // Fragment Resume事件（修复：getLifecycleOwner() 在 Fragment 场景返回 viewLifecycleOwner，
        // instanceof Fragment 恒为 false，需通过 viewDelegate.isFragment() 判断宿主类型）
        if (viewModel.getUC().getOnResumeEvent() != null && owner != null) {
            if (viewDelegate.isFragment()){
                viewModel.getUC().getOnResumeEvent().observe(owner, o -> viewDelegate.initView());
            }else {
                viewModel.getUC().getOnResumeEvent().observe(owner, o -> viewDelegate.refreshView());
            }
        }
    }

    private void handleOnLoadEvent() {
        if (viewDelegate.isFragment()) {
            // Fragment 宿主：延迟到 onResume 事件中懒加载（loadView 为默认空实现）
            viewDelegate.loadView();
        } else {
            viewDelegate.initView();
            initSwipeBack();
        }
        registerEventBusIfNeeded();
        viewDelegate.initViewObservable();
        viewModel.registerRxBus();
    }

    /**
     * 按需注册 EventBus（模板方法抽取：初始化与销毁共用的注册目标解析逻辑）
     */
    private void registerEventBusIfNeeded() {
        if (!viewDelegate.isRegisterEventBus() || isEventBusRegistered) {
            return;
        }
        try {
            Object target = resolveEventBusTarget();
            if (target != null && !EventBus.getDefault().isRegistered(target)) {
                EventBusUtil.register(target);
                isEventBusRegistered = true;
            }
        } catch (Exception e) {
            Object target = resolveEventBusTarget();
            isEventBusRegistered = target != null && EventBus.getDefault().isRegistered(target);
        }
    }

    /**
     * 解析 EventBus 的注册目标（Activity / Fragment / LifecycleOwner）
     */
    private Object resolveEventBusTarget() {
        if (viewDelegate.isFragment() && viewDelegate.getFragment() != null) {
            return viewDelegate.getFragment();
        } else if (viewDelegate.FragmentActivity() != null) {
            return viewDelegate.FragmentActivity();
        } else if (viewDelegate.getLifecycleOwner() != null) {
            return viewDelegate.getLifecycleOwner();
        }
        return null;
    }

    private void finishActivity() {
        SoftKeyboardUtil.hideSoftKeyboard(viewDelegate.FragmentActivity());
        viewDelegate.FragmentActivity().finish();
    }

    protected void showDialog() {
        dialogHelper.show();
    }

    protected void showDialog(String title) {
        dialogHelper.show(title);
    }

    protected void dismissDialog() {
        dialogHelper.dismiss();
    }

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    protected void startActivity(Class<?> clz) {
        navigatorHelper.startActivity(clz);
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    protected void startActivity(Class<?> clz, Bundle bundle) {
        navigatorHelper.startActivity(clz, bundle);
    }

    protected void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        navigatorHelper.startActivityForResult(clz, requestCode, bundle);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     */
    protected void startContainerActivity(String canonicalName) {
        navigatorHelper.startContainerActivity(canonicalName);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     * @param bundle        跳转所携带的信息
     */
    protected void startContainerActivity(String canonicalName, Bundle bundle) {
        navigatorHelper.startContainerActivity(canonicalName, bundle);
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        permissionHelper.requestPermission(iPermission, permissions);
    }

    /**
     * 发送广播
     */
    public void sendReceiver(Bundle bundle){
        Intent intent = new Intent(Constant.RECEIVER_ACTION);
        if (bundle != null){
            intent.putExtras(bundle);
        }
        viewDelegate.getViewContext().sendBroadcast(intent);
    }

    public void sendReceiver(){
        sendReceiver(null);
    }

    private void initSwipeBack() {
        if (viewDelegate.isSwipeBack()) {
            final SwipePanel swipeLayout = new SwipePanel(viewDelegate.FragmentActivity());
            swipeLayout.setLeftDrawable(R.drawable.ca);
            swipeLayout.setLeftEdgeSize(DpiUtils.dip2px(viewDelegate.FragmentActivity(),16));
            swipeLayout.setLeftSwipeColor(viewDelegate.FragmentActivity().getResources().getColor(R.color.colorPrimary));
            swipeLayout.wrapView(viewDelegate.FragmentActivity().findViewById(android.R.id.content));
            swipeLayout.setOnFullSwipeListener(direction -> {
                swipeLayout.close(direction);
                viewDelegate.FragmentActivity().finish();
            });
        }
    }

    protected void onDestroy() {
        try {
            //解除Messenger注册
            if (viewModel != null) {
                Messenger.getDefault().unregister(viewModel);
                viewModel.removeRxBus();
            }
            if(binding != null){
                binding.unbind();
            }

            if (viewDelegate.isRegisterEventBus()) {
                Object target = viewDelegate.getCurrentActivity() != null
                        ? viewDelegate.getCurrentActivity()
                        : resolveEventBusTarget();
                if (target != null && EventBus.getDefault().isRegistered(target)) {
                    EventBusUtil.unregister(target);
                }
                isEventBusRegistered = false;
            }
            if (viewDelegate.hasWifi()) {
                permissionHelper.stopWifiListening();
            }
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
    }
}
