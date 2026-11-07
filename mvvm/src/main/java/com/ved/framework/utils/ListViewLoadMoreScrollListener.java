package com.ved.framework.utils;

import android.widget.AbsListView;
import android.widget.ListView;

import com.ved.framework.binding.command.BindingCommand;

/**
 * ListView 加载更多滚动监听器
 * 当滚动到列表底部时触发 onLoadMoreCommand
 */
public class ListViewLoadMoreScrollListener implements AbsListView.OnScrollListener {

    private final LoadMoreTrigger loadMoreTrigger;

    public ListViewLoadMoreScrollListener(BindingCommand<Integer> onLoadMoreCommand) {
        this.loadMoreTrigger = new LoadMoreTrigger(onLoadMoreCommand);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // no-op
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (!(view instanceof ListView)) return;
        ListView listView = (ListView) view;
        if (isReachBottom(listView, firstVisibleItem, visibleItemCount, totalItemCount)) {
            loadMoreTrigger.trigger(totalItemCount);
        }
    }

    private boolean isReachBottom(ListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        return firstVisibleItem + visibleItemCount >= totalItemCount
                && totalItemCount != 0
                && totalItemCount != view.getHeaderViewsCount() + view.getFooterViewsCount();
    }
}
