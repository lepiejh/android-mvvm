#===============================================================================
# mvvm 框架（com.ved.framework）—— 下发给接入方的混淆规则
#
# 本文件由 mvvm/build.gradle 的 consumerProguardFiles 声明，会被打进 AAR 的
# proguard.txt，接入方 App 开启 minifyEnabled true 时由 AGP 自动合并生效，
# 接入方无需手动拷贝任何规则。
#
# 约束：本文件只放 -keep / -keepattributes / -dontwarn，
#       绝不放 -printmapping、-repackageclasses、-optimizations、-dontobfuscate
#       之类会篡改接入方构建行为的选项。
#===============================================================================


#------------------------------------------------------------------------------
# 一、字节码属性：框架大量使用反射 + 泛型，缺一不可
#------------------------------------------------------------------------------
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes Exceptions
-keepattributes *Annotation*,AnnotationDefault,MethodParameters


#------------------------------------------------------------------------------
# 二、框架 API：类名 + 公开成员不可混淆、不可裁剪
#------------------------------------------------------------------------------
# 框架内部的 private 方法已在库构建阶段混淆完毕，此处继续锁住对外 API。
-keep class com.ved.framework.** {
    public protected <methods>;
}
-keep interface com.ved.framework.** { *; }

-keepclassmembers enum com.ved.framework.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

-keepclassmembers class com.ved.framework.** {
    <init>(...);
}

# 字段保留原名：Gson / 反射 / DataBinding 按字段名读写
-keepclassmembers class com.ved.framework.** {
    <fields>;
}


#------------------------------------------------------------------------------
# 三、编译期生成产物：接入方的生成代码按全限定名硬引用这些类
#------------------------------------------------------------------------------
# DataBinding：接入方生成的 DataBinderMapperImpl 会 new com.ved.framework.DataBinderMapperImpl()
-keep class com.ved.framework.DataBinderMapperImpl { *; }
-keep class com.ved.framework.BR { *; }
# DataBindingInfo 不需保留：它只是 AGP 触发注解处理器用的空壳类，不会进入 AAR 的 classes.jar，
# 写死类名会报“无法解析类”，且运行时无任何代码引用它。
-keep class com.ved.framework.databinding.** { *; }

# Glide：运行时 Class.forName("com.bumptech.glide.GeneratedAppGlideModuleImpl")
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.bumptech.glide.GeneratedRequestManagerFactory { *; }
-keep public class * implements com.bumptech.glide.module.AppGlideModule
-keep public class * implements com.bumptech.glide.module.LibraryGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Lifecycle：运行时 Class.forName(观察者类名 + "_LifecycleAdapter")
-keep class com.ved.framework.**_LifecycleAdapter { *; }
-keepclassmembers class * {
    @androidx.lifecycle.OnLifecycleEvent <methods>;
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}


#------------------------------------------------------------------------------
# 四、反射 / 字节码注入安全区
#------------------------------------------------------------------------------
# EventBus：@Subscribe 方法由 EventBus 反射调用（含接入方自己写的 private 订阅方法）
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# BusUtils：@Bus 方法名以字符串常量注入，运行时 getDeclaredMethod(funName) 反查。
# 字符串不会被 R8 改写，方法名必须原样保留。
-keepclassmembers class * {
    @com.ved.framework.utils.bland.code.BusUtils$Bus <methods>;
}
-keepclassmembers class com.ved.framework.utils.bland.code.BusUtils {
    void registerBus(...);
    static void registerBus4Test(...);
}

# DataBinding 适配器（含接入方自定义的 @BindingAdapter）
-keepclassmembers class * {
    @androidx.databinding.BindingAdapter <methods>;
    @androidx.databinding.BindingConversion <methods>;
    @androidx.databinding.BindingMethod <methods>;
}

# Java 序列化
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

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# JNI：native 方法名与 so 符号绑定
-keepclasseswithmembernames class * {
    native <methods>;
}
# MMKV：native 层反向回调 Java 侧 private static 方法
-keep class com.tencent.mmkv.** { *; }

# 自定义 View 由 LayoutInflater 反射构造
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# 布局 android:onClick 与 WebView JS 桥
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 资源 R 类：资源 id 在运行期按字段名反查（getIdentifier），字段名不能改
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Gson：TypeToken 匿名子类依赖 Signature + InnerClasses + EnclosingMethod
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }

# Retrofit / OkHttp（R8 full mode 需要的 allowshrinking 变体）
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Kotlin
-keep class kotlin.Metadata { *; }

# RxJava3：UnsafeAccess 通过字段名做 CAS
-keepclassmembers class io.reactivex.rxjava3.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}


#------------------------------------------------------------------------------
# 五、AndroidManifest 组件（框架清单已声明，接入方合并后按类名查找）
#------------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keep class com.ved.framework.utils.bland.code.MessengerUtils$* { *; }


#------------------------------------------------------------------------------
# 六、框架依赖的第三方库
#------------------------------------------------------------------------------
-keep class com.trello.rxlifecycle4.** { *; }
-keep interface com.trello.rxlifecycle4.** { *; }
-keep class com.tbruyelle.rxpermissions3.** { *; }
-keep interface com.tbruyelle.rxpermissions3.** { *; }
-keep class me.tatarka.bindingcollectionadapter2.** { *; }
-keep class com.gyf.immersionbar.** { *; }
-keep class com.afollestad.materialdialogs.** { *; }
-keep class com.blankj.swipepanel.** { *; }
-keep class com.scwang.smartrefresh.** { *; }
-keep class com.stx.xhb.** { *; }
-keep class com.androidkun.xtablayout.** { *; }
-keep class com.orhanobut.dialog.** { *; }
-keep class com.haibin.calendarview.** { *; }
-keep class com.yanzhenjie.** { *; }
-keep class me.jessyan.** { *; }
-keep class org.apache.commons.** { *; }
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }


#------------------------------------------------------------------------------
# 七、可选依赖：接入方没有引入时抑制 missing class 报错
#------------------------------------------------------------------------------
-dontwarn update.**
-dontwarn model.**
-dontwarn listener.**
-dontwarn androidx.constraintlayout.**
-dontwarn com.kyleduo.switchbutton.**
-dontwarn com.yanzhenjie.**
-dontwarn com.haibin.calendarview.**
-dontwarn com.androidkun.xtablayout.**
-dontwarn com.orhanobut.dialog.**
-dontwarn com.stx.xhb.**
-dontwarn com.scwang.smartrefresh.**
-dontwarn com.gyf.immersionbar.**
-dontwarn com.afollestad.materialdialogs.**
-dontwarn com.blankj.**
-dontwarn me.jessyan.**
-dontwarn com.tencent.mmkv.**
-dontwarn com.bumptech.glide.**
-dontwarn com.google.gson.**
-dontwarn com.trello.rxlifecycle4.**
-dontwarn com.tbruyelle.rxpermissions3.**
-dontwarn me.tatarka.bindingcollectionadapter2.**
-dontwarn io.reactivex.**
-dontwarn java.util.concurrent.Flow*
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okhttp3.internal.platform.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.**
-dontwarn org.greenrobot.eventbus.**
-dontwarn sun.misc.**
-dontwarn android.support.**
-dontwarn androidx.databinding.**
-dontwarn com.google.android.material.**
# 传递依赖引用的可选类（JitPack 构建日志中 R8 实测报出），接入方同样会遇到
-dontwarn org.graalvm.nativeimage.**
-dontwarn com.liulishuo.filedownloader.**


#------------------------------------------------------------------------------
# 【必读】本文件不可删除，也不可把 -dontshrink / -dontoptimize 写进来
#------------------------------------------------------------------------------
# 一、为什么不可删除：
#   proguard-rules.pro 只作用于「本库自己的构建」，产物 AAR 里的 private 方法已改名；
#   但接入方 App 开启 minifyEnabled 时，App 的 R8 会把本 AAR 当普通输入再跑一轮
#   裁剪+优化+改名。本文件经 build.gradle 的 consumerProguardFiles 打进 AAR 的
#   proguard.txt，随依赖自动合并进接入方的 R8 配置，是框架在接入方侧唯一的护栏。
#   实测（用独立 R8 模拟接入方 App，输入为本库已混淆的 AAR classes.jar：
#   604 类 / 7318 成员 / 4948 个 public+protected）：
#     ・带本文件：输出 605 类 / 6860 成员，public+protected 4948 -> 4950（零丢失），
#       SPUtils / BaseActivity / BaseViewModel / Messenger / BusUtils 全部在位；
#     ・删掉本文件：只剩 67 类 / 305 成员 / 167 个 public+protected，
#       584 个 com.ved.framework 类被整体删除，4783 个 public/protected API 丢失，
#       上面五个类无一幸存。
#   框架里大量成员只被「编译期注入到接入方的代码」或反射调用（@Bus 的 registerBus、
#   EventBus @Subscribe、DataBinding 生成类、_LifecycleAdapter、Glide
#   GeneratedAppGlideModuleImpl），在 App 的 R8 看来毫无调用方，必被删掉或改名。
#
# 二、为什么 -dontshrink / -dontoptimize 只能写在 proguard-rules.pro：
#   这两项是「构建行为开关」，一旦随 AAR 下发，就会把接入方 App 的裁剪与优化
#   一起关掉，导致 App 体积暴涨、运行性能下降 —— 那是接入方自己的决策，库无权替它做。
#   「只做混淆、不裁剪不优化」只约束本库自身的构建。
#   接入方 App 开启 R8 后，本文件的 -keep 保证框架的类名与 public/protected API
#   既不被改名也不被裁剪；而框架内部的 private 成员在本库出厂时已完成改名。
#------------------------------------------------------------------------------
