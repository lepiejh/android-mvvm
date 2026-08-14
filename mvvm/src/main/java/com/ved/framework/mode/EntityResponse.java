package com.ved.framework.mode;

import com.ved.framework.net.IEntityResponse;

/**
 * 默认业务响应实体（对应后台 {code, msg, data} 结构）。
 *
 * <p>仅作为框架内置的默认实现，供后台键名恰好为 code/msg/data 的项目直接使用；
 * 键名不同的项目可自行定义响应实体基类并实现 {@link IEntityResponse} 接口。
 *
 * @param <T> 业务数据类型
 */
public class EntityResponse<T> implements IEntityResponse<T> {

    private int code;
    private String msg;
    private T data;

    @Override
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
