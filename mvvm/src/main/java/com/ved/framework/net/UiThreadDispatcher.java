package com.ved.framework.net;

import com.ved.framework.base.BaseViewModel;
import com.ved.framework.utils.CorpseUtils;

import androidx.annotation.Nullable;

/**
 * UI 线程调度器：
 * 统一封装「有 ViewModel 时借助其协程任务在 UI 线程回调，无 ViewModel 时回退到主线程 Handler」的双分支逻辑，
 * 消除 {@link ARequest} 与 {@link RetrofitClient} 拦截器中的重复代码。
 */
final class UiThreadDispatcher {

    private UiThreadDispatcher() {
        // 工具类，禁止实例化
    }

    /**
     * 保证 action 一定在 UI 线程执行
     *
     * @param viewModel 可能为 null；为 null 时回退到主线程 Handler 调度
     * @param action    需要在 UI 线程执行的任务
     */
    static void runOnUiThread(@Nullable BaseViewModel viewModel, Runnable action) {
        if (viewModel != null) {
            viewModel.fetchWithCancel(CorpseUtils.INSTANCE.generateSecureRandomString(12),
                    (coroutineScope, continuation) -> null,
                    continuation -> {
                        action.run();
                        return null;
                    }, throwable -> null, throwable -> null);
        } else {
            CorpseUtils.INSTANCE.handlerThread(() -> {
                action.run();
                return null;
            });
        }
    }
}
