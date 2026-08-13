package com.ved.framework.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MyGson {
    // Gson 实例是线程安全的，全局复用一份即可（享元模式），
    // 避免每次调用 getGson() 都重新构建 GsonBuilder 造成的对象创建开销。
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new NumericBooleanToStringTypeAdapter())
            .create();

    private MyGson() {
    }

    private static class SingletonHolder {
        private static final MyGson INSTANCE = new MyGson();
    }

    public static MyGson getInstance() {
        return MyGson.SingletonHolder.INSTANCE;
    }

    public Gson getGson() {
        return gson;
    }
}
