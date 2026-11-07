package com.ved.framework.http;

import com.ved.framework.http.download.DownLoadSubscriber;
import com.ved.framework.http.download.ProgressCallBack;
import com.ved.framework.http.interceptor.ProgressInterceptor;
import com.ved.framework.net.RetrofitClient;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by ved on 2017/5/11.
 * 文件下载管理，封装一行代码实现下载
 */
public class DownLoadManager {

    private static Retrofit retrofit;

    private DownLoadManager() {
        buildNetWork();
    }

    // 静态内部类单例：由 JVM 保证类加载时的线程安全，且延迟到首次使用时才初始化
    private static class SingletonHolder {
        private static final DownLoadManager INSTANCE = new DownLoadManager();
    }

    /**
     * 单例模式
     *
     * @return DownLoadManager
     */
    public static DownLoadManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    //下载
    public void load(String downUrl, final ProgressCallBack<ResponseBody> callBack) {
        retrofit.create(ApiService.class)
                .download(downUrl)
                .subscribeOn(Schedulers.io())//请求网络 在调度者的io线程
                .observeOn(Schedulers.io()) //指定线程保存文件
                .doOnNext(new Consumer<ResponseBody>() {
                    @Override
                    public void accept(ResponseBody responseBody) throws Exception {
                        callBack.saveFile(responseBody);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) //在主线程中更新ui
                .subscribe(new DownLoadSubscriber<ResponseBody>(callBack));
    }

    private void buildNetWork() {
        // 复用全局基础 OkHttpClient（享元模式）：连接池、缓存、Cookie、SSL 等与业务请求共享，
        // 修复原来自建整套 client 导致的 TCP 连接无法复用、下载请求丢失 Cookie/代理配置等问题。
        // newBuilder() 是浅拷贝，派生过程不会影响全局 baseClient 本身；
        // 仅追加下载专用的进度拦截器，并保留原来 20s 的连接超时（读/写超时继承全局 30s，对大文件下载更宽松）。
        OkHttpClient okHttpClient = RetrofitClient.getBaseClient().newBuilder()
                .addInterceptor(new ProgressInterceptor())
                .connectTimeout(20, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                // @Url 传入的是完整下载地址，baseUrl 仅为 Retrofit 要求的占位
                .baseUrl("http://www.baidu.com")
                .build();
    }

    private interface ApiService {
        @Streaming
        @GET
        Observable<ResponseBody> download(@Url String url);
    }
}
