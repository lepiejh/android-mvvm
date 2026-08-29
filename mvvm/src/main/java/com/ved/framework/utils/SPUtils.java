package com.ved.framework.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 不能跨进程保存、获取数据
 * 如需跨进程 使用 MMKV
 *
 * 进程 A 写入
 * MMKV kv = MMKV.mmkvWithID(XX, MMKV.MULTI_PROCESS_MODE)
 * kv.encode(XX,XX)
 *
 * 进程 B 读取
 *  MMKV kv = MMKV.mmkvWithID(XX, MMKV.MULTI_PROCESS_MODE)
 *  String xx = kv.decodeString(XX)
 */
@SuppressLint("ApplySharedPref")
public final class SPUtils {

    private static final Map<String, SPUtils> sSPMap = new HashMap<>();
    private final SharedPreferences sp;

    public static SPUtils getInstance() {
        return getInstance("");
    }

    public static SPUtils getInstance(@Nullable String spName) {
        return getInstance(spName, Context.MODE_PRIVATE);
    }

    /**
     * Return the single {@link SPUtils} instance
     *
     * @param mode Operating mode.
     * @return the single {@link SPUtils} instance
     */
    public static SPUtils getInstance(final int mode) {
        return getInstance("", mode);
    }

    /**
     * Return the single {@link SPUtils} instance
     *
     * @param spName The name of sp.
     * @param mode   Operating mode.
     * @return the single {@link SPUtils} instance
     */
    public static SPUtils getInstance(@Nullable String spName, final int mode) {
        if (isSpace(spName)) spName = "spUtils";
        SPUtils sp = sSPMap.get(spName);
        if (sp == null) {
            sp = new SPUtils(spName, mode);
            sSPMap.put(spName, sp);
        }
        return sp;
    }

    private SPUtils(@Nullable final String spName) {
        sp = Utils.getContext().getSharedPreferences(spName, Context.MODE_PRIVATE);
    }

    private SPUtils(@Nullable final String spName, final int mode) {
        sp = Utils.getContext().getSharedPreferences(spName, mode);
    }

    private static boolean isSpace(@Nullable final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public void put(@Nullable String key, @Nullable Object object) {
        saveValue(key, object);
    }

    ///////////////////////////////////////////////////////////////////////////
    // 以下为合并自 com.ved.framework.utils.bland.code.SPUtils 的重载方法
    // 为保证加密/解密语义一致，put 系列统一走 saveValue（commit），isCommit 参数保留但以 commit 实现
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Put the string value in sp.
     *
     * @param key   The key of sp.
     * @param value The value of sp.
     */
    public void put(@NonNull final String key, final String value) {
        saveValue(key, value);
    }

    /**
     * Put the string value in sp.
     *
     * @param key      The key of sp.
     * @param value    The value of sp.
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void put(@NonNull final String key, final String value, final boolean isCommit) {
        saveValue(key, value);
    }

    /**
     * Put the int value in sp.
     *
     * @param key   The key of sp.
     * @param value The value of sp.
     */
    public void put(@NonNull final String key, final int value) {
        saveValue(key, value);
    }

    /**
     * Put the int value in sp.
     *
     * @param key      The key of sp.
     * @param value    The value of sp.
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void put(@NonNull final String key, final int value, final boolean isCommit) {
        saveValue(key, value);
    }

    /**
     * Put the long value in sp.
     *
     * @param key   The key of sp.
     * @param value The value of sp.
     */
    public void put(@NonNull final String key, final long value) {
        saveValue(key, value);
    }

    /**
     * Put the long value in sp.
     *
     * @param key      The key of sp.
     * @param value    The value of sp.
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void put(@NonNull final String key, final long value, final boolean isCommit) {
        saveValue(key, value);
    }

    /**
     * Put the float value in sp.
     *
     * @param key   The key of sp.
     * @param value The value of sp.
     */
    public void put(@NonNull final String key, final float value) {
        saveValue(key, value);
    }

    /**
     * Put the float value in sp.
     *
     * @param key      The key of sp.
     * @param value    The value of sp.
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void put(@NonNull final String key, final float value, final boolean isCommit) {
        saveValue(key, value);
    }

    /**
     * Put the boolean value in sp.
     *
     * @param key   The key of sp.
     * @param value The value of sp.
     */
    public void put(@NonNull final String key, final boolean value) {
        saveValue(key, value);
    }

    /**
     * Put the boolean value in sp.
     *
     * @param key      The key of sp.
     * @param value    The value of sp.
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void put(@NonNull final String key, final boolean value, final boolean isCommit) {
        saveValue(key, value);
    }

    /**
     * Put the set of string value in sp.
     *
     * @param key   The key of sp.
     * @param value The value of sp.
     */
    public void put(@NonNull final String key, final Set<String> value) {
        put(key, value, false);
    }

    /**
     * Put the set of string value in sp.
     *
     * @param key      The key of sp.
     * @param value    The value of sp.
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void put(@NonNull final String key,
                    final Set<String> value,
                    final boolean isCommit) {
        if (isCommit) {
            sp.edit().putStringSet(key, value).commit();
        } else {
            sp.edit().putStringSet(key, value).apply();
        }
    }

    /**
     * Return the set of string value in sp.
     *
     * @param key The key of sp.
     * @return the set of string value if sp exists
     * or {@code Collections.<String>emptySet()} otherwise
     */
    public Set<String> getStringSet(@NonNull final String key) {
        return getStringSet(key, Collections.<String>emptySet());
    }

    /**
     * Return the set of string value in sp.
     *
     * @param key          The key of sp.
     * @param defaultValue The default value if the sp doesn't exist.
     * @return the set of string value if sp exists or {@code defaultValue} otherwise
     */
    public Set<String> getStringSet(@NonNull final String key,
                                    final Set<String> defaultValue) {
        return sp.getStringSet(key, defaultValue);
    }

    /**
     * Remove the preference in sp.
     *
     * @param key      The key of sp.
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void remove(@NonNull final String key, final boolean isCommit) {
        if (isCommit) {
            sp.edit().remove(key).commit();
        } else {
            sp.edit().remove(key).apply();
        }
    }

    /**
     * Remove all preferences in sp.
     *
     * @param isCommit True to use {@link SharedPreferences.Editor#commit()},
     *                 false to use {@link SharedPreferences.Editor#apply()}
     */
    public void clear(final boolean isCommit) {
        if (isCommit) {
            sp.edit().clear().commit();
        } else {
            sp.edit().clear().apply();
        }
    }

    public Object get(@Nullable String key, @Nullable Object defaultObject) {
        return getValue(key, defaultObject);
    }

    public void putInt(String key, int value){
        put(key,value);
    }

    public int getInt(String key,int defaultObject){
        return (int) get(key,defaultObject);
    }

    public int getInt(String key){
        return getInt(key,0);
    }

    public void putBoolean(String key, boolean value){
        put(key,value);
    }

    public boolean getBoolean(String key,boolean defaultObject){
        return (boolean) get(key,defaultObject);
    }

    public boolean getBoolean(String key){
        return getBoolean(key,false);
    }

    public void putLong(String key, long value){
        put(key,value);
    }

    public long getLong(String key,long defaultObject){
        return (long) get(key,defaultObject);
    }

    public long getLong(String key){
        return getLong(key, 0L);
    }

    public void putFloat(String key, float value){
        put(key,value);
    }

    public float getFloat(String key,float defaultObject){
        return (float) get(key,defaultObject);
    }

    public float getFloat(String key){
        return getFloat(key,0f);
    }

    public void putDouble(String key, double value){
        put(key,value);
    }

    public double getDouble(String key,double defaultObject){
        return StringUtils.parseDouble(get(key,defaultObject));
    }

    public double getDouble(String key){
        return getDouble(key,0.0d);
    }

    public void putString(String key, String value){
        put(key,value);
    }

    public String getString(String key,String defaultObject){
        return (String) get(key,defaultObject);
    }

    public String getString(String key){
        return getString(key,"");
    }

    private boolean saveValue(@Nullable String key, @Nullable Object value) {
        if (null == sp) {
            return false;
        }
        SharedPreferences.Editor editor = sp.edit();
        if (value instanceof String) {
            return editor.putString(key, encryptDES((String) value)).commit();
        } else if (value instanceof Boolean) {
            return editor.putBoolean(key, (Boolean) value).commit();
        } else if (value instanceof Float) {
            return editor.putFloat(key, (Float) value).commit();
        } else if (value instanceof Integer) {
            return editor.putInt(key, (Integer) value).commit();
        } else if (value instanceof Long) {
            return editor.putLong(key, (Long) value).commit();
        } else if (value instanceof Double) {
            return editor.putLong(key, Double.doubleToRawLongBits((Double) value)).commit();
        }else if (value instanceof Collection){
            Collection<?> collection = (Collection<?>) value;
            if (!collection.isEmpty()) {
                Class<?> elementType = collection.iterator().next().getClass();
                return saveCollection(elementType, collection);
            } else {
                return saveEntity("");
            }
        }else {
            return saveEntity(value);
        }
    }

    private Object getValue(@Nullable String key, @Nullable Object defaultValue) {
        if (null == sp) {
            if (defaultValue instanceof String) {
                return decryptDES("");
            } else if (defaultValue instanceof Boolean) {
                return false;
            } else if (defaultValue instanceof Float) {
                return 0f;
            } else if (defaultValue instanceof Integer) {
                return 0;
            } else if (defaultValue instanceof Long) {
                return 0L;
            } else if (defaultValue instanceof Double) {
                return 0.0d;
            }else {
                return "";
            }
        }
        if (defaultValue instanceof String) {
            return decryptDES(sp.getString(key, (String) defaultValue));
        } else if (defaultValue instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Float) {
            return sp.getFloat(key, (Float) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return sp.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof Long) {
            return sp.getLong(key, (Long) defaultValue);
        } else if (defaultValue instanceof Double) {
            return Double.longBitsToDouble(sp.getLong(key, Double.doubleToRawLongBits((Double) defaultValue)));
        }else if (defaultValue instanceof Collection){
            Collection<?> collection = (Collection<?>) defaultValue;
            if (collection.isEmpty()) {
                return defaultValue;
            }
            Class<?> elementType = collection.iterator().next().getClass();
            Collection<?> ret = getCollection(elementType);
            return ret != null ? ret : defaultValue;
        }else {
            if (defaultValue != null) {
                return getEntity(defaultValue.getClass(), defaultValue);
            }else {
                return "";
            }
        }
    }

    public boolean contains(@Nullable String key) {
        return null != sp && sp.contains(key);
    }

    public boolean remove(@Nullable String key) {
        if (null == sp) {
            return false;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        return editor.commit();
    }

    public boolean clear() {
        if (null == sp) {
            return false;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        return editor.commit();
    }

    public Map<String, ?> getAll() {
        if (null == sp) {
            return null;
        }
        return sp.getAll();
    }

    private boolean saveEntity(@Nullable final Object obj) {
        if (null != obj) {
            final String innerKey = getKey(obj.getClass());
            if (StringUtils.isNotEmpty(innerKey)) {
                String value = JsonPraise.objToJson(obj);
                if (TextUtils.isEmpty(value)) {
                    return false;
                }
                return saveValue(innerKey, encryptDES(value));
            }
        }
        return false;
    }

    //保存集合
    private <T> boolean saveCollection(@Nullable final Class<? extends T> clazz, @Nullable Collection<? extends T> dataList) {
        if (null == dataList || dataList.isEmpty()) {
            return false;
        }
        final String innerKey = getKey(clazz);
        if (StringUtils.isNotEmpty(innerKey)) {
            String value = JsonPraise.objToJson(dataList);
            if (TextUtils.isEmpty(value)) {
                return false;
            }
            return saveValue(innerKey, encryptDES(value));
        }

        return false;
    }

    private String encryptDES(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        //数字类型不做加密
        if (RegexUtils.isNumber(value)){
            return value;
        }
        String encryptDes = DES.encrypt(value);
        if (StringUtils.isNotEmpty(encryptDes)) {
            return encryptDes;
        } else {
            String encrypt = AesEncryptUtil.encrypt(value);
            if (StringUtils.isNotEmpty(encrypt)) {
                return encrypt;
            } else {
                // 1. 处理null和空字符串
                if (value == null) {
                    return null;
                }
                if (value.isEmpty()) {
                    return "";
                }
                try {
                    // 2. 标准化字符编码
                    byte[] bytes;
                    bytes = value.getBytes(StandardCharsets.UTF_8);

                    // 3. 编码为URL安全的Base64（无填充）
                    return Base64.encodeToString(bytes, Base64.NO_WRAP)
                            .replace('+', '-')
                            .replace('/', '_')
                            .replace("=", "");
                } catch (Exception e) {
                    KLog.e(e.getMessage());
                    return value; // 返回原始值或根据需求返回null
                }
            }
        }
    }

    private <T> T getEntity(@Nullable final Class<? extends T> clazz, @Nullable final T defaultValue) {
        final String innerKey = getKey(clazz);
        if (!TextUtils.isEmpty(innerKey)) {
            T ret = JsonPraise.jsonToObj(decryptDES((String) getValue(innerKey, "")), clazz);
            if (null != ret) {
                return ret;
            }
        }
        return defaultValue;
    }

    //获取集合
    private <T> Collection<T> getCollection(@Nullable final Class<? extends T> clazz) {
        final String innerKey = getKey(clazz);
        if (!TextUtils.isEmpty(innerKey)) {
            Gson gson = new Gson();
            String json = decryptDES((String) getValue(innerKey, ""));
            return gson.fromJson(json, new ParameterizedTypeImpl(clazz));
        }
        return null;
    }

    private class ParameterizedTypeImpl implements ParameterizedType {
        Class clazz;

        public ParameterizedTypeImpl(@Nullable Class clz) {
            clazz = clz;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{clazz};
        }

        @Override
        public Type getRawType() {
            return Collection.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    private String decryptDES(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        //数字类型不做解密
        if (RegexUtils.isNumber(value)){
            return value;
        }
        String decryptDES = DES.desEncrypt(value);
        if (StringUtils.isNotEmpty(decryptDES)){
            return decryptDES;
        }else {
            String desEncrypt = AesEncryptUtil.desEncrypt(value);
            if (StringUtils.isNotEmpty(desEncrypt)){
                return desEncrypt;
            }else {
                String base64 = null;
                try {
                    // 2. Base64预处理
                    String processedBase64 = StringUtils.trim(value)
                            .replaceAll("[^A-Za-z0-9+/=_-]", "")
                            .replace('-', '+')
                            .replace('_', '/');

                    // 3. 补全padding
                    switch (processedBase64.length() % 4) {
                        case 2: processedBase64 += "=="; break;
                        case 3: processedBase64 += "="; break;
                    }

                    // 4. 解码
                    byte[] decodedBytes = Base64.decode(processedBase64, Base64.NO_WRAP);
                    base64 = new String(decodedBytes, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    KLog.e("Invalid Base64: " + value + ", Error: " + e.getMessage());
                    return value;
                } catch (Exception e) {
                    KLog.e("Unexpected error decoding: " + value + ", Error: " + e.getMessage());
                    return value;
                }
                return base64;
            }
        }
    }

    private String getKey(@Nullable final Class<?> clazz) {
        if (null != clazz) {
            return clazz.getName();
        }
        return null;
    }
}
