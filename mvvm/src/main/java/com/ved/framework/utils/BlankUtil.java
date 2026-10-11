package com.ved.framework.utils;

import java.util.List;

/**
 * 判断各种数据是否为null的工具类
 *
 * @author L
 */
public class BlankUtil {


    /**
     * 判断text是否为null或者为空字符串
     */
    public static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    /**
     * 判断list是否为null或空数组
     */
    public static boolean isEmptyList(List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 判断obj是否为null
     */
    public static boolean isEmptyObj(Object obj) {
        return obj == null;
    }

    /**
     * 判断是否为0
     */
    public static boolean isZero(Integer integer) {
        return integer == null || integer == 0;
    }

}
