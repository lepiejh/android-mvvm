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

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

/**
 * A SingleLiveEvent used for Snackbar messages. Like a {@link SingleLiveEvent} but also prevents
 * null messages and uses a custom observer.
 * <p>
 * Note that only one observer is going to be notified of changes.
 */
public class SnackbarMessage extends SingleLiveEvent<Integer> {

    /**
     * 观察 Snackbar 消息。
     *
     * <p>为什么带 {@code @SuppressWarnings("overloads")}：本方法与父类
     * {@code LiveData.observe(LifecycleOwner, Observer<? super Integer>)} 构成重载，
     * 而 {@link SnackbarObserver} 和 {@code Observer<Integer>} 都是单抽象方法的函数式接口，
     * javac 因此警告「调用方直接传裸 lambda 时可能产生歧义」。
     *
     * <p>不采用重命名（如 {@code observeSnackbar}）的原因：本类已随 AAR 发布，
     * {@code observe(owner, SnackbarObserver)} 是外部使用方正在调用的公开签名，
     * 重命名属于破坏性变更。调用方如果写 lambda，只需显式声明参数类型即可消歧：
     * <pre>{@code msg.observe(owner, (SnackbarMessage.SnackbarObserver) resId -> {...});}</pre>
     */
    @SuppressWarnings("overloads")
    public void observe(LifecycleOwner owner, final SnackbarObserver observer) {
        super.observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer t) {
                if (t == null) {
                    return;
                }
                observer.onNewMessage(t);
            }
        });
    }

    public interface SnackbarObserver {
        /**
         * Called when there is a new message to be shown.
         * @param snackbarMessageResourceId The new message, non-null.
         */
        void onNewMessage(@StringRes int snackbarMessageResourceId);
    }

}
