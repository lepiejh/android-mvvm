package com.ved.framework.base;

import android.app.Application;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

class ViewModelProxyImpl<VM extends BaseViewModel<?>> implements ViewModelProxy<VM> {
    private final Object obj;
    private VM viewModel;

    public ViewModelProxyImpl(Object obj) {
        this.obj = obj;
    }

    public <T extends ViewModel> T createViewModel(Object obj, Class<T> cls) {
        if (obj instanceof FragmentActivity) {
            // 默认工厂无法创建继承 AndroidViewModel 的 BaseViewModel（需要 Application 构造参数），
            // 所以使用自定义 Factory：优先调用 (Application) 构造，找不到再回退到无参构造。
            return new ViewModelProvider((FragmentActivity) obj, createFactory((FragmentActivity) obj)).get(cls);
        } else if (obj instanceof Fragment) {
            Fragment fragment = (Fragment) obj;
            if (fragment.getActivity() != null) {
                return new ViewModelProvider(fragment, createFactory(fragment.getActivity())).get(cls);
            }
        }
        return null;
    }

    /**
     * 自定义 ViewModelProvider.Factory：
     * - 优先匹配 {@code (Application)} 构造，用于创建 AndroidViewModel 及其子类（如 BaseViewModel）；
     * - 若无该构造，则回退到无参构造，用于创建普通 ViewModel。
     * <p>
     * 之所以不直接用 {@code ViewModelProvider.AndroidViewModelFactory.getInstance(...)}，
     * 是因为不同版本的 androidx.lifecycle 中该方法返回类型存在差异（有时返回
     * AndroidViewModelFactory 自身，有时返回 ViewModelProvider.Factory），在部分版本上
     * 还会因两个 Factory 接口包名不同（android.arch vs androidx）而出现「不兼容」的编译错误。
     * 自己实现 Factory 可以彻底规避版本差异。
     */
    private ViewModelProvider.Factory createFactory(FragmentActivity activity) {
        final Application application = activity.getApplication();
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                // 1. 优先尝试 (Application) 构造：AndroidViewModel / BaseViewModel
                try {
                    return modelClass.getConstructor(Application.class)
                            .newInstance(application);
                } catch (NoSuchMethodException ignored) {
                    // 没有 (Application) 构造，走下面的无参构造分支
                } catch (Exception e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                }

                // 2. 回退到无参构造：普通 ViewModel
                try {
                    return modelClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                }
            }
        };
    }

    @Override
    public VM createViewModel() {
        if (viewModel == null) {
            Class<?> modelClass = resolveViewModelClass();
            // 反射拿到的第 2 个泛型实参在运行期必然就是 VM 的真实类型，但静态上只能拿到
            // 无界的 Class<?>；而 createViewModel(Object, Class<T>) 要求 T extends ViewModel，
            // 所以这里必须补一次带界强转、再补一次到 VM 的强转。把 @SuppressWarnings 收窄
            // 到这一个局部变量上，而不是盖住整个方法，以免将来新增的真问题被一并掩盖。
            @SuppressWarnings("unchecked")
            VM created = (VM) createViewModel(obj, (Class<? extends ViewModel>) modelClass);
            viewModel = created;
            return created;
        }
        return viewModel;
    }

    /**
     * 解析宿主 Activity/Fragment 泛型声明中第 2 个实参对应的 Class。
     * <p>
     * 支持以下三种宿主声明形式（均以 BaseActivity&lt;Binding, Xxx&gt; 为例）：
     * <ul>
     *   <li>{@code BaseActivity<AaBinding, MyViewModel>} —— 实参是普通 Class，直接返回；</li>
     *   <li>{@code BaseActivity<AaBinding, BaseViewModel<BaseModel>>} —— 实参是 ParameterizedType，
     *       取 rawType（BaseViewModel）作为 Class 返回；</li>
     *   <li>{@code BaseActivity<AaBinding, BaseViewModel<?>>} —— 实参是 WildcardType，
     *       取上界（BaseViewModel）作为 Class 返回。</li>
     * </ul>
     * 若宿主未显式指定泛型实参（type 不是 ParameterizedType），默认使用 BaseViewModel.class。
     */
    private Class<?> resolveViewModelClass() {
        Type type = obj.getClass().getGenericSuperclass();
        if (!(type instanceof ParameterizedType)) {
            // 没有指定泛型参数，默认使用 BaseViewModel
            return BaseViewModel.class;
        }
        Type arg = ((ParameterizedType) type).getActualTypeArguments()[1];
        return toClass(arg);
    }

    /**
     * 把泛型实参的 Type 归一化为 Class。
     * <p>
     * - Class：直接返回；
     * - ParameterizedType：返回其 rawType；
     * - WildcardType：返回其第一个上界；
     * - 其它（如 TypeVariable、GenericArrayType）：回退到 BaseViewModel。
     */
    private Class<?> toClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            // rawType 一定是 Class（JLS 规定），这里递归一下更稳
            return toClass(raw);
        }
        if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds.length > 0) {
                // 例如 BaseViewModel<? extends BaseModel>，取 BaseModel；
                // 对于无界通配符 <?>，upperBounds[0] 是 Object，
                // 会在下面 instanceof Class 时命中并最终返回 Object，
                // 所以这里先做一次 BaseViewModel 子类判断再决定是否采纳。
                Type upper = upperBounds[0];
                if (upper instanceof Class
                        && BaseViewModel.class.isAssignableFrom((Class<?>) upper)) {
                    return (Class<?>) upper;
                }
            }
            // <?> 这种无界通配符，或上界不是 BaseViewModel 子类时，回退到 BaseViewModel
            return BaseViewModel.class;
        }
        return BaseViewModel.class;
    }
}