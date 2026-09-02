package com.ved.framework.utils.recyclerview;


import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by ved on 2017/6/16.
 */
public class LineManagers {
    protected LineManagers() {
    }

    public interface LineManagerFactory {
        RecyclerView.ItemDecoration create(RecyclerView recyclerView);
    }


    public static LineManagerFactory both() {
        return create(DividerLine.LineDrawMode.BOTH);
    }

    public static LineManagerFactory horizontal() {
        return create(DividerLine.LineDrawMode.HORIZONTAL);
    }

    public static LineManagerFactory vertical() {
        return create(DividerLine.LineDrawMode.VERTICAL);
    }

    /**
     * 简单工厂模式：根据分隔线绘制模式创建 LineManagerFactory
     */
    private static LineManagerFactory create(final DividerLine.LineDrawMode mode) {
        return new LineManagerFactory() {
            @Override
            public RecyclerView.ItemDecoration create(RecyclerView recyclerView) {
                return new DividerLine(recyclerView.getContext(), mode);
            }
        };
    }
}
