#===============================================================================
# mvvm 框架（com.ved.framework）库模块混淆规则
#
# 混淆目标（对外发布的 AAR）：
#   1. public 类名 + public/protected 成员一律不混淆、不裁剪
#      —— 接入方按全限定名引用，AndroidManifest / DataBinding / Glide / Lifecycle
#         的生成代码也按名字硬引用这些类。
#      实测：608 个类 0 丢失，391 个保住类名的类里 public/protected 成员 0 丢失
#      （成员总量 4972 -> 4974，多出的 2 个是 R8 补的桥接方法）。
#   2. private / 包级「方法」放开重命名。
#      实测：725 个 private + 550 个包级方法中 1011 个成员被改名，例如
#      SPUtils.isSpace(String)->c(String)、SPUtils.saveValue(String,Object)->b(...)、
#      BusUtils.init()->b()；而 registerBus(...) 因 @Bus 插件按名字注入调用而被第四节保住。
#   3. private / 包级「字段」放开重命名 —— 实测 1016 个字段被改名：
#      SPUtils.sMmkvInited->e、sSPMap->d、sp->a、changeListeners->b、daoCache->c，
#      SpDao.tableName->b / entityClass->c / keyMapper->d / lock->e / async->f，
#      SpDao$TableSnapshot.list->a / index->b，ParameterizedTypeImpl.clazz->a。
#      数据载体（entity.** / net.** / Serializable / @SerializedName）豁免，见第二节。
#   4. private / 包级「内部类」放开重命名 —— 实测 608 个类中 217 个非 public 类被改名：
#      SPUtils$ParameterizedTypeImpl->SPUtils$b、SpDao$TableSnapshot->SpDao$a、
#      Messenger$WeakActionAndToken->Messenger$a、BaseView->base.a、
#      RetrofitClient->net.f、PinchImageView$FlingAnimator->PinchImageView$a。
#      public API 类 0 丢失，Manifest / DataBinding / Glide / Lifecycle 全部原名保留。
#   5. 「局部变量 / 方法参数名」：DEX 里根本不存在这种东西，无需也无法混淆 —— 见第一节。
#   6. 只做混淆，不做裁剪与优化（-dontshrink / -dontoptimize）—— 详见「零」。
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
# MethodParameters 已移除：它是唯一能把「方法参数名」带进 DEX 的属性
# （DEX 的 debug_info_item 开头就存了各参数的名字，反编译器据此还原）。
# 实测本库产出的 classes.jar 里该属性出现 0 次 —— javac 不加 -parameters 就不生成它，
# 所以它此前是空操作；移除是为了防止将来有人往 compileOptions 里加 -parameters，
# 把 key / value / tableName 这类参数名原样泄漏进 APK。
#
# 另：LocalVariableTable（局部变量名）从来没被保留过。更重要的是 —— DEX 字节码里
# 压根没有「局部变量」这个概念，方法体只有编号寄存器 v0..vN，局部变量名在 javac
# 编译完成的那一刻就已经彻底消失，不存在「混淆」的对象。
# 反编译看到的 str / obj / z / cls / list / map / tArr / bArr 全是 JADX 按变量类型
# 现场编出来的显示名（String->str、Object->obj、boolean->z、Class->cls、int->i），
# 拿源码一对就能证实它们不是残留的真名：
#   源码 saveValue(String key, Object value)          -> 反编译 m484b(String str, Object obj)
#   源码 SpDao(SPUtils sp, String tableName,
#             Class<T> entityClass, KeyMapper<T,K> keyMapper, boolean async)
#                                                     -> 反编译 SpDao(SPUtils sPUtils, String str,
#                                                          Class<T> cls, KeyMapper<T,K> keyMapper, boolean z)
#   源码 ParameterizedTypeImpl(Class<?> clz)          -> 反编译 ParameterizedTypeImpl(Class<?> cls)
-keepattributes *Annotation*,AnnotationDefault
# 保留行号，配合 AGP 自动产出的 mapping.txt 可还原被重命名的 private 方法堆栈：
# mvvm/build/outputs/mapping/<variant>/mapping.txt
-keepattributes SourceFile,LineNumberTable


#------------------------------------------------------------------------------
# 二、框架自身：public API 全量保留，非 public 的类 / 字段 / 方法放开混淆
#------------------------------------------------------------------------------
# 关键在 `public` 这个「类」访问修饰符：R8 拿它去匹配 class 文件自身的 access_flags。
#   ・源码里 public 的类（含 public 嵌套类：SPUtils$SpDao、SPUtils$Filter、
#     SPUtils$KeyMapper、ToastUtils$UtilsMaxWidthRelativeLayout）→ 命中
#     → 类名 + public/protected 方法保留；
#   ・源码里 private / 包级的类（内部类、匿名类、lambda 类）→ 不命中 → 类名放开改名。
#
# 为什么不能正面写 `-keep private class ...`：R8 的类选择器不接受 private，实测直接解析失败
#     Error in ...proguard-rules.pro at line N, column 7:
#     Expected [!]interface|@interface|class|enum
# 根因是 JVMS 规定 class 文件的 access_flags 里不允许出现 ACC_PRIVATE —— 嵌套类的
# private/static 标记只记录在 InnerClasses 属性中（javap 打印
# SPUtils$ParameterizedTypeImpl 得到的是 `final class`，而不是源码里的
# `private static final class`，就是这个原因），而 R8 的类匹配读的是前者。
# 所以只能反向写：保住 public 的，剩下的 private + 包级自然全部放开。
# 效果与「只挑 private 内部类改名」完全等价，而且不需要维护任何类名清单。
#
# 实测（以已发布的 v0.1.3 AAR classes.jar 为输入跑独立 R8，608 个类）：
#   ・217 个非 public 类被改名，输出仍是 608 个类，public API 0 丢失；
#   ・Manifest 声明的 ContainerActivity / DefaultErrorActivity / CaocInitProvider /
#     UtilsTransActivity / UtilsTransActivity4MainProcess / UtilsFileProvider /
#     MessengerUtils$ServerService 全部原名保留；
#   ・DataBinderMapperImpl、MyAppGlideModule、IBaseViewModel_LifecycleAdapter 原名保留；
#   ・res/layout/ac.xml 里 LayoutInflater 按名字反射的
#     ToastUtils$UtilsMaxWidthRelativeLayout 原名保留（它是 public static final class）；
#   ・binding.**（@BindingAdapter 宿主）与 databinding.** 下 0 个类被改名；
#   ・全库 grep Class.forName / getDeclaredField / newInstance，命中的字符串字面量全是
#     android.app.ActivityThread、android.os.SystemProperties、android.app.StatusBarManager
#     这类系统类，其余都是运行期传入的 className 参数，没有一处按名字反查本库的类。
-keep public class com.ved.framework.** {
    public protected <methods>;
}

# 接口的成员全部是 public（含 default 方法与常量），整体保留，避免被裁剪。
# 同样只保 public 接口：包级接口（net.IResult、DownLoadManager$ApiService）放开改名，
# Retrofit 靠方法上的 @GET/@POST 注解建代理，不依赖任何名字。
-keep public interface com.ved.framework.** { *; }

# 兜底：无论「声明类」本身是不是 public，它的 public / protected 成员一律不改名。
# 上面那条 -keep public class 只保护 ACC_PUBLIC 类的成员，但第四节还有
#     -keepnames class * implements java.io.Serializable
# 它会把一批「非 public、但传递实现了 Serializable」的类的「类名」保住 —— 类名保住了，
# 成员却不在 -keep public class 的覆盖范围内，于是被改名。实测踩到 4 个类共 11 个成员：
#   net.ResultException extends IOException                       (class 声明，包级)
#     getErrMsg()/setErrMsg(String)/getErrCode()/setErrCode(int) -> b()/a(String)/a()/a(int)
#   utils.bland.code.ThreadUtils$LinkedBlockingQueue4Util         (private static final class)
#     extends LinkedBlockingQueue<Runnable>
#     public boolean offer(Runnable) -> public boolean a(Runnable)
#     ★ 这条是真 bug 不是观感问题：协变覆写被改名后就不再覆写父类方法，
#       父类 offer(Object) 的桥接方法会转调一个已不存在的实现。
#   utils.bland.code.PermissionUtils$PermissionActivityImpl       (static final class)
#     extends UtilsTransActivity$TransActivityDelegate
#     public static void start(int) -> public static void a(int)
#   net.HttpsUtils (class 声明，包级) getSslSocketFactory(...) 5 个重载
# 加上下面这条后，「所有类和公开的方法都不混淆」这个约束对全部 608 个类都成立，
# 与声明类是否 public 无关；代价仅是这 11 个成员保住原名。
-keepclassmembers class com.ved.framework.** {
    public protected <methods>;
}

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

# 字段：public / protected 一律保留原名（它们是对外 API 的一部分，
# DataBinding 生成的绑定类也按名字访问 public 字段）；
# private / 包级字段放开改名 —— 实测 1016 个字段被改名。
# 为什么这次敢放开：按名字访问字段的四条路径都已被下面的例外规则逐一挡住，
#   ・Gson 用 Field.getName() 反射读写「实例」字段（只跳过 static/transient）
#     → entity.** / net.** 两个数据载体包 + @SerializedName 标注字段整体豁免；
#   ・Java 序列化按字段名写流 → 第四节 `-keepclassmembers class * implements
#     java.io.Serializable { !static !transient <fields>; }` 已覆盖（本库共 5 处：
#     SerializableHttpCookie、DownLoadStateBean、CaocConfig、
#     UtilsTransActivity$TransActivityDelegate、SpanUtils 的内部类）；
#     serialVersionUID 与 serialPersistentFields 额外在下面显式保住；
#   ・DataBinding 只按名字访问 public 字段与 BR 常量 → 第 1 行已覆盖；
#   ・RxJava 靠字段名做 CAS 的 producerIndex/consumerIndex 属于 io.reactivex，
#     不在 com.ved.framework 下，且第四节已单独保住。
# 另：全库 grep 过 getDeclaredField / getField，命中的全是 android.app.ActivityThread、
# android.os.storage.StorageVolume、android.telephony.SignalStrength 这类系统类，
# 没有一处用字符串字面量反查本库自己的字段名，所以改名不会引发 NoSuchFieldError。
# 接入方如果把自己的实体类放进 com.ved.framework.entity / net 包（不推荐），
# 也会自动落进下面的豁免名单。
-keepclassmembers class com.ved.framework.** {
    public protected <fields>;
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
}
# 数据载体：字段名就是 JSON 的 key，改名后 Gson 会静默解析成 null（最难查的一类 bug）
-keepclassmembers class com.ved.framework.entity.** { <fields>; }
-keepclassmembers class com.ved.framework.net.** { <fields>; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 关于「private 内部类」（结论与完整实测证据见本节开头，这里只补充两个现场现象）：
#   ・javap 打印 SPUtils$ParameterizedTypeImpl 得到的是 `final class ...`，而不是源码里的
#     `private static final class` —— private/static 只写在 InnerClasses 属性里，
#     class 文件的 access_flags 没有它们，而 R8 的类匹配读的正是 access_flags。
#     这也是为什么 `-keep private class X` 在 R8 里直接是语法错误，
#     而 `-keep public class X` 可用：不命中 public 的就是 private/包级，无需枚举名单。
#   ・改名后在 SpDao 的字段签名里能直接看到效果：
#     `private volatile SPUtils$SpDao$a<T, K> g;`（原为 TableSnapshot<T, K> table）。


#------------------------------------------------------------------------------
# 三、注解处理器 / 编译期生成产物：被「按名字」硬引用，必须整体保留
#------------------------------------------------------------------------------
# --- DataBinding ---
# 接入方 App 生成的 DataBinderMapperImpl 里是 new com.ved.framework.DataBinderMapperImpl()，
# 属于跨模块的编译期硬引用，一旦被改名，本框架的 aa.xml / ab.xml 绑定运行时直接失效。
-keep class com.ved.framework.DataBinderMapperImpl { *; }
# 它的两个包级嵌套类 InnerBrLookup / InnerLayoutIdLookup 不在上面那条的覆盖范围内
# （-keep class X 不包含 X$Y），第二节的 `-keep public class` 也不会命中它们。
# 二者只被外层 mapper 内部引用，改名本身安全；但它们是 DataBinding 的编译期生成产物，
# 万一哪个 AGP 版本改成按名字引用，就会变成极难排查的绑定静默失效，
# 这里花 2 个类的代价买断这个风险。
-keep class com.ved.framework.DataBinderMapperImpl$* { *; }
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
