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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    public void putByte(String key, byte value){
        put(key, value);
    }

    public byte getByte(String key, byte defaultObject){
        return (byte) get(key, defaultObject);
    }

    public byte getByte(String key){
        return getByte(key, (byte) 0);
    }

    public void putShort(String key, short value){
        put(key, value);
    }

    public short getShort(String key, short defaultObject){
        return (short) get(key, defaultObject);
    }

    public short getShort(String key){
        return getShort(key, (short) 0);
    }

    public void putChar(String key, char value){
        put(key, value);
    }

    public char getChar(String key, char defaultObject){
        return (char) get(key, defaultObject);
    }

    public char getChar(String key){
        return getChar(key, (char) 0);
    }

    public void putBytes(String key, byte[] value){
        put(key, value);
    }

    public byte[] getBytes(String key, byte[] defaultObject){
        return (byte[]) get(key, defaultObject);
    }

    public byte[] getBytes(String key){
        return getBytes(key, new byte[0]);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Map / Collection / 数组 / 对象 的便捷存取方法
    // 说明：Map、数组按传入的 key 存取（JSON 序列化后加密）；
    //      Collection、Object 沿用框架既有实现（以元素/对象的类型名为内部 key）。
    ///////////////////////////////////////////////////////////////////////////

    public void putMap(String key, Map<?, ?> value){
        put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String key, Map<K, V> defaultObject){
        return (Map<K, V>) get(key, defaultObject);
    }

    public Map<String, Object> getMap(String key){
        return getMap(key, new HashMap<String, Object>());
    }

    public void putCollection(String key, Collection<?> value){
        put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> getCollection(String key, Collection<T> defaultObject){
        return (Collection<T>) get(key, defaultObject);
    }

    public void putArray(String key, Object value){
        put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getArray(String key, T defaultObject){
        return (T) get(key, defaultObject);
    }

    public void putObject(String key, Object value){
        put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObject(String key, T defaultObject){
        return (T) get(key, defaultObject);
    }

    ///////////////////////////////////////////////////////////////////////////
    // 类 GreenDAO 的增删改查（CRUD）：面向以 List<T> 形式保存的实体集合
    // 通过 dao(entityClass, keyMapper) 获取 SpDao<T, K>，实体主键由 KeyMapper 提供
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 实体主键提取器，用于 load/update/deleteByKey/insertOrReplace 等按主键操作的方法。
     * 采用自定义函数式接口而非 java.util.function.Function，以兼容 minSdk 19。
     *
     * @param <T> 实体类型
     * @param <K> 主键类型
     */
    public interface KeyMapper<T, K> {
        K mapKey(T entity);
    }

    /**
     * 查询过滤条件，替代 GreenDAO 的 QueryBuilder/queryRaw（SharedPreferences 无 SQL）。
     *
     * @param <T> 实体类型
     */
    public interface Filter<T> {
        boolean accept(T entity);
    }

    /**
     * 获取一个类 GreenDAO 的 DAO，对以 List&lt;T&gt; 形式保存的实体集合进行增删改查。
     * 数据以 entityClass.getName() 为键存储，与 putCollection/getCollection 互通。
     *
     * @param entityClass 实体类型（相当于“表”）
     * @param keyMapper   主键提取器，例如 user -&gt; user.getId()
     * @param <T>         实体类型
     * @param <K>         主键类型
     * @return 绑定该实体类型与主键提取器的 SpDao
     */
    public <T, K> SpDao<T, K> dao(@NonNull Class<T> entityClass, @NonNull KeyMapper<T, K> keyMapper) {
        return new SpDao<>(this, entityClass, keyMapper);
    }

    /**
     * 获取一个以自定义“表名”为存储键的类 GreenDAO DAO，用于按表名做增删改查。
     * 与 {@link #dao(Class, KeyMapper)} 的区别：存储键使用传入的 tableName 而非
     * entityClass.getName()，便于同一实体类型分表存储，或使用与类名解耦的稳定表名。
     * tableName 为空时回退为 entityClass.getName()。
     *
     * @param tableName   表名（SharedPreferences 存储键），需保证唯一
     * @param entityClass 实体类型
     * @param keyMapper   主键提取器，例如 user -&gt; user.getId()
     * @param <T>         实体类型
     * @param <K>         主键类型
     * @return 绑定该表名、实体类型与主键提取器的 SpDao
     */
    public <T, K> SpDao<T, K> dao(@NonNull String tableName, @NonNull Class<T> entityClass, @NonNull KeyMapper<T, K> keyMapper) {
        return new SpDao<>(this, tableName, entityClass, keyMapper);
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
        } else if (value instanceof Byte) {
            // byte 无原生存储类型，借用 int 保存
            return editor.putInt(key, (Byte) value).commit();
        } else if (value instanceof Short) {
            // short 无原生存储类型，借用 int 保存
            return editor.putInt(key, (Short) value).commit();
        } else if (value instanceof Character) {
            // char 无原生存储类型，借用 int（Unicode 码点）保存
            return editor.putInt(key, (Character) value).commit();
        } else if (value instanceof Float) {
            return editor.putFloat(key, (Float) value).commit();
        } else if (value instanceof Integer) {
            return editor.putInt(key, (Integer) value).commit();
        } else if (value instanceof Long) {
            return editor.putLong(key, (Long) value).commit();
        } else if (value instanceof Double) {
            return editor.putLong(key, Double.doubleToRawLongBits((Double) value)).commit();
        } else if (value instanceof byte[]) {
            // 字节数组：Base64 编码后按字符串（加密）保存
            return editor.putString(key, encryptDES(Base64.encodeToString((byte[]) value, Base64.NO_WRAP))).commit();
        }else if (value instanceof Collection){
            Collection<?> collection = (Collection<?>) value;
            if (!collection.isEmpty()) {
                Class<?> elementType = collection.iterator().next().getClass();
                return saveCollection(elementType, collection);
            } else {
                return saveEntity("");
            }
        } else if (value instanceof Map || (value != null && value.getClass().isArray())) {
            // Map / 数组（int[]/long[]/String[]/对象数组等）：序列化为 JSON 后按字符串（加密）保存
            String json = JsonPraise.objToJson(value);
            if (TextUtils.isEmpty(json)) {
                return false;
            }
            return editor.putString(key, encryptDES(json)).commit();
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
            } else if (defaultValue instanceof Byte) {
                return (byte) 0;
            } else if (defaultValue instanceof Short) {
                return (short) 0;
            } else if (defaultValue instanceof Character) {
                return (char) 0;
            } else if (defaultValue instanceof Float) {
                return 0f;
            } else if (defaultValue instanceof Integer) {
                return 0;
            } else if (defaultValue instanceof Long) {
                return 0L;
            } else if (defaultValue instanceof Double) {
                return 0.0d;
            }else {
                return defaultValue != null ? defaultValue : "";
            }
        }
        if (defaultValue instanceof String) {
            return decryptDES(sp.getString(key, (String) defaultValue));
        } else if (defaultValue instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Byte) {
            return (byte) sp.getInt(key, (Byte) defaultValue);
        } else if (defaultValue instanceof Short) {
            return (short) sp.getInt(key, (Short) defaultValue);
        } else if (defaultValue instanceof Character) {
            return (char) sp.getInt(key, (Character) defaultValue);
        } else if (defaultValue instanceof Float) {
            return sp.getFloat(key, (Float) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return sp.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof Long) {
            return sp.getLong(key, (Long) defaultValue);
        } else if (defaultValue instanceof Double) {
            return Double.longBitsToDouble(sp.getLong(key, Double.doubleToRawLongBits((Double) defaultValue)));
        } else if (defaultValue instanceof byte[]) {
            // 字节数组：读取字符串并解密后 Base64 解码
            String encoded = decryptDES(sp.getString(key, ""));
            if (TextUtils.isEmpty(encoded)) {
                return defaultValue;
            }
            try {
                return Base64.decode(encoded, Base64.NO_WRAP);
            } catch (Exception e) {
                KLog.e(e.getMessage());
                return defaultValue;
            }
        }else if (defaultValue instanceof Collection){
            Collection<?> collection = (Collection<?>) defaultValue;
            if (collection.isEmpty()) {
                return defaultValue;
            }
            Class<?> elementType = collection.iterator().next().getClass();
            Collection<?> ret = getCollection(elementType);
            return ret != null ? ret : defaultValue;
        } else if (defaultValue instanceof Map
                || (defaultValue != null && defaultValue.getClass().isArray())) {
            // Map / 数组：读取字符串并解密后按运行时类型反序列化
            String json = decryptDES(sp.getString(key, ""));
            if (TextUtils.isEmpty(json)) {
                return defaultValue;
            }
            Object ret = JsonPraise.jsonToObj(json, defaultValue.getClass());
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
        return saveCollectionByKey(getKey(clazz), dataList);
    }

    //按表名（存储键）保存集合，供 SpDao 按自定义表名读写复用
    private <T> boolean saveCollectionByKey(@Nullable final String key, @Nullable Collection<? extends T> dataList) {
        if (null == dataList || dataList.isEmpty()) {
            return false;
        }
        if (StringUtils.isNotEmpty(key)) {
            String value = JsonPraise.objToJson(dataList);
            if (TextUtils.isEmpty(value)) {
                return false;
            }
            return saveValue(key, encryptDES(value));
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
        String encryptDes = ThreeDesCbcUtil.encrypt(value);
        if (StringUtils.isNotEmpty(encryptDes)) {
            return encryptDes;
        } else {
            String encrypt = AesCbcUtil.encrypt(value);
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
                    return CryptoHelper.urlSafeBase64Encode(value.getBytes(StandardCharsets.UTF_8));
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
        return getCollectionByKey(getKey(clazz), clazz);
    }

    //按表名（存储键）获取集合，供 SpDao 按自定义表名读写复用
    private <T> Collection<T> getCollectionByKey(@Nullable final String key, @Nullable final Class<? extends T> clazz) {
        if (!TextUtils.isEmpty(key)) {
            Gson gson = new Gson();
            String json = decryptDES((String) getValue(key, ""));
            return gson.fromJson(json, new ParameterizedTypeImpl(clazz));
        }
        return null;
    }

    private static final class ParameterizedTypeImpl implements ParameterizedType {
        private final Class<?> clazz;

        ParameterizedTypeImpl(@Nullable final Class<?> clz) {
            this.clazz = clz;
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
        String decryptDES = ThreeDesCbcUtil.decrypt(value);
        if (StringUtils.isNotEmpty(decryptDES)){
            return decryptDES;
        }else {
            String desEncrypt = AesCbcUtil.decrypt(value);
            if (StringUtils.isNotEmpty(desEncrypt)){
                return desEncrypt;
            }else {
                String base64 = null;
                try {
                    String processedBase64 = CryptoHelper.preprocessBase64(value);
                    if (processedBase64 == null) {
                        KLog.e("Invalid Base64: " + value);
                        return value;
                    }
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

    /**
     * 类 GreenDAO 的 DAO 实现，面向以 List&lt;T&gt; 形式保存在 SharedPreferences 中的实体集合。
     * <p>
     * 与 GreenDAO 的差异：
     * <ul>
     *     <li>存储：整个 List&lt;T&gt; 序列化为 JSON 加密后存于键 entityClass.getName()，
     *         与 putCollection/getCollection 互通；空表以“移除该键”表示。</li>
     *     <li>无 SQLite rowId / SQL / 会话缓存，故不提供 queryBuilder、queryRaw、
     *         loadByRowId、detach、insertWithoutSettingPk 等方法；
     *         以 query(Filter)、loadByIndex(int) 等作为替代。</li>
     *     <li>...InTx 批量方法采用“一次读取 + 一次写入（commit）”，即 SP 下的事务近似。</li>
     *     <li>所有写操作对 lock 加锁，保证进程内“读-改-写”原子性。</li>
     * </ul>
     *
     * @param <T> 实体类型
     * @param <K> 主键类型
     */
    public static final class SpDao<T, K> {

        private final SPUtils sp;
        private final String tableName;
        private final Class<T> entityClass;
        private final KeyMapper<T, K> keyMapper;
        private final Object lock = new Object();

        private SpDao(SPUtils sp, Class<T> entityClass, KeyMapper<T, K> keyMapper) {
            this(sp, entityClass.getName(), entityClass, keyMapper);
        }

        private SpDao(SPUtils sp, @Nullable String tableName, Class<T> entityClass, KeyMapper<T, K> keyMapper) {
            this.sp = sp;
            this.entityClass = entityClass;
            this.keyMapper = keyMapper;
            this.tableName = TextUtils.isEmpty(tableName) ? entityClass.getName() : tableName;
        }

        // ---------------- 内部持久化辅助（复用 SPUtils 集合存取） ----------------

        private List<T> readTable() {
            Collection<T> c = sp.getCollectionByKey(tableName, entityClass);
            return c != null ? new ArrayList<>(c) : new ArrayList<T>();
        }

        private boolean writeTable(List<T> list) {
            if (list == null || list.isEmpty()) {
                return sp.remove(tableName);
            }
            return sp.saveCollectionByKey(tableName, list);
        }

        private int indexOfKey(List<T> list, K key) {
            if (key == null) {
                return -1;
            }
            for (int i = 0; i < list.size(); i++) {
                T item = list.get(i);
                if (item != null && key.equals(keyMapper.mapKey(item))) {
                    return i;
                }
            }
            return -1;
        }

        private int indexOfEntity(List<T> list, T entity) {
            int i = indexOfKey(list, keyMapper.mapKey(entity));
            if (i >= 0) {
                return i;
            }
            // 主键为 null 或未命中时，退化为 equals 匹配
            return list.indexOf(entity);
        }

        // ---------------- 元信息 ----------------

        public Class<T> entityClass() {
            return entityClass;
        }

        public String tableName() {
            return tableName;
        }

        // ---------------- Create / Insert ----------------

        public boolean insert(T entity) {
            if (entity == null) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                list.add(entity);
                return writeTable(list);
            }
        }

        public boolean insertOrReplace(T entity) {
            if (entity == null) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                int i = indexOfKey(list, keyMapper.mapKey(entity));
                if (i >= 0) {
                    list.set(i, entity);
                } else {
                    list.add(entity);
                }
                return writeTable(list);
            }
        }

        public boolean insertInTx(List<T> entities) {
            if (entities == null || entities.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                list.addAll(entities);
                return writeTable(list);
            }
        }

        @SafeVarargs
        public final boolean insertInTx(T... entities) {
            if (entities == null || entities.length == 0) {
                return false;
            }
            return insertInTx(Arrays.asList(entities));
        }

        public boolean insertOrReplaceInTx(List<T> entities) {
            if (entities == null || entities.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                for (T entity : entities) {
                    if (entity == null) {
                        continue;
                    }
                    int i = indexOfKey(list, keyMapper.mapKey(entity));
                    if (i >= 0) {
                        list.set(i, entity);
                    } else {
                        list.add(entity);
                    }
                }
                return writeTable(list);
            }
        }

        @SafeVarargs
        public final boolean insertOrReplaceInTx(T... entities) {
            if (entities == null || entities.length == 0) {
                return false;
            }
            return insertOrReplaceInTx(Arrays.asList(entities));
        }

        // ---------------- Update ----------------

        public boolean update(T entity) {
            if (entity == null) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                int i = indexOfKey(list, keyMapper.mapKey(entity));
                if (i < 0) {
                    return false;
                }
                list.set(i, entity);
                return writeTable(list);
            }
        }

        public boolean updateInTx(List<T> entities) {
            if (entities == null || entities.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                boolean changed = false;
                for (T entity : entities) {
                    if (entity == null) {
                        continue;
                    }
                    int i = indexOfKey(list, keyMapper.mapKey(entity));
                    if (i >= 0) {
                        list.set(i, entity);
                        changed = true;
                    }
                }
                return changed && writeTable(list);
            }
        }

        @SafeVarargs
        public final boolean updateInTx(T... entities) {
            if (entities == null || entities.length == 0) {
                return false;
            }
            return updateInTx(Arrays.asList(entities));
        }

        // ---------------- Delete ----------------

        public boolean delete(T entity) {
            if (entity == null) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                int i = indexOfEntity(list, entity);
                if (i < 0) {
                    return false;
                }
                list.remove(i);
                return writeTable(list);
            }
        }

        public boolean deleteByKey(K key) {
            synchronized (lock) {
                List<T> list = readTable();
                int i = indexOfKey(list, key);
                if (i < 0) {
                    return false;
                }
                list.remove(i);
                return writeTable(list);
            }
        }

        public int deleteAll() {
            synchronized (lock) {
                int removed = readTable().size();
                sp.remove(tableName);
                return removed;
            }
        }

        public boolean deleteInTx(List<T> entities) {
            if (entities == null || entities.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                boolean changed = false;
                for (T entity : entities) {
                    if (entity == null) {
                        continue;
                    }
                    int i = indexOfEntity(list, entity);
                    if (i >= 0) {
                        list.remove(i);
                        changed = true;
                    }
                }
                return changed && writeTable(list);
            }
        }

        @SafeVarargs
        public final boolean deleteInTx(T... entities) {
            if (entities == null || entities.length == 0) {
                return false;
            }
            return deleteInTx(Arrays.asList(entities));
        }

        public boolean deleteByKeyInTx(List<K> keys) {
            if (keys == null || keys.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = readTable();
                boolean changed = false;
                for (K key : keys) {
                    int i = indexOfKey(list, key);
                    if (i >= 0) {
                        list.remove(i);
                        changed = true;
                    }
                }
                return changed && writeTable(list);
            }
        }

        @SafeVarargs
        public final boolean deleteByKeyInTx(K... keys) {
            if (keys == null || keys.length == 0) {
                return false;
            }
            return deleteByKeyInTx(Arrays.asList(keys));
        }

        // ---------------- Read / Query ----------------

        public T load(K key) {
            List<T> list = readTable();
            int i = indexOfKey(list, key);
            return i >= 0 ? list.get(i) : null;
        }

        public List<T> loadAll() {
            return readTable();
        }

        public long count() {
            return readTable().size();
        }

        public T refresh(T entity) {
            if (entity == null) {
                return null;
            }
            return load(keyMapper.mapKey(entity));
        }

        public T loadByIndex(int index) {
            List<T> list = readTable();
            if (index < 0 || index >= list.size()) {
                return null;
            }
            return list.get(index);
        }

        public List<T> query(Filter<T> filter) {
            List<T> result = new ArrayList<>();
            if (filter == null) {
                return result;
            }
            for (T entity : readTable()) {
                if (entity != null && filter.accept(entity)) {
                    result.add(entity);
                }
            }
            return result;
        }

        public boolean exists(T entity) {
            if (entity == null) {
                return false;
            }
            return indexOfEntity(readTable(), entity) >= 0;
        }

        public boolean existsByKey(K key) {
            return indexOfKey(readTable(), key) >= 0;
        }
    }
}
