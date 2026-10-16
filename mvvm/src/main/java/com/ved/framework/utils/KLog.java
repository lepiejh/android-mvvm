package com.ved.framework.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class KLog {

    private static boolean IS_SHOW_LOG = false;

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

    /** 写入缓冲的 flush 间隔（毫秒）。普通日志攒着刷，降低IO次数 */
    private static final long FLUSH_INTERVAL_MS = 1000L;

    /** 累计写入多少字节后检查一次文件大小，避免每条日志都 stat 一次 */
    private static final long SIZE_CHECK_THRESHOLD = 64 * 1024L;

    /** 导出/读取日志的最大字符数，防止把几十MB日志读进内存导致OOM */
    private static final int MAX_EXPORT_CHARS = 4 * 1024 * 1024;

    /**
     * 单线程写入日志。这里【不能】用 Executors.newSingleThreadExecutor()：
     * 它内部是无界队列，一旦日志产生速度超过写盘速度，任务会无限堆积直到OOM。
     * 改为有界队列 + 丢弃最旧任务：日志只是调试用，丢几条远好过把APP搞崩
     */
    private static final int LOG_QUEUE_CAPACITY = 2000;
    private static final ExecutorService LOG_EXECUTOR = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(LOG_QUEUE_CAPACITY),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    /** 保护日志文件句柄与相关状态的锁 */
    private static final Object FILE_LOCK = new Object();

    // 复用的写入句柄。原来每条日志都 new FileWriter/close 一次，
    // 高频日志(如串口每帧打点)会产生大量文件开关操作，严重拖慢并可能造成任务堆积
    private static BufferedWriter sLogWriter = null;
    private static FileOutputStream sLogStream = null;
    private static String sCurrentLogFileName = null;
    private static long sUncheckedBytes = 0L;
    private static long sLastFlushMs = 0L;

    /**
     * 时间格式。SimpleDateFormat 【不是线程安全的】，不能做成静态共享实例，
     * 否则多线程同时打日志会产出错乱的时间戳甚至抛异常。这里用 ThreadLocal 每个线程一份
     */
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @SuppressLint("ConstantLocale")
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        }
    };
    private static final ThreadLocal<SimpleDateFormat> FILE_DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @SuppressLint("ConstantLocale")
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        }
    };

    // ============================================

    private static final String DEFAULT_MESSAGE = "execute";
    private static final String LINE_SEPARATOR = System.lineSeparator();
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

            // 清理旧日志文件。放到写入线程做，避免在主线程做文件IO造成启动卡顿
            LOG_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (FILE_LOCK) {
                        cleanOldLogFiles();
                    }
                }
            });
        }
    }

    /**
     * 设置是否保存日志到文件
     * @param isSaveToFile true-保存，false-不保存
     */
    public static void setSaveToFile(boolean isSaveToFile) {
        IS_SAVE_TO_FILE = isSaveToFile;
        if (!isSaveToFile) {
            // 关闭保存时把缓冲落盘，避免最后一段日志丢在缓冲区里
            LOG_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (FILE_LOCK) {
                        flushWriter();
                    }
                }
            });
        }
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
            final String newDir = logDir.endsWith(File.separator) ? logDir : logDir + File.separator;
            // 换目录前必须先关掉旧目录里打开的文件句柄，否则会继续往旧文件里写
            LOG_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (FILE_LOCK) {
                        closeWriter();
                        LOG_FILE_DIR = newDir;
                        File dir = new File(LOG_FILE_DIR);
                        if (!dir.exists()) {
                            boolean d = dir.mkdirs();
                            Log.i("KLog", d ? "日志目录创建成功" : "日志目录创建失败");
                        }
                    }
                }
            });
        }
    }

    /**
     * 获取日志文件保存目录
     */
    public static String getLogFileDir() {
        return LOG_FILE_DIR;
    }

    /**
     * 强制把缓冲中的日志落盘（同步执行）。
     * 遥控器这类设备经常直接断电/关机，退出前或关键时刻调一次，
     * 能把还在缓冲区里的日志写进文件，否则最后一段日志会丢
     */
    public static void flushLog() {
        if (!IS_SAVE_TO_FILE) {
            return;
        }
        synchronized (FILE_LOCK) {
            flushWriter();
        }
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
                synchronized (FILE_LOCK) {
                    try {
                        String today = FILE_DATE_FORMAT.get().format(new Date());
                        String fileName = LOG_FILE_PREFIX + today + LOG_FILE_SUFFIX;

                        // 跨天了：关掉昨天的文件，打开今天的
                        if (!fileName.equals(sCurrentLogFileName) || sLogWriter == null) {
                            closeWriter();
                            if (!openWriter(fileName)) {
                                return;
                            }
                        }

                        String timestamp = DATE_FORMAT.get().format(new Date());
                        String logEntry = String.format(Locale.getDefault(), "[%s] [%s] [%s] %s%s",
                                timestamp, level, tag, logContent, LINE_SEPARATOR);

                        sLogWriter.write(logEntry);
                        sUncheckedBytes += logEntry.length();

                        // WARN 及以上立即落盘并 fsync：这类日志往往是崩溃前的最后几条，
                        // 不能留在缓冲区里等定时 flush，否则断电就丢了
                        boolean urgent = "WARN".equals(level) || "ERROR".equals(level)
                                || "ASSERT".equals(level);
                        long now = System.currentTimeMillis();
                        if (urgent || now - sLastFlushMs >= FLUSH_INTERVAL_MS) {
                            flushWriter();
                            if (urgent) {
                                forceWriter();
                            }
                        }

                        // 累计写入一定量后再检查文件大小，超过限制就归档
                        if (sUncheckedBytes >= SIZE_CHECK_THRESHOLD) {
                            sUncheckedBytes = 0;
                            flushWriter();
                            File logFile = new File(LOG_FILE_DIR, sCurrentLogFileName);
                            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                                archiveCurrentLogFile();
                            }
                        }

                    } catch (IOException e) {
                        Log.e("KLog", "写入日志文件失败: " + e.getMessage());
                        // 写入异常(比如存储满了)时关掉句柄，下次写入重新打开，避免一直失败
                        closeWriter();
                    }
                }
            }
        });
    }

    /**
     * 打开日志文件句柄。
     * 用 OutputStreamWriter + UTF-8 显式指定编码：原来用 FileWriter 走的是平台默认编码，
     * 而读取时用的是 UTF-8，编码不一致会直接乱码
     */
    private static boolean openWriter(String fileName) throws IOException {
        File dir = new File(LOG_FILE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e("KLog", "日志目录创建失败: " + LOG_FILE_DIR);
            return false;
        }
        File logFile = new File(dir, fileName);
        sLogStream = new FileOutputStream(logFile, true);
        sLogWriter = new BufferedWriter(
                new OutputStreamWriter(sLogStream, StandardCharsets.UTF_8), 32 * 1024);
        sCurrentLogFileName = fileName;
        sUncheckedBytes = 0L;
        return true;
    }

    /** 把缓冲刷到文件（只到OS层，不保证落盘） */
    private static void flushWriter() {
        if (sLogWriter == null) {
            return;
        }
        try {
            sLogWriter.flush();
        } catch (IOException e) {
            Log.e("KLog", "flush日志文件失败: " + e.getMessage());
        }
        sLastFlushMs = System.currentTimeMillis();
    }

    /** 强制刷到磁盘（fsync），断电也不丢 */
    private static void forceWriter() {
        if (sLogStream == null) {
            return;
        }
        try {
            sLogStream.getChannel().force(false);
        } catch (Exception e) {
            // fsync 失败不影响主流程
        }
    }

    private static void closeWriter() {
        try {
            if (sLogWriter != null) {
                sLogWriter.flush();
            }
        } catch (IOException e) {
            Log.e("KLog", "关闭日志文件失败: " + e.getMessage());
        } finally {
            if (sLogWriter != null) {
                try {
                    sLogWriter.close();
                } catch (IOException ignored) {
                }
            }
            sLogWriter = null;
            if (sLogStream != null) {
                try {
                    sLogStream.close();
                } catch (IOException ignored) {
                }
            }
            sLogStream = null;
            sCurrentLogFileName = null;
            sUncheckedBytes = 0L;
        }
    }

    /**
     * 归档当前日志文件。
     * 注意：这里【不能】调用 KLog.i() 之类的日志方法 —— 本方法运行在日志写入线程内，
     * 再打日志会往同一个单线程池队列里塞任务，属于自产自销的递归入队。统一用原生 Log
     */
    private static void archiveCurrentLogFile() {
        closeWriter();
        if (TextUtils.isEmpty(sCurrentLogFileName)) {
            return;
        }
        File logFile = new File(LOG_FILE_DIR, LOG_FILE_PREFIX
                + FILE_DATE_FORMAT.get().format(new Date()) + LOG_FILE_SUFFIX);
        if (!logFile.exists()) {
            return;
        }
        try {
            String baseName = LOG_FILE_PREFIX + FILE_DATE_FORMAT.get().format(new Date());
            // 同一秒内重复归档时文件名会撞车，原来直接 renameTo 会失败导致文件一直超限。
            // 这里循环加序号直到找到一个不存在的名字
            String stamp = new SimpleDateFormat("HHmmss", Locale.getDefault()).format(new Date());
            File archiveFile;
            int index = 0;
            do {
                String name = baseName + "_" + stamp + (index == 0 ? "" : "_" + index) + LOG_FILE_SUFFIX;
                archiveFile = new File(logFile.getParent(), name);
                index++;
            } while (archiveFile.exists());

            boolean ok = logFile.renameTo(archiveFile);
            Log.i("KLog", "归档日志文件" + (ok ? "成功: " + archiveFile.getName() : "失败"));
        } catch (Exception e) {
            Log.e("KLog", "归档日志文件失败: " + e.getMessage());
        }
        // 归档后再清理一轮，控制文件数量
        cleanOldLogFiles();
    }

    /**
     * 清理旧的日志文件，只保留最新的 MAX_LOG_FILES 个文件。
     * 排序必须用 Collections.sort（全版本可用）：原来用的 List.sort() 是 API 24 才有的，
     * 在低版本Android(5.1/6.0等)上排序根本不执行，于是按 listFiles() 的不确定顺序
     * 删除"第10个之后"的文件 —— 很可能把当天的日志删掉、反而留下旧的
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
                // 按最后修改时间倒序排序，最新的在前（不用 Long.compare，兼容低版本）
                Collections.sort(logFiles, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        long diff = f2.lastModified() - f1.lastModified();
                        return diff > 0 ? 1 : (diff < 0 ? -1 : 0);
                    }
                });
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
     * 获取所有日志文件列表（最新的在前）
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

        // 按最后修改时间排序，最新的在前（同样用兼容低版本的写法）
        Collections.sort(result, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                long diff = f2.lastModified() - f1.lastModified();
                return diff > 0 ? 1 : (diff < 0 ? -1 : 0);
            }
        });
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
            // 防止把几十MB日志一次性读进内存导致OOM
            if (sb.length() >= MAX_EXPORT_CHARS) {
                sb.append("... 日志过大，已截断 ...").append(LINE_SEPARATOR);
                break;
            }
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

        BufferedReader reader = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            reader = new BufferedReader(isr);

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(LINE_SEPARATOR);
            }
        } catch (IOException e) {
            return "读取日志文件失败: " + e.getMessage();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
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
            // 先关掉句柄再删，否则句柄还指向已被删除的文件，后续写入会写到已删除的inode上
            synchronized (FILE_LOCK) {
                closeWriter();
                List<File> files = getLogFiles();
                for (File file : files) {
                    boolean b = file.delete();
                    Log.i("KLog", b ? "文件删除成功: " + file.getName() : "文件删除失败: " + file.getName());
                }
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
     * 导出日志到外部存储（用户可访问的位置）。
     * Android 10(API 29) 起分区存储生效，getExternalStoragePublicDirectory 已废弃且
     * 拿到的位置不可写，所以高版本改用应用自己的外部目录，无需任何权限
     * @param context 上下文
     * @param destPath 目标路径，如果为null则保存到 Downloads 目录
     * @return 导出后的文件路径，失败返回null
     */
    @SuppressWarnings("deprecation")
    public static String exportLogsToExternal(Context context, String destPath) {
        List<File> logFiles = getLogFiles();
        if (logFiles.isEmpty()) {
            Log.w("KLog", "没有日志文件可导出");
            return null;
        }

        BufferedWriter writer = null;
        try {
            File destFile;
            if (TextUtils.isEmpty(destPath)) {
                File downloadDir;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 应用专属外部目录，不需要任何存储权限
                    downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                } else {
                    downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                    );
                }
                if (downloadDir == null) {
                    Log.e("KLog", "无法获取导出目录");
                    return null;
                }
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                destFile = new File(downloadDir, "app_logs_" + timestamp + ".txt");
            } else {
                destFile = new File(destPath);
            }

            // 确保父目录存在
            if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
                boolean b = destFile.getParentFile().mkdirs();
                Log.i("KLog", b ? "导出目录创建成功" : "导出目录创建失败");
            }

            // 合并所有日志前先落盘，保证导出的是最新内容
            flushLog();

            String allLogs = getAllLogContent();
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(destFile), StandardCharsets.UTF_8));
            writer.write(allLogs);
            writer.flush();

            Log.i("KLog", "日志导出成功: " + destFile.getAbsolutePath());
            return destFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("KLog", "导出日志失败: " + e.getMessage());
            return null;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
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
        if (!IS_SHOW_LOG && !IS_SAVE_TO_FILE) {
            // 如果既不显示也不保存，直接返回
            return;
        }

        // 打日志本身绝不能把业务搞崩：栈帧解析、字符串处理都可能出意外，统一兜住
        try {
            printLogInternal(type, tagStr, objectMsg);
        } catch (Throwable t) {
            Log.e("KLog", "打印日志异常: " + t.getMessage());
        }
    }

    private static void printLogInternal(int type, String tagStr, Object objectMsg) {
        String msg;

        StackTraceElement targetElement = getTargetStackTraceElement();

        String className = targetElement != null ? targetElement.getFileName() : "Unknown";
        String methodName = targetElement != null ? targetElement.getMethodName() : "unknown";
        int lineNumber = targetElement != null ? targetElement.getLineNumber() : 0;

        String tag = (tagStr == null ? className : tagStr);
        methodName = !TextUtils.isEmpty(methodName)
                ? methodName.substring(0, 1).toUpperCase(Locale.getDefault()) + methodName.substring(1)
                : methodName;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ (").append(className).append(":").append(lineNumber).append(")#").append(methodName).append(" ] ");

        if (objectMsg == null) {
            msg = "Log with null Object";
        } else {
            msg = objectMsg.toString();
        }
        if (type != JSON) {
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
                        // 原来这里直接 return，导致只开文件保存时这条空json不会被记录。
                        // 改为不 return，继续走下面的写文件流程
                    } else {
                        String message;

                        try {
                            if (msg.startsWith("{")) {
                                JSONObject jsonObject = new JSONObject(msg);
                                message = jsonObject.toString(JSON_INDENT);
                            } else if (msg.startsWith("[")) {
                                JSONArray jsonArray = new JSONArray(msg);
                                message = jsonArray.toString(JSON_INDENT);
                            } else {
                                message = msg;
                            }
                        } catch (JSONException e) {
                            e(tag, (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()) + "\n" + msg);
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
                }

                // JSON 日志也保存到文件
                if (IS_SAVE_TO_FILE) {
                    String fullLog = getString(msg, logStr);
                    writeLogToFile(levelName, tag, fullLog);
                }
                return;
            }
        }

        // 保存日志到文件（非JSON类型）
        if (IS_SAVE_TO_FILE) {
            writeLogToFile(levelName, tag, logStr);
        }
    }

    /**
     * 定位"调用KLog的那个类"的栈帧。
     * 原来是硬编码 stackTrace[4]：调用链深度一变就越界。
     * 尤其是在子线程里直接调用 KLog（栈比主线程浅）会 ArrayIndexOutOfBoundsException，
     * 业务代码打个日志就崩溃。这里改为跳过 KLog 自身的所有栈帧，动态定位
     */
    private static StackTraceElement getTargetStackTraceElement() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return null;
        }

        boolean foundKLog = false;
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className != null && className.equals(KLog.class.getName())) {
                foundKLog = true;
                continue;
            }
            // 已经跳过了KLog自己的栈帧，第一个非KLog的帧就是调用方
            if (foundKLog) {
                return element;
            }
        }

        // 兜底：没找到KLog栈帧(极端情况)，取一个安全下标，绝不越界
        int index = Math.min(4, stackTrace.length - 1);
        return index >= 0 ? stackTrace[index] : null;
    }

    @NonNull
    private static String getString(String msg, String logStr) {
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
        return logStr + LINE_SEPARATOR + jsonToSave;
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
