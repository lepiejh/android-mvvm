package com.ved.framework.base;

import com.ved.framework.bus.RxBus;
import com.ved.framework.bus.event.eventbus.MessageEvent;

import io.reactivex.rxjava3.core.Observable;

/**
 * 普通（非粘性）事件订阅策略
 */
public class DefaultEventStrategy extends BaseEventStrategy {

    @Override
    protected Observable<MessageEvent> createObservable() {
        return RxBus.getDefault()
                .toObservable(MessageEvent.class);
    }
}
