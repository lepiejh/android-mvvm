package com.ved.framework.bus;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * 全局订阅容器：
 * 对外暴露静态方法（兼容旧调用），内部通过私有静态持有者实现 {@link ISubscription} 接口。
 * <p>
 * 注意：Java 禁止同一类中声明与接口方法同签名的 static/instance 方法，
 * 因此将接口实现下沉到私有内部类 {@link Holder}，外部类仅做静态转发。
 */
public class RxSubscriptions {
    private static final ISubscription INSTANCE = new Holder();

    /** 接口实现持有者：真正承载全局订阅的生命周期管理 */
    private static final class Holder implements ISubscription {
        private final CompositeDisposable mSubscriptions = new CompositeDisposable();

        @Override
        public boolean isDisposed() {
            return mSubscriptions.isDisposed();
        }

        @Override
        public void add(Disposable s) {
            if (s != null) {
                mSubscriptions.add(s);
            }
        }

        @Override
        public void remove(Disposable s) {
            if (s != null) {
                mSubscriptions.remove(s);
            }
        }

        @Override
        public void clear() {
            mSubscriptions.clear();
        }

        @Override
        public void dispose() {
            mSubscriptions.dispose();
        }
    }

    private RxSubscriptions() {
    }

    public static ISubscription getInstance() {
        return INSTANCE;
    }

    public static boolean isDisposed() {
        return INSTANCE.isDisposed();
    }

    public static void add(Disposable s) {
        INSTANCE.add(s);
    }

    public static void remove(Disposable s) {
        INSTANCE.remove(s);
    }

    public static void clear() {
        INSTANCE.clear();
    }

    public static void dispose() {
        INSTANCE.dispose();
    }
}
