package com.ved.framework.base;

import android.os.Bundle;

import com.ved.framework.bus.event.SingleLiveEvent;
import com.ved.framework.bus.event.eventbus.MessageEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * UI 事件载体：集中持有 ViewModel -> View 的全部一次性事件（对话框 / 跳转 / 权限 / 生命周期 等）。
 *
 * <p><b>封装边界：</b>本类只作为“事件容器”对外公开，而获取它的入口
 * {@link ICommand} / {@code UICommand} / {@code BaseView} 全部是包级的：
 * {@code EventKey} 注册表、懒加载 {@code get(EventKey)} 这些实现细节都藏在包内，
 * 外部只能拿到各个 {@code getXxxEvent()} 返回的 {@link SingleLiveEvent}。
 *
 * <p><b>为什么 {@code BaseViewModel} 不再提供 {@code getUC()}：</b>Kotlin 没有包级可见性，
 * 它把 Java 的包级类型看作 {@code public/&#47;*package*&#47;}，并禁止其出现在任何非 private 的
 * Kotlin 声明里（internal / protected 同样报错，只有 private 例外）。所以
 * {@code BaseViewModel} 只交出 {@code provideCommand(): Any}，同包的 {@code BaseView}
 * 向下转型为 {@link ICommand} 后再取 {@code liveData}，避免把框架内部管线抖到 Kotlin 公开签名上。
 *
 * <p><b>泛型实参为什么是 {@code Object}：</b>本类从不直接使用继承自
 * {@link SingleLiveEvent} 的 {@code setValue/getValue/observe}，真正的事件都放在下面的
 * {@code events} 注册表里。以前写裸类型 {@code extends SingleLiveEvent} 会把所有继承下来的
 * 成员全部抹除成 raw，因此报 rawtypes + unchecked；写成 {@code SingleLiveEvent<Object>}
 * 后消除告警，且擦除后签名与原来完全一致（二进制兼容）。
 *
 * <p>新增事件时：在下面的 {@code EventKey} 里加枚举值，并补一个对应的 public getter；
 * 若 {@code BaseView} 需要观察，再在那里加一行 {@code uc.getXxxEvent().observe(...)}。
 */
class UIChangeLiveData extends SingleLiveEvent<Object> implements IUIChangeLiveData{

    /**
     * 事件类型注册表（注册表模式）：将原来十多个重复字段收敛为统一的注册表，
     * 由泛型方法 {@code get(EventKey)} 统一懒加载，消除重复样板代码。
     * <p>声明为 private：枚举值只在本类内部用作 Map 的 key，不对外暴露。
     */
    private enum EventKey {
        SHOW_DIALOG, DISMISS_DIALOG,
        START_ACTIVITY, START_ACTIVITY_FOR_RESULT, START_CONTAINER_ACTIVITY,
        REQUEST_PERMISSION, REQUEST_CALL_PHONE, REQUEST_WIFI_RSSI,
        SEND_RECEIVER, FINISH, ON_BACK_PRESSED, ON_LOAD, ON_RESUME,VIEW_EVENT
    }

    private final Map<EventKey, SingleLiveEvent<?>> events = new EnumMap<>(EventKey.class);

    /**
     * 获取通用事件 LiveData
     */
    @Override
    public SingleLiveEvent<MessageEvent<?>> getViewEvent() {
        return get(EventKey.VIEW_EVENT);
    }

    /**
     * 发送通用事件
     */
    @Override
    public void setViewEvent(MessageEvent<?> event) {
        getViewEvent().setValue(event);
    }

    @Override
    public SingleLiveEvent<Map<String, Object>> getRequestCallPhoneEvent() {
        return get(EventKey.REQUEST_CALL_PHONE);
    }

    @Override
    public SingleLiveEvent<Map<String, Object>> getRequestWifiRssiEvent() {
        return get(EventKey.REQUEST_WIFI_RSSI);
    }

    @Override
    public SingleLiveEvent<Map<String, Object>> getRequestPermissionEvent() {
        return get(EventKey.REQUEST_PERMISSION);
    }

    @Override
    public SingleLiveEvent<Map<String, Object>> getStartActivityForResultEvent() {
        return get(EventKey.START_ACTIVITY_FOR_RESULT);
    }

    @Override
    public SingleLiveEvent<String> getShowDialogEvent() {
        return get(EventKey.SHOW_DIALOG);
    }

    @Override
    public SingleLiveEvent<Void> getDismissDialogEvent() {
        return get(EventKey.DISMISS_DIALOG);
    }

    @Override
    public SingleLiveEvent<Map<String, Object>> getStartActivityEvent() {
        return get(EventKey.START_ACTIVITY);
    }

    @Override
    public SingleLiveEvent<Bundle> getReceiverEvent() {
        return get(EventKey.SEND_RECEIVER);
    }

    @Override
    public SingleLiveEvent<Map<String, Object>> getStartContainerActivityEvent() {
        return get(EventKey.START_CONTAINER_ACTIVITY);
    }

    @Override
    public SingleLiveEvent<Void> getFinishEvent() {
        return get(EventKey.FINISH);
    }

    @Override
    public SingleLiveEvent<Void> getOnBackPressedEvent() {
        return get(EventKey.ON_BACK_PRESSED);
    }

    @Override
    public SingleLiveEvent<Void> getOnLoadEvent() {
        return get(EventKey.ON_LOAD);
    }

    @Override
    public SingleLiveEvent<Void> getOnResumeEvent() {
        return get(EventKey.ON_RESUME);
    }

    /**
     * 泛型注册表取值：按事件类型懒加载对应的 {@link SingleLiveEvent}。
     * 加 synchronized 保证多线程并发首次访问时的安全性。
     */
    @SuppressWarnings("unchecked")
    private synchronized <T> SingleLiveEvent<T> get(EventKey key) {
        SingleLiveEvent<?> event = events.get(key);
        if (event == null) {
            event = new SingleLiveEvent<>();
            events.put(key, event);
        }
        return (SingleLiveEvent<T>) event;
    }
}
