package com.ved.framework.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射 Bean 复制工具（模板方法模式）：
 * <p>
 * 单对象复制 {@link #sourceToTarget(Object, Class)} 与列表复制 {@link #copyProperties(List, Class)}
 * 共享同一个复制流程模板 {@link #copyTo(Object, Class)}，消除原先两份 100% 重复的反射遍历逻辑。
 */
class CopyUtils {

    /**
     * 复制流程模板方法：按「字段名 + 字段类型」匹配源对象与目标对象属性，
     * 通过 getter/setter 完成单对象属性复制。字段级异常被吞掉并跳过（与旧行为一致）。
     *
     * @throws Exception 目标对象创建失败时抛出（由调用方决定处理策略）
     */
    private static <T> T copyTo(Object source, Class<T> tClass) throws Exception {
        Class<?> sClass = source.getClass();
        Field[] sFields = sClass.getDeclaredFields();
        Field[] tFields = tClass.getDeclaredFields();
        // 创建目标对象，这一步等同于 UserTwo target = new UserTwo()
        T target = tClass.getDeclaredConstructor().newInstance();
        // 循环取到源对象的单个属性
        for (Field sField : sFields) {
            // 循环取到目标对象的单个属性
            for (Field tField : tFields) {
                // 判断源对象的属性名、属性类型是否和目标对象的属性名、属性类型一致
                if (sField.getName().equals(tField.getName())
                        && sField.getGenericType().equals(tField.getGenericType())) {
                    try {
                        // 获取源对象属性 get 方法并调用
                        Method sMethod = sClass.getMethod("get" + capitalize(sField.getName()));
                        Object sFieldValue = sMethod.invoke(source);
                        // 获取目标对象属性 set 方法并调用，将源对象 get 方法返回值作为参数传入
                        Method tMethod = tClass.getMethod("set" + capitalize(tField.getName()), tField.getType());
                        tMethod.invoke(target, sFieldValue);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return target;
    }

    /**
     * 将属性名字段首字母转大写（getUsername / setId 的方法名拼接辅助）
     */
    private static String capitalize(String fieldName) {
        char[] chars = fieldName.toCharArray();
        chars[0] -= 32;
        return String.valueOf(chars);
    }

    /**
     * 对象属性复制（单对象）
     *
     * @param source 源对象
     * @param tClass 目标对象类型
     * @return 复制完成的目标对象；源对象为 null 或复制失败时返回 null
     */
    public static <T> T sourceToTarget(Object source, Class<T> tClass) {
        if (source == null) {
            return null;
        }
        try {
            return copyTo(source, tClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 对象属性复制（列表）
     *
     * @param source 源对象列表
     * @param tClass 目标对象类型
     * @return 复制完成的目标对象列表
     * @throws Exception 源列表为空或目标对象创建失败时抛出
     */
    public static <T, E> List<E> copyProperties(List<T> source, Class<E> tClass) throws Exception {
        // 判断传入源数据是否为空，如果为空，则抛自定义异常
        if (null == source) {
            throw new Exception("数据源为空");
        }
        List<E> targetList = new ArrayList<>();
        // 循环取到单个源对象，依次执行复制流程模板
        for (T t : source) {
            try {
                targetList.add(copyTo(t, tClass));
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("目标对象创建失败");
            }
        }
        return targetList;
    }
}
