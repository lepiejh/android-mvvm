# android-mvvm

基于 DataBinding + RxJava 的 Android MVVM 快速开发框架。

## 目录

- [ViewGroup 动态添加 View](#viewgroup-动态添加-view)
- [ARequest 网络请求与取消](#arequest-网络请求与取消)
- [Messenger 消息总线](#messenger-消息总线)
- [Drawables 动态背景](#drawables-动态背景)
- [CorpseUtils 工具方法](#corpseutils-工具方法)

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

## Drawables 动态背景

对应类：`com.ved.framework.binding.viewadapter.drawable.Drawables`

### 功能

- 在布局 XML 中直接用 `app:` 属性配置 View 的背景 Drawable，免去手写 `res/drawable` 下的 shape / selector 资源文件；
- 支持形状、纯色、描边/虚线、圆角、渐变、尺寸、margin、圆环等全部 shape 属性；
- 支持多状态背景（按下/选中/勾选/禁用/聚焦等），自动生成 selector；
- 也可通过 `Drawables.create(...)` 在 Java 代码中创建 Drawable。

### 用法一：布局中配置普通背景

所有属性均为可选（`requireAll = false`），用到哪个写哪个。基础属性：

| 属性 | 类型 | 说明 |
|---|---|---|
| `app:drawable_shapeMode` | integer | 形状：`RECTANGLE=0`、`OVAL=1`、`LINE=2`、`RING=3` |
| `app:drawable_solidColor` | color | 填充色 |
| `app:drawable_strokeColor` | color | 描边颜色 |
| `app:drawable_strokeWidth` | float(dp) | 描边宽度 |
| `app:drawable_strokeDash` / `drawable_strokeDashGap` | float(dp) | 虚线：线段长 / 间隔 |
| `app:drawable_radius` | float(dp) | 统一圆角 |
| `app:drawable_radiusLT` / `radiusLB` / `radiusRT` / `radiusRB` | float(dp) | 四个角分别圆角 |
| `app:drawable_startColor` / `centerColor` / `endColor` | color | 渐变三色 |
| `app:drawable_orientation` | integer | 渐变方向：`TOP_BOTTOM=0`、`TR_BL=1`、`RIGHT_LEFT=2`、`BR_TL=3`、`BOTTOM_TOP=4`、`BL_TR=5`、`LEFT_RIGHT=6`、`TL_BR=7` |
| `app:drawable_gradientType` | integer | 渐变类型：`LINEAR=0`、`RADIAL=1`、`SWEEP=2` |
| `app:drawable_radialCenterX` / `radialCenterY` / `radialRadius` | float | 径向渐变中心（0~1）与半径 |
| `app:drawable_width` / `height` | float(dp) | Drawable 尺寸 |
| `app:drawable_marginLeft` / `marginTop` / `marginRight` / `marginBottom` | float(dp) | Drawable 内边距偏移（包一层 InsetDrawable） |
| `app:drawable_ringThickness` / `ringThicknessRatio` / `ringInnerRadius` / `ringInnerRadiusRatio` | float | 圆环（RING 模式）参数 |
| `app:drawable` | reference | 直接指定 drawable 资源 |

示例——圆角描边按钮（布局中先 import `Drawables` 以便使用常量，也可直接写数字）：

```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="圆角按钮"
    android:padding="16dp"
    app:drawable_shapeMode="@{Drawables.ShapeMode.RECTANGLE}"
    app:drawable_solidColor="@{0xFF4CAF50}"
    app:drawable_strokeColor="@{0xFF333333}"
    app:drawable_strokeWidth="@{2f}"
    app:drawable_radius="@{8f}"/>
```

示例——线性渐变背景：

```xml
<Button
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="渐变按钮"
    app:drawable_startColor="@{0xFF4CAF50}"
    app:drawable_endColor="@{0xFF2196F3}"
    app:drawable_orientation="@{Drawables.Orientation.LEFT_RIGHT}"
    app:drawable_radius="@{20f}"/>
```

### 用法二：多状态背景（selector）

状态前缀：`drawable_`（默认）、`drawable_pressed_`（按下）、`drawable_selected_`（选中）、`drawable_checked_`（勾选）、`drawable_checkable_`（可勾选）、`drawable_enabled_`（可用）、`drawable_focused_`（聚焦）。

每个状态都支持用法一中的全部 shape 属性（`drawable_pressed_solidColor`、`drawable_pressed_radius` ...）。

示例——按下变色的按钮：

```xml
<Button
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="状态按钮"
    app:drawable_solidColor="@{0xFF4CAF50}"
    app:drawable_radius="@{8f}"
    app:drawable_pressed_solidColor="@{0xFF388E3C}"
    app:drawable_pressed_radius="@{8f}"/>
```

说明：

- 未指定默认状态时，会自动保留 View 原有背景作为默认状态；
- 状态优先级与 Android selector 一致（pressed / selected / checked / focused / enabled / 默认）。

### 用法三：Java 代码创建 Drawable

```java
GradientDrawable drawable = Drawables.create(
        Drawables.ShapeMode.RECTANGLE,          // shapeMode
        0xFF4CAF50,                             // solidColor
        0xFF333333, 2, 0, 0,                    // strokeColor, strokeWidth, strokeDash, strokeDashGap
        8, 0, 0, 0, 0,                          // radius, radiusLT, radiusLB, radiusRT, radiusRB
        0, 0, 0,                                // startColor, centerColor, endColor
        Drawables.Orientation.TOP_BOTTOM,
        Drawables.GradientType.LINEAR,          // orientation, gradientType
        0.5f, 0.5f, 0,                          // radialCenterX, radialCenterY, radialRadius
        0, 0,                                   // width, height
        0, 0, 0, 0,                             // marginLeft, marginTop, marginRight, marginBottom
        0, 0, 0, 0);                            // ringThickness, ringThicknessRatio, ringInnerRadius, ringInnerRadiusRatio
view.setBackground(drawable);
```

### 注意事项

- `LINE` 模式只能画水平线（线高由 strokeWidth 决定），且引用虚线的 View 需添加 `android:layerType="software"`，否则虚线无法显示；
- 设置 `margin*` 属性时，InsetDrawable 会导致 View 自身 padding 失效，框架会自动恢复原 padding；
- `RING` 模式通过反射设置圆环参数，对性能有要求的场景请谨慎使用。

## CorpseUtils 工具方法

对应类：`com.ved.framework.utils.CorpseUtils`（Kotlin `object`，Java 中通过 `CorpseUtils.INSTANCE` 访问）

### expandTouchView 扩大点击区域

#### 功能与原理

扩展方法：在不改变 View 视觉尺寸的前提下，扩大 View 的触摸响应区域（默认向外扩大 `10dp`，单位 dp）。

原理：获取目标 View 的父 View，通过 `getHitRect()` 取目标 View 在父容器中的矩形，四周各扩大 `expandSize` 后，给**父 View** 设置 `TouchDelegate`——只要触摸点落在扩大后的矩形内（且在父 View 区域内），事件就会转发给目标 View。

#### 使用方法

Kotlin：

```kotlin
import com.ved.framework.utils.expandTouchView

// 把 targetView 的点击区域向外扩大 20dp
targetView.expandTouchView(20f)

// 使用默认 10dp
targetView.expandTouchView()
```

Java：

```java
CorpseUtils.INSTANCE.expandTouchView(view, 20f);
```

#### expandTouchView 不生效的常见原因

| 原因 | 说明 |
|---|---|
| **目标 View 没有父 View** | 代码中 `parent as? View` 为 null 时会静默跳过。对根布局、尚未 add 进容器的 View 调用无效（方法注释也明确要求 targetView 必须有父 View） |
| **同一父容器中多个子 View 都调用了** | `parentView.touchDelegate` 是单一赋值，后调用的会**覆盖**先调用的，导致前一个失效。同一父 View 下只建议给一个子 View 扩大 |
| **调用时机过早** | 若在 View 未完成布局、未 attach 到 Window 时调用，`getHitRect()` 拿到的矩形可能是 0 或错误值。虽然内部已用 `post` 延迟一帧，但极端场景（异步数据加载后 View 尚未 measure）仍会失效。建议在布局完成后再调用 |
| **目标 View 尺寸为 0 或 GONE** | 矩形区域无效，扩大无意义 |
| **触摸点落在可点击的兄弟 View 上** | 事件被兄弟 View 消费，不会走到父 View 的 TouchDelegate，属正常行为，不是失效 |

> 提示：`expandSize` 单位为 dp，内部自动 `dip2px` 转换；传入 0 或负数等于没扩大。

### noOpDelegate 空实现代理

#### 功能

利用 Java 动态代理（`Proxy.newProxyInstance`）生成一个**接口的空实现实例**：所有方法调用都被忽略（引用类型方法返回 null）。

适用场景：

- 需要一个回调/监听器但暂时什么都不想处理（如空 `OnClickListener`）；
- 作为接口类型参数的占位符/兜底，避免写一长串空实现类；
- 替代「接口 + 空实现类」的样板代码。

#### 使用方法

Kotlin（`reified` 泛型自动推断）：

```kotlin
import com.ved.framework.utils.noOpDelegate

// 接口必须实现时，用空实现占位
view.setOnClickListener(noOpDelegate<View.OnClickListener>())
```

Java（需显式指定泛型）：

```java
view.setOnClickListener(CorpseUtils.INSTANCE.<View.OnClickListener>noOpDelegate());
```

#### 注意事项

- **T 必须是接口**：动态代理只能代理接口，传入普通类会抛 `IllegalArgumentException`；
- **避免在基本类型返回值的方法上使用**：代理方法默认返回 `null`，若接口方法返回 `int`/`boolean` 等基本类型且被调用方解包，会抛 `NullPointerException`，仅适用于返回 `void` 或引用类型（可 null）的接口。
