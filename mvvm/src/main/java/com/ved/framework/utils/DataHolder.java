package com.ved.framework.utils;


import java.util.HashMap;
import java.util.Map;

public class DataHolder {//解决activity之间传递数据过大导致崩溃的问题

    // key 固定是 setData/getData 的 String 形参，value 是任意待传递对象；
    // 以前写裸类型 Map / HashMap 会报 rawtypes，补上泛型后擦除不变，行为完全一致。
    // 加 final：本字段只在构造时赋值，之后只 put/get/clear，从不重新指向。
    private final Map<String, Object> dataList = new HashMap<>();

    private static volatile DataHolder instance;

    public static DataHolder getInstance() {

        if (instance == null) {

            synchronized (DataHolder.class) {

                if (instance == null) {

                    instance = new DataHolder();

                }

            }

        }

        return instance;

    }

    public void setData(String key, Object o) {
        dataList.put(key, o);

    }

    public Object getData(String key) {
        Object o = dataList.get(key);
        dataList.clear();
        return o;
    }

}