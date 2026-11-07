package com.ved.framework.base;

import android.content.Intent;
import android.os.Bundle;

import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.LifecycleOwner;

import com.blankj.swipepanel.SwipePanel;
import com.ved.framework.R;
import com.ved.framework.bus.Messenger;
import com.ved.framework.bus.event.SingleLiveEvent;
import com.ved.framework.bus.event.eventbus.EventBusUtil;
import com.ved.framework.entity.ParameterField;
import com.ved.framework.permission.IPermission;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.SoftKeyboardUtil;
import com.ved.framework.utils.bland.code.ColorUtils;
import com.ved.framework.utils.bland.code.SizeUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class BaseView<V extends ViewDataBinding, VM extends BaseViewModel<?>> {
    protected V binding;
    protected VM viewModel;
    private final IBaseView<V, VM> viewDelegate;

    // 职责助手（单一职责 + 委托模式）：对话框 / 导航 / 权限系统能力
    private final DialogHelper<V, VM> dialogHelper;
    private final NavigatorHelper<V, VM> navigatorHelper;
    private final PermissionHelper<V, VM> permissionHelper;

    // EventBus 注册状态跟踪：记录实际注册的目标对象（观察者模式的订阅配对），
    // 注销时用它精确解除，避免 Activity 宿主场景下 getCurrentActivity() 与
    // 注册时 resolveEventBusTarget() 解析出的对象不一致导致"注册了却注销不掉"
    private Object eventBusTarget;

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

    /**
     * UI 事件管线查表分发（表驱动 + 观察者模式）：
     * 每个条目 {@link EventBinding} = (事件源 LiveData, 事件处理器)，统一注册后一次遍历完成 observe。
     * 新增 UI 事件时只需在表中加一行（复杂解析逻辑下沉到私有 handler 方法），
     * 不再需要重复的 observe 样板，注册顺序与原实现保持一致。
     */
    private void setupViewModelObservers() {
        LifecycleOwner owner = viewDelegate.getLifecycleOwner();
        IUIChangeLiveData uc = viewModel.getUC();

        List<EventBinding<?>> dispatchTable = Arrays.asList(
                // 对话框相关
                bind(uc.getShowDialogEvent(), v -> showDialog()),
                bind(uc.getDismissDialogEvent(), v -> dismissDialog()),
                // 权限 / 电话 / WiFi
                bind(uc.getRequestPermissionEvent(), this::onRequestPermissionEvent),
                bind(uc.getRequestCallPhoneEvent(), params -> permissionHelper.callPhone((String) params.get(Constant.PHONE_NUMBER))),
                bind(uc.getRequestWifiRssiEvent(), o -> permissionHelper.getWifiRssi()),
                // 活动跳转相关
                bind(uc.getStartActivityEvent(), this::onStartActivityEvent),
                bind(uc.getStartActivityForResultEvent(), this::onStartActivityForResultEvent),
                bind(uc.getStartContainerActivityEvent(), this::onStartContainerActivityEvent),
                // 生命周期相关
                bind(uc.getFinishEvent(), v -> finishActivity()),
                bind(uc.getOnBackPressedEvent(), v -> viewDelegate.FragmentActivity().onBackPressed()),
                bind(uc.getOnLoadEvent(), o -> handleOnLoadEvent()),
                // 广播相关
                bind(uc.getReceiverEvent(), o -> sendReceiver()),
                // Fragment Resume 事件（getLifecycleOwner() 在 Fragment 场景返回 viewLifecycleOwner，
                // instanceof Fragment 恒为 false，需通过 viewDelegate.isFragment() 判断宿主类型）
                bind(uc.getOnResumeEvent(), o -> {
                    if (viewDelegate.isFragment()) {
                        viewDelegate.initView();
                    } else {
                        viewDelegate.refreshView();
                    }
                }));

        for (EventBinding<?> entry : dispatchTable) {
            entry.register(owner);
        }
    }

    /** 构造一个 (事件源, 处理器) 绑定条目，泛型 T 由事件源实参自动推断 */
    private static <T> EventBinding<T> bind(SingleLiveEvent<T> event, EventAction<? super T> handler) {
        return new EventBinding<>(event, handler);
    }

    /**
     * 事件处理器（自定义函数式接口）：
     * 不用 {@code java.util.function.Consumer} 是因为它要求 API 24+，
     * 本框架 minSdk 19 且未开启 core library desugaring，低版本设备会 NoClassDefFoundError。
     */
    private interface EventAction<T> {
        void onEvent(T value);
    }

    /**
     * 查表条目：事件 LiveData 与其处理器的强类型配对。
     * 泛型参数保证处理器入参与事件载荷类型一致，注册时无需任何强转。
     */
    private static final class EventBinding<T> {
        private final SingleLiveEvent<T> event;
        private final EventAction<? super T> handler;

        private EventBinding(SingleLiveEvent<T> event, EventAction<? super T> handler) {
            this.event = event;
            this.handler = handler;
        }

        private void register(LifecycleOwner owner) {
            event.observe(owner, handler::onEvent);
        }
    }

    private void onRequestPermissionEvent(Map<String, Object> params) {
        IPermission iPermission = (IPermission) params.get(Constant.PERMISSION);
        String[] permissions = (String[]) params.get(Constant.PERMISSION_NAME);
        requestPermission(iPermission, permissions);
    }

    private void onStartActivityEvent(Map<String, Object> params) {
        startActivity((Class<?>) params.get(ParameterField.CLASS), (Bundle) params.get(ParameterField.BUNDLE));
    }

    private void onStartActivityForResultEvent(Map<String, Object> params) {
        Integer requestCodeObj = (Integer) params.get(ParameterField.REQUEST_CODE);
        startActivityForResult(
                (Class<?>) params.get(ParameterField.CLASS),
                requestCodeObj != null ? requestCodeObj : 0,
                (Bundle) params.get(ParameterField.BUNDLE));
    }

    private void onStartContainerActivityEvent(Map<String, Object> params) {
        startContainerActivity(
                (String) params.get(ParameterField.CANONICAL_NAME),
                (Bundle) params.get(ParameterField.BUNDLE));
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
        if (!viewDelegate.isRegisterEventBus() || eventBusTarget != null) {
            return;
        }
        try {
            Object target = resolveEventBusTarget();
            if (target != null && !EventBus.getDefault().isRegistered(target)) {
                EventBusUtil.register(target);
            }
            eventBusTarget = target;
        } catch (Exception e) {
            Object target = resolveEventBusTarget();
            eventBusTarget = target != null && EventBus.getDefault().isRegistered(target) ? target : null;
            KLog.e(e.getMessage());
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
            swipeLayout.setLeftEdgeSize(SizeUtils.dp2px(16));
            swipeLayout.setLeftSwipeColor(ColorUtils.getColor(R.color.colorPrimary));
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

            if (viewDelegate.isRegisterEventBus() && eventBusTarget != null) {
                if (EventBus.getDefault().isRegistered(eventBusTarget)) {
                    EventBusUtil.unregister(eventBusTarget);
                }
                eventBusTarget = null;
            }
            if (viewDelegate.hasWifi()) {
                permissionHelper.stopWifiListening();
            }
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
    }
}
