package com.ved.framework.base;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

class ViewModelProxyImpl<VM extends BaseViewModel> implements ViewModelProxy<VM> {
    private final Object obj;
    private VM viewModel;

    public ViewModelProxyImpl(Object obj) {
        this.obj = obj;
    }

    public <T extends ViewModel> T createViewModel(Object obj, Class<T> cls) {
        if (obj instanceof FragmentActivity){
            // 默认工厂无法创建继承 AndroidViewModel 的 BaseViewModel（需要 Application 构造参数），
            // 改用 AndroidViewModelFactory
            return new ViewModelProvider((FragmentActivity) obj, createFactory((FragmentActivity) obj)).get(cls);
        }else if (obj instanceof Fragment){
            Fragment fragment = (Fragment) obj;
            if (fragment.getActivity() != null) {
                return new ViewModelProvider(fragment, createFactory(fragment.getActivity())).get(cls);
            }
        }
        return null;
    }

    private ViewModelProvider.Factory createFactory(FragmentActivity activity) {
        return ViewModelProvider.AndroidViewModelFactory.getInstance(activity.getApplication());
    }

    @Override
    public VM createViewModel() {
        if (viewModel == null) {
            Class<?> modelClass;
            Type type = obj.getClass().getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                // Class<?> 是可具体化（reifiable）类型，这个强转不会产生 unchecked 告警；
                // 以前写裸类型 (Class) 则既报 rawtypes 又报 unchecked。
                //
                // 【已知限制，不要随手“修”】这里假定第 2 个泛型实参本身是一个普通 Class，
                // 即宿主必须写成 BaseActivity<XxxBinding, MyViewModel> 这种形式。
                // 如果写成 BaseActivity<XxxBinding, BaseViewModel<BaseModel>>，反射拿到的
                // 将是 ParameterizedTypeImpl 而不是 Class，下面的强转会在运行期抛
                // ClassCastException。框架自带的 ContainerActivity / DefaultErrorActivity 因此
                // 都故意保留裸类型 BaseViewModel（那两处有对应注释）。
                // 要彻底解决需要在这里补上“实参仍是 ParameterizedType 则取其 rawType”的分支，
                // 那属于行为变更，应单独评估后再做，不要混在消告警的改动里。
                modelClass = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[1];
            } else {
                //如果没有指定泛型参数，则默认使用BaseViewModel
                modelClass = BaseViewModel.class;
            }
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
}