package com.ved.framework.base;

import com.ved.framework.bus.RxBus;
import com.ved.framework.bus.event.eventbus.MessageEvent;

import io.reactivex.rxjava3.core.Observable;

/**
 * 粘性事件订阅策略
 */
public class StickyEventStrategy extends BaseEventStrategy {

    @Override
    protected Observable<MessageEvent> createObservable() {
        return RxBus.getDefault()
                .toObservableSticky(MessageEvent.class);
    }
}
