package com.ved.framework.base;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.ved.framework.bus.event.eventbus.MessageEvent;

/**
 * Created by ved on 2017/6/15.
 */
public interface IBaseViewModel extends LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    default void onAny(LifecycleOwner owner, Lifecycle.Event event) {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void onCreate();

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    default void onDestroy() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    default void onStart() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    default void onStop() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void onResume();

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    default void onPause() {
    }

    /**
     * 注册RxBus
     */
    void registerRxBus();

    /**
     * 移除RxBus
     */
    void removeRxBus();

    /**
     * 接收到分发事件
     *
     * @param event 事件
     */
    default void receiveEvent(MessageEvent<?> event) {
    }

    /**
     * 接受到分发的粘性事件
     *
     * @param event 粘性事件
     */
    default void receiveStickyEvent(MessageEvent<?> event) {
    }

    /**
     * 执行RxBus事件
     * @param event  事件
     */
    default void onEvent(MessageEvent<?> event) {
    }

    /**
     * 执行RxBus事件 发生异常
     * @param throwable  异常原因
     */
    void onError(Throwable throwable);

    /**
     * 是否开启粘性的RxBus事件
     * @return   true:开启  false:关闭
     */
    boolean onEventSticky();

    /**
     * 是否启动RxBus
     * @return  true:开启  false:关闭
     */
    boolean openEventSubscription();
}
