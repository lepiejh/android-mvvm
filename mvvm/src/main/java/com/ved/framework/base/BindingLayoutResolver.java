package com.ved.framework.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ViewDataBinding;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 绑定布局解析器：根据 {@link ViewDataBinding} 泛型自动推断页面布局文件。
 * <p>
 * 例如 {@code class XxxActivity : BaseActivity<XxxActivityBinding, XxxViewModel>()}，
 * 绑定类 {@code XxxActivityBinding} 按 DataBinding 命名规则反推出布局 {@code xxx_activity}。
 * 这样页面类无需再覆写 {@code initContentView()}。
 */
public final class BindingLayoutResolver {

    private BindingLayoutResolver() {
    }

    private static final String BINDING_SUFFIX = "Binding";

    /** 缓存 layoutId，避免每次创建页面都做反射 + getIdentifier */
    private static final Map<String, Integer> LAYOUT_ID_CACHE = new ConcurrentHashMap<>();

    /**
     * 从宿主类解析布局 id，无法解析时抛出统一异常。
     * <p>
     * 供 {@code BaseActivity} / {@code BaseFragment} / {@code BaseDialogFragment} 的
     * 默认 {@code initContentView()} 委托调用，收敛重复的解析 + 异常处理逻辑（委托模式）。
     *
     * @param context   上下文（Activity / Fragment 均可）
     * @param hostClass 宿主类（页面 Class，如 {@code HistoricalDetailsActivity.class}）
     * @return 布局资源 id
     * @throws IllegalStateException 无法从 ViewDataBinding 泛型推断布局文件时抛出
     */
    public static int resolveLayoutIdOrThrow(@NonNull Context context, @NonNull Class<?> hostClass) {
        int layoutId = resolveLayoutId(context, hostClass);
        if (layoutId == 0) {
            throw new IllegalStateException("无法从 ViewDataBinding 泛型推断布局文件，"
                    + "请确认泛型声明为具体的 Binding 类（如 XxxBinding），或覆写 initContentView() 返回布局 id。"
                    + "class=" + hostClass.getName());
        }
        return layoutId;
    }

    /**
     * 从宿主类解析布局 id。
     *
     * @param context   上下文（Activity / Fragment 均可）
     * @param hostClass 宿主类（页面 Class，如 {@code HistoricalDetailsActivity.class}）
     * @return 布局资源 id，无法解析时返回 0
     */
    public static int resolveLayoutId(@NonNull Context context, @NonNull Class<?> hostClass) {
        Class<?> bindingClass = resolveBindingClass(hostClass);
        if (bindingClass == null) {
            return 0;
        }
        String cacheKey = context.getPackageName() + ":" + bindingClass.getName();
        Integer cached = LAYOUT_ID_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String layoutName = toLayoutName(bindingClass.getSimpleName());
        if (layoutName == null) {
            return 0;
        }
        int id = context.getResources().getIdentifier(layoutName, "layout", context.getPackageName());
        if (id != 0) {
            LAYOUT_ID_CACHE.put(cacheKey, id);
        }
        return id;
    }

    /**
     * 沿泛型继承链向上解析第一个类型参数（ViewDataBinding 子类）。
     * 支持多层继承，例如 {@code A -> BaseA<ABinding, VM> -> BaseActivity<VB, VM>}，
     * 从最底层开始，首个解析为具体绑定类的类型参数即为目标。
     */
    @Nullable
    private static Class<?> resolveBindingClass(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Type genericSuperclass = current.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                Type[] arguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
                if (arguments.length > 0 && arguments[0] instanceof Class) {
                    Class<?> first = (Class<?>) arguments[0];
                    if (first != ViewDataBinding.class && ViewDataBinding.class.isAssignableFrom(first)) {
                        return first;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 绑定类名转布局文件名：
     * {@code HistoricalDetailsActivityBinding} -> {@code historical_details_activity}
     * <p>
     * 规则：去掉 {@code Binding} 后缀；大写字母前（前一个字符是小写/数字）补下划线并转小写；
     * 布局名中数字前的下划线（如 {@code item_2_view} -> {@code Item_2ViewBinding}）保留原样。
     */
    @Nullable
    private static String toLayoutName(String bindingClassName) {
        if (bindingClassName == null || !bindingClassName.endsWith(BINDING_SUFFIX)) {
            return null;
        }
        String name = bindingClassName.substring(0, bindingClassName.length() - BINDING_SUFFIX.length());
        StringBuilder sb = new StringBuilder(name.length());
        char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                if (i > 0 && (Character.isLowerCase(chars[i - 1]) || Character.isDigit(chars[i - 1]))) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
