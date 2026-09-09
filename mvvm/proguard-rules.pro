#===============================================================================
# mvvm 框架（com.ved.framework）库模块混淆规则
#
# 混淆目标（对外发布的 AAR）：
#   1. 所有类名 + public/protected 方法一律不混淆、不裁剪
#      —— 接入方按全限定名引用，AndroidManifest / DataBinding / Glide / Lifecycle
#         的生成代码也按名字硬引用这些类。
#   2. 类内部的 private / 包级方法放开，允许 R8 重命名与优化（隐藏实现、缩小体积）。
#   3. 字段一律保留原名 —— Gson、反射、DataBinding 都按字段名读写，
#      改字段名会直接导致 JSON 解析结果为 null。
#
# 配套文件：consumer-rules.pro（由 build.gradle 的 consumerProguardFiles 下发给接入方，
#           保证接入方 App 自己开启混淆时同样不会破坏本框架）。
#===============================================================================


#------------------------------------------------------------------------------
# 一、全局属性：反射 / 泛型 / 注解依赖的字节码属性必须保留
#------------------------------------------------------------------------------
# Signature                     : 泛型签名。SPUtils 的 ParameterizedTypeImpl、
#                                 ViewModelProxyImpl / BindingLayoutResolver 的
#                                 getGenericSuperclass() 强依赖它。
# InnerClasses,EnclosingMethod  : 匿名内部类。JsonPraise / GsonUtils 里的
#                                 new TypeToken<Map<String,Object>>(){} 靠
#                                 getGenericSuperclass() 取类型；
#                                 androidx.lifecycle.Lifecycling 还用
#                                 getCanonicalName()==null 判断匿名类。
# Exceptions                    : Retrofit 接口方法声明的受检异常。
# *Annotation*                  : EventBus @Subscribe、DataBinding @BindingAdapter、
#                                 Lifecycle @OnLifecycleEvent、BusUtils.@Bus。
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes Exceptions
-keepattributes *Annotation*,AnnotationDefault,MethodParameters
# 保留行号，配合 AGP 自动产出的 mapping.txt 可还原被重命名的 private 方法堆栈：
# mvvm/build/outputs/mapping/<variant>/mapping.txt
-keepattributes SourceFile,LineNumberTable


#------------------------------------------------------------------------------
# 二、框架自身：类名 + 公开 API 全量保留，private 成员放开混淆
#------------------------------------------------------------------------------
# -keep class 只锁定「类名 + 花括号里列出的成员」；
# 未列出的 private / 包级方法仍可被 R8 重命名或直接裁剪 —— 这正是本次的目标。
# 类名通配 ** 同时覆盖内部类、嵌套类与匿名类（如 BusUtils$Bus、SPUtils$SpDao）。
-keep class com.ved.framework.** {
    public protected <methods>;
}

# 接口的成员全部是 public（含 default 方法与常量），整体保留，避免被裁剪
-keep interface com.ved.framework.** { *; }

# 枚举：values()/valueOf() 由编译器与反射调用，枚举常量名即序列化值
-keepclassmembers enum com.ved.framework.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# 构造器全保留：Activity/Service/ViewModel/自定义 View/Parcelable 均按签名反射实例化，
# 且 <init> 本身无法重命名，这里的作用只是阻止 R8 把「暂时没人调用」的构造器裁掉。
-keepclassmembers class com.ved.framework.** {
    <init>(...);
}

# 字段一律不改名（Gson / 反射 / DataBinding 按字段名访问）
-keepclassmembers class com.ved.framework.** {
    <fields>;
}


#------------------------------------------------------------------------------
# 三、注解处理器 / 编译期生成产物：被「按名字」硬引用，必须整体保留
#------------------------------------------------------------------------------
# --- DataBinding ---
# 接入方 App 生成的 DataBinderMapperImpl 里是 new com.ved.framework.DataBinderMapperImpl()，
# 属于跨模块的编译期硬引用，一旦被改名，本框架的 aa.xml / ab.xml 绑定运行时直接失效。
-keep class com.ved.framework.DataBinderMapperImpl { *; }
-keep class com.ved.framework.BR { *; }
# 不要给 DataBindingInfo 写 -keep：它只是 AGP 为触发 DataBinding 注解处理器生成的空壳类
# （类体为空，仅带 @BindingBuildInfo），其源码目录 build/generated/source/dataBinding/trigger
# 不是 IDE 的源码根，且 AGP 打 AAR 时会把它从 classes.jar 中剔除，写死类名会报
# “无法解析类 com.ved.framework.DataBindingInfo”；运行时也没有任何代码引用它，无需保留。
-keep class com.ved.framework.databinding.** { *; }
-keep class androidx.databinding.** { *; }
-dontwarn androidx.databinding.**

# --- Glide ---
# Glide 运行时用 Class.forName("com.bumptech.glide.GeneratedAppGlideModuleImpl") 加载，
# 该类由本模块的 annotationProcessor 生成（打进本模块的 classes.jar），
# 改名后 Glide 会静默退化成默认配置，MyAppGlideModule 的设置全部丢失。
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.bumptech.glide.GeneratedRequestManagerFactory { *; }
-keep public class * implements com.bumptech.glide.module.AppGlideModule
-keep public class * implements com.bumptech.glide.module.LibraryGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# --- Lifecycle ---
# androidx.lifecycle.Lifecycling 用 Class.forName(观察者类名 + "_LifecycleAdapter") 加载，
# 本模块由 lifecycle-compiler 生成了 IBaseViewModel_LifecycleAdapter。
-keep class com.ved.framework.**_LifecycleAdapter { *; }
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent <methods>;
}
# ViewModelProvider.NewInstanceFactory 反射调用 ViewModel 的构造器
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}


#------------------------------------------------------------------------------
# 四、反射 / 字节码注入安全区：这些 private 成员不能改名
#------------------------------------------------------------------------------
# --- EventBus（greenrobot 3.2.0）---
# @Subscribe 方法由 EventBus 反射调用；ThreadMode 枚举名参与注解解析。
# 本框架的 onEventBusCome / onStickyEventBusCome 是 public（已被第二节覆盖），
# 这里同时保护接入方自己写的 private @Subscribe 方法。
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-dontwarn org.greenrobot.eventbus.**

# --- BusUtils（blankj 的 @Bus 机制）---
# bus 插件在编译期扫描 @Bus，把「类名 + 方法名」以字符串常量注入 registerBus(...) 调用；
# 运行时 BusUtils.getMethodByBusInfo() 用 getDeclaredMethod(funName) 反查。
# 字符串常量不会被 R8 改写，所以被 @Bus 标注的方法必须保留原名，否则必然 NoSuchMethodException。
-keepclassmembers class * {
    @com.ved.framework.utils.bland.code.BusUtils$Bus <methods>;
}
# registerBus 本身是 private 且仅由注入的字节码调用，R8 会判定为「无调用者」而裁掉；
# registerBus4Test 是包级方法，同样需要显式保留。
-keepclassmembers class com.ved.framework.utils.bland.code.BusUtils {
    void registerBus(...);
    static void registerBus4Test(...);
}

# --- DataBinding 适配器 ---
# @BindingAdapter 方法都是 public static（已被第二节覆盖），此处显式兜底，
# 同时保护接入方自定义的 BindingAdapter。
-keepclassmembers class * {
    @androidx.databinding.BindingAdapter <methods>;
    @androidx.databinding.BindingConversion <methods>;
    @androidx.databinding.BindingMethod <methods>;
}

# --- Java 序列化 ---
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# --- Parcelable ---
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# --- JNI ---
# native 方法名与 so 里注册的符号绑定，改名即 UnsatisfiedLinkError。
-keepclasseswithmembernames class * {
    native <methods>;
}
# MMKV 的 native 层会反向回调 Java 侧的 private static 方法
# （onMMKVCRCCheckFail / onMMKVFileLengthError 等），必须整体保留。
-keep class com.tencent.mmkv.** { *; }
-dontwarn com.tencent.mmkv.**

# --- 自定义 View / XML ---
# 自定义 View 由 LayoutInflater 按 (Context, AttributeSet[, int[, int]]) 反射构造
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}
# 布局里 android:onClick="xxx" 绑定的方法
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
# WebView 的 JS 桥
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# --- 资源 R 类 ---
# R 字段是 public static，且资源 id 在运行期按名字反查（getIdentifier），
# 字段名一旦被改写，getIdentifier("xxx") 全部返回 0。
-keepclassmembers class **.R$* {
    public static <fields>;
}

# --- Gson ---
# TypeToken 匿名子类依赖 Signature + InnerClasses + EnclosingMethod（见第一节）
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }
-dontwarn com.google.gson.**
-dontwarn sun.misc.**

# --- Retrofit / OkHttp ---
# R8 full mode 下 Retrofit 需要的 allowshrinking 变体规则
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okhttp3.internal.platform.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Kotlin ---
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**

# --- RxJava3 ---
# UnsafeAccess 通过字段名做 CAS，字段名不能改
-keepclassmembers class io.reactivex.rxjava3.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-dontwarn io.reactivex.**
-dontwarn java.util.concurrent.Flow*


#------------------------------------------------------------------------------
# 五、AndroidManifest 中声明的组件：清单里的类名就是运行时查找的 key
#------------------------------------------------------------------------------
# 本模块清单声明了 ContainerActivity、DefaultErrorActivity、
# UtilsTransActivity、UtilsTransActivity4MainProcess、UtilsFileProvider、
# MessengerUtils$ServerService。
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
# MessengerUtils$ServerService 是嵌套 Service，显式兜底
-keep class com.ved.framework.utils.bland.code.MessengerUtils$* { *; }


#------------------------------------------------------------------------------
# 六、第三方库
# 说明：库模块 R8 的输入只有本模块的 classes.jar，依赖库的类不在其中，
#       因此下面的 -keep 对本模块构建基本是空操作；它们真正的生效位置是
#       consumer-rules.pro（下发给接入方 App 的 R8）。此处保留是为了
#       让本文件单独拷进 App 工程时依然完整可用。
#------------------------------------------------------------------------------
# 注意：以下包名已按本框架实际依赖校正
#   rxlifecycle2  -> rxlifecycle4   （config.gradle: com.trello.rxlifecycle4:4.0.0）
#   rxpermissions2-> rxpermissions3 （代码实际 import com.tbruyelle.rxpermissions3）
#   bindingcollectionadapter -> bindingcollectionadapter2
-keep class com.trello.rxlifecycle4.** { *; }
-keep interface com.trello.rxlifecycle4.** { *; }
-dontwarn com.trello.rxlifecycle4.**

-keep class com.tbruyelle.rxpermissions3.** { *; }
-keep interface com.tbruyelle.rxpermissions3.** { *; }
-dontwarn com.tbruyelle.rxpermissions3.**

-keep class me.tatarka.bindingcollectionadapter2.** { *; }
-dontwarn me.tatarka.bindingcollectionadapter2.**

-keep class com.gyf.immersionbar.** { *; }
-dontwarn com.gyf.immersionbar.**

-keep class com.afollestad.materialdialogs.** { *; }
-dontwarn com.afollestad.materialdialogs.**

-keep class com.blankj.swipepanel.** { *; }
-dontwarn com.blankj.swipepanel.**

-keep class com.scwang.smartrefresh.** { *; }
-dontwarn com.scwang.smartrefresh.**

-keep class com.stx.xhb.** { *; }
-dontwarn com.stx.xhb.**

-keep class com.androidkun.xtablayout.** { *; }
-dontwarn com.androidkun.xtablayout.**

-keep class com.orhanobut.dialog.** { *; }
-dontwarn com.orhanobut.dialog.**

-keep class com.haibin.calendarview.** { *; }
-dontwarn com.haibin.calendarview.**

-keep class com.yanzhenjie.** { *; }
-dontwarn com.yanzhenjie.**

-keep class me.jessyan.** { *; }
-dontwarn me.jessyan.**

-keep class org.apache.commons.** { *; }
-dontwarn org.apache.commons.**
-dontwarn org.apache.http.**

# AndroidX（原 android.support.** 规则在本工程已全部迁移到 AndroidX，属无效规则）
-dontwarn android.support.**
-dontwarn androidx.**
# material 由 immersionbar / smartrefresh / android-dialog 传递引入，
# NoScreenBehavior 继承 AppBarLayout$Behavior、SnackbarUtils 使用 Snackbar，
# 本模块未直接声明该依赖，需抑制 missing class
-dontwarn com.google.android.material.**


#------------------------------------------------------------------------------
# 七、compileOnly 依赖：运行期不在 classpath 上，抑制 R8 的 missing class 报错
#------------------------------------------------------------------------------
# android-update（com.github.lepiejh:android-update）使用单段包名 update/model/listener
-dontwarn update.**
-dontwarn model.**
-dontwarn listener.**
# 其余 compileOnly 依赖
-dontwarn androidx.constraintlayout.**
-dontwarn com.kyleduo.switchbutton.**
-dontwarn com.yanzhenjie.recyclerview.**


#------------------------------------------------------------------------------
# 已移除的错误规则（原因见下）
#------------------------------------------------------------------------------
# [删除] -keep class com.ved.framework.** { *; }
#        连 private 方法一起保留，与「private 方法要混淆」的目标直接冲突。
# [删除] -repackageclasses '' / -flattenpackagehierarchy ''
#        两者语义互相覆盖；且库模块把被改名的类平铺到根包，
#        极易与接入方的类撞名。本框架类名全部保留，这两条只有副作用。
# [删除] -keepclassmembers class * { void *(**On*Event); }
#        无意义的模板残留，会保留任意类中「参数名以 On 开头 Event 结尾」的 void 方法。
# [删除] -keep class com.sunloto.shandong.bean.** { *; }
#        其他工程（彩票项目）遗留，本工程无此包。
# [删除] android.support.** / android.databinding.** / com.squareup.okhttp.**
#        / jp.wasabeef.glide.transformations.** / rx.*（RxJava1）
#        本工程已全面 AndroidX + okhttp3 + RxJava3，这些包根本不存在。
# [修正] -dontwarn om.afollestad.materialdialogs.**  -> com.afollestad...（少了首字母 c）
#
# 关于日志输出选项：-dump / -printseeds / -printusage / -printmapping 已全部移除。
# AGP 会自行注入 -printmapping 指向 build/outputs/mapping/，
# 在此重复声明会与 AGP 的路径互相覆盖，导致 mapping.txt 落到意外位置、
# 线上崩溃堆栈无法还原。如需 seeds/usage 分析，临时手动加回即可。
