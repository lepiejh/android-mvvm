package com.ved.framework.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.ColorInt;
import androidx.core.internal.view.SupportMenu;

/**
 * Created by ved on 2017/5/14.
 * 字符串相关工具类
 */
public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    // 将字节数组转换为十六进制字符串
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static boolean isSpace(String s){
        if (TextUtils.isEmpty(s)){
            return true;
        }else {
            if (TextUtils.equals(s,"null") || TextUtils.equals(s,"NULL")){
                return true;
            }else return TextUtils.isEmpty(String.valueOf(s).replace(" ", ""));
        }
    }

    public static String spaceInt(String s){
        if (isSpace(s)){
            return "0";
        }else {
            String bd;
            try {
                bd = toBigDecimal(s);
            } catch (Exception e) {
                KLog.e(e.getMessage());
                bd = s;
            }
            return bd;
        }
    }

    public static long parseLong(String s){
        if (isSpace(s)){
            return 0L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            KLog.e(e.getMessage());
            return 0L;
        }
    }

    public static float parseFloat(String s){
        if (isSpace(s)){
            return 0.0f;
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            KLog.e(e.getMessage());
            return 0.0f;
        }
    }

    public static double parseDouble(String s) {
        if (isSpace(s)) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            KLog.e(e.getMessage());
            return 0.0d;
        }
    }

    public static int parseInt(String s){
        if (isSpace(s)){
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            KLog.e(e.getMessage());
            return 0;
        }
    }

    public static String parseStr(Object o){
        return o == null ? "" : String.valueOf(o);
    }

    public static int parseInt(Object o){
        return parseInt(parseStr(o));
    }

    public static long parseLong(Object o){
        return parseLong(parseStr(o));
    }

    public static double parseDouble(Object o){
        if (o == null) {
            return 0.0d;
        }
        return parseDouble(parseStr(o));
    }

    public static float parseFloat(Object o){
        return parseFloat(parseStr(o));
    }

    public static int parseOneRadix(int radix){
        if ((radix & 1) == 1) {
            return 1;
        } else if ((radix & 2) == 2) {
            return 2;
        } else {
            return 0;
        }
    }

    /**
     * 判断是否为正数
     * @return   true  正数   false   负数
     */
    public static boolean sign(String s){
        if (isSpace(s)){
            return true;
        }else {
            int d = 0;
            try {
                d = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                KLog.e(e.getMessage());
            }
            return d >= 0;
        }
    }

    /**
     * 动态改变文本中的某些字体颜色
     *
     * @param str        文本
     * @param colorResId 颜色资源
     * @param start      开始位置(从0开始数，包括首尾)
     * @param end        结束位置（从结尾1开始数，不包括结束位）
     * @return
     */
    public static SpannableStringBuilder getRepayNumBuilder(String str, @ColorInt int colorResId, int start, int end) {
        if (TextUtils.isEmpty(str)) return new SpannableStringBuilder("");
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        builder.setSpan(new ForegroundColorSpan(colorResId), start, str.length() - end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    /**
     * 判断字符串是否为null或全为空格
     *
     * @param s 待校验字符串
     * @return {@code true}: null或全空格<br> {@code false}: 不为null且不全空格
     */
    public static boolean isTrimEmpty(final String s) {
        return (s == null || s.trim().length() == 0);
    }

    /**
     * 判断两字符串是否相等
     *
     * @param a 待校验字符串a
     * @param b 待校验字符串b
     * @return {@code true}: 相等<br>{@code false}: 不相等
     */
    public static boolean equals(final CharSequence a, final CharSequence b) {
        if (a == b) return true;
        int length;
        if (a != null && b != null && (length = a.length()) == b.length()) {
            if (a instanceof String && b instanceof String) {
                return a.equals(b);
            } else {
                for (int i = 0; i < length; i++) {
                    if (a.charAt(i) != b.charAt(i)) return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 移除小数
     *
     * @param s
     * @return
     */
    public static String removeDecimal(String s) {
        if (TextUtils.isEmpty(s)) return "";
        if (s.contains(".")) {
            return s.substring(0, s.lastIndexOf("."));
        } else {
            return s;
        }
    }

    /**
     * 判断两字符串忽略大小写是否相等
     *
     * @param a 待校验字符串a
     * @param b 待校验字符串b
     * @return {@code true}: 相等<br>{@code false}: 不相等
     */
    public static boolean equalsIgnoreCase(final String a, final String b) {
        return a == null ? b == null : a.equalsIgnoreCase(b);
    }

    /**
     * null转为长度为0的字符串
     *
     * @param s 待转字符串
     * @return s为null转为长度为0字符串，否则不改变
     */
    public static String null2Length0(final String s) {
        return s == null ? "" : s;
    }

    /**
     * 返回字符串长度
     *
     * @param s 字符串
     * @return null返回0，其他返回自身长度
     */
    public static int length(final CharSequence s) {
        return s == null ? 0 : s.length();
    }

    /**
     * 首字母大写
     *
     * @param s 待转字符串
     * @return 首字母大写字符串
     */
    public static String upperFirstLetter(final String s) {
        if (isEmpty(s) || !Character.isLowerCase(s.charAt(0))) return s;
        return String.valueOf((char) (s.charAt(0) - 32)) + s.substring(1);
    }

    /**
     * 首字母小写
     *
     * @param s 待转字符串
     * @return 首字母小写字符串
     */
    public static String lowerFirstLetter(final String s) {
        if (isEmpty(s) || !Character.isUpperCase(s.charAt(0))) return s;
        return String.valueOf((char) (s.charAt(0) + 32)) + s.substring(1);
    }

    /**
     * 反转字符串
     *
     * @param s 待反转字符串
     * @return 反转字符串
     */
    public static String reverse(final String s) {
        int len = length(s);
        if (len <= 1) return s;
        int mid = len >> 1;
        char[] chars = s.toCharArray();
        char c;
        for (int i = 0; i < mid; ++i) {
            c = chars[i];
            chars[i] = chars[len - i - 1];
            chars[len - i - 1] = c;
        }
        return new String(chars);
    }

    /**
     * 判断某个activity是否在前台显示
     */
    public static boolean isForeground(Activity activity) {
        return isForeground(activity, activity.getClass().getName());
    }

    /**
     * 判断某个界面是否在前台,返回true，为显示,否则不是
     */
    private static boolean isForeground(Activity context, String className) {
        if (context == null || TextUtils.isEmpty(className))
            return false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            return className.equals(cpn.getClassName());
        }
        return false;
    }

    /**
     * 转化为半角字符
     *
     * @param s 待转字符串
     * @return 半角字符串
     */
    public static String toDBC(final String s) {
        if (isEmpty(s)) return s;
        char[] chars = s.toCharArray();
        for (int i = 0, len = chars.length; i < len; i++) {
            if (chars[i] == 12288) {
                chars[i] = ' ';
            } else if (65281 <= chars[i] && chars[i] <= 65374) {
                chars[i] = (char) (chars[i] - 65248);
            } else {
                chars[i] = chars[i];
            }
        }
        return new String(chars);
    }

    /**
     * 转化为全角字符
     *
     * @param s 待转字符串
     * @return 全角字符串
     */
    public static String toSBC(final String s) {
        if (isEmpty(s)) return s;
        char[] chars = s.toCharArray();
        for (int i = 0, len = chars.length; i < len; i++) {
            if (chars[i] == ' ') {
                chars[i] = (char) 12288;
            } else if (33 <= chars[i] && chars[i] <= 126) {
                chars[i] = (char) (chars[i] + 65248);
            } else {
                chars[i] = chars[i];
            }
        }
        return new String(chars);
    }

    public static String removePoint(String s){
        if (s.endsWith(".0")){
            return s.substring(0,s.length()-2);
        }else {
            return s;
        }
    }

    public static boolean isEmpty(String s){
        if (!TextUtils.isEmpty(s)) {
            s = s.replaceAll("[\r\n]", "").replace(" ","");
        }
        return TextUtils.isEmpty(s) || Objects.equals(s,"null") || Objects.equals(s,"\"null\"");
    }

    public static boolean isNotEmpty(Object o){
        return !isEmpty(parseStr(o));
    }

    public static int toInt(String s){
        if (s.startsWith("0")){
            s = s.substring(1);
        }
        return parseInt(s);
    }

    public static String split(String s){
        if (!TextUtils.isEmpty(s)){
            if (s.contains(",")){
                for (String number : s.split(",")){
                    if (!TextUtils.isEmpty(number)){
                        return number;
                    }
                }
            }
        }
        return s;
    }

    public static String toBigDecimal(String s){
        if (!TextUtils.isEmpty(s)) {
            BigDecimal db1 = new BigDecimal(s);
            return db1.toPlainString();
        }else {
            return "0";
        }
    }

    public static boolean startsWith(String s,String h){
        if (isSpace(s)){
            return false;
        }
        if (isSpace(h)){
            return false;
        }
        return trim(s).startsWith(trim(h));
    }

    public static String trim(String s){
        if (isSpace(s)){
            return "";
        }
        return s.trim();
    }

    public static String subPhone(String phone){
        if (isSpace(phone)){
            return "";
        }else {
            if (phone.length() > 4){
                return phone.substring(phone.length()-4);
            }else {
                return phone;
            }
        }
    }

    public static String phoneNumber(String phone){
        if (isSpace(phone)){
            return "";
        }else {
            return phone.substring(0,3)+ "****"+subPhone(phone);
        }
    }

    /**
     * @deprecated
     * 因为 Geocoder.getFromLocation 是一个 同步阻塞调用，它在主线程中执行时会阻塞主线程，导致 UI 无响应
     * 使用com.ved.framework.utils.CorpseUtils#fetchAddressFromLocation替换
     */
    public static Address getAddress(double latitude, double longitude) {
        List<Address> addressList = null;
        Geocoder geocoder = new Geocoder(Utils.getContext());
        try {
            addressList = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            KLog.e(e.getMessage());
        }
        if (addressList != null && addressList.size() > 0) {
            return addressList.get(0);
        }else {
            return null;
        }
    }

    public static String calculateTime(int time) {
        int minute;
        int second;
        if (time >= 60) {
            minute = time / 60;
            second = time % 60;
            //分钟在0~9
            if (minute < 10) {
                //判断秒
                if (second < 10) {
                    return "0" + minute + ":" + "0" + second;
                } else {
                    return "0" + minute + ":" + second;
                }
            } else {
                //分钟大于10再判断秒
                if (second < 10) {
                    return minute + ":" + "0" + second;
                } else {
                    return minute + ":" + second;
                }
            }
        } else {
            second = time;
            if (second >= 0 && second < 10) {
                return "00:" + "0" + second;
            } else {
                return "00:" + second;
            }
        }
    }

    /**
     * 二进制数组转十六进制字符串
     *
     * @param bytes byte array to be converted
     * @return string containing hex values
     */
    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte element : bytes) {
            int v = element & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    public static byte[] hexStringToByteArray(String str) {
        int length = str.length();
        byte[] bArr = new byte[(length / 2)];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }

    public static String byteArrayToHexString(char[] hexArray,byte[] bArr) {
        char[] cArr = new char[(bArr.length * 2)];
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i] & 255;
            int i3 = i * 2;
            cArr[i3] = hexArray[i2 >>> 4];
            cArr[i3 + 1] = hexArray[i2 & 15];
        }
        return new String(cArr);
    }

    public static String byteArrayToHexString(char[] hexArray,byte[] bArr, int i) {
        char[] cArr = new char[(bArr.length * 2)];
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = bArr[i2] & 255;
            int i4 = i2 * 2;
            cArr[i4] = hexArray[i3 >>> 4];
            cArr[i4 + 1] = hexArray[i3 & 15];
        }
        return new String(cArr);
    }

    public static int getIntValue(int i){
        int i2 = 4;
        for (int i3 = 0; i3 < 5; i3++) {
            if ((i & i2) == i2) {
                return i3 + 1;
            }
            i2 <<= 1;
        }
        return 0;
    }

    public static int getIntValue(String str,int i){
        try {
            return Integer.valueOf(str, i).intValue();
        } catch (NumberFormatException e) {
            KLog.e(e.getMessage());
            return 0;
        }
    }

    public static String intToInt(int i,String n){
        return String.format("%0"+n, Integer.valueOf(i));
    }

    public static String parseDataByIndex(String str, int i) {
        if (str == null) {
            return "0";
        }
        int i2 = ((i - 1) * 4) + 6;
        if (i <= 0 || i2 < 0 || i2 >= str.length()) {
            return "0";
        }
        int endIndex = Math.min(i2 + 4, str.length());
        try {
            return str.substring(i2, endIndex);
        } catch (Exception e) {
            return "0";
        }
    }

    public static int getThrottle(int countThrottle){
        if (isSpace(parseStr(countThrottle)) || countThrottle == 0){
            return Constant.CLICK_INTERVAL;
        }
        return countThrottle;
    }

    public static long minus(Object a,Object b){
        return parseLong(parseStr(a)) - parseLong(parseStr(b));
    }

    public static String getCRC(String str,String n) {
        int[] byteArrayFromString = getByteArrayFromString(str);
        int i = SupportMenu.USER_MASK;
        int i2 = 0;
        while (i2 < byteArrayFromString.length) {
            int i3 = i ^ byteArrayFromString[i2];
            for (int i4 = 0; i4 < 8; i4++) {
                i3 = (i3 & 1) == 1 ? (i3 >> 1) ^ 40961 : i3 >> 1;
            }
            i2++;
            i = i3;
        }
        return String.format("%0"+n+"%0"+n, Integer.valueOf(i % 256), Integer.valueOf(i / 256));
    }

    private static int[] getByteArrayFromString(String str) {
        String[] splitByNumber = splitByNumber(str, 2);
        int[] iArr = new int[splitByNumber.length];
        int i = 0;
        for (String str2 : splitByNumber) {
            iArr[i] = getIntValue(str2,16);
            i++;
        }
        return iArr;
    }

    private static String[] splitByNumber(String str, int i) {
        int length = (str.length() / i) + (str.length() % i == 0 ? 0 : 1);
        String[] strArr = new String[length];
        String str2 = str;
        for (int i2 = 0; i2 < length; i2++) {
            strArr[i2] = str2.substring(0, i);
            str2 = str2.substring(i);
        }
        return strArr;
    }
}
