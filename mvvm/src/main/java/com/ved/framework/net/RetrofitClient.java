package com.ved.framework.net;

import com.ved.framework.base.BaseViewModel;
import com.ved.framework.http.cookie.CookieJarImpl;
import com.ved.framework.http.cookie.store.PersistentCookieStore;
import com.ved.framework.http.interceptor.CacheInterceptor;
import com.ved.framework.utils.Configure;
import com.ved.framework.utils.Constant;
import com.ved.framework.utils.CorpseUtils;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.MyGson;
import com.ved.framework.utils.StringUtils;
import com.ved.framework.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.net.Proxy;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.jessyan.retrofiturlmanager.RetrofitUrlManager;
import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;

class RetrofitClient {

    private RetrofitClient() {
        // 防止反射破坏单例
        if (RetrofitClient.getInstance() != null) {
            throw new IllegalStateException("u can't instantiate me...");
        }
    }

    private static class SingletonHolder {
        private static final RetrofitClient INSTANCE = new RetrofitClient();
    }

    public static RetrofitClient getInstance() {
        return SingletonHolder.INSTANCE;
    }

    //防止反序列化产生多个对象
    private Object readResolve() throws ObjectStreamException {
        return RetrofitClient.getInstance();
    }

    public <T> T create(final Class<T> service, int i, Map<String, String> headers, IResult iResult, @Nullable BaseViewModel viewModel, @Nullable IResponse<?> iResponse) {
        if (service == null) {
            throw new RuntimeException("Api service is null!");
        }
        // 建造者模式：封装 OkHttpClient 的构建细节
        OkHttpClient client = new HttpClientBuilder()
                .headers(headers)
                .iResult(iResult)
                .viewModel(viewModel)
                .iResponse(iResponse)
                .build();
        return new Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonDConverterFactory.create(MyGson.getInstance().getGson()))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .baseUrl(Configure.getUrl().get(i))
                .build().create(service);
    }

    public static <T> T execute(Observable<T> observable, Observer<T> subscriber) {
        observable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);

        return null;
    }

    /**
     * 全局基础 OkHttpClient（享元/单例思想）：
     * 连接池、Cache 目录、Cookie 持久化、SSL、超时、代理等与具体请求无关的组件全局唯一，
     * 避免每次 create() 都重建 ConnectionPool 导致 TCP 连接无法复用、Cache 目录被重复打开。
     * 使用 DCL 双重检查锁保证多线程下的安全初始化。
     */
    private static volatile OkHttpClient baseClient;

    private static OkHttpClient getBaseClient() {
        OkHttpClient client = baseClient;
        if (client == null) {
            synchronized (RetrofitClient.class) {
                client = baseClient;
                if (client == null) {
                    client = createBaseClient();
                    baseClient = client;
                }
            }
        }
        return client;
    }

    private static OkHttpClient createBaseClient() {
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        return RetrofitUrlManager.getInstance().with(new OkHttpClient.Builder())
                .cache(new Cache(new File(Utils.getContext().getCacheDir(), "ved_cache"), Constant.CACHE_TIMEOUT))
                .cookieJar(new CookieJarImpl(new PersistentCookieStore(Utils.getContext())))
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                .connectTimeout(Constant.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constant.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(Constant.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(8, 15, TimeUnit.SECONDS))
                .proxy(Proxy.NO_PROXY)
                .build();
    }

    /**
     * 建造者模式：封装 OkHttpClient 的构建细节，
     * 将拦截器、缓存、Cookie、SSL、超时、连接池等配置集中在 Builder 中维护，
     * 新增配置只需扩展 Builder 方法，符合开闭原则。
     */
    private static final class HttpClientBuilder {

        private Map<String, String> headers;
        private IResult iResult;
        private BaseViewModel viewModel;
        private IResponse<?> iResponse;
        private int connectTimeout = Constant.DEFAULT_TIMEOUT;
        private int readTimeout = Constant.DEFAULT_TIMEOUT;
        private int writeTimeout = Constant.DEFAULT_TIMEOUT;
        private int poolMaxIdle = 8;
        private long poolKeepAliveDuration = 15;

        public HttpClientBuilder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public HttpClientBuilder iResult(IResult iResult) {
            this.iResult = iResult;
            return this;
        }

        public HttpClientBuilder viewModel(BaseViewModel viewModel) {
            this.viewModel = viewModel;
            return this;
        }

        public HttpClientBuilder iResponse(IResponse<?> iResponse) {
            this.iResponse = iResponse;
            return this;
        }

        public HttpClientBuilder connectTimeout(int seconds) {
            this.connectTimeout = seconds;
            return this;
        }

        public HttpClientBuilder readTimeout(int seconds) {
            this.readTimeout = seconds;
            return this;
        }

        public HttpClientBuilder writeTimeout(int seconds) {
            this.writeTimeout = seconds;
            return this;
        }

        public HttpClientBuilder connectionPool(int maxIdle, long keepAliveDuration) {
            this.poolMaxIdle = maxIdle;
            this.poolKeepAliveDuration = keepAliveDuration;
            return this;
        }

        public OkHttpClient build() {
            // 复用全局基础 client 的连接池/Cache/SSL/Cookie（享元模式），
            // 仅通过 newBuilder() 追加与本次调用相关的拦截器，保持拦截器顺序与原实现一致。
            OkHttpClient.Builder builder = getBaseClient().newBuilder()
                    .addInterceptor(new MyInterceptor(headers))
                    .addInterceptor(new CacheInterceptor(Utils.getContext()))
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        long startTime = System.currentTimeMillis();
                        Response response;
                        try {
                            response = chain.proceed(chain.request());
                        } catch (IOException e) {
                            UiThreadDispatcher.runOnUiThread(viewModel, () -> {
                                if (viewModel != null) {
                                    viewModel.dismissDialog();
                                }
                                if (iResponse != null) iResponse.onError(e.getMessage(), e instanceof SocketException);
                            });
                            throw e; // 继续抛出，让 RxJava 的 onError 处理
                        }
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        ResponseBody body = response.body();
                        if (body == null) {
                            // 空 body（如 204 No Content）直接返回，避免后续 NPE
                            return response;
                        }
                        MediaType mediaType = body.contentType();
                        String content = body.string();
                        CorpseUtils.INSTANCE.inspectRequestBody(request);
                        KLog.e("Interceptor", "请求体返回：| Response:" + content);
                        KLog.e("Interceptor", "----------请求耗时:" + duration + "毫秒----------");
                        if (StringUtils.isNotEmpty(content)) {
                            try {
                                JSONObject jsonObject = new JSONObject(content);
                                int code = 0;
                                String message = null;
                                if (CorpseUtils.INSTANCE.isStandardJson(content)) {
                                    code = jsonObject.optInt("code");
                                    message = jsonObject.optString("msg");
                                } else {
                                    code = jsonObject.optInt("status");
                                    message = jsonObject.optString("message");
                                }
                                if (iResult != null) {
                                    iResult.onInfoResult(message, code);
                                }
                            } catch (Exception e) {
                                KLog.e(e.getMessage());
                            }
                        }
                        return response.newBuilder().body(ResponseBody.create(mediaType, content)).build();
                    });
            // 兼容 Builder 预留的个性化配置（默认值与全局 baseClient 一致时无需重复设置）
            if (connectTimeout != Constant.DEFAULT_TIMEOUT) {
                builder.connectTimeout(connectTimeout, TimeUnit.SECONDS);
            }
            if (readTimeout != Constant.DEFAULT_TIMEOUT) {
                builder.readTimeout(readTimeout, TimeUnit.SECONDS);
            }
            if (writeTimeout != Constant.DEFAULT_TIMEOUT) {
                builder.writeTimeout(writeTimeout, TimeUnit.SECONDS);
            }
            if (poolMaxIdle != 8 || poolKeepAliveDuration != 15) {
                builder.connectionPool(new ConnectionPool(poolMaxIdle, poolKeepAliveDuration, TimeUnit.SECONDS));
            }
            return builder.build();
        }
    }

}
