package com.ved.framework.base;

import android.os.Bundle;

import com.ved.framework.bus.event.SingleLiveEvent;
import com.ved.framework.bus.event.eventbus.MessageEvent;

import java.util.Map;

/**
 * UI 事件载体接口（面向接口编程）：
 * 暴露 ViewModel -> View 的全部一次性事件，具体实现 {@link UIChangeLiveData} 保持包可见，
 * 避免框架内部实现细节泄漏到 AAR 公开 API。
 *
 * <p>由于 Kotlin 无包级可见性，{@code BaseViewModel.getUC()} 的返回值必须是 public 类型，
 * 因此这里用接口作为契约，实现类收敛到包内。
 */
public interface IUIChangeLiveData {

    // ==================== 对话框 ====================
    SingleLiveEvent<String> getShowDialogEvent();
    SingleLiveEvent<Void> getDismissDialogEvent();

    // ==================== 跳转 ====================
    SingleLiveEvent<Map<String, Object>> getStartActivityEvent();
    SingleLiveEvent<Map<String, Object>> getStartActivityForResultEvent();
    SingleLiveEvent<Map<String, Object>> getStartContainerActivityEvent();

    // ==================== 权限 / 系统能力 ====================
    SingleLiveEvent<Map<String, Object>> getRequestPermissionEvent();
    SingleLiveEvent<Map<String, Object>> getRequestCallPhoneEvent();
    SingleLiveEvent<Map<String, Object>> getRequestWifiRssiEvent();

    // ==================== 广播 / 生命周期 ====================
    SingleLiveEvent<Bundle> getReceiverEvent();
    SingleLiveEvent<Void> getFinishEvent();
    SingleLiveEvent<Void> getOnBackPressedEvent();
    SingleLiveEvent<Void> getOnLoadEvent();
    SingleLiveEvent<Void> getOnResumeEvent();

    // ==================== 通用事件 ====================
    SingleLiveEvent<MessageEvent<?>> getViewEvent();
    void setViewEvent(MessageEvent<?> event);
}