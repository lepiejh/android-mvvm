package com.ved.framework.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class MyGson {
    private MyGson() {
    }

    private static class SingletonHolder {
        private static final MyGson INSTANCE = new MyGson();
    }

    public static MyGson getInstance() {
        return MyGson.SingletonHolder.INSTANCE;
    }

    public Gson getGson(){
        return new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                // 如果字段有@Expose注解且值为null，则跳过
                return f.getAnnotation(Expose.class) != null &&
                        !f.getAnnotation(Expose.class).serialize();
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        }).registerTypeAdapterFactory(new NullStringToEmptyAdapterFactory()).create();
    }
}
