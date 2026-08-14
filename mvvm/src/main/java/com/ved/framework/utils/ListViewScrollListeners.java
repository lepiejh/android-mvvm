package com.ved.framework.utils;

import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * ListView 滚动监听器注册表：
 * {@link ListView#setOnScrollListener} 同一时刻只能保存一个监听器，
 * 当 XML 同时绑定 {@code onScrollChangeCommand} 与 {@code onLoadMoreCommand} 时后者会覆盖前者。
 * 本类通过弱引用注册表聚合多个监听器并统一分发（组合模式），保证互不覆盖。
 */
public final class ListViewScrollListeners {

    private static final Map<ListView, List<AbsListView.OnScrollListener>> REGISTRY =
            Collections.synchronizedMap(new WeakHashMap<ListView, List<AbsListView.OnScrollListener>>());

    private ListViewScrollListeners() {
        // 工具类，禁止实例化
    }

    /**
     * 注册一个滚动监听器；同一 ListView 多次注册不会被覆盖
     */
    public static void addListener(ListView listView, AbsListView.OnScrollListener listener) {
        if (listView == null || listener == null) {
            return;
        }
        synchronized (REGISTRY) {
            List<AbsListView.OnScrollListener> listeners = REGISTRY.get(listView);
            if (listeners == null) {
                listeners = new ArrayList<>();
                REGISTRY.put(listView, listeners);
                listView.setOnScrollListener(new CompositeScrollListener(listeners));
            }
            listeners.add(listener);
        }
    }

    /**
     * 组合监听器：将注册到同一 ListView 的所有监听器统一分发
     */
    private static final class CompositeScrollListener implements AbsListView.OnScrollListener {
        private final List<AbsListView.OnScrollListener> listeners;

        CompositeScrollListener(List<AbsListView.OnScrollListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            for (AbsListView.OnScrollListener listener : listeners) {
                listener.onScrollStateChanged(view, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            for (AbsListView.OnScrollListener listener : listeners) {
                listener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }
        }
    }
}
