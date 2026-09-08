package com.ved.framework.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.tencent.mmkv.MMKV;

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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于 MMKV（内存映射）的键值存储，完全替代 SharedPreferences：读写性能远高于 SP，且无 XML 全量解析开销。
 * 默认实例为单进程模式；跨进程读写使用 {@link #getMultiProcessInstance()}（MULTI_PROCESS_MODE），
 * 多进程对同一 spName 的读写互相可见。
 * 升级兼容：旧版本 SharedPreferences XML 中的同名数据，在首次创建实例时自动一次性导入 MMKV。
 */
@SuppressLint("ApplySharedPref")
public final class SPUtils {

    private static final Map<String, SPUtils> sSPMap = new HashMap<>();
    private static boolean sMmkvInited;
    /** 底层存储：MMKV（内存映射），完全替代 SharedPreferences。 */
    private final MMKV sp;
    /** 变更监听（SpDao 缓存失效用）：MMKV 不实现 SP 监听回调，改由本类在写路径自行派发。 */
    private final List<SharedPreferences.OnSharedPreferenceChangeListener> changeListeners = new CopyOnWriteArrayList<>();
    /** 按表名缓存的 SpDao 实例，使内存写穿缓存与变更监听在多次 dao() 调用之间存活。 */
    private final Map<String, SpDao<?, ?>> daoCache = new HashMap<>();

    public static SPUtils getInstance() {
        return getInstance("");
    }

    public static SPUtils getInstance(@Nullable String spName) {
        return obtainInstance(spName, false);
    }

    /**
     * Return the single {@link SPUtils} instance
     *
     * @param mode 仅 {@link Context#MODE_MULTI_PROCESS} 创建跨进程实例，其余取值均为单进程
     * @return the single {@link SPUtils} instance
     */
    public static SPUtils getInstance(final int mode) {
        return getInstance("", mode);
    }

    /**
     * Return the single {@link SPUtils} instance
     *
     * @param spName The name of sp.
     * @param mode   仅 {@link Context#MODE_MULTI_PROCESS} 创建跨进程实例，其余取值均为单进程
     * @return the single {@link SPUtils} instance
     */
    public static SPUtils getInstance(@Nullable String spName, final int mode) {
        return obtainInstance(spName, mode == Context.MODE_MULTI_PROCESS);
    }

    /**
     * 跨进程实例：底层 MMKV 以 {@link MMKV#MULTI_PROCESS_MODE} 打开，
     * 多个进程对同一 spName 的读写互相可见（MMKV 自动处理进程间同步）。
     *
     * @return 跨进程的 {@link SPUtils} 实例
     */
    public static SPUtils getMultiProcessInstance() {
        return getMultiProcessInstance("");
    }

    /**
     * 跨进程实例，见 {@link #getMultiProcessInstance()}。
     *
     * @param spName The name of sp.
     * @return 跨进程的 {@link SPUtils} 实例
     */
    public static SPUtils getMultiProcessInstance(@Nullable String spName) {
        return obtainInstance(spName, true);
    }

    /**
     * 取得（或创建并缓存）指定名称与进程模式的实例。
     * 缓存键含进程模式，避免单进程/跨进程请求返回错误模式的实例。
     */
    private static SPUtils obtainInstance(@Nullable String spName, final boolean multiProcess) {
        if (isSpace(spName)) spName = "spUtils";
        final String cacheKey = spName + (multiProcess ? "#multi" : "#single");
        synchronized (sSPMap) {
            SPUtils instance = sSPMap.get(cacheKey);
            if (instance == null) {
                instance = new SPUtils(spName, multiProcess);
                sSPMap.put(cacheKey, instance);
            }
            return instance;
        }
    }

    private SPUtils(@Nullable final String spName, final boolean multiProcess) {
        ensureMmkvInit();
        sp = MMKV.mmkvWithID(spName, multiProcess ? MMKV.MULTI_PROCESS_MODE : MMKV.SINGLE_PROCESS_MODE);
        migrateLegacySp(spName);
    }

    /** MMKV 使用前必须初始化；框架启动路径已初始化过一次，此处兜底且幂等。 */
    private static void ensureMmkvInit() {
        ensureMmkvInit(Utils.getContext());
    }

    /**
     * 确保 MMKV 已初始化（幂等），供框架内其他组件（cookie 存储、崩溃记录等）在使用 MMKV 前调用。
     *
     * @param context 任意 Context，内部取 ApplicationContext
     */
    public static synchronized void ensureMmkvInit(@NonNull Context context) {
        if (!sMmkvInited) {
            MMKV.initialize(context.getApplicationContext());
            sMmkvInited = true;
        }
    }

    /**
     * 升级兼容：旧版本以 SharedPreferences XML 存储的同名数据，在 MMKV 侧为空时一次性导入，
     * 导入成功后清空旧 XML，避免重复迁移。仅首次创建实例时执行。
     */
    private void migrateLegacySp(@Nullable final String spName) {
        try {
            if (sp.count() > 0) {
                return;
            }
            SharedPreferences legacy = Utils.getContext().getSharedPreferences(spName, Context.MODE_PRIVATE);
            if (sp.importFromSharedPreferences(legacy) > 0) {
                legacy.edit().clear().commit();
            }
        } catch (Exception e) {
            KLog.e(e.getMessage());
        }
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
    // 为保证加密/解密语义一致，put 系列统一走 saveValue；带 isCommit 的重载按其值选择 commit()/apply()
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
        saveValue(key, value, isCommit);
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
        saveValue(key, value, isCommit);
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
        saveValue(key, value, isCommit);
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
        saveValue(key, value, isCommit);
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
        saveValue(key, value, isCommit);
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
        notifyChanged(key);
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
        notifyChanged(null);
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
        return obtainDao(entityClass.getName(), entityClass, keyMapper, false);
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
        return obtainDao(tableName, entityClass, keyMapper, false);
    }

    /**
     * {@link #dao(Class, KeyMapper)} 的异步写版本：写操作使用 apply() 而非 commit()，
     * 不在调用线程上阻塞等待落盘，适合主线程或频繁写入场景（规避卡顿/ANR）。
     * 读逻辑与缓存机制与 dao(...) 完全一致。
     *
     * @param entityClass 实体类型（相当于“表”）
     * @param keyMapper   主键提取器
     * @return 使用异步写的 SpDao
     */
    public <T, K> SpDao<T, K> daoAsync(@NonNull Class<T> entityClass, @NonNull KeyMapper<T, K> keyMapper) {
        return obtainDao(entityClass.getName(), entityClass, keyMapper, true);
    }

    /**
     * {@link #dao(String, Class, KeyMapper)} 的异步写版本：按自定义表名 + apply() 异步写。
     *
     * @param tableName   表名（SharedPreferences 存储键）
     * @param entityClass 实体类型
     * @param keyMapper   主键提取器
     * @return 使用异步写的 SpDao
     */
    public <T, K> SpDao<T, K> daoAsync(@NonNull String tableName, @NonNull Class<T> entityClass, @NonNull KeyMapper<T, K> keyMapper) {
        return obtainDao(tableName, entityClass, keyMapper, true);
    }

    /**
     * 取得（或创建并缓存）绑定某表名的 SpDao。相同 (表名, 读写模式) 复用同一实例，
     * 从而让其内存写穿缓存与 SharedPreferences 变更监听在多次调用之间持续生效。
     * 注意：同一表名以首次创建时的 entityClass/keyMapper 为准（与 GreenDAO 会话按类型缓存 DAO 一致），
     * 因此同一表名应始终对应同一实体类型与主键提取器。
     */
    @SuppressWarnings("unchecked")
    private <T, K> SpDao<T, K> obtainDao(@Nullable String tableName, @NonNull Class<T> entityClass,
                                         @NonNull KeyMapper<T, K> keyMapper, final boolean async) {
        final String resolved = TextUtils.isEmpty(tableName) ? entityClass.getName() : tableName;
        final String cacheKey = resolved + (async ? "#async" : "#sync");
        synchronized (daoCache) {
            SpDao<?, ?> existing = daoCache.get(cacheKey);
            if (existing != null) {
                return (SpDao<T, K>) existing;
            }
            SpDao<T, K> created = new SpDao<>(this, resolved, entityClass, keyMapper, async);
            daoCache.put(cacheKey, created);
            return created;
        }
    }

    private boolean saveValue(@Nullable String key, @Nullable Object value) {
        return saveValue(key, value, false);
    }

    /**
     * 保存任意类型值。{@code isCommit} 为 true 走同步 {@link SharedPreferences.Editor#commit()}，
     * 为 false 走异步 {@link SharedPreferences.Editor#apply()}（MMKV 下由系统择机落盘，避免主线程强制刷盘）。
     * Collection/Map/数组/实体等复杂类型委托各自的保存逻辑，isCommit 仅作用于基础类型的直接写入分支。
     */
    private boolean saveValue(@Nullable String key, @Nullable Object value, final boolean isCommit) {
        if (null == sp) {
            return false;
        }
        SharedPreferences.Editor editor = sp.edit();
        if (value instanceof String) {
            editor.putString(key, encryptDES((String) value));
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Byte) {
            // byte 无原生存储类型，借用 int 保存
            editor.putInt(key, (Byte) value);
        } else if (value instanceof Short) {
            // short 无原生存储类型，借用 int 保存
            editor.putInt(key, (Short) value);
        } else if (value instanceof Character) {
            // char 无原生存储类型，借用 int（Unicode 码点）保存
            editor.putInt(key, (Character) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Double) {
            editor.putLong(key, Double.doubleToRawLongBits((Double) value));
        } else if (value instanceof byte[]) {
            // 字节数组：Base64 编码后按字符串（加密）保存
            editor.putString(key, encryptDES(Base64.encodeToString((byte[]) value, Base64.NO_WRAP)));
        } else if (value instanceof Collection) {
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
            editor.putString(key, encryptDES(json));
        } else {
            return saveEntity(value);
        }
        return commitOrApply(editor, isCommit);
    }

    /** 按 isCommit 选择同步 commit() 或异步 apply()；apply() 无返回值，视为成功返回 true。 */
    private boolean commitOrApply(@NonNull final SharedPreferences.Editor editor, final boolean isCommit) {
        if (isCommit) {
            return editor.commit();
        }
        editor.apply();
        return true;
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
        boolean ok = editor.commit();
        if (ok) {
            notifyChanged(key);
        }
        return ok;
    }

    public boolean clear() {
        if (null == sp) {
            return false;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        boolean ok = editor.commit();
        if (ok) {
            notifyChanged(null);
        }
        return ok;
    }

    /**
     * MMKV 因类型擦除不实现原生 getAll()（调用会抛异常），此处基于 allKeys() 逐键探测类型还原：
     * 依次尝试 String/StringSet/byte[]/Integer/Long/Float/Double，均不命中按 Boolean 返回。
     * 仅供遍历场景使用；已知类型时请用具体的 get 方法（更快且类型精确）。
     */
    public Map<String, ?> getAll() {
        Map<String, Object> result = new HashMap<>();
        String[] keys = sp.allKeys();
        if (keys == null) {
            return result;
        }
        for (String key : keys) {
            if (key != null) {
                result.put(key, decodeRawValue(key));
            }
        }
        return result;
    }

    private Object decodeRawValue(@NonNull final String key) {
        String str = sp.decodeString(key, null);
        if (str != null) {
            return str;
        }
        Set<String> strSet = sp.decodeStringSet(key, null);
        if (strSet != null) {
            return strSet;
        }
        byte[] bytes = sp.decodeBytes(key, null);
        if (bytes != null) {
            return bytes;
        }
        int intValue = sp.decodeInt(key, Integer.MIN_VALUE);
        if (intValue != Integer.MIN_VALUE) {
            return intValue;
        }
        long longValue = sp.decodeLong(key, Long.MIN_VALUE);
        if (longValue != Long.MIN_VALUE) {
            return longValue;
        }
        float floatValue = sp.decodeFloat(key, Float.NaN);
        if (!Float.isNaN(floatValue)) {
            return floatValue;
        }
        double doubleValue = sp.decodeDouble(key, Double.NaN);
        if (!Double.isNaN(doubleValue)) {
            return doubleValue;
        }
        return sp.decodeBool(key, false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // MMKV 原生能力（合并自已移除的 MMKVUtils）
    ///////////////////////////////////////////////////////////////////////////

    /** 返回底层 MMKV 实例供高级用法直接使用；注意绕过本类写路径的写入不会触发 SpDao 缓存失效通知。 */
    public MMKV mmkv() {
        return sp;
    }

    public String[] allKeys() {
        return sp.allKeys();
    }

    public long totalSize() {
        return sp.totalSize();
    }

    public long actualSize() {
        return sp.actualSize();
    }

    public void removeValuesForKeys(@Nullable final String[] keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        sp.removeValuesForKeys(keys);
        notifyChanged(null);
    }

    /** 将内存中变更同步刷盘。MMKV 默认自动刷盘，一般无需手动调用。 */
    public void sync() {
        sp.sync();
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
        return saveTable(key, dataList, false);
    }

    /**
     * DAO 专用：按表名写入集合，可选同步 commit 或异步 apply。
     * on-disk 格式与 saveValue 的 String 分支一致（JSON 双重加密），
     * 因此与 putCollection/getCollection 完全互通。
     *
     * @param commit true 用 commit()（阻塞至落盘，返回真实结果）；false 用 apply()（异步落盘，乐观返回 true）
     */
    private boolean saveTable(@Nullable final String key, @Nullable final Collection<?> data, final boolean commit) {
        if (null == sp || null == data || data.isEmpty()) {
            return false;
        }
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        String value = JsonPraise.objToJson(data);
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        SharedPreferences.Editor editor = sp.edit().putString(key, encryptDES(encryptDES(value)));
        if (commit) {
            boolean ok = editor.commit();
            if (ok) {
                notifyChanged(key);
            }
            return ok;
        }
        editor.apply();
        notifyChanged(key);
        return true;
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
                // 方法入口已拦截 null/空串，此处直接做 Base64 兜底
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

        @NonNull
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{clazz};
        }

        @NonNull
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
     * 供 SpDao 注册变更监听，用于写入时失效内存缓存。
     * MMKV 原生不实现 OnSharedPreferenceChangeListener（调用会抛异常），
     * 改由本类在影响表键的写路径（saveTable/remove/clear/removeValuesForKeys）同步派发；
     * key 为 null 表示批量变更（如 clear），监听方应无条件失效。
     */
    private void registerSpChangeListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {
        changeListeners.add(listener);
    }

    /** 派发变更事件；仅在写路径成功后调用，回调线程即写入线程（与原 SP commit 监听语义一致）。 */
    private void notifyChanged(@Nullable final String key) {
        if (changeListeners.isEmpty()) {
            return;
        }
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : changeListeners) {
            listener.onSharedPreferenceChanged(sp, key);
        }
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
    public static final class SpDao<T, K> implements SharedPreferences.OnSharedPreferenceChangeListener {

        private final SPUtils sp;
        private final String tableName;
        private final Class<T> entityClass;
        private final KeyMapper<T, K> keyMapper;
        private final Object lock = new Object();
        /** true：写入用 apply()（异步落盘，不阻塞调用线程）；false：写入用 commit()（同步落盘）。 */
        private final boolean async;

        // ---------------- 内存写穿缓存（copy-on-write） ----------------

        /**
         * 表快照 + 主键索引，作为整体通过单个 volatile 字段发布，
         * 避免 list 与 index 分别 volatile 时读到不一致组合。null 表示未加载或已失效。
         */
        private static final class TableSnapshot<E, Key> {
            final List<E> list;             // 发布后不再原地修改（写时复制）
            final Map<Key, Integer> index;  // 主键 -> list 下标，供 load/existsByKey O(1) 命中
            TableSnapshot(List<E> list, Map<Key, Integer> index) {
                this.list = list;
                this.index = index;
            }
        }

        private volatile TableSnapshot<T, K> table;
        /** 标记“本实例自身正在写入”，用于抑制自身写入触发的缓存失效。 */
        private volatile boolean selfWrite;

        private SpDao(SPUtils sp, String tableName, Class<T> entityClass, KeyMapper<T, K> keyMapper, boolean async) {
            this.sp = sp;
            this.tableName = tableName;
            this.entityClass = entityClass;
            this.keyMapper = keyMapper;
            this.async = async;
            sp.registerSpChangeListener(this);
        }

        /**
         * 变更回调：本表被“外部”写入（如 putCollection）或批量变更时失效内存缓存，保证进程内一致；
         * 自身写入期间（selfWrite）不失效，保留写穿缓存。
         * 事件由 SPUtils 写路径同步派发（MMKV 原生不支持 SP 监听回调）。
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // key == null 表示批量变更（clear 等）：无条件失效；
            // 否则仅当本表被“外部”改动时失效；自身写入（selfWrite）保留写穿缓存。
            // 注意：多进程实例下其他进程的写入不会触发本回调（MMKV 限制），跨进程写后需 refresh() 主动重载。
            if (key == null || (tableName.equals(key) && !selfWrite)) {
                table = null;
            }
        }

        // ---------------- 内部持久化辅助（复用 SPUtils 集合存取） ----------------

        /** 从磁盘加载（解密 + Gson 解析），仅在缓存缺失时调用。 */
        private List<T> loadFromDisk() {
            Collection<T> c = sp.getCollectionByKey(tableName, entityClass);
            return c != null ? new ArrayList<>(c) : new ArrayList<T>();
        }

        private Map<K, Integer> buildIndex(List<T> list) {
            Map<K, Integer> map = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                T item = list.get(i);
                if (item == null) {
                    continue;
                }
                K k = keyMapper.mapKey(item);
                if (k != null && !map.containsKey(k)) {
                    map.put(k, i);   // 主键重复时保留首个，与 indexOfKey 行为一致
                }
            }
            return map;
        }

        /** 返回当前表快照；必要时加载并建立索引。调用方不得原地修改返回的 list。 */
        private TableSnapshot<T, K> snapshot() {
            TableSnapshot<T, K> t = table;
            if (t != null) {
                return t;
            }
            synchronized (lock) {
                if (table == null) {
                    List<T> loaded = loadFromDisk();
                    table = new TableSnapshot<>(loaded, buildIndex(loaded));
                }
                return table;
            }
        }

        /** 主动失效缓存，下次读取将从磁盘重新加载（refresh 使用）。 */
        private void invalidate() {
            table = null;
        }

        /**
         * 持久化新表并写穿缓存（发布新的不可变快照）。须在持有 lock 时调用；
         * newList 将成为新快照，调用方之后不得再修改它。
         */
        private boolean persist(List<T> newList) {
            final List<T> safe = (newList != null) ? newList : new ArrayList<T>();
            boolean ok;
            selfWrite = true;
            try {
                if (safe.isEmpty()) {
                    if (async) {
                        sp.remove(tableName, false);
                        ok = true;
                    } else {
                        ok = sp.remove(tableName);
                    }
                } else {
                    ok = sp.saveTable(tableName, safe, !async);
                }
            } finally {
                selfWrite = false;
            }
            table = new TableSnapshot<>(safe, buildIndex(safe));
            return ok;
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
                List<T> list = new ArrayList<>(snapshot().list);
                list.add(entity);
                return persist(list);
            }
        }

        public boolean insertOrReplace(T entity) {
            if (entity == null) {
                return false;
            }
            synchronized (lock) {
                List<T> list = new ArrayList<>(snapshot().list);
                int i = indexOfKey(list, keyMapper.mapKey(entity));
                if (i >= 0) {
                    list.set(i, entity);
                } else {
                    list.add(entity);
                }
                return persist(list);
            }
        }

        public boolean insertInTx(List<T> entities) {
            if (entities == null || entities.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = new ArrayList<>(snapshot().list);
                list.addAll(entities);
                return persist(list);
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
                List<T> list = new ArrayList<>(snapshot().list);
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
                return persist(list);
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
                List<T> list = new ArrayList<>(snapshot().list);
                int i = indexOfKey(list, keyMapper.mapKey(entity));
                if (i < 0) {
                    return false;
                }
                list.set(i, entity);
                return persist(list);
            }
        }

        public boolean updateInTx(List<T> entities) {
            if (entities == null || entities.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = new ArrayList<>(snapshot().list);
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
                return changed && persist(list);
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
                List<T> list = new ArrayList<>(snapshot().list);
                int i = indexOfEntity(list, entity);
                if (i < 0) {
                    return false;
                }
                list.remove(i);
                return persist(list);
            }
        }

        public boolean deleteByKey(K key) {
            synchronized (lock) {
                List<T> list = new ArrayList<>(snapshot().list);
                int i = indexOfKey(list, key);
                if (i < 0) {
                    return false;
                }
                list.remove(i);
                return persist(list);
            }
        }

        public int deleteAll() {
            synchronized (lock) {
                int removed = snapshot().list.size();
                selfWrite = true;
                try {
                    if (async) {
                        sp.remove(tableName, false);
                    } else {
                        sp.remove(tableName);
                    }
                } finally {
                    selfWrite = false;
                }
                table = new TableSnapshot<>(new ArrayList<T>(), new HashMap<K, Integer>());
                return removed;
            }
        }

        public boolean deleteInTx(List<T> entities) {
            if (entities == null || entities.isEmpty()) {
                return false;
            }
            synchronized (lock) {
                List<T> list = new ArrayList<>(snapshot().list);
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
                return changed && persist(list);
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
                List<T> list = new ArrayList<>(snapshot().list);
                boolean changed = false;
                for (K key : keys) {
                    int i = indexOfKey(list, key);
                    if (i >= 0) {
                        list.remove(i);
                        changed = true;
                    }
                }
                return changed && persist(list);
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
        // 读操作命中内存写穿缓存：无解密、无 JSON 解析、无磁盘 I/O；
        // load/existsByKey 借助主键索引 O(1) 命中。

        /** 按主键加载实体；命中缓存索引，O(1)。未找到返回 null。 */
        public T load(K key) {
            if (key == null) {
                return null;
            }
            TableSnapshot<T, K> t = snapshot();
            Integer i = t.index.get(key);
            return (i != null && i >= 0 && i < t.list.size()) ? t.list.get(i) : null;
        }

        /** 返回全部实体的快照副本（修改返回值不影响缓存）。 */
        public List<T> loadAll() {
            return new ArrayList<>(snapshot().list);
        }

        public long count() {
            return snapshot().list.size();
        }

        /** 强制绕过缓存从磁盘重读后，返回该实体的最新持久化版本。 */
        public T refresh(T entity) {
            if (entity == null) {
                return null;
            }
            invalidate();
            return load(keyMapper.mapKey(entity));
        }

        public T loadByIndex(int index) {
            List<T> list = snapshot().list;
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
            for (T entity : snapshot().list) {
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
            return indexOfEntity(snapshot().list, entity) >= 0;
        }

        /** 按主键判断是否存在；命中缓存索引，O(1)。 */
        public boolean existsByKey(K key) {
            if (key == null) {
                return false;
            }
            return snapshot().index.containsKey(key);
        }
    }
}
