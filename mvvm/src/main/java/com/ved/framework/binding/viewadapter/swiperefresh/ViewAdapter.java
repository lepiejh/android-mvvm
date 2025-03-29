package com.ved.framework.binding.viewadapter.swiperefresh;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.ved.framework.binding.command.BindingCommand;

import androidx.databinding.BindingAdapter;


/**
 * Created by ved on 2017/6/18.
 */
public class ViewAdapter {
    //下拉刷新命令
    @BindingAdapter({"onRefreshCommand"})
    public static void onRefreshCommand(SmartRefreshLayout swipeRefreshLayout, final BindingCommand onRefreshCommand) {
        swipeRefreshLayout.setOnRefreshListener(refreshLayout -> {
            if (onRefreshCommand != null) {
                onRefreshCommand.execute();
            }
        });
    }

    //加载更多命令
    @BindingAdapter({"onLoadMoreCommand"})
    public static void onLoadMoreCommand(SmartRefreshLayout swipeRefreshLayout, final BindingCommand onLoadMoreCommand) {

        swipeRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
            if (onLoadMoreCommand != null) {
                onLoadMoreCommand.execute();
            }
        });
    }

    @BindingAdapter({"onEnableRefresh"})
    public static void onEnableRefreshCommand(SmartRefreshLayout swipeRefreshLayout,boolean isEnableRefresh) {
        swipeRefreshLayout.setEnableRefresh(isEnableRefresh);
    }

    @BindingAdapter({"onEnableLoadMore"})
    public static void onEnableLoadMoreCommand(SmartRefreshLayout swipeRefreshLayout,boolean isEnableLoadMore) {
        swipeRefreshLayout.setEnableLoadMore(isEnableLoadMore);
    }
}
