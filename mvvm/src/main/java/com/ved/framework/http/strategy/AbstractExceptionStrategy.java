package com.ved.framework.http.strategy;

import com.ved.framework.http.ResponseThrowable;

/**
 * 抽象异常策略基类
 *
 * 模板方法：封装策略的公共逻辑（类型匹配 + 结果构造），
 * 子类只需实现具体的转换逻辑。
 */
public abstract class AbstractExceptionStrategy implements IExceptionStrategy {

    @Override
    public boolean matches(Throwable e) {
        return e != null && matchType(e);
    }

    @Override
    public ResponseThrowable handle(Throwable e) {
        ResponseThrowable ex = new ResponseThrowable(e, code());
        ex.message = message();
        return ex;
    }

    /**
     * 子类实现：判断异常类型是否匹配
     */
    protected abstract boolean matchType(Throwable e);

    /**
     * 子类实现：返回错误码
     */
    protected abstract int code();

    /**
     * 子类实现：返回错误提示信息
     */
    protected abstract String message();
}
