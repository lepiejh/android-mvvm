package com.ved.framework.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.ved.framework.utils.bland.code.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class KLog {

    public static boolean IS_SHOW_LOG = false;

    // ============ 日志文件保存功能 ============

    /** 是否保存日志到本地文件 */
    private static boolean IS_SAVE_TO_FILE = false;

    /** 日志文件保存目录（默认使用应用内部存储） */
    private static String LOG_FILE_DIR = null;

    /** 日志文件最大大小（字节），默认 5MB */
    private static final long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024;

    /** 最大保留日志文件数量 */
    private static final int MAX_LOG_FILES = 10;

    /** 日志文件名前缀 */
    private static final String LOG_FILE_PREFIX = "app_log_";

    /** 日志文件后缀 */
    private static final String LOG_FILE_SUFFIX = ".txt";

    /** 单线程池，用于异步写入日志文件 */
    private static final ExecutorService LOG_EXECUTOR = Executors.newSingleThreadExecutor();

    /** 日期格式 */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // ============================================

    private static final String DEFAULT_MESSAGE = "execute";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final int JSON_INDENT = 4;

    private static final int V = 0x1;
    private static final int D = 0x2;
    private static final int I = 0x3;
    private static final int W = 0x4;
    private static final int E = 0x5;
    private static final int A = 0x6;
    private static final int JSON = 0x7;

    public static void init(boolean isShowLog) {
        IS_SHOW_LOG = isShowLog;
        LogUtils.getConfig().setLogSwitch(isShowLog);
        LogUtils.getConfig().setConsoleSwitch(isShowLog);
        LogUtils.getConfig().setLogHeadSwitch(isShowLog);
        LogUtils.getConfig().setLog2FileSwitch(isShowLog);
        LogUtils.getConfig().setBorderSwitch(isShowLog);
        LogUtils.getConfig().setSingleTagSwitch(isShowLog);
    }

    // ============ 日志文件管理方法 ============

    /**
     * 初始化日志文件保存功能（使用应用内部存储，不需要任何权限）
     * @param context 上下文
     * @param isSaveToFile 是否保存到文件
     */
    public static void initFileLog(Context context, boolean isSaveToFile) {
        initFileLog(context, isSaveToFile, null);
    }

    /**
     * 初始化日志文件保存功能
     * @param context 上下文
     * @param isSaveToFile 是否保存到文件
     * @param logDir 日志保存目录（如果为null，使用默认的内部存储目录）
     */
    public static void initFileLog(Context context, boolean isSaveToFile, String logDir) {
        IS_SAVE_TO_FILE = isSaveToFile;
        if (isSaveToFile) {
            if (TextUtils.isEmpty(logDir)) {
                // 默认使用应用内部存储，不需要任何权限
                LOG_FILE_DIR = context.getFilesDir().getAbsolutePath()
                        + File.separator + "logs" + File.separator;
            } else {
                LOG_FILE_DIR = logDir.endsWith(File.separator) ? logDir : logDir + File.separator;
            }

            // 创建日志目录
            File dir = new File(LOG_FILE_DIR);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    Log.i("KLog", "日志目录创建成功: " + LOG_FILE_DIR);
                } else {
                    Log.e("KLog", "日志目录创建失败: " + LOG_FILE_DIR);
                }
            }

            // 清理旧日志文件
            cleanOldLogFiles();
        }
    }

    /**
     * 设置是否保存日志到文件
     * @param isSaveToFile true-保存，false-不保存
     */
    public static void setSaveToFile(boolean isSaveToFile) {
        IS_SAVE_TO_FILE = isSaveToFile;
    }

    /**
     * 获取当前是否保存日志到文件
     */
    public static boolean isSaveToFile() {
        return IS_SAVE_TO_FILE;
    }

    /**
     * 设置日志文件保存目录
     * @param logDir 目录路径
     */
    public static void setLogFileDir(String logDir) {
        if (!TextUtils.isEmpty(logDir)) {
            LOG_FILE_DIR = logDir.endsWith(File.separator) ? logDir : logDir + File.separator;
            File dir = new File(LOG_FILE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    /**
     * 获取日志文件保存目录
     */
    public static String getLogFileDir() {
        return LOG_FILE_DIR;
    }

    /**
     * 异步写入日志到文件
     */
    private static void writeLogToFile(final String level, final String tag, final String logContent) {
        if (!IS_SAVE_TO_FILE || TextUtils.isEmpty(LOG_FILE_DIR)) {
            return;
        }

        LOG_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String today = FILE_DATE_FORMAT.format(new Date());
                    String fileName = LOG_FILE_PREFIX + today + LOG_FILE_SUFFIX;
                    File logFile = new File(LOG_FILE_DIR, fileName);

                    // 检查文件大小，如果超过限制则归档
                    if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                        archiveLogFile(logFile);
                    }

                    String timestamp = DATE_FORMAT.format(new Date());
                    String logEntry = String.format("[%s] [%s] [%s] %s%s",
                            timestamp, level, tag, logContent, LINE_SEPARATOR);

                    FileWriter writer = new FileWriter(logFile, true);
                    writer.write(logEntry);
                    writer.flush();
                    writer.close();

                } catch (IOException e) {
                    Log.e("KLog", "写入日志文件失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 归档日志文件
     */
    private static void archiveLogFile(File logFile) {
        try {
            String timestamp = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
            String baseName = logFile.getName().replace(LOG_FILE_SUFFIX, "");
            File archiveFile = new File(logFile.getParent(), baseName + "_" + timestamp + LOG_FILE_SUFFIX);
            logFile.renameTo(archiveFile);

            // 清理旧的归档文件
            cleanOldLogFiles();
        } catch (Exception e) {
            Log.e("KLog", "归档日志文件失败: " + e.getMessage());
        }
    }

    /**
     * 清理旧的日志文件，只保留最新的 MAX_LOG_FILES 个文件
     */
    private static void cleanOldLogFiles() {
        try {
            if (TextUtils.isEmpty(LOG_FILE_DIR)) {
                return;
            }

            File dir = new File(LOG_FILE_DIR);
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }

            File[] files = dir.listFiles();
            if (files == null || files.length <= MAX_LOG_FILES) {
                return;
            }

            List<File> logFiles = new ArrayList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(LOG_FILE_PREFIX)) {
                    logFiles.add(file);
                }
            }

            if (logFiles.size() > MAX_LOG_FILES) {
                // 按最后修改时间排序，保留最新的
                logFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (int i = MAX_LOG_FILES; i < logFiles.size(); i++) {
                    boolean deleted = logFiles.get(i).delete();
                    if (deleted) {
                        Log.d("KLog", "删除旧日志文件: " + logFiles.get(i).getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.e("KLog", "清理旧日志文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有日志文件列表
     * @return 日志文件列表
     */
    public static List<File> getLogFiles() {
        List<File> result = new ArrayList<>();
        if (TextUtils.isEmpty(LOG_FILE_DIR)) {
            return result;
        }

        File dir = new File(LOG_FILE_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return result;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(LOG_FILE_PREFIX)) {
                result.add(file);
            }
        }

        // 按最后修改时间排序，最新的在前
        result.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        return result;
    }

    /**
     * 获取所有日志文件的内容（合并所有日志文件）
     * @return 所有日志内容
     */
    public static String getAllLogContent() {
        StringBuilder sb = new StringBuilder();
        List<File> logFiles = getLogFiles();

        for (File file : logFiles) {
            sb.append("========== ").append(file.getName()).append(" ==========").append(LINE_SEPARATOR);
            sb.append(readLogFile(file));
            sb.append(LINE_SEPARATOR);
            sb.append(LINE_SEPARATOR);
        }

        return sb.toString();
    }

    /**
     * 读取单个日志文件内容
     * @param file 日志文件
     * @return 文件内容
     */
    public static String readLogFile(File file) {
        StringBuilder sb = new StringBuilder();
        if (file == null || !file.exists()) {
            return "日志文件不存在";
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(LINE_SEPARATOR);
            }

            reader.close();
            isr.close();
            fis.close();
        } catch (IOException e) {
            return "读取日志文件失败: " + e.getMessage();
        }

        return sb.toString();
    }

    /**
     * 按日期获取日志文件
     * @param date 日期字符串，格式：yyyy-MM-dd
     * @return 日志文件，如果不存在返回null
     */
    public static File getLogFileByDate(String date) {
        if (TextUtils.isEmpty(LOG_FILE_DIR) || TextUtils.isEmpty(date)) {
            return null;
        }

        String fileName = LOG_FILE_PREFIX + date + LOG_FILE_SUFFIX;
        File file = new File(LOG_FILE_DIR, fileName);
        return file.exists() ? file : null;
    }

    /**
     * 获取指定日期的日志内容
     * @param date 日期字符串，格式：yyyy-MM-dd
     * @return 日志内容
     */
    public static String getLogContentByDate(String date) {
        File file = getLogFileByDate(date);
        if (file == null) {
            return "未找到 " + date + " 的日志文件";
        }
        return readLogFile(file);
    }

    /**
     * 清除所有日志文件
     * @return 是否清除成功
     */
    public static boolean clearAllLogFiles() {
        try {
            List<File> files = getLogFiles();
            for (File file : files) {
                file.delete();
            }
            return true;
        } catch (Exception e) {
            Log.e("KLog", "清除日志文件失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取日志文件总大小
     * @return 总大小（字节）
     */
    public static long getLogFilesTotalSize() {
        long totalSize = 0;
        List<File> files = getLogFiles();
        for (File file : files) {
            totalSize += file.length();
        }
        return totalSize;
    }

    /**
     * 获取格式化的日志文件大小
     */
    public static String getLogFilesTotalSizeFormatted() {
        long size = getLogFilesTotalSize();
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    /**
     * 导出日志到外部存储（用户可访问的位置）
     * 注意：此方法需要 WRITE_EXTERNAL_STORAGE 权限
     * @param context 上下文
     * @param destPath 目标路径，如果为null则保存到 Downloads 目录
     * @return 导出后的文件路径，失败返回null
     */
    public static String exportLogsToExternal(Context context, String destPath) {
        List<File> logFiles = getLogFiles();
        if (logFiles.isEmpty()) {
            Log.w("KLog", "没有日志文件可导出");
            return null;
        }

        try {
            File destFile;
            if (TextUtils.isEmpty(destPath)) {
                // 保存到 Downloads 目录
                File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                );
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                destFile = new File(downloadDir, "app_logs_" + timestamp + ".txt");
            } else {
                destFile = new File(destPath);
            }

            // 确保父目录存在
            if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }

            // 合并所有日志
            String allLogs = getAllLogContent();
            FileWriter writer = new FileWriter(destFile);
            writer.write(allLogs);
            writer.flush();
            writer.close();

            Log.i("KLog", "日志导出成功: " + destFile.getAbsolutePath());
            return destFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("KLog", "导出日志失败: " + e.getMessage());
            return null;
        }
    }

    // ============================================

    public static void v() {
        printLog(V, null, DEFAULT_MESSAGE);
    }

    public static void v(Object msg) {
        printLog(V, null, msg);
    }

    public static void v(String tag, Object msg) {
        printLog(V, tag, msg);
    }

    public static void d() {
        printLog(D, null, DEFAULT_MESSAGE);
    }

    public static void d(Object msg) {
        printLog(D, null, msg);
    }

    public static void d(String tag, Object msg) {
        printLog(D, tag, msg);
    }

    public static void i() {
        printLog(I, null, DEFAULT_MESSAGE);
    }

    public static void i(Object msg) {
        printLog(I, null, msg);
    }

    public static void i(String tag, Object msg) {
        printLog(I, tag, msg);
    }

    public static void w() {
        printLog(W, null, DEFAULT_MESSAGE);
    }

    public static void w(Object msg) {
        printLog(W, null, msg);
    }

    public static void w(String tag, Object msg) {
        printLog(W, tag, msg);
    }

    public static void e() {
        printLog(E, null, DEFAULT_MESSAGE);
    }

    public static void e(Object msg) {
        printLog(E, null, msg);
    }

    public static void e(String tag, Object msg) {
        printLog(E, tag, msg);
    }

    public static void a() {
        printLog(A, null, DEFAULT_MESSAGE);
    }

    public static void a(Object msg) {
        printLog(A, null, msg);
    }

    public static void a(String tag, Object msg) {
        printLog(A, tag, msg);
    }


    public static void json(String jsonFormat) {
        printLog(JSON, null, jsonFormat);
    }

    public static void json(String tag, String jsonFormat) {
        printLog(JSON, tag, jsonFormat);
    }


    private static void printLog(int type, String tagStr, Object objectMsg) {
        String msg;
        if (!IS_SHOW_LOG && !IS_SAVE_TO_FILE) {
            // 如果既不显示也不保存，直接返回
            return;
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int index = 4;
        String className = stackTrace[index].getFileName();
        String methodName = stackTrace[index].getMethodName();
        int lineNumber = stackTrace[index].getLineNumber();

        String tag = (tagStr == null ? className : tagStr);
        methodName = methodName.substring(0, 1).toUpperCase() + methodName.substring(1);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ (").append(className).append(":").append(lineNumber).append(")#").append(methodName).append(" ] ");

        if (objectMsg == null) {
            msg = "Log with null Object";
        } else {
            msg = objectMsg.toString();
        }
        if (msg != null && type != JSON) {
            stringBuilder.append(msg);
        }

        String logStr = stringBuilder.toString();

        // 获取日志级别名称
        String levelName = getLevelName(type);

        switch (type) {
            case V:
                if (IS_SHOW_LOG) {
                    Log.v(tag, logStr);
                }
                break;
            case D:
                if (IS_SHOW_LOG) {
                    Log.d(tag, logStr);
                }
                break;
            case I:
                if (IS_SHOW_LOG) {
                    Log.i(tag, logStr);
                }
                break;
            case W:
                if (IS_SHOW_LOG) {
                    Log.w(tag, logStr);
                }
                break;
            case E:
                if (IS_SHOW_LOG) {
                    Log.e(tag, logStr);
                }
                break;
            case A:
                if (IS_SHOW_LOG) {
                    Log.wtf(tag, logStr);
                }
                break;
            case JSON: {
                if (IS_SHOW_LOG) {
                    if (TextUtils.isEmpty(msg)) {
                        Log.d(tag, "Empty or Null json content");
                        return;
                    }

                    String message = null;

                    try {
                        if (msg.startsWith("{")) {
                            JSONObject jsonObject = new JSONObject(msg);
                            message = jsonObject.toString(JSON_INDENT);
                        } else if (msg.startsWith("[")) {
                            JSONArray jsonArray = new JSONArray(msg);
                            message = jsonArray.toString(JSON_INDENT);
                        }
                    } catch (JSONException e) {
                        e(tag, e.getCause().getMessage() + "\n" + msg);
                        return;
                    }

                    printLine(tag, true);
                    message = logStr + LINE_SEPARATOR + message;
                    String[] lines = message.split(LINE_SEPARATOR);
                    StringBuilder jsonContent = new StringBuilder();
                    for (String line : lines) {
                        jsonContent.append("║ ").append(line).append(LINE_SEPARATOR);
                    }

                    if (jsonContent.toString().length() > 3200) {
                        Log.w(tag, "jsonContent.length = " + jsonContent.toString().length());
                        int chunkCount = jsonContent.toString().length() / 3200;
                        for (int i = 0; i <= chunkCount; i++) {
                            int max = 3200 * (i + 1);
                            if (max >= jsonContent.toString().length()) {
                                Log.w(tag, jsonContent.toString().substring(3200 * i));
                            } else {
                                Log.w(tag, jsonContent.toString().substring(3200 * i, max));
                            }
                        }
                    } else {
                        Log.w(tag, jsonContent.toString());
                    }
                    printLine(tag, false);
                }

                // JSON 日志也保存到文件
                if (IS_SAVE_TO_FILE) {
                    String jsonToSave = msg;
                    try {
                        if (msg.startsWith("{")) {
                            JSONObject jsonObject = new JSONObject(msg);
                            jsonToSave = jsonObject.toString(JSON_INDENT);
                        } else if (msg.startsWith("[")) {
                            JSONArray jsonArray = new JSONArray(msg);
                            jsonToSave = jsonArray.toString(JSON_INDENT);
                        }
                    } catch (JSONException e) {
                        jsonToSave = msg;
                    }
                    String fullLog = logStr + LINE_SEPARATOR + jsonToSave;
                    writeLogToFile(levelName, tag, fullLog);
                }
                return;
            }
        }

        // 保存日志到文件（非JSON类型）
        if (IS_SAVE_TO_FILE && type != JSON) {
            writeLogToFile(levelName, tag, logStr);
        }
    }

    private static void printLine(String tag, boolean isTop) {
        if (IS_SHOW_LOG) {
            if (isTop) {
                Log.w(tag, "╔═══════════════════════════════════════════════════════════════════════════════════════");
            } else {
                Log.w(tag, "╚═══════════════════════════════════════════════════════════════════════════════════════");
            }
        }
    }

    // 获取日志级别名称
    private static String getLevelName(int type) {
        switch (type) {
            case V: return "VERBOSE";
            case D: return "DEBUG";
            case I: return "INFO";
            case W: return "WARN";
            case E: return "ERROR";
            case A: return "ASSERT";
            case JSON: return "JSON";
            default: return "UNKNOWN";
        }
    }
}