/*
 *  Copyright 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ved.framework.bus.event;

import com.ved.framework.utils.KLog;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 * <p>
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 * <p>
 * Note that only one observer is going to be notified of changes.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean mPending = new AtomicBoolean(false);
    private WeakReference<Observer<? super T>> mObserverRef;
    private WeakReference<LifecycleOwner> mOwnerRef;

    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull final Observer<? super T> observer) {

        if (hasActiveObservers()) {
            KLog.w("Multiple observers registered but only one will be notified of changes.");
            clearObserver();
        }

        // 保存引用
        mObserverRef = new WeakReference<>(observer);
        mOwnerRef = new WeakReference<>(owner);

        super.observe(owner, t -> {
            if (mPending.compareAndSet(true, false)) {
                Observer<? super T> realObserver = mObserverRef != null ? mObserverRef.get() : null;
                if (realObserver != null) {
                    try {
                        realObserver.onChanged(t);
                    } catch (Exception e) {
                        KLog.e(e.getMessage());
                    }
                } else {
                    // Observer 已被回收，清理资源并重置状态
                    KLog.w("Observer has been garbage collected, clearing pending state");
                    clearObserver();
                    // 注意：事件已经丢失
                }
            }
        });
    }

    @MainThread
    public void setValue(@Nullable T t) {
        // 检查 Observer 是否存活
        if (!isObserverAlive()) {
            KLog.w("Observer is not alive, event will be dropped: " + t);
            // 可以选择记录事件到日志，但不发送
            return;
        }
        mPending.set(true);
        super.setValue(t);
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    public void call() {
        setValue(null);
    }

    /**
     * 检查 Observer 是否还存活
     */
    private boolean isObserverAlive() {
        if (mObserverRef == null) {
            return false;
        }
        Observer<? super T> observer = mObserverRef.get();
        if (observer == null) {
            return false;
        }
        // 检查 LifecycleOwner 是否还存活
        if (mOwnerRef != null) {
            LifecycleOwner owner = mOwnerRef.get();
            if (owner == null) {
                return false;
            }
            // 检查生命周期状态
            return owner.getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED;
        }
        return true;
    }

    public void clearObserver() {
        if (mObserverRef != null) {
            Observer<? super T> oldObserver = mObserverRef.get();
            if (oldObserver != null) {
                super.removeObserver(oldObserver);
            }
            mObserverRef.clear();
            mObserverRef = null;
        }
        if (mOwnerRef != null) {
            mOwnerRef.clear();
            mOwnerRef = null;
        }
        mPending.set(false);
    }
}