package com.ved.framework.base;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import com.blankj.swipepanel.SwipePanel;
import com.orhanobut.dialog.dialog.DialogStrategyFactory;
import com.orhanobut.dialog.dialog.IDialogStrategy;
import com.orhanobut.dialog.navigation.ActivityNavigator;
import com.ved.framework.R;
import com.ved.framework.bus.Messenger;
import com.ved.framework.bus.event.eventbus.EventBusUtil;
import com.ved.framework.entity.ParameterField;
import com.ved.framework.permission.IPermission;
import com.ved.framework.permission.RxPermission;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.DpiUtils;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.SoftKeyboardUtil;
import com.ved.framework.utils.phone.PhoneUtils;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

class BaseView<V extends ViewDataBinding, VM extends BaseViewModel> {
    protected V binding;
    protected VM viewModel;
    private final IBaseView<V,VM> viewDelegate;
    private IDialogStrategy dialogStrategy;

    protected BaseView(IBaseView<V, VM> viewDelegate) {
        this.viewDelegate = viewDelegate;
    }

    protected final void initialize(Bundle savedInstanceState) {
        initViewDataBinding(savedInstanceState);
        registerUIChangeLiveDataCallBack();
        initDialogStrategy();
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
            callPhone(phoneNumber);
        });

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

        // Fragment Resume事件
        if (viewModel.getUC().getOnResumeEvent() != null && viewDelegate.getLifecycleOwner() instanceof Fragment) {
            viewModel.getUC().getOnResumeEvent().observe(owner, o -> viewDelegate.initView());
        }
    }

    private void handleOnLoadEvent() {
        if (viewDelegate.getLifecycleOwner() instanceof FragmentActivity) {
            viewDelegate.initView();
            initSwipeBack();
        }

        if (viewDelegate.isRegisterEventBus()) {
            EventBusUtil.register(this);
        }

        viewDelegate.initViewObservable();
        viewModel.registerRxBus();
    }

    private void finishActivity() {
        SoftKeyboardUtil.hideSoftKeyboard(viewDelegate.FragmentActivity());
        viewDelegate.FragmentActivity().finish();
    }

    protected void showDialog() {
        showDialog("加载中...");
    }

    private void initDialogStrategy() {
        dialogStrategy = DialogStrategyFactory.createStrategy(viewDelegate);
    }

    protected void showDialog(String title){
        dialogStrategy.show(title);
    }

    protected void dismissDialog() {
        dialogStrategy.dismiss();
    }

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    protected void startActivity(Class<?> clz) {
        startActivity(clz, null);
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    protected void startActivity(Class<?> clz, Bundle bundle) {
        ActivityNavigator.with(viewDelegate.getViewContext())
                .target(clz)
                .bundle(bundle)
                .navigate();
    }

    protected void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        Intent intent = new Intent(viewDelegate.getViewContext(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }

        if (viewDelegate.getLifecycleOwner() instanceof FragmentActivity) {
            ((FragmentActivity) viewDelegate.getLifecycleOwner()).startActivityForResult(intent, requestCode);
        } else if (viewDelegate.getLifecycleOwner() instanceof Fragment) {
            ((Fragment) viewDelegate.getLifecycleOwner()).startActivityForResult(intent, requestCode);
        }
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     */
    protected void startContainerActivity(String canonicalName) {
        startContainerActivity(canonicalName, null);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     * @param bundle        跳转所携带的信息
     */
    protected void startContainerActivity(String canonicalName, Bundle bundle) {
        ActivityNavigator.with(viewDelegate.getViewContext())
                .target(ContainerActivity.class)
                .putExtra(ParameterField.FRAGMENT, canonicalName)
                .bundle(bundle)
                .navigate();
    }

    public void callPhone(String phoneNumber) {
        requestPermission(new IPermission() {
            @Override
            public void onGranted() {
                PhoneUtils.callPhone(phoneNumber);
            }

            @Override
            public void onDenied(boolean denied) {
                viewDelegate.requestCallPhone(denied);
            }
        }, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE);
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        if (viewDelegate.getLifecycleOwner() instanceof FragmentActivity) {
            RxPermission.requestPermission((FragmentActivity) viewDelegate.getLifecycleOwner(), iPermission, permissions);
        } else if (viewDelegate.getLifecycleOwner() instanceof Fragment) {
            RxPermission.requestPermission((Fragment) viewDelegate.getLifecycleOwner(), iPermission, permissions);
        }
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
            Messenger.getDefault().unregister(viewModel);
            if (viewModel != null) {
                viewModel.removeRxBus();
            }
            if(binding != null){
                binding.unbind();
            }
            if (viewDelegate.isRegisterEventBus()) {
                EventBusUtil.unregister(this);
            }
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
    }
}
