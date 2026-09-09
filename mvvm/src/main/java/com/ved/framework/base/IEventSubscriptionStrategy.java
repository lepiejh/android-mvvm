package com.ved.framework.base;

interface IEventSubscriptionStrategy {
    void setupSubscription(BaseViewModel<?> viewModel);

    void remove();
}
