package com.ved.framework.binding.utils;

import com.ved.framework.binding.command.BindingCommand;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * 加载更多防抖触发器（组合模式）
 * <p>
 * 通过 {@link PublishSubject} + throttleFirst 防止连续滚动时重复触发加载更多，
 * 供 RecyclerView / ListView 等滚动监听器组合复用。
 */
public final class LoadMoreTrigger {

    private static final long THROTTLE_SECONDS = 1;

    private final PublishSubject<Integer> methodInvoke = PublishSubject.create();

    private final BindingCommand<Integer> onLoadMoreCommand;

    public LoadMoreTrigger(BindingCommand<Integer> onLoadMoreCommand) {
        this.onLoadMoreCommand = onLoadMoreCommand;
        methodInvoke.throttleFirst(THROTTLE_SECONDS, TimeUnit.SECONDS)
                .subscribe(integer -> {
                    if (onLoadMoreCommand != null) {
                        onLoadMoreCommand.execute(integer);
                    }
                });
    }

    /**
     * 触发加载更多（带防抖）
     *
     * @param count 触底时的 item 总数
     */
    public void trigger(int count) {
        if (onLoadMoreCommand != null) {
            methodInvoke.onNext(count);
        }
    }
}
