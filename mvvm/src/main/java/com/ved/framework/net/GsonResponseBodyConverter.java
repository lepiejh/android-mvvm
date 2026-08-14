package com.ved.framework.net;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.ved.framework.utils.Configure;
import com.ved.framework.utils.CorpseUtils;
import com.ved.framework.utils.JsonPraise;
import com.ved.framework.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * 响应体转换器：业务码校验 + 实体解析。
 *
 * <p>通用性设计：不写死任何响应实体类，也不使用反射。框架只依赖 {@link IEntityResponse} 约定接口——
 * 实际项目在自身代码中定义响应实体基类并实现该接口，字段名 / JSON 键名完全由项目决定
 * （Gson 按实际字段反序列化），框架通过接口读取 code / msg / data。
 *
 * <p>核心原理：Retrofit 传入的 {@code type} 即接口方法声明的返回泛型
 * （如 {@code Observable<BaseResponse<User>>} 会拿到 {@code BaseResponse<User>}），
 * 因此直接解析进 {@code type} 后做一次 {@code instanceof IEntityResponse} 判断即可。
 *
 * @param <T> 接口声明的方法返回泛型
 */
final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final Type type;

    GsonResponseBodyConverter(Gson gson, Type type) {
        this.gson = gson;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T convert(ResponseBody value) throws IOException {
        // 注意：value.string() 会消费并关闭 body，之后不可再读取 value 流
        String response = value.string();
        if (TextUtils.isEmpty(response)) {
            // 空响应：统一转业务异常，避免空串解析抛晦涩异常
            throw new ResultException("服务器返回数据为空", -1);
        }
        Object result;
        try {
            result = gson.fromJson(response, type);
        } catch (Exception e) {
            // 后台返回非预期数据（非法 JSON / 与声明类型不匹配等）时，优先从原始 JSON 中
            // 提取业务码与消息，保证错误可读、可处理，而不是把解析异常冒泡给用户。
            throw buildParseException(response);
        }
        if (result instanceof IEntityResponse<?>) {
            // 项目自定义的响应实体基类实现了约定接口：直接通过接口做业务码校验（与字段名无关）
            IEntityResponse<?> entityResponse = (IEntityResponse<?>) result;
            int code = entityResponse.getCode();
            if (code != Configure.getCode()) {
                throw new ResultException(resolveErrorMsg(entityResponse), code);
            }
            // 兜底复核：实体字段名与后台键名不一致时，getCode() 会取到默认值且恰好等于成功码，
            // 后台真实的业务失败会被误判为成功。此处以原始 JSON 中配置键名的业务码为准再校验一次。
            if (JsonPraise.hasKey(response, Configure.getCodeKey())) {
                int rawCode = StringUtils.parseInt(JsonPraise.optCode(response, Configure.getCodeKey()));
                if (rawCode != code) {
                    String rawMsg = JsonPraise.optCode(response, Configure.getMsgKey());
                    throw new ResultException(TextUtils.isEmpty(rawMsg) ? resolveErrorMsg(entityResponse) : rawMsg, rawCode);
                }
            }
            return (T) result;
        }
        // 兜底兼容：声明类型未实现接口，但响应为 {code,msg,data} 标准包装结构时，
        // 仍按配置键名做业务码校验（与声明了包装实体时的行为保持一致）
        if (CorpseUtils.INSTANCE.isStandardJson(response)) {
            int code = StringUtils.parseInt(JsonPraise.optCode(response, Configure.getCodeKey()));
            if (code != Configure.getCode()) {
                String msg = JsonPraise.optCode(response, Configure.getMsgKey());
                throw new ResultException(TextUtils.isEmpty(msg) ? "服务器异常" : msg, code);
            }
        }
        return (T) result;
    }

    /**
     * 解析失败时构造业务异常：优先提取后台返回的业务码/消息，保证错误信息可读、可处理。
     */
    private ResultException buildParseException(String response) {
        int code = StringUtils.parseInt(JsonPraise.optCode(response, Configure.getCodeKey()));
        String msg = JsonPraise.optCode(response, Configure.getMsgKey());
        if (!TextUtils.isEmpty(msg)) {
            return new ResultException(msg, code);
        }
        if (CorpseUtils.INSTANCE.isStandardJson(response)) {
            return new ResultException("数据格式异常", code);
        }
        return new ResultException("服务器返回数据格式异常", -1);
    }

    /**
     * 解析业务错误信息：优先取 data（部分后端把错误详情放在 data 字段），其次取 msg，最后兜底。
     */
    private String resolveErrorMsg(IEntityResponse<?> entityResponse) {
        Object data = entityResponse.getData();
        if (data != null) {
            String dataStr = String.valueOf(data);
            if (!TextUtils.isEmpty(dataStr)) {
                return dataStr;
            }
        }
        String msg = entityResponse.getMsg();
        return TextUtils.isEmpty(msg) ? "服务器异常" : msg;
    }
}
