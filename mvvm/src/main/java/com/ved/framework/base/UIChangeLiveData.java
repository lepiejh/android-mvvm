package com.ved.framework.base;

import android.os.Bundle;

import com.ved.framework.bus.event.SingleLiveEvent;
import com.ved.framework.bus.event.eventbus.MessageEvent;

import java.util.EnumMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

/**
 * UI 事件载体：集中持有 ViewModel -> View 的全部一次性事件（对话框 / 跳转 / 权限 / 生命周期 等）。
 *
 * <p><b>本类保持包级可见（不加 public）</b>，与同包的 {@link ICommand} / {@code UICommand} /
 * {@code BaseView} 封装粒度一致：{@code EventKey} 注册表、懒加载 {@code get(EventKey)}
 * 这些实现细节全部藏在包内，也不会被库模块混淆规则的 {@code -keep public class} 锁死类名。
 *
 * <p><b>为什么它能一直待在包级：</b>{@code BaseViewModel} 已经删掉了
 * {@code fun getUC(): UIChangeLiveData}。Kotlin 没有包级可见性，它把 Java 的包级类型看作
 * {@code public/&#47;*package*&#47;}，并禁止其出现在任何非 private 的 Kotlin 声明里
 * （internal / protected 同样报错，只有 private 例外），所以那个 public 函数一存在就必须把
 * 本类抬成 public。现在改成：{@code BaseViewModel} 只交出 {@code provideCommand(): Any}，
 * 同包的 {@code BaseView} 向下转型为 {@link ICommand} 后取 {@code liveData}，
 * 于是本类再也不需要出现在任何 Kotlin 的公开签名里。
 *
 * <p>新增事件时：在下面的 {@code EventKey} 里加枚举值，并补一个对应的 public getter；
 * 若 {@code BaseView} 需要观察，再在那里加一行 {@code uc.getXxxEvent().observe(...)}。
 */
public final class UIChangeLiveData extends SingleLiveEvent {

    /**
     * 事件类型注册表（注册表模式）：将 13 个重复字段收敛为统一的注册表，
     * 由泛型方法 {@link #get(EventKey)} 统一懒加载，消除重复样板代码。
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
    public SingleLiveEvent<MessageEvent<?>> getViewEvent() {
        return get(EventKey.VIEW_EVENT);
    }

    /**
     * 发送通用事件
     */
    public void setViewEvent(MessageEvent<?> event) {
        getViewEvent().setValue(event);
    }

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
