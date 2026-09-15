package com.ved.framework.bus.event.eventbus;

import org.greenrobot.eventbus.EventBus;

public class EventBusUtil {

    public static void register(Object subscriber) {
        EventBus.getDefault().register(subscriber);
    }

    public static void unregister(Object subscriber) {
        EventBus.getDefault().unregister(subscriber);
    }

    public static <T> void sendEvent(MessageEvent<T> event) {
        EventBus.getDefault().post(event);
    }

    public static <T> void sendStickyEvent(MessageEvent<T> event) {
        EventBus.getDefault().postSticky(event);
    }
}