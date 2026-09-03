package com.ved.framework.base.helper;

import androidx.databinding.ViewDataBinding;

import com.orhanobut.dialog.dialog.DialogStrategyFactory;
import com.orhanobut.dialog.dialog.IDialogStrategy;
import com.ved.framework.base.BaseViewModel;
import com.ved.framework.base.IBaseView;

/**
 * 对话框助手（单一职责原则）：
 * 封装 MVVM 加载对话框策略的创建与显隐控制，
 * 使 BaseView 从对话框细节中解耦。
 */
public class DialogHelper<V extends ViewDataBinding, VM extends BaseViewModel> {

    private static final String DEFAULT_TITLE = "加载中...";

    private final IDialogStrategy dialogStrategy;

    public DialogHelper(IBaseView<V, VM> viewDelegate) {
        this.dialogStrategy = DialogStrategyFactory.createStrategy(viewDelegate);
    }

    /** 显示默认加载对话框 */
    public void show() {
        show(DEFAULT_TITLE);
    }

    /** 显示指定标题的加载对话框 */
    public void show(String title) {
        dialogStrategy.show(title);
    }

    /** 关闭加载对话框 */
    public void dismiss() {
        dialogStrategy.dismiss();
    }
}
