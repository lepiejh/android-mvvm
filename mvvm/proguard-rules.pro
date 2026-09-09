#===============================================================================
# mvvm 框架（com.ved.framework）库模块混淆规则
#
# 混淆目标（对外发布的 AAR）：
#   1. 所有类名 + public/protected 方法一律不混淆、不裁剪
#      —— 接入方按全限定名引用，AndroidManifest / DataBinding / Glide / Lifecycle
#         的生成代码也按名字硬引用这些类。
#   2. 类内部的 private / 包级方法放开，允许 R8 重命名（隐藏实现）。
#      实测：725 个 private + 550 个包级方法中 1011 个成员被改名，例如
#      SPUtils.isSpace(String)->c(String)、SPUtils.saveValue(String,Object)->b(...)、
#      BusUtils.init()->b()；而 registerBus(...) 因 @Bus 插件按名字注入调用而被第四节保住。
#   3. 字段一律保留原名（822 个 private 字段全部不改）—— 原因见第二节末尾。
#   4. 只做混淆，不做裁剪与优化（-dontshrink / -dontoptimize）—— 详见「零」。
#   5. 接入方 IDE 里 private 方法「爆红」是源码与字节码不一致的显示问题，
#      不影响编译与运行 —— 详见「零之二」。
#
#===============================================================================


#------------------------------------------------------------------------------
# 零、R8 模式开关：本库「只做混淆」，不裁剪、不优化
#------------------------------------------------------------------------------
# 库模块不同于 App：它没有 Manifest 声明的启动入口，R8 只能把 -keep 规则当作根。
#   ・开启裁剪(shrink)：R8 会把「在本模块内看不到调用方」的 private 方法直接删掉；
#   ・开启优化(optimize)：R8 还会把 private 方法内联进调用点、合并类、删参数。
# 两者叠加的后果就是 AAR 里 private 方法凭空消失，而框架自身与接入方的反射调用
# （@Bus 的 BusUtils.registerBus、EventBus @Subscribe、Gson、MMKV 的 JNI 回调）
# 在运行时抛 NoSuchMethodError / NoSuchFieldError。
# 所以这里显式关掉裁剪与优化，只保留「重命名」这一项能力。
# 代价：AAR 不会因裁剪而变小，但 private 方法依旧被改名（这才是需求）。
-dontshrink
-dontoptimize


#------------------------------------------------------------------------------
# 零之二、接入方 IDE 里 private 方法「爆红」不是 bug，无需为本文件加白名单
#------------------------------------------------------------------------------
# 现象：接入方工程点开 SPUtils.SpDao，看到
#         sp.getCollectionByKey(...)          // L1171
#         sp.saveTable(...)                   // L1227
#         sp.registerSpChangeListener(this)   // L1149
#       三处爆红；把本库 minifyEnabled 改为 false 重新发布后就不红了。
#
# 原因：JitPack 除 AAR 外还会自行打一个「未混淆的 -sources.jar」（构建日志里的
#       "Creating -sources.jar"，它直接打包仓库源码，不由 Gradle 任务产出，本文件
#       管不到）。Android Studio 把这份原始 .java 挂到已混淆的 classes.jar 上显示：
#       文字是原始的，符号却按字节码解析；而这三个 private 方法在字节码里已改名，
#       自然解析不到 —— 纯属显示层的不一致，不是编译错误，也不是运行时错误。
#
# 实测（javac -source 8 复刻 SPUtils/SpDao 结构 → 用本规则跑 R8 → 再拿 R8 产物
#       当 classpath 编译一段接入方代码）：
#         ・三个 private 方法与 javac 合成的 access$000/100/200 桥接全部被改名；
#         ・SpDao 内的调用点被 R8 同步改写并指向改名后的方法，字节码自洽；
#         ・InnerClasses / Signature 均保留，SpDao<T,K> 仍是 SPUtils 的嵌套泛型类；
#         ・接入方代码 javac 编译通过，R8 exit=0。
#
# 消除红线的办法（都在接入方侧，不必放松本库的混淆强度）：
#   1. Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle
#      → 取消勾选自动下载 "Sources"，IDE 改用内置反编译器直接读 classes.jar，
#      看到的就是混淆后的真实字节码（也才能验证混淆确实生效）；
#   2. File → Project Structure → Libraries → 选中该库 → 移除 Sources 附件。
#
# 为什么不给这三个方法单独加 -keep：这种「嵌套类调用外部类 private 方法」的合成桥接
# 在本框架里有 174 处、分布在 53 个类（javap 统计 access$NNN，且该统计取自尚不含
# SpDao 的旧构建产物，实际更多）。逐个白名单既不可维护，也直接违背目标 2。


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

# 字段一律不改名（Gson / 反射 / DataBinding 按字段名访问）。
# 为什么「private 变量」也不改名 —— 本框架的字段是数据载体，不是实现细节：
#   ・SPUtils.saveEntity/getEntity、GsonUtils、JsonPraise 走 Gson，Gson 用
#     Field.getName() 反射读写实例字段，改名后 JSON 直接解析成 null（静默错误，最难查）；
#   ・Serializable 的默认序列化按字段名写出流，改名后旧数据反序列化失败；
#   ・DataBinding 生成的绑定类按字段名访问 public 字段；
#   ・第四节 RxJava 的 producerIndex/consumerIndex 靠字段名做 CAS。
# 实测数据（javap 统计 classes.jar）：private static 389 + private 实例 433 = 822 个字段。
# 其中只有 private static 那 389 个在理论上可安全改名（Gson 与 Java 序列化都跳过 static），
# 但它们承载的是 sSPMap / TAG / NULL 这类内部状态，改名对「隐藏实现」几乎没有增益，
# 却要额外承担 serialVersionUID、Kotlin companion 字段等边角风险 —— 收益远小于风险，故不做。
# 若确实需要，可把下面两行取消注释（务必自测 Gson 与 SP 存量数据）：
#   -keepclassmembers class com.ved.framework.** { !static <fields>; public protected static <fields>; }
#   （即用上面这行替换下面的 <fields>; 那一行，只放行 private/包级 static 字段改名）
-keepclassmembers class com.ved.framework.** {
    <fields>;
}

# 关于「private 内部类」：javap 实测 classes.jar 里 604 个类含 300 个嵌套类，
# 但没有任何一个带 private 修饰 —— Java 编译产物中嵌套类的 private 标记只存在于
# InnerClasses 属性里，class 文件自身的 access_flags 没有 ACC_PRIVATE，
# ProGuard/R8 的类匹配读的是后者，因此不存在「只挑 private 内部类改名」的写法。
# 而且类名必须全保留：接入方按全限定名引用（SPUtils$SpDao、XBannerDataWrapper 等），
# DataBinding 布局、AndroidManifest、ReflectFragmentFactory 也都按名字硬引用。
# 这些嵌套类内部的 private 方法已经按第 2 条改名了，实现细节同样被隐藏。


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
# 传递依赖引用的可选类，编译期不在 classpath 上（JitPack 构建日志里 R8 实测报出）：
#   R8: Missing class: org.graalvm.nativeimage.hosted.Feature
#   R8: Missing class: com.liulishuo.filedownloader.FileDownloadLargeFileListener
# 本工程源码无任何引用，仅运行期由对应库按需加载，抑制即可。
-dontwarn org.graalvm.nativeimage.**
-dontwarn com.liulishuo.filedownloader.**


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
