package com.ved.framework.binding.viewadapter.listview;

import android.widget.AbsListView;
import android.widget.ListView;

import com.ved.framework.command.BindingCommand;
import com.ved.framework.utils.ListViewLoadMoreScrollListener;
import com.ved.framework.utils.ListViewScrollListeners;
import com.ved.framework.entity.ListViewScrollDataWrapper;

import androidx.databinding.BindingAdapter;

/**
 * Created by ved on 2017/6/18.
 */
public final class ViewAdapter {

    @SuppressWarnings("unchecked")
    @BindingAdapter(value = {"onScrollChangeCommand", "onScrollStateChangedCommand"}, requireAll = false)
    public static void onScrollChangeCommand(final ListView listView,
                                             final BindingCommand<ListViewScrollDataWrapper> onScrollChangeCommand,
                                             final BindingCommand<Integer> onScrollStateChangedCommand) {
        ListViewScrollListeners.addListener(listView, new AbsListView.OnScrollListener() {
            private int scrollState;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                this.scrollState = scrollState;
                if (onScrollStateChangedCommand != null) {
                    onScrollStateChangedCommand.execute(scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (onScrollChangeCommand != null) {
                    onScrollChangeCommand.execute(new ListViewScrollDataWrapper(scrollState, firstVisibleItem, visibleItemCount, totalItemCount));
                }
            }
        });
    }


    @BindingAdapter(value = {"onItemClickCommand"}, requireAll = false)
    public static void onItemClickCommand(final ListView listView, final BindingCommand<Integer> onItemClickCommand) {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (onItemClickCommand != null) {
                onItemClickCommand.execute(position);
            }
        });
    }


    @BindingAdapter({"onLoadMoreCommand"})
    public static void onLoadMoreCommand(final ListView listView, final BindingCommand<Integer> onLoadMoreCommand) {
        ListViewScrollListeners.addListener(listView, new ListViewLoadMoreScrollListener(onLoadMoreCommand));
    }
}
