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

/**
 * 请求基类（Builder + 参数对象 + 模板方法）：
 * <ul>
 *     <li>public 侧 {@code withXxx()} 链式 Builder（对外 API 不变）；</li>
 *     <li>{@link #build()} 后把散落字段装配进 {@link RequestConfig} 参数对象，
 *         请求主流程 {@link #request(RequestConfig)} 单参流转；</li>
 *     <li>错误处理按场景拆分为单一职责方法，{@link #handleThrowableError} 是
 *         业务异常 / 传输层异常的分派点。</li>
 * </ul>
 *
 * @param <T> Retrofit ApiService 类型
 * @param <K> 响应数据体类型
 */
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
        RequestConfig<T, K> config = new RequestConfig<>();
        config.viewModel = viewModel;
        config.service = service;
        config.method = method;
        config.index = index;
        config.isLoading = isLoading;
        config.viewState = viewState;
        config.seatSuccess = seatSuccess;
        config.seatError = seatError;
        config.response = response;
        config.headers = headers;
        return request(config);
    }

    /**
     * 请求主流程（模板方法）：网络守卫 → 状态视图/弹窗 → 执行 → 异常兜底。
     * 原实现 10 个参数逐层透传，现收敛为参数对象单参流转。
     */
    private PublishSubject<Object> request(RequestConfig<T, K> config) {
        PublishSubject<Object> lifecycleDisposable = PublishSubject.create();
        if (NetUtil.getNetWorkStart(Utils.getContext()) == 1) {
            handleNoNetwork(config);
            return lifecycleDisposable;
        }
        if (config.viewState != null && config.seatSuccess != null) {
            config.seatSuccess.onStateView();
        }
        if (config.isLoading && config.viewModel != null) {
            config.viewModel.showDialog();
        }
        try {
            execute(config, lifecycleDisposable);
        } catch (Exception e) {
            KLog.e(e.getMessage());
            dispatchError(config);
        }
        return lifecycleDisposable;
    }

    /** 无网络场景：直接走错误回调，不发起请求 */
    private void handleNoNetwork(RequestConfig<T, K> config) {
        if (config.response != null) {
            config.response.onError("网络异常", false);
        }
        if (config.viewState != null && config.seatSuccess != null) {
            config.seatSuccess.onNoNetworkView();
        }
        exceptionHandling(config.viewModel, "网络异常", -1);
    }

    /**
     * 组装 Retrofit 服务并发起订阅（原 request() 的 try 块内容）。
     * 声明 throws Exception：{@code viewModel.accept} 实现的 RxJava Consumer
     * 签名为 {@code accept(T) throws Throwable}，受检异常由调用方的 catch 统一兜底。
     */
    private void execute(RequestConfig<T, K> config, PublishSubject<Object> lifecycleDisposable) throws Exception {
        if (config.method == null) {
            return;
        }
        final AtomicReference<String> msg = new AtomicReference<>();
        Observable<K> o = config.method.method(
                RetrofitClient.getInstance().create(
                        config.service, config.index, config.headers,
                        (message, code) -> {
                            if (code != Configure.getCode()) {
                                msg.set(message);
                            }
                        },
                        config.viewModel, config.response));

        if (config.viewModel != null && config.viewModel.getLifecycleProvider() != null) {
            o = o.compose(RxUtils.bindToLifecycle(config.viewModel.getLifecycleProvider()));
        }

        // 显式声明 ObservableTransformer<K, K>，避免 compose 推断成裸类型
        ObservableTransformer<K, K> errorTransformer = observable ->
                observable.onErrorResumeNext(
                        // 显式声明 Function，避免 lambda 被推断成 ObservableSource
                        (Function<Throwable, ObservableSource<? extends K>>) throwable -> {
                            KLog.e(throwable.getMessage());
                            parseError(config, null, msg.get());
                            return Observable.error(throwable);
                        });

        Disposable disposable = o.compose(RxUtils.schedulersTransformer())
                .compose(errorTransformer)
                .doOnDispose(RetrofitClient::evictConnections)
                .takeUntil(lifecycleDisposable)
                .subscribe(
                        // 显式声明 Consumer<K>
                        (Consumer<K>) response -> parseSuccess(config, response),
                        // 显式声明 Consumer<Throwable>，内部再转 ResponseThrowable
                        (Consumer<Throwable>) throwable -> {
                            ResponseThrowable rt = (throwable instanceof ResponseThrowable)
                                    ? (ResponseThrowable) throwable : null;
                            parseError(config, rt, null);
                        }
                );

        if (config.viewModel != null) {
            config.viewModel.accept(disposable);
        }
    }

    private void parseSuccess(RequestConfig<T, K> config, K response) {
        if (config.viewState != null) {
            config.viewState.setVisibility(View.GONE);
        }
        if (config.isLoading && config.viewModel != null) {
            config.viewModel.dismissDialog();
        }
        if (config.response != null) {
            config.response.onSuccess(response);
        }
    }

    private void dispatchError(RequestConfig<T, K> config) {
        UiThreadDispatcher.runOnUiThread(config.viewModel, () ->
                parseError(config, null, "连接服务器失败或其他异常"));
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

    /**
     * 错误处理入口（守卫序列 + 分派）：弹窗收尾 → 取消短路 → 错误视图 → 错误回调
     * → 按 {@link #handleThrowableError} 分派业务/传输层异常。
     */
    private void parseError(RequestConfig<T, K> config, @Nullable ResponseThrowable throwable, @Nullable String error) {
        if (config.isLoading && config.viewModel != null) {
            config.viewModel.dismissDialog();
        }
        // 主动取消（页面销毁/dispose）不算错误，静默返回
        if (isCanceledException(throwable)) {
            return;
        }
        if (config.viewState != null && config.seatError != null) {
            config.seatError.onErrorView();
        }
        if (config.response != null && StringUtils.isNotEmpty(error)) {
            config.response.onError(error, false);
        }
        if (throwable != null) {
            handleThrowableError(config, throwable);
        } else if (StringUtils.isNotEmpty(error)) {
            exceptionHandling(config.viewModel, error, -2);
        }
    }

    /**
     * 错误分派点（策略分派）：业务异常（{@link ResultException}，服务端约定的错误码）
     * 走 {@link #handleResultException}；其余传输层异常走通用回调。
     */
    private void handleThrowableError(RequestConfig<T, K> config, ResponseThrowable throwable) {
        KLog.e(throwable.message);
        if (throwable.getCause() instanceof ResultException) {
            handleResultException(config, throwable);
        } else {
            if (config.response != null) {
                config.response.onError(throwable.message, false);
            }
            if (config.seatError != null) {
                config.seatError.onEmptyView(throwable.message);
            }
        }
    }

    /** 业务异常（服务端返回码非成功）：按 errCode 交给子类与视图层处理 */
    private void handleResultException(RequestConfig<T, K> config, ResponseThrowable throwable) {
        ResultException resultException = (ResultException) throwable.getCause();
        if (resultException == null) {
            return;
        }
        exceptionHandling(config.viewModel, resultException.getErrMsg(), resultException.getErrCode());
        if (config.viewState != null && config.seatError != null) {
            config.seatError.onErrorHandler(resultException.getErrCode());
        }
        if (config.response != null) {
            config.response.onError(TextUtils.isEmpty(resultException.getErrMsg()) ? throwable.message : resultException.getErrMsg(), false);
        }
        if (config.viewState != null && config.seatError != null) {
            config.seatError.onEmptyView();
        }
    }

    /**
     * 请求参数对象：{@code withXxx()} Builder 装配完成后字段的载体，
     * 请求主流程全部经它单参流转（消除原 10 参数逐层透传）。
     * 包级私有静态内部类，仅在本类内部使用，对外零暴露。
     */
    private static final class RequestConfig<T, K> {
        @Nullable
        BaseViewModel<?> viewModel;
        @Nullable
        Class<? extends T> service;
        @Nullable
        IMethod<T, K> method;
        int index;
        boolean isLoading;
        @Nullable
        View viewState;
        @Nullable
        ISeatSuccess seatSuccess;
        @Nullable
        ISeatError seatError;
        @Nullable
        IResponse<K> response;
        @Nullable
        Map<String, String> headers;
    }

    public abstract void exceptionHandling(@Nullable BaseViewModel<?> viewModel, @Nullable String error, int code);
}
