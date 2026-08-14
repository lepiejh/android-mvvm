package com.ved.framework.net;

import android.text.TextUtils;
import android.view.View;

import com.ved.framework.base.BaseViewModel;
import com.ved.framework.http.ResponseThrowable;
import com.ved.framework.utils.Configure;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.NetUtil;
import com.ved.framework.utils.RxUtils;
import com.ved.framework.utils.StringUtils;
import com.ved.framework.utils.Utils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * 网络请求，链式配置请求参数，通过 build() 返回的生命周期事件流可取消请求。
 * <p>
 * 详细使用方法（含取消网络请求）见项目 README.md「ARequest 网络请求与取消」章节。
 *
 * @param <T> service interface
 * @param <K> 返回的数据类型
 */
public abstract class ARequest<T, K> {
    private BaseViewModel viewModel;
    private Class<? extends T> service;
    private IMethod<T, K> method;
    private int index = 0;
    private boolean isLoading = false;
    private View viewState;
    private ISeatSuccess seatSuccess;
    private ISeatError seatError;
    private IResponse<K> response;
    private Map<String, String> headers;

    public ARequest<T, K> withViewModel(BaseViewModel viewModel) {
        this.viewModel = viewModel;
        return this;
    }

    public ARequest<T, K> withService(Class<? extends T> service) {
        this.service = service;
        return this;
    }

    public ARequest<T, K> withMethod(IMethod<T, K> method) {
        this.method = method;
        return this;
    }

    public ARequest<T, K> withIndex(int index) {
        this.index = index;
        return this;
    }

    public ARequest<T, K> withLoading(boolean isLoading) {
        this.isLoading = isLoading;
        return this;
    }

    public ARequest<T, K> withViewState(View viewState) {
        this.viewState = viewState;
        return this;
    }

    public ARequest<T, K> withSeatSuccess(ISeatSuccess seatSuccess) {
        this.seatSuccess = seatSuccess;
        return this;
    }

    public ARequest<T, K> withSeatError(ISeatError seatError) {
        this.seatError = seatError;
        return this;
    }

    public ARequest<T, K> withResponse(IResponse<K> response) {
        this.response = response;
        return this;
    }

    public ARequest<T, K> withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public PublishSubject<Object> build(){
        return request(viewModel,method,service,viewState,seatSuccess,seatError,headers,index,isLoading,response);
    }

    private PublishSubject<Object> request(@Nullable BaseViewModel viewModel,
                                          @Nullable IMethod<T, K> method,@Nullable Class<? extends T> service,
                                          View view,ISeatSuccess seatSuccess,ISeatError seatError,Map<String, String> headers,
                                          int index,boolean isLoading, @Nullable IResponse<K> iResponse) {
        PublishSubject<Object> lifecycleDisposable = PublishSubject.create();
        if (NetUtil.getNetWorkStart(Utils.getContext()) == 1) {
            if (iResponse != null) {
                iResponse.onError("网络异常",false);
            }
            if (view != null && seatSuccess != null) {
                //手机无网络
                seatSuccess.onNoNetworkView();
            }
            exceptionHandling(viewModel, "网络异常", -1);
        } else {
            if (view!= null && seatSuccess != null) {
                seatSuccess.onStateView();
            }
            if (isLoading && viewModel != null) {
                viewModel.showDialog();
            }
            try {
                final AtomicReference<String> msg = new AtomicReference<>();
                if (method != null) {
                    Observable o = method.method(RetrofitClient.getInstance().create(service, index, headers, (message, code) -> {
                        if (code!= Configure.getCode())
                        {
                            msg.set(message);
                        }
                    },viewModel,iResponse));
                    if (viewModel != null && viewModel.getLifecycleProvider() != null) {
                        // 修复：接收 compose 返回值，请求才能真正绑定 View 生命周期（页面销毁时自动取消订阅）
                        o = o.compose(RxUtils.bindToLifecycle(viewModel.getLifecycleProvider()));
                    }
                    Disposable disposable = o.compose(RxUtils.schedulersTransformer())
                            .compose(observable -> observable
                                    .onErrorResumeNext((Function<Throwable, ObservableSource>) throwable -> {
                                        KLog.e(throwable.getMessage());
                                        parseError(isLoading,viewModel,msg.get(),view,seatError,iResponse,null);
                                        return Observable.error(throwable);
                                    }))
                            .takeUntil(lifecycleDisposable)
                            .subscribe((Consumer<K>) response ->
                                            parseSuccess(viewModel,view, isLoading, iResponse, response),
                                    (Consumer<ResponseThrowable>) throwable ->
                                            parseError(isLoading,viewModel, null,view,seatError,iResponse, throwable));
                    // 将订阅纳入 ViewModel 的 CompositeDisposable 统一管理，onCleared 时自动释放
                    if (viewModel != null) {
                        viewModel.accept(disposable);
                    }
                }
            } catch (Exception e) {
                KLog.e(e.getMessage());
                dispatchError(isLoading, viewModel, "连接服务器失败或其他异常", view, seatError, iResponse);
            }
        }
        return lifecycleDisposable;
    }

    private void parseSuccess(@Nullable BaseViewModel viewModel, View viewState,boolean isLoading, IResponse<K> iResponse, K response) {
        if (viewState!= null) {
            viewState.setVisibility(View.GONE);
        }
        if (isLoading && viewModel != null) {
            viewModel.dismissDialog();
        }
        if (iResponse != null) {
            iResponse.onSuccess(response);
        }
    }

    /**
     * 统一错误分发模板方法：优先借助 ViewModel 的协程任务在 UI 线程回调，
     * 无 ViewModel 时回退到主线程 Handler，保证错误回调一定发生在 UI 线程。
     */
    private void dispatchError(boolean isLoading, @Nullable BaseViewModel viewModel, String error,
                               View view, ISeatError seatError, IResponse<K> iResponse) {
        UiThreadDispatcher.runOnUiThread(viewModel, () ->
                parseError(isLoading, viewModel, error, view, seatError, iResponse, null));
    }

    private void parseError(boolean isLoading, @Nullable BaseViewModel viewModel, String error, View viewState,
                            ISeatError seatError, IResponse<K> iResponse, ResponseThrowable throwable) {
        if (isLoading && viewModel != null) {
            viewModel.dismissDialog();
        }
        if (viewState != null && seatError != null) {
            seatError.onErrorView();
        }
        if (iResponse != null && StringUtils.isNotEmpty(error)) {
            iResponse.onError(error, false);
        }
        if (throwable != null) {
            KLog.e(throwable.message);
            if (throwable.getCause() instanceof ResultException) {
                handleResultException(viewModel, viewState, seatError, iResponse, throwable);
            } else {
                if (iResponse != null) {
                    iResponse.onError(throwable.message, false);
                }
                if (seatError != null) {
                    seatError.onEmptyView(throwable.message);
                }
            }
        } else if (StringUtils.isNotEmpty(error)) {
            exceptionHandling(viewModel, error, -2);
        }
    }

    /**
     * 处理 ResultException 业务异常：透出错误码与错误信息到响应回调与占位视图
     */
    private void handleResultException(@Nullable BaseViewModel viewModel, View viewState,
                                       ISeatError seatError, IResponse<K> iResponse, ResponseThrowable throwable) {
        ResultException resultException = (ResultException) throwable.getCause();
        exceptionHandling(viewModel, resultException.getErrMsg(), resultException.getErrCode());
        if (viewState != null && seatError != null) {
            seatError.onErrorHandler(resultException.getErrCode());
        }
        if (iResponse != null) {
            iResponse.onError(TextUtils.isEmpty(resultException.getErrMsg()) ? throwable.message : resultException.getErrMsg(), false);
        }
        if (viewState != null && seatError != null) {
            seatError.onEmptyView();
        }
    }

    public abstract void exceptionHandling(@Nullable BaseViewModel viewModel, @Nullable String error, int code);
}