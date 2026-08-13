package com.ved.framework.net;

import com.ved.framework.utils.SPUtils;
import com.ved.framework.utils.StringUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class MyInterceptor implements Interceptor {
    private final Map<String, String> headers;

    public MyInterceptor(Map<String, String> headers) {
        this.headers = headers;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = chain.request()
                .newBuilder();
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String headerKey : keys) {
                builder.addHeader(headerKey, Objects.requireNonNull(headers.get(headerKey))).build();
            }
        }
        String token = SPUtils.getInstance().getString("token");
        if (StringUtils.isNotEmpty(token)){
            builder.addHeader("token", token);
        }
        request = builder.build();
        // 注意：绝不能在此关闭 Response！
        // 该 Response 会沿拦截器链返回给 Retrofit，由 GsonResponseBodyConverter 读取 body。
        // 若在此 close()，后续 value.string() 会抛 IllegalStateException("closed")，导致所有请求解析失败。
        return chain.proceed(request);
    }
}