package com.ved.framework.net;

import java.io.IOException;

class ResultException extends IOException {

    /** 消除 -Xlint:serial 警告；取值为 JVM 自动推导的默认值（serialver 实测），勿改。
     *  混淆规则里的 {@code static final long serialVersionUID} 会保住本字段名。 */
    private static final long serialVersionUID = -7552585072612350525L;

    private String errMsg;
    private int errCode;

    public ResultException(String errMsg, int errCode){
        this.errMsg = errMsg;
        this.errCode = errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }
}
