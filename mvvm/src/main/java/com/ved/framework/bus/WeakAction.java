package com.ved.framework.bus;

import com.ved.framework.command.BindingAction;
import com.ved.framework.command.BindingConsumer;

import java.lang.ref.WeakReference;


/**
 * About : kelin的WeakBindingAction
 */
public class WeakAction<T> {
    private BindingAction action;
    private BindingConsumer<T> consumer;
    private boolean isLive;
    private Object target;
    // 只弱引用订阅方（target）本身，取出来也只当 Object 用（getTarget / isLive），
    // 所以实参写 Object；以前是裸类型 WeakReference，会报 rawtypes。
    private WeakReference<Object> reference;

    public WeakAction(Object target, BindingAction action) {
        reference = new WeakReference<>(target);
        this.action = action;

    }

    public WeakAction(Object target, BindingConsumer<T> consumer) {
        reference = new WeakReference<>(target);
        this.consumer = consumer;
    }

    public void execute() {
        if (action != null && isLive()) {
            action.call();
        }
    }

    public void execute(T parameter) {
        if (consumer != null
                && isLive()) {
            consumer.call(parameter);
        }
    }

    public void markForDeletion() {
        reference.clear();
        reference = null;
        action = null;
        consumer = null;
    }

    public BindingAction getBindingAction() {
        return action;
    }

    /**
     * 返回构造时传入的消费者。
     * <p>从裸类型 {@code BindingConsumer} 收紧为 {@code BindingConsumer<T>}：擦除后仍是
     * {@code BindingConsumer}，二进制兼容；对以裸类型使用 {@code WeakAction} 的调用方
     * （如 {@code Messenger}）而言返回值依旧是裸类型，源码也兼容。
     */
    public BindingConsumer<T> getBindingConsumer() {
        return consumer;
    }

    public boolean isLive() {
        if (reference == null) {
            return false;
        }
        if (reference.get() == null) {
            return false;
        }
        return true;
    }


    public Object getTarget() {
        if (reference != null) {
            return reference.get();
        }
        return null;
    }
}
