package com.ved.framework.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author: Lyb
 * on: 2020/6/11
 * @ClassInfo:
 */
public class BigDecimalUtils {
    /**
     * 保留两位小数
     *
     * @param value
     * @return
     */
    public static String toDecimal(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.toString();
    }

    public static String toDecimal(BigDecimal value) {
        if (BlankUtil.isEmptyObj(value)) {
            return "";
        }
        value = value.setScale(2, BigDecimal.ROUND_HALF_UP);
        return value.toString();
    }

    public static String toDecimal(BigDecimal value, int point) {
        if (BlankUtil.isEmptyObj(value)) {
            return "";
        }
        value = value.setScale(point, BigDecimal.ROUND_DOWN);
        return value.toString();
    }

    public static String toEndDecimal(BigDecimal value) {
        if (BlankUtil.isEmptyObj(value)) {
            return "";
        }
        return toDecimal(value, 2).substring(toDecimal(value, 2).indexOf("."));
    }

    /**
     * 保留两位小数
     *
     * @param value
     * @param point
     * @return
     */
    public static String toDecimal(String value, int point) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.toString();
    }

    /**
     * 保留两位小数
     *
     * @param value
     * @param point
     * @return
     */
    public static String toDecimal(float value, int point) {
        double d = Double.parseDouble(String.valueOf(value));  // 保证精度不丢失
        BigDecimal bd = new BigDecimal(d);
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.toString();
    }

    /**
     * 3      * 提供精确加法计算的add方法
     * 4      * @param value1 被加数
     * 5      * @param value2 加数
     * 6      * @return 两个参数的和
     * 7
     */
    public static double add(String value1, String value2) {
        BigDecimal b1 = new BigDecimal(value1);
        BigDecimal b2 = new BigDecimal(value2);
        return b1.add(b2).doubleValue();
    }

    public static double add(String value1, String value2, String value3) {
        BigDecimal b1 = new BigDecimal(value1);
        BigDecimal b2 = new BigDecimal(value2);
        BigDecimal b3 = new BigDecimal(value3);
        return b1.add(b2).add(b3).doubleValue();
    }

    public static double add(BigDecimal value1, BigDecimal value2) {
        BigDecimal b1 = value1;
        BigDecimal b2 = value2;
        return b1.add(b2).doubleValue();
    }

    public static double add(BigDecimal value1, BigDecimal value2, BigDecimal value3) {
        BigDecimal b1 = value1;
        BigDecimal b2 = value2;
        BigDecimal b3 = value3;
        return b1.add(b2).add(b3).doubleValue();
    }

    /**
     * 提供精确减法运算的sub方法
     *
     * @param value1 被减数
     * @param value2 减数
     *               1    * @return 两个参数的差
     */
    public static double sub(String value1, double value2) {
        BigDecimal b1 = new BigDecimal(value1);
        BigDecimal b2 = new BigDecimal(value2);
        return b1.subtract(b2).doubleValue();
    }

    public static double sub(BigDecimal value1, BigDecimal value2) {
        BigDecimal b1 = value1;
        BigDecimal b2 = value2;
        return b1.subtract(b2).doubleValue();
    }

    /**
     * 提供精确乘法运算的mul方法
     *
     * @param value1 被乘数
     * @param value2 乘数
     * @return 两个参数的积
     */
    public static double mul(String value1, String value2) {
        BigDecimal b1 = new BigDecimal(value1);
        BigDecimal b2 = new BigDecimal(value2);
        return b1.multiply(b2).doubleValue();
    }

    public static double mul(BigDecimal value1, BigDecimal value2) {
        BigDecimal b1 = value1;
        BigDecimal b2 = value2;
        return b1.multiply(b2).doubleValue();
    }

    /**
     * 提供精确的除法运算方法div
     *
     * @param value1 被除数
     * @param value2 除数
     * @param scale  精确范围
     * @return 两个参数的商
     * @throws IllegalAccessException
     */
    public static double div(String value1, String value2, int scale) throws IllegalAccessException {
        //如果精确范围小于0，抛出异常信息
        if (scale < 0) {
            throw new IllegalAccessException("精确度不能小于0");
        }
        BigDecimal b1 = new BigDecimal(value1);
        BigDecimal b2 = new BigDecimal(value2);
        return b1.divide(b2, scale).doubleValue();
    }

    public static double div(BigDecimal value1, BigDecimal value2, int scale) throws IllegalAccessException {
        //如果精确范围小于0，抛出异常信息
        if (scale < 0) {
            throw new IllegalAccessException("精确度不能小于0");
        }
        BigDecimal b1 = value1;
        BigDecimal b2 = value2;
        return b1.divide(b2, scale).doubleValue();
    }

    public static double round(double d, int i, RoundingMode roundingMode) {
        return new BigDecimal(d).setScale(i, roundingMode).doubleValue();
    }

    public static double round(double d, int i, int roundingMode) {
        return new BigDecimal(d).setScale(i, roundingMode).doubleValue();
    }
}
