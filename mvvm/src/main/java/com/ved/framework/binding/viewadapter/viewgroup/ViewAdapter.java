package com.ved.framework.binding.viewadapter.viewgroup;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableList;
import androidx.databinding.ViewDataBinding;

import com.ved.framework.R;
import com.ved.framework.base.IBindingItemViewModel;

import me.tatarka.bindingcollectionadapter2.ItemBinding;

/**
 * ViewGroup 绑定适配器：根据 {@link ObservableList} 动态向 ViewGroup 中添加子 View。
 * <p>
 * 主要功能：
 * <ul>
 *     <li>将 {@code ObservableList<IBindingItemViewModel>} 绑定到任意 ViewGroup；</li>
 *     <li>列表发生增、删、改、移动时，自动全量刷新 ViewGroup 中的子 View，无需手动调用；</li>
 *     <li>所有 item 使用同一布局（app:itemView 指定的 layout），布局内 variable 固定命名为 viewModel；</li>
 *     <li>item 对应的 ViewModel 需实现 {@link IBindingItemViewModel}，可在
 *     {@link IBindingItemViewModel#injecDataBinding(ViewDataBinding)} 中拿到 item 的 binding 做后续操作。</li>
 * </ul>
 * <p>
 * 详细使用方法见项目 README.md「ViewGroup 动态添加 View」章节。
 *
 * Created by ved on 2017/6/18.
 */
public final class ViewAdapter {

    @BindingAdapter(value = {"itemView", "observableList"})
    public static void addViews(ViewGroup viewGroup, final ItemBinding itemBinding,
                                final ObservableList<IBindingItemViewModel> viewModelList) {
        // 1. 移除上一次绑定的监听回调，避免重复注册造成的内存泄漏与重复刷新
        Object old = viewGroup.getTag(R.id.viewgroup_binding_callback);
        if (old instanceof ViewListChangedCallback) {
            ((ViewListChangedCallback) old).unregister();
        }
        viewGroup.setTag(R.id.viewgroup_binding_callback, null);

        if (itemBinding == null || viewModelList == null) {
            // 绑定属性为空时，清空所有子 View
            clearViews(viewGroup);
            return;
        }

        // 2. 注册监听，列表发生任何变化都自动重建子 View
        ViewListChangedCallback callback = new ViewListChangedCallback(viewGroup, itemBinding, viewModelList);
        viewGroup.setTag(R.id.viewgroup_binding_callback, callback);
        viewModelList.addOnListChangedCallback(callback);

        // 3. 首次 / 重新渲染
        callback.rebuild();
    }

    /** 解绑并清空 ViewGroup 中所有子 View */
    private static void clearViews(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            ViewDataBinding binding = DataBindingUtil.getBinding(viewGroup.getChildAt(i));
            if (binding != null) {
                binding.unbind();
            }
        }
        viewGroup.removeAllViews();
    }

    /** 根据列表数据全量重建子 View */
    private static void renderViews(ViewGroup viewGroup, ItemBinding itemBinding,
                                    ObservableList<IBindingItemViewModel> viewModelList) {
        clearViews(viewGroup);
        if (viewModelList == null || viewModelList.isEmpty()) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        for (IBindingItemViewModel viewModel : viewModelList) {
            ViewDataBinding binding = DataBindingUtil.inflate(inflater, itemBinding.layoutRes(), viewGroup, true);
            binding.setVariable(itemBinding.variableId(), viewModel);
            viewModel.injecDataBinding(binding);
        }
    }

    /** ObservableList 变化监听，任何增删改操作都触发全量重建 */
    private static final class ViewListChangedCallback
            extends ObservableList.OnListChangedCallback<ObservableList<IBindingItemViewModel>> {

        private final ViewGroup viewGroup;
        private final ItemBinding itemBinding;
        private final ObservableList<IBindingItemViewModel> viewModelList;

        ViewListChangedCallback(ViewGroup viewGroup, ItemBinding itemBinding,
                                ObservableList<IBindingItemViewModel> viewModelList) {
            this.viewGroup = viewGroup;
            this.itemBinding = itemBinding;
            this.viewModelList = viewModelList;
        }

        void rebuild() {
            if (viewGroup == null || itemBinding == null) {
                return;
            }
            renderViews(viewGroup, itemBinding, viewModelList);
        }

        void unregister() {
            if (viewModelList != null) {
                viewModelList.removeOnListChangedCallback(this);
            }
        }

        @Override
        public void onChanged(ObservableList<IBindingItemViewModel> sender) {
            rebuild();
        }

        @Override
        public void onItemRangeChanged(ObservableList<IBindingItemViewModel> sender, int positionStart, int itemCount) {
            rebuild();
        }

        @Override
        public void onItemRangeInserted(ObservableList<IBindingItemViewModel> sender, int positionStart, int itemCount) {
            rebuild();
        }

        @Override
        public void onItemRangeRemoved(ObservableList<IBindingItemViewModel> sender, int positionStart, int itemCount) {
            rebuild();
        }

        @Override
        public void onItemRangeMoved(ObservableList<IBindingItemViewModel> sender, int fromPosition, int toPosition, int itemCount) {
            rebuild();
        }
    }
}
