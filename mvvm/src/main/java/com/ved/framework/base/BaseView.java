package com.ved.framework.base;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.blankj.swipepanel.SwipePanel;
import com.mumu.dialog.MMLoading;
import com.orhanobut.dialog.manager.DialogManager;
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

import java.util.Map;

import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;

class BaseView<V extends ViewDataBinding, VM extends BaseViewModel> {
    protected V binding;
    protected VM viewModel;
    private MMLoading mmLoading;
    private final IBaseView<V,VM> iBaseView;

    protected BaseView(IBaseView<V, VM> iBaseView) {
        this.iBaseView = iBaseView;
    }

    protected final void initialize(Bundle savedInstanceState) {
        initViewDataBinding(savedInstanceState);
        registerUIChangeLiveDataCallBack();
    }

    protected void initViewDataBinding(Bundle savedInstanceState) {
        binding = iBaseView.getBinding(savedInstanceState);
        viewModel = iBaseView.ensureViewModelCreated();
        if (binding != null && viewModel != null) {
            binding.setVariable(Constant.variableId, viewModel);
            binding.setLifecycleOwner(iBaseView.getLifecycleOwner());
            iBaseView.getViewLifecycle().addObserver(viewModel);
            viewModel.injectLifecycleProvider(iBaseView.getLifecycleProvider());
        } else {
            KLog.e("Critical: Binding or ViewModel is null");
        }
    }

    private void registerUIChangeLiveDataCallBack() {
        if (null == viewModel){
            viewModel = iBaseView.ensureViewModelCreated();
        }
        if (viewModel != null) {
            viewModel.getUC().getShowDialogEvent().observe(iBaseView.getLifecycleOwner(), (Observer<String>) this::showDialog);
            viewModel.getUC().getDismissDialogEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Void>) v -> dismissDialog());

            viewModel.getUC().getRequestPermissionEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
                IPermission iPermission = (IPermission) params.get(Constant.PERMISSION);
                String[] permissions = (String[]) params.get(Constant.PERMISSION_NAME);
                requestPermission(iPermission, permissions);
            });

            viewModel.getUC().getRequestCallPhoneEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
                String phoneNumber = (String) params.get(Constant.PHONE_NUMBER);
                callPhone(phoneNumber);
            });

            viewModel.getUC().getStartActivityEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
                Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
                Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
                startActivity(clz, bundle);
            });

            viewModel.getUC().getStartActivityForResultEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
                Class<?> clz = (Class<?>) params.get(ParameterField.CLASS);
                Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
                int requestCode = (int) params.get(ParameterField.REQUEST_CODE);
                startActivityForResult(clz, requestCode, bundle);
            });

            viewModel.getUC().getStartContainerActivityEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Map<String, Object>>) params -> {
                String canonicalName = (String) params.get(ParameterField.CANONICAL_NAME);
                Bundle bundle = (Bundle) params.get(ParameterField.BUNDLE);
                startContainerActivity(canonicalName, bundle);
            });

            viewModel.getUC().getFinishEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Void>) v -> {
                SoftKeyboardUtil.hideSoftKeyboard(iBaseView.FragmentActivity());
                iBaseView.FragmentActivity().finish();
            });

            viewModel.getUC().getOnBackPressedEvent().observe(iBaseView.getLifecycleOwner(), (Observer<Void>) v -> iBaseView.FragmentActivity().onBackPressed());

            viewModel.getUC().getOnLoadEvent().observe(iBaseView.getLifecycleOwner(), o -> {
                if (iBaseView.getLifecycleOwner() instanceof FragmentActivity){
                    iBaseView.initView();
                    initSwipeBack();
                }
                if (iBaseView.isRegisterEventBus()) {
                    EventBusUtil.register(this);
                }
                //页面事件监听的方法，一般用于ViewModel层转到View层的事件注册
                iBaseView.initViewObservable();
                //注册RxBus
                viewModel.registerRxBus();
            });

            viewModel.getUC().getReceiverEvent().observe(iBaseView.getLifecycleOwner(),o -> sendReceiver());

            if (viewModel.getUC().getOnResumeEvent() != null && iBaseView.getLifecycleOwner() instanceof Fragment) {
                viewModel.getUC().getOnResumeEvent().observe(iBaseView.getLifecycleOwner(),
                        o -> iBaseView.initView());
            }
        }
    }

    protected void showDialog() {
        showDialog("加载中...");
    }

    protected void showDialog(String title){
        if (iBaseView.customDialog()) {
            iBaseView.showCustomDialog();
        } else {
            if (iBaseView.mvvmDialog()) {
                showDialog(title,true);
            } else {
                DialogManager.Companion.getInstance().showProgressDialog(iBaseView.FragmentActivity(),title);
            }
        }
    }

    protected void showDialog(String title,boolean isShowMessage) {
        if (mmLoading == null) {
            MMLoading.Builder builder = new MMLoading.Builder(iBaseView.FragmentActivity())
                    .setMessage(title)
                    .setShowMessage(isShowMessage)
                    .setCancelable(false)
                    .setCancelOutside(false);
            mmLoading = builder.create();
        }else {
            mmLoading.dismiss();
            MMLoading.Builder builder = new MMLoading.Builder(iBaseView.FragmentActivity())
                    .setMessage(title)
                    .setShowMessage(isShowMessage)
                    .setCancelable(false)
                    .setCancelOutside(false);
            mmLoading = builder.create();
        }
        mmLoading.getWindow().setDimAmount(0f);
        mmLoading.show();
    }

    protected void dismissDialog() {
        if (iBaseView.customDialog()) {
            iBaseView.dismissCustomDialog();
        } else {
            if (iBaseView.mvvmDialog()) {
                if (mmLoading != null && mmLoading.isShowing()) {
                    mmLoading.dismiss();
                }
            }else {
                DialogManager.Companion.getInstance().dismiss();
            }
        }
    }

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    protected void startActivity(Class<?> clz) {
        iBaseView.getViewContext().startActivity(new Intent(iBaseView.getViewContext(), clz));
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    protected void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent(iBaseView.getViewContext(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        iBaseView.getViewContext().startActivity(intent);
    }

    protected void startActivityForResult(Class<?> clz, int requestCode, Bundle bundle) {
        Intent intent = new Intent(iBaseView.getViewContext(), clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (iBaseView.getLifecycleOwner() instanceof FragmentActivity) {
            ((FragmentActivity) iBaseView.getLifecycleOwner()).startActivityForResult(intent, requestCode);
        } else if (iBaseView.getLifecycleOwner() instanceof Fragment) {
            ((Fragment) iBaseView.getLifecycleOwner()).startActivityForResult(intent, requestCode);
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
        Intent intent = new Intent(iBaseView.getViewContext(), ContainerActivity.class);
        intent.putExtra(ParameterField.FRAGMENT, canonicalName);
        if (bundle != null) {
            intent.putExtra(ParameterField.BUNDLE, bundle);
        }
        iBaseView.getViewContext().startActivity(intent);
    }

    public void callPhone(String phoneNumber) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermission(new IPermission() {
                @Override
                public void onGranted() {
                    PhoneUtils.callPhone(phoneNumber);
                }

                @Override
                public void onDenied(boolean denied) {
                    iBaseView.requestCallPhone(denied);
                }
            }, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE);
        } else {
            requestPermission(new IPermission() {
                @Override
                public void onGranted() {
                    PhoneUtils.callPhone(phoneNumber);
                }

                @Override
                public void onDenied(boolean denied) {
                    iBaseView.requestCallPhone(denied);
                }
            }, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE);
        }
    }

    public void requestPermission(IPermission iPermission, String... permissions) {
        if (iBaseView.getLifecycleOwner() instanceof FragmentActivity) {
            RxPermission.requestPermission((FragmentActivity) iBaseView.getLifecycleOwner(), iPermission, permissions);
        } else if (iBaseView.getLifecycleOwner() instanceof Fragment) {
            RxPermission.requestPermission((Fragment) iBaseView.getLifecycleOwner(), iPermission, permissions);
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
        iBaseView.getViewContext().sendBroadcast(intent);
    }

    public void sendReceiver(){
        sendReceiver(null);
    }

    private void initSwipeBack() {
        if (iBaseView.isSwipeBack()) {
            final SwipePanel swipeLayout = new SwipePanel(iBaseView.FragmentActivity());
            swipeLayout.setLeftDrawable(R.drawable.ca);
            swipeLayout.setLeftEdgeSize(DpiUtils.dip2px(iBaseView.FragmentActivity(),16));
            swipeLayout.setLeftSwipeColor(iBaseView.FragmentActivity().getResources().getColor(R.color.colorPrimary));
            swipeLayout.wrapView(iBaseView.FragmentActivity().findViewById(android.R.id.content));
            swipeLayout.setOnFullSwipeListener(direction -> {
                swipeLayout.close(direction);
                iBaseView.FragmentActivity().finish();
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
            if (iBaseView.isRegisterEventBus()) {
                EventBusUtil.unregister(this);
            }
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
    }
}
