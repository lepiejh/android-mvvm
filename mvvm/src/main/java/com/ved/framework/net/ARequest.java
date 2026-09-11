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

import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.Nullable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subjects.PublishSubject;

public abstract class ARequest<T, K> {

    private BaseViewModel<?> viewModel;
    private Class<? extends T> service;
    private IMethod<T, K> method;
    private int index = 0;
    private boolean isLoading = false;
    private View viewState;
    private ISeatSuccess seatSuccess;
    private ISeatError seatError;
    private IResponse<K> response;
    private Map<String, String> headers;

    public ARequest<T, K> withViewModel(BaseViewModel<?> viewModel) {
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

    public PublishSubject<Object> build() {
        return request(viewModel, method, service, viewState, seatSuccess, seatError, headers, index, isLoading, response);
    }

    private PublishSubject<Object> request(@Nullable BaseViewModel<?> viewModel,
                                           @Nullable IMethod<T, K> method,
                                           @Nullable Class<? extends T> service,
                                           View view,
                                           ISeatSuccess seatSuccess,
                                           ISeatError seatError,
                                           Map<String, String> headers,
                                           int index,
                                           boolean isLoading,
                                           @Nullable IResponse<K> iResponse) {
        PublishSubject<Object> lifecycleDisposable = PublishSubject.create();
        if (NetUtil.getNetWorkStart(Utils.getContext()) == 1) {
            if (iResponse != null) {
                iResponse.onError("网络异常", false);
            }
            if (view != null && seatSuccess != null) {
                seatSuccess.onNoNetworkView();
            }
            exceptionHandling(viewModel, "网络异常", -1);
        } else {
            if (view != null && seatSuccess != null) {
                seatSuccess.onStateView();
            }
            if (isLoading && viewModel != null) {
                viewModel.showDialog();
            }
            try {
                final AtomicReference<String> msg = new AtomicReference<>();
                if (method != null) {
                    Observable<K> o = method.method(
                            RetrofitClient.getInstance().create(
                                    service, index, headers,
                                    (message, code) -> {
                                        if (code != Configure.getCode()) {
                                            msg.set(message);
                                        }
                                    },
                                    viewModel, iResponse)
                    );

                    if (viewModel != null && viewModel.getLifecycleProvider() != null) {
                        o = o.compose(RxUtils.bindToLifecycle(viewModel.getLifecycleProvider()));
                    }

                    // 显式声明 ObservableTransformer<K, K>，避免 compose 推断成裸类型
                    ObservableTransformer<K, K> errorTransformer = observable ->
                            observable.onErrorResumeNext(
                                    // 显式声明 Function，避免 lambda 被推断成 ObservableSource
                                    (Function<Throwable, ObservableSource<? extends K>>) throwable -> {
                                        KLog.e(throwable.getMessage());
                                        parseError(isLoading, viewModel, msg.get(), view, seatError, iResponse, null);
                                        return Observable.error(throwable);
                                    });

                    Disposable disposable = o.compose(RxUtils.schedulersTransformer())
                            .compose(errorTransformer)
                            .doOnDispose(RetrofitClient::evictConnections)
                            .takeUntil(lifecycleDisposable)
                            .subscribe(
                                    // 显式声明 Consumer<K>
                                    (Consumer<K>) response ->
                                            parseSuccess(viewModel, view, isLoading, iResponse, response),
                                    // 显式声明 Consumer<Throwable>，内部再转 ResponseThrowable
                                    (Consumer<Throwable>) throwable -> {
                                        ResponseThrowable rt = (throwable instanceof ResponseThrowable)
                                                ? (ResponseThrowable) throwable : null;
                                        parseError(isLoading, viewModel, null, view, seatError, iResponse, rt);
                                    }
                            );

                    if (viewModel != null) {
                        viewModel.accept(disposable);
                    }
                }
            } catch (Exception e) {
                KLog.e(e.getMessage());
                dispatchError(isLoading, viewModel, view, seatError, iResponse);
            }
        }
        return lifecycleDisposable;
    }

    private void parseSuccess(@Nullable BaseViewModel<?> viewModel, View viewState, boolean isLoading,
                              IResponse<K> iResponse, K response) {
        if (viewState != null) {
            viewState.setVisibility(View.GONE);
        }
        if (isLoading && viewModel != null) {
            viewModel.dismissDialog();
        }
        if (iResponse != null) {
            iResponse.onSuccess(response);
        }
    }

    private void dispatchError(boolean isLoading, @Nullable BaseViewModel<?> viewModel,
                               View view, ISeatError seatError, IResponse<K> iResponse) {
        UiThreadDispatcher.runOnUiThread(viewModel, () ->
                parseError(isLoading, viewModel, "连接服务器失败或其他异常", view, seatError, iResponse, null));
    }

    private boolean isCanceledException(@Nullable ResponseThrowable throwable) {
        if (throwable == null || throwable.getCause() == null) {
            return false;
        }
        Throwable t = throwable.getCause();
        if (t instanceof IOException && "Canceled".equals(t.getMessage())) {
            return true;
        }
        return t instanceof SocketException && "Socket closed".equals(t.getMessage());
    }

    private void parseError(boolean isLoading, @Nullable BaseViewModel<?> viewModel, String error, View viewState,
                            ISeatError seatError, IResponse<K> iResponse, ResponseThrowable throwable) {
        if (isLoading && viewModel != null) {
            viewModel.dismissDialog();
        }
        if (isCanceledException(throwable)) {
            return;
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

    private void handleResultException(@Nullable BaseViewModel<?> viewModel, View viewState,
                                       ISeatError seatError, IResponse<K> iResponse, ResponseThrowable throwable) {
        ResultException resultException = (ResultException) throwable.getCause();
        if (resultException == null)return;
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

    public abstract void exceptionHandling(@Nullable BaseViewModel<?> viewModel, @Nullable String error, int code);
}