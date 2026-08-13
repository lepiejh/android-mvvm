# android-mvvm

基于 DataBinding + RxJava 的 Android MVVM 快速开发框架。

## 目录

- [ViewGroup 动态添加 View](#viewgroup-动态添加-view)

## ViewGroup 动态添加 View

对应类：`com.ved.framework.binding.viewadapter.viewgroup.ViewAdapter`

### 功能

- 将 `ObservableList<IBindingItemViewModel>` 绑定到任意 ViewGroup；
- 列表发生增、删、改、移动时，自动全量刷新 ViewGroup 中的子 View，无需手动调用；
- 所有 item 使用同一布局（`app:itemView` 指定的 layout），布局内 variable 固定命名为 `viewModel`；
- item 对应的 ViewModel 需实现 `IBindingItemViewModel`，可在 `injecDataBinding()` 中拿到 item 的 binding 做后续操作。

### 使用方法

#### 1. 准备 item 布局 `res/layout/item_binding_view.xml`（variable 名字必须为 viewModel）

```xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <data>
        <variable name="viewModel" type="com.xxx.ItemViewModel"/>
    </data>
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@{viewModel.title}"/>
</layout>
```

#### 2. ItemViewModel 实现 `IBindingItemViewModel` 接口

```java
public class ItemViewModel implements IBindingItemViewModel<ItemBindingViewBinding> {
    public final ObservableField<String> title = new ObservableField<>("标题");

    @Override
    public void injecDataBinding(ItemBindingViewBinding binding) {
        // 可选：拿到 item 的 binding 后，可在此做额外初始化
    }
}
```

#### 3. 页面 ViewModel 中持有列表数据

```java
public class DemoViewModel {
    public final ObservableList<IBindingItemViewModel> views = new ObservableArrayList<>();

    public DemoViewModel() {
        views.add(new ItemViewModel());
    }
}
```

#### 4. 在页面布局中绑定（根节点必须是 `<layout>`，并引入 app 命名空间）

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    app:itemView="@{@layout/item_binding_view}"
    app:observableList="@{viewModel.views}"/>
```

### 注意事项

- `app:itemView` 与 `app:observableList` 两个属性必须同时绑定，缺一不可；
- 对 `views` 列表调用 `add`/`remove`/`clear`/`set` 等操作后，界面会自动刷新；
- 列表为空或为 null 时，ViewGroup 中的子 View 会被自动清空；
- 重复绑定（如页面重建）时会自动移除上一次的监听，不会重复注册。
