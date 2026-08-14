package com.ved.framework.net;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.ved.framework.utils.Configure;
import com.ved.framework.utils.CorpseUtils;
import com.ved.framework.utils.JsonPraise;
import com.ved.framework.utils.KLog;
import com.ved.framework.utils.SPUtils;
import com.ved.framework.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;

final class GsonResponseBodyConverter<T> implements Converter<ResponseBody,
        T> {
    private final Gson gson;
    private final Type type;

    GsonResponseBodyConverter(Gson gson, Type type) {
        this.gson = gson;
        this.type = type;
    }

    // ==================== 享元模式：EntityResponse 解析元数据缓存 ====================
    // 反射开销较大（getDeclaredMethods 会复制整个方法数组），而 EntityResponse 类结构是固定的，
    // 因此只在类加载时解析一次，之后每次网络响应转换都复用缓存结果。
    private static final Class<?> ENTITY_RESPONSE_CLASS = resolveEntityResponseClass();
    // 返回类型为 int 的方法（业务 code），取最后一个匹配，与原逐次反射的语义保持一致
    private static final Method ENTITY_RESPONSE_CODE_METHOD = resolveMethodByReturnType("int");
    // 返回类型为 Object 的方法（错误时取 data/msg 内容），取最后一个匹配
    private static final Method ENTITY_RESPONSE_CONTENT_METHOD = resolveMethodByReturnType("Object");
    // 返回类型为 String 的方法（错误信息），取最后一个匹配
    private static final Method ENTITY_RESPONSE_MSG_METHOD = resolveMethodByReturnType("String");

    private static Class<?> resolveEntityResponseClass() {
        try {
            return Class.forName("com.ved.framework.mode.EntityResponse");
        } catch (ClassNotFoundException e) {
            KLog.e(e.getMessage());
            return null;
        }
    }

    private static Method resolveMethodByReturnType(String returnSimpleName) {
        if (ENTITY_RESPONSE_CLASS == null) {
            return null;
        }
        Method found = null;
        for (Method method : ENTITY_RESPONSE_CLASS.getDeclaredMethods()) {
            if (returnSimpleName.equals(method.getReturnType().getSimpleName())) {
                found = method;
            }
        }
        return found;
    }
    // ==========================================================================

    /**
     * 针对数据返回成功、错误不同类型字段处理
     */
    @Override
    public T convert(ResponseBody value) throws IOException {
        // 注意：value.string() 会消费并关闭 body，之后不可再读取 value 流
        String response = value.string();
        boolean isStandardJson = CorpseUtils.INSTANCE.isStandardJson(response);
        if (ENTITY_RESPONSE_CLASS == null) {
            // 该分支实际为死代码（EntityResponse 类始终存在），保留兼容逻辑
            if (isStandardJson) {
                int code = StringUtils.parseInt(JsonPraise.optCode(response, "code"));
                if (code == Configure.getCode()) {
                    try {
                        // 直接解析已取出的字符串，避免访问已关闭的 body 抛 IllegalStateException
                        return (T) gson.getAdapter(TypeToken.get(type)).fromJson(response);
                    } catch (RuntimeException e) {
                        KLog.e(e.getMessage());
                        throw new ResultException("服务器异常", -2);
                    }
                } else {
                    String pram = SPUtils.getInstance().getString("msg", "");
                    String msg = JsonPraise.optCode(response, pram);
                    throw new ResultException(msg, code);
                }
            } else {
                return gson.fromJson(response, type);
            }
        } else {
            if (isStandardJson) {
                Object result;
                try {
                    result = gson.fromJson(response, ENTITY_RESPONSE_CLASS);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    return gson.fromJson(response, type);
                }
                int code = invokeCode(ENTITY_RESPONSE_CODE_METHOD, result);
                if (code == Configure.getCode()) {
                    return gson.fromJson(response, type);
                } else {
                    // 修复：复用上方已解析的 result，避免对同一 response 二次 Gson 解析
                    Object errResponse = result;
                    if (ENTITY_RESPONSE_CONTENT_METHOD != null) {
                        String errorMsg = invokeToString(ENTITY_RESPONSE_CONTENT_METHOD, errResponse);
                        if (!TextUtils.isEmpty(errorMsg)) {
                            throw new ResultException(errorMsg, code);
                        } else if (ENTITY_RESPONSE_MSG_METHOD != null) {
                            throw new ResultException(invokeToString(ENTITY_RESPONSE_MSG_METHOD, errResponse), code);
                        } else {
                            throw new ResultException("", code);
                        }
                    } else if (ENTITY_RESPONSE_MSG_METHOD != null) {
                        throw new ResultException(invokeToString(ENTITY_RESPONSE_MSG_METHOD, errResponse), code);
                    } else {
                        throw new ResultException("服务器异常", code);
                    }
                }
            } else {
                return gson.fromJson(response, type);
            }
        }
    }

    /**
     * 反射调用方法并将结果安全转为 String（统一处理 null / String / Integer / 其他对象）
     *
     * @param method 目标方法，为 null 时直接返回 null
     * @param target 反射调用目标对象
     */
    private String invokeToString(Method method, Object target) {
        if (method == null) {
            return null;
        }
        try {
            Object o = method.invoke(target);
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof Integer) {
                return String.valueOf((int) o);
            } else if (o != null) {
                return o.toString();
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            KLog.e(e.getMessage());
        }
        return null;
    }

    /**
     * 反射调用方法并将结果安全转为 int 业务码
     *
     * @param method 目标方法，为 null 时返回 0
     * @param target 反射调用目标对象
     */
    private int invokeCode(Method method, Object target) {
        if (method == null) {
            return 0;
        }
        try {
            Object o = method.invoke(target);
            if (o instanceof Integer) {
                return (int) o;
            } else if (o instanceof String) {
                return StringUtils.parseInt((String) o);
            } else if (o != null) {
                return StringUtils.parseInt(o.toString());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            KLog.e(e.getMessage());
        }
        return 0;
    }
}
