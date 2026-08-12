package com.ved.framework.http.strategy;

import android.net.ParseException;

import com.google.gson.JsonParseException;
import com.google.gson.stream.MalformedJsonException;
import com.ved.framework.http.ErrorCode;

import org.json.JSONException;

/**
 * 数据解析异常策略
 */
public class ParseExceptionStrategy extends AbstractExceptionStrategy {

    @Override
    protected boolean matchType(Throwable e) {
        return e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof ParseException
                || e instanceof MalformedJsonException;
    }

    @Override
    protected int code() {
        return ErrorCode.PARSE_ERROR;
    }

    @Override
    protected String message() {
        return "解析错误";
    }
}
