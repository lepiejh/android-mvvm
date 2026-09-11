package com.ved.framework.http;

/**
 * Created by ved on 2017/5/11.
 */

public class ResponseThrowable extends Exception {

    /** 消除 -Xlint:serial 警告；取值为 JVM 自动推导的默认值（serialver 实测），勿改。 */
    private static final long serialVersionUID = -56955830893512870L;

    public int code;
    public String message;

    public ResponseThrowable(Throwable throwable, int code) {
        super(throwable);
        this.code = code;
    }
}
