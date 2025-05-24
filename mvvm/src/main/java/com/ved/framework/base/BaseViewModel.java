package com.ved.framework.base;

import android.app.Application;
import android.os.Bundle;

import com.trello.rxlifecycle4.LifecycleProvider;
import com.ved.framework.bus.RxBus;
import com.ved.framework.bus.RxSubscriptions;
import com.ved.framework.bus.event.eventbus.EventBusUtil;
import com.ved.framework.bus.event.eventbus.MessageEvent;
import com.ved.framework.permission.IPermission;

import java.lang.ref.WeakReference;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by ved on 2017/6/15.
 */
public class BaseViewModel<M extends BaseModel> extends AndroidViewModel implements IBaseViewModel, Consumer<Disposable> {
    protected M model;
    //弱引用持有
    private WeakReference<LifecycleProvider> lifecycle;
    //管理RxJava，主要针对RxJava异步操作造成的内存泄漏
    private CompositeDisposable mCompositeDisposable;
    private Disposable mEventSubscription;
    private final UICommand command;

    public BaseViewModel(Application application) {
        this(application,null);
    }

    public BaseViewModel(Application application,M model) {
        super(application);
        this.model = model;
        mCompositeDisposable = new CompositeDisposable();
        this.command = new UICommand();
    }

    protected void addSubscribe(Disposable disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(disposable);
    }

    @Override
    public boolean openEventSubscription() {
        return false;
    }

    @Override
    public boolean onEventSticky() {
        return false;
    }

    /**
     * 注入RxLifecycle生命周期
     */
    public void injectLifecycleProvider(LifecycleProvider lifecycle) {
        this.lifecycle = new WeakReference<>(lifecycle);
    }

    public LifecycleProvider getLifecycleProvider() {
        return lifecycle.get();
    }

    public UIChangeLiveData getUC() {
        return command.getLiveData();
    }

    public void showDialog() {
        command.showDialog();
    }

    public void showDialog(String title) {
        command.showDialog(title);
    }

    public void dismissDialog() {
        command.dismissDialog();
    }

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    public void startActivity(Class<?> clz) {
        command.startActivity(clz);
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        command.startActivity(clz, bundle);
    }

    public void sendReceiver(){
        command.sendReceiver();
    }

    public void sendReceiver(Bundle bundle) {
        command.sendReceiver(bundle);
    }

    public void startActivityForResult(Class<?> clz,int requestCode) {
        command.startActivityForResult(clz, requestCode);
    }

    public void startActivityForResult(Class<?> clz, Bundle bundle,int requestCode) {
        command.startActivityForResult(clz, bundle, requestCode);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     */
    public void startContainerActivity(String canonicalName) {
        command.startContainerActivity(canonicalName);
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     * @param bundle        跳转所携带的信息
     */
    public void startContainerActivity(String canonicalName, Bundle bundle) {
        command.startContainerActivity(canonicalName, bundle);
    }

    public void requestPermissions(IPermission iPermission, String... permissions){
        command.requestPermissions(iPermission, permissions);
    }

    public void callPhone(String phoneNumber){
        command.callPhone(phoneNumber);
    }

    /**
     * 关闭界面
     */
    public void finish() {
        command.finish();
    }

    /**
     * 返回上一层
     */
    public void onBackPressed() {
        command.onBackPressed();
    }

    @Override
    public void onAny(LifecycleOwner owner, Lifecycle.Event event) {
    }

    @Override
    public void onCreate() {
        getUC().getOnLoadEvent().call();
    }

    public void sendRxEvent(MessageEvent<?> messageEvent){
        if (onEventSticky()) {
            RxBus.getDefault().postSticky(messageEvent);
        } else {
            RxBus.getDefault().post(messageEvent);
        }
    }

    public void sendEvent(MessageEvent<?> messageEvent){
        if (onEventSticky()) {
            EventBusUtil.sendStickyEvent(messageEvent);
        } else {
            EventBusUtil.sendEvent(messageEvent);
        }
    }

    private void onStartEventSubscription(){
        if (onEventSticky()) {
            mEventSubscription = RxBus.getDefault().toObservableSticky(MessageEvent.class).subscribe(this::onEvent, this::onError);
        } else {
            mEventSubscription = RxBus.getDefault().toObservable(MessageEvent.class).subscribe(this::onEvent, this::onError);
        }
        RxSubscriptions.add(mEventSubscription);
    }

    @Override
    public void onEvent(MessageEvent<?> event){
    }

    public void onError(Throwable throwable){
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onResume() {
        getUC().getOnResumeEvent().call();
    }

    @Override
    public void onPause() {
    }

    @Override
    public void registerRxBus() {
        if (openEventSubscription()){
            onStartEventSubscription();
        }
    }

    @Override
    public void removeRxBus() {
        if (openEventSubscription()){
            if (mEventSubscription != null){
                RxSubscriptions.remove(mEventSubscription);
            }
        }
    }

    @Override
    public void receiveEvent(MessageEvent<?> event) {
    }

    @Override
    public void receiveStickyEvent(MessageEvent<?> event) {
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            if (model != null) {
                model.onCleared();
            }
            if (mCompositeDisposable != null) {
                mCompositeDisposable.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void accept(Disposable disposable) throws Exception {
        addSubscribe(disposable);
    }
}
