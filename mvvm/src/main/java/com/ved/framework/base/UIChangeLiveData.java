package com.ved.framework.base;

import android.os.Bundle;

import com.ved.framework.bus.event.SingleLiveEvent;

import java.util.EnumMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

public final class UIChangeLiveData extends SingleLiveEvent {

    /**
     * 事件类型注册表（注册表模式）：将 13 个重复字段收敛为统一的注册表，
     * 由泛型方法 {@link #get(EventKey)} 统一懒加载，消除重复样板代码。
     */
    private enum EventKey {
        SHOW_DIALOG, DISMISS_DIALOG,
        START_ACTIVITY, START_ACTIVITY_FOR_RESULT, START_CONTAINER_ACTIVITY,
        REQUEST_PERMISSION, REQUEST_CALL_PHONE, REQUEST_WIFI_RSSI,
        SEND_RECEIVER, FINISH, ON_BACK_PRESSED, ON_LOAD, ON_RESUME
    }

    private final Map<EventKey, SingleLiveEvent<?>> events = new EnumMap<>(EventKey.class);

    public SingleLiveEvent<Map<String, Object>> getRequestCallPhoneEvent() {
        return get(EventKey.REQUEST_CALL_PHONE);
    }

    public SingleLiveEvent<Map<String, Object>> getRequestWifiRssiEvent() {
        return get(EventKey.REQUEST_WIFI_RSSI);
    }

    public SingleLiveEvent<Map<String, Object>> getRequestPermissionEvent() {
        return get(EventKey.REQUEST_PERMISSION);
    }

    public SingleLiveEvent<Map<String, Object>> getStartActivityForResultEvent() {
        return get(EventKey.START_ACTIVITY_FOR_RESULT);
    }

    public SingleLiveEvent<String> getShowDialogEvent() {
        return get(EventKey.SHOW_DIALOG);
    }

    public SingleLiveEvent<Void> getDismissDialogEvent() {
        return get(EventKey.DISMISS_DIALOG);
    }

    public SingleLiveEvent<Map<String, Object>> getStartActivityEvent() {
        return get(EventKey.START_ACTIVITY);
    }

    public SingleLiveEvent<Bundle> getReceiverEvent() {
        return get(EventKey.SEND_RECEIVER);
    }

    public SingleLiveEvent<Map<String, Object>> getStartContainerActivityEvent() {
        return get(EventKey.START_CONTAINER_ACTIVITY);
    }

    public SingleLiveEvent<Void> getFinishEvent() {
        return get(EventKey.FINISH);
    }

    public SingleLiveEvent<Void> getOnBackPressedEvent() {
        return get(EventKey.ON_BACK_PRESSED);
    }

    public SingleLiveEvent<Void> getOnLoadEvent() {
        return get(EventKey.ON_LOAD);
    }

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

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer observer) {
        super.observe(owner, observer);
    }
}
