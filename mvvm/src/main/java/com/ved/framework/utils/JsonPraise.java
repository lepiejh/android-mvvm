package com.ved.framework.utils;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ved.framework.http.MapTypeAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Map;

import androidx.annotation.Nullable;

public class JsonPraise {

    private static final Gson gson = MyGson.getInstance().getGson();

    // 带 MapTypeAdapter 的 Gson 实例（享元模式）：仅需一份，线程安全，全局复用
    private static final Gson mapGson = new GsonBuilder()
            .registerTypeAdapter(new TypeToken<Map<String, Object>>() {
            }.getType(), new MapTypeAdapter())
            .create();

    /**
     * 判断字符串是否为json格式
     * @param jsonString
     * @return
     */
    public static boolean isJSONValid(String jsonString) {
        try {
            new JSONObject(jsonString);
        } catch (JSONException e) {
            return false;
        }
        return true;
    }

    /**
     * obj转换为json字符串
     *
     * @param obj
     * @return
     */
    public static String objToJson(Object obj) {
        String tmp = "";
        try {
            tmp = gson.toJson(obj);
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return tmp;
    }

    public static <T> T jsonToObj(@Nullable final String json, @Nullable final Class<? extends T> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            KLog.e(e.getMessage());
            return null;
        }
    }

    /**
     * 函数名称: parseData 函数描述: 将json字符串转换为map
     *
     * @param data
     * @return
     */
    public static Map<String, Object> jsonToMapObj(String data) {
        return gson.fromJson(data, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    public static Map<String, Object> gsonToMap(String strJson) {
        return mapGson.fromJson(strJson, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    /**
     * 函数名称: parseData 函数描述: 将json字符串转换为map
     *
     * @param data
     * @return
     */
    public static Map<String, String> jsonToMap(String data) {
        return gson.fromJson(data, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    public static <T> T parseJSON(String json, Type type) {
        return gson.fromJson(json, type);
    }

    /**
     * 获取参数值
     *
     * @param jsonStr
     * @return
     * @throws JSONException
     */
    public static String optCode(String jsonStr, String key) {
        String code = null;
        try {
            code = new JSONObject(jsonStr).opt(key).toString();
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
        return code;
    }

    public static boolean hasKey(String jsonStr, String key){
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            return jsonObject.has(key);
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 将001的data对象转换为Map<String,Object>
     *
     * @param data
     * @return
     */
    public static Map<String, Object> jsonToObjMap(String data) {
        return gson.fromJson(data, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

    /**
     * 获取对象值
     *
     * @param jsonStr
     * @return
     * @throws JSONException
     */
    public static Object opt001ObjData(String jsonStr,
                                       @SuppressWarnings("rawtypes") Class clazz, String keys) throws Exception {
        JSONObject dataJson = new JSONObject(jsonStr);
        Object opt = dataJson.opt(keys);
        if (!(opt instanceof JSONObject)) {
            // 目标 key 不存在或不是对象，避免 obj.toString() 空指针
            return null;
        }
        JSONObject obj = (JSONObject) opt;
        return optObj(obj.toString(), clazz);
    }

    /**
     * 获取List值
     *
     * @return
     * @throws JSONException
     */
    public static Object opt001ListData(JSONArray obj, Type type)
            throws Exception {
        if (obj != null) {
            return optObj(obj.toString(), type);
        } else {
            return null;
        }
    }

    /**
     * 获取List值
     *
     * @return
     * @throws JSONException
     */
    public static Object opt001ListData(String json, Type type) {
        if (json != null) {
            return optObj(json, type);
        } else {
            return null;
        }
    }

    /**
     * 获取List值
     *
     * @param jsonStr
     * @return
     * @throws JSONException
     */
    public static Object opt001ListData(String jsonStr, Type type, String keys)
            throws JSONException {
        if (TextUtils.isEmpty(jsonStr)) {
            return null;
        }
        JSONObject dataJson = new JSONObject(jsonStr);

        JSONArray goods = dataJson.optJSONArray(keys);
        if (goods != null) {
            return optObj(goods.toString(), type);
        } else {
            return null;
        }
    }

    public static Object optObj(String jsonStr, Type type) {
        return gson.fromJson(jsonStr, type);
    }

    @SuppressWarnings("unchecked")
    public static Object optObj(String jsonStr,
                                @SuppressWarnings("rawtypes") Class clazz) {
        return gson.fromJson(jsonStr, clazz);
    }

    public static String mapToJson(Map<String, String> map) {
        if (map == null) {
            return "";
        }
        return gson.toJson(map);
    }

}
