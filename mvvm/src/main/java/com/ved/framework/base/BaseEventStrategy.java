package com.ved.framework.base;

import com.ved.framework.bus.event.eventbus.MessageEvent;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * EventBus 订阅策略模板方法基类：
 * 统一完成订阅的创建、注册与注销，子类只需实现 {@link #createObservable()} 决定普通/粘性事件流。
 * 订阅挂载到 ViewModel 级订阅容器，随 ViewModel 生命周期自动清理，避免内存泄漏。
 */
public abstract class BaseEventStrategy implements IEventSubscriptionStrategy {
    private Disposable eventSubscription;

    protected abstract Observable<MessageEvent> createObservable();

    @Override
    public void setupSubscription(BaseViewModel<?> viewModel) {
        eventSubscription = createObservable()
                .subscribe(viewModel::onEvent, viewModel::onError);
        viewModel.add(eventSubscription);
    }

    @Override
    public void remove() {
        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
            eventSubscription = null;
        }
    }
}
