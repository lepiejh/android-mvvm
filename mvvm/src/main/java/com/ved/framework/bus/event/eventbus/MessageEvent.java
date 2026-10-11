package com.ved.framework.bus.event.eventbus;

public class MessageEvent<T> {
    /**
     * {@code MessageEvent.class} 的静态类型只能是 {@code Class<MessageEvent>}（泛型实参 MessageEvent 是裸类型），
     * Java 的类字面量无法直接表达 {@code Class<MessageEvent<?>>}。
     * 这里集中做一次收窄的类型转换，供 RxBus 订阅时拿到带完整类型实参的 Observable，避免调用方到处裸用泛型。
     */
    @SuppressWarnings("unchecked")
    public static final Class<MessageEvent<?>> TYPE =
            (Class<MessageEvent<?>>) (Class<?>) MessageEvent.class;

    private int code;
    private T data;

    public MessageEvent(int code) {
        this.code = code;
    }

    public MessageEvent(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
