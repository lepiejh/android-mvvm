package com.ved.framework.widget;

import android.content.Context;
import android.text.InputFilter;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

public class FilterEditText extends AppCompatEditText {
    public FilterEditText(@NonNull Context context) {
        super(context);
        init();
    }

    public FilterEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FilterEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 保留 XML 中已配置的过滤器(如 maxLength), 追加自定义字符过滤器, 避免覆盖
        InputFilter[] original = getFilters();
        InputFilter[] filters = new InputFilter[original.length + 1];
        System.arraycopy(original, 0, filters, 0, original.length);
        filters[original.length] = new SpecialCharacterFilter();
        setFilters(filters);
    }
}
