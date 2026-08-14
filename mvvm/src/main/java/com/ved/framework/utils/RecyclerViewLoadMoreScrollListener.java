package com.ved.framework.utils;

import com.ved.framework.binding.command.BindingCommand;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView 加载更多滚动监听器
 * 当滚动到列表底部时触发 onLoadMoreCommand
 */
public class RecyclerViewLoadMoreScrollListener extends RecyclerView.OnScrollListener {

    private final LoadMoreTrigger loadMoreTrigger;

    public RecyclerViewLoadMoreScrollListener(BindingCommand<Integer> onLoadMoreCommand) {
        this.loadMoreTrigger = new LoadMoreTrigger(onLoadMoreCommand);
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (!(manager instanceof LinearLayoutManager)) return;
        LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int pastVisiblesItems = layoutManager.findFirstVisibleItemPosition();
        if (isReachBottom(visibleItemCount, totalItemCount, pastVisiblesItems)) {
            int adapterCount = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : totalItemCount;
            loadMoreTrigger.trigger(adapterCount);
        }
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        // no-op
    }

    private boolean isReachBottom(int visibleItemCount, int totalItemCount, int pastVisiblesItems) {
        return (visibleItemCount + pastVisiblesItems) >= totalItemCount;
    }
}
