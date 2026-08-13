# android-mvvm

基于 DataBinding + RxJava 的 Android MVVM 快速开发框架。

## 目录

- [ViewGroup 动态添加 View](#viewgroup-动态添加-view)
- [ARequest 网络请求与取消](#arequest-网络请求与取消)
- [Messenger 消息总线](#messenger-消息总线)

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

## ARequest 网络请求与取消

对应类：`com.ved.framework.net.ARequest`

### 功能

- 链式配置网络请求参数：Service、Method、Loading、占位视图、回调等；
- `build()` 返回生命周期事件流 `PublishSubject<Object>`，向事件流发送事件即可取消网络请求；
- 传入 `ViewModel` 后，请求自动与 ViewModel 生命周期同步（`RxUtils.bindToLifecycle`），页面销毁时自动取消。

### 使用方法

#### 1. 发起网络请求

```java
PublishSubject<Object> lifecycleDisposable = new ARequest<ApiService, UserBean>() {
    @Override
    public void exceptionHandling(@Nullable BaseViewModel viewModel, @Nullable String error, int code) {
        // 统一异常处理
    }
}
        .withViewModel(viewModel)                       // 传入 ViewModel（可选，传入后请求与生命周期绑定）
        .withService(ApiService.class)                  // Retrofit Service
        .withMethod(apiService -> apiService.getUser()) // 实际请求，返回 Observable
        .withLoading(true)                              // 是否显示 loading 对话框
        .withViewState(view)                            // 占位视图
        .withSeatSuccess(seatSuccess)                   // 成功占位视图回调
        .withSeatError(seatError)                       // 错误占位视图回调
        .withResponse(new IResponse<UserBean>() {
            @Override
            public void onSuccess(@Nullable UserBean userBean) {
                // 请求成功
            }

            @Override
            public void onError(@Nullable String msg, boolean socketClosed) {
                // 请求失败
            }
        })
        .build(); // 返回生命周期事件流，用于取消请求
```

#### 2. 取消网络请求

通过 `build()` 返回的生命周期事件流取消：

```kotlin
val lifecycleDisposable = PublishSubject.create<Unit>()

// ... 发起请求后，需要取消时：
lifecycleDisposable.onNext(Unit) // 取消网络请求
```

> 注意：取消网络请求后，需要延时 1 秒再重新请求才能生效，可能是由于请求取消后资源未完全释放或 OkHttp 连接池未及时清理。

## Messenger 消息总线

对应类：`com.ved.framework.bus.Messenger`

### 功能

- 基于 token 的消息总线，用于 ViewModel 与 ViewModel 之间解耦通信；
- 采用弱引用保存订阅者，失效订阅会被自动清理，避免内存泄漏；
- 支持空消息与携带数据的消息两种模式。

### 使用方法

#### 1. 定义一个静态 String 类型的 token

```java
public static final String TOKEN_LOGINVIEWMODEL_REFRESH = "token_loginviewmodel_refresh";
```

#### 2. 在 ViewModel 中注册消息监听

注册一个空消息监听：

```java
// 参数1：接受人（上下文）
// 参数2：定义的 token
// 参数3：执行的回调监听
Messenger.getDefault().register(this, LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH, new BindingAction() {
    @Override
    public void call() {

    }
});
```

注册一个带数据回调的消息监听：

```java
// 参数1：接受人（上下文）
// 参数2：定义的 token
// 参数3：实体的泛型约束
// 参数4：执行的回调监听
Messenger.getDefault().register(this, LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH, String.class, new BindingConsumer<String>() {
    @Override
    public void call(String s) {

    }
});
```

#### 3. 在需要回调的地方使用 token 发送消息

```java
// 发送一个空消息（参数为定义的 token）
Messenger.getDefault().sendNoMsg(LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH);

// 发送一个带数据的消息（参数1：回调的实体；参数2：定义的 token）
Messenger.getDefault().send("refresh", LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH);
```

#### 4. 注销监听（防止内存泄漏）

```java
// 页面/ViewModel 销毁时注销
Messenger.getDefault().unregister(this);
```

### 注意事项

- token 最好不要重名，否则可能出现逻辑上的 bug；为了更好的维护和清晰的逻辑，建议以 `aa_bb_cc` 的格式定义 token：`aa` 为 TOKEN，`bb` 为 ViewModel 的类名，`cc` 为动作名（功能名）；
- 为了避免大量使用 Messenger，建议**只在 ViewModel 与 ViewModel 之间使用**；View 与 ViewModel 之间采用 `ObservableField` 去监听 UI 上的逻辑，可在继承了 Base 的 Activity 或 Fragment 中重写 `initViewObservable()` 方法来初始化 UI 的监听。
