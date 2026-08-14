package com.ved.framework.net;

/**
 * 业务响应体统一约定接口。
 *
 * <p>框架不绑定任何具体响应实体类（旧版写死的 {@code EntityResponse} 只是本接口的一个默认实现）。
 * 实际项目在自身代码中定义响应实体基类并实现本接口，即可接入框架统一的业务码校验；
 * 字段名 / JSON 键名完全由项目自定义（Gson 按实际字段反序列化），框架只依赖下面三个约定方法，
 * 不再使用反射、不再依赖类名与包名。
 *
 * <pre>{@code
 * public class BaseResponse<T> implements IEntityResponse<T> {
 *     private int status;          // 与后台字段对应，键名可不同（status/result/ret...）
 *     private String message;      // message/info/msg...
 *     private T result;            // result/data/obj...
 *
 *     @Override public int getCode() { return status; }
 *     @Override public String getMsg() { return message; }
 *     @Override public T getData() { return result; }
 * }
 * }</pre>
 *
 * <p>后台键名就是 code/msg/data 时，可直接复用框架自带的 {@code com.ved.framework.mode.EntityResponse}。
 * 若后台键名不是默认的 code/msg/data，还需调用 {@code Configure.setResponseKeys(...)} 配置键名，
 * 供 {@code isStandardJson} 识别与拦截器回调读取使用。
 *
 * @param <T> 业务数据类型
 */
public interface IEntityResponse<T> {

    /**
     * 业务码（成功与否由 {@code Configure.getCode()} 判断，默认 0）。
     */
    int getCode();

    /**
     * 提示消息。
     */
    String getMsg();

    /**
     * 业务数据。
     */
    T getData();
}
