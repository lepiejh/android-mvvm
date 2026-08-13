package com.ved.framework.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 利用反射实现对象之间属性复制（模板方法模式）：
 * <p>
 * 全部复制 / 排除复制 / 包含复制共享同一套复制流程 {@link #copyProperties(Object, Object, NameFilter)}，
 * 三种场景的差异仅在于 getter 方法名的过滤条件，由 {@link NameFilter} 以「策略」形式注入模板。
 */
public class BeanPropertiesCopyUtil {

    /**
     * getter 方法名过滤谓词：模板方法中的差异点
     */
    private interface NameFilter {
        boolean accept(String methodName);
    }

    /**
     * 复制对象全部属性
     *
     * @param from 源对象
     * @param to   目标对象
     */
    public static void copyProperties(Object from, Object to) throws Exception {
        copyProperties(from, to, methodName -> true);
    }

    /**
     * 复制对象属性（排除指定属性）
     *
     * @param from          源对象
     * @param to            目标对象
     * @param excludsArray  排除属性列表（小写属性名）
     */
    public static void copyPropertiesExclude(Object from, Object to, String[] excludsArray) throws Exception {
        final List<String> excludesList = excludsArray != null && excludsArray.length > 0
                ? Arrays.asList(excludsArray) : null;
        copyProperties(from, to, methodName -> {
            // 排除 getId 系列（id 字段复制无意义）
            if (methodName.contains("getId")) {
                return false;
            }
            // 排除列表检测
            return excludesList == null
                    || !excludesList.contains(methodName.substring(3).toLowerCase());
        });
    }

    /**
     * 复制对象属性（仅复制指定属性）
     *
     * @param from          源对象
     * @param to            目标对象
     * @param includsArray  包含属性列表（首字母小写属性名）
     */
    public static void copyPropertiesInclude(Object from, Object to, String[] includsArray) throws Exception {
        if (includsArray == null || includsArray.length == 0) {
            return;
        }
        final List<String> includesList = Arrays.asList(includsArray);
        copyProperties(from, to, methodName -> {
            String fieldName = methodName.substring(3);
            // 包含列表检测（属性名首字母转小写）
            return includesList.contains(fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1));
        });
    }

    /**
     * 复制流程模板方法：遍历源对象 getter，过滤后写入目标对象对应 setter
     */
    private static void copyProperties(Object from, Object to, NameFilter filter) throws Exception {
        Method[] fromMethods = from.getClass().getDeclaredMethods();
        Method[] toMethods = to.getClass().getDeclaredMethods();
        for (Method fromMethod : fromMethods) {
            String fromMethodName = fromMethod.getName();
            if (!fromMethodName.contains("get") || !filter.accept(fromMethodName)) {
                continue;
            }
            Method toMethod = findMethodByName(toMethods, "set" + fromMethodName.substring(3));
            if (toMethod == null) {
                continue;
            }
            Object value = fromMethod.invoke(from);
            if (value == null) {
                continue;
            }
            // 集合类判空处理
            if (value instanceof Collection && ((Collection<?>) value).size() <= 0) {
                continue;
            }
            toMethod.invoke(to, value);
        }
    }

    /**
     * 从方法数组中获取指定名称的方法
     *
     * @param methods 方法数组
     * @param name    方法名
     * @return 匹配的方法，不存在返回 null
     */
    public static Method findMethodByName(Method[] methods, String name) {
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }
}
