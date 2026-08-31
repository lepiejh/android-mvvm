package com.ved.framework.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.format.Formatter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ved on 2017/5/14.
 * SD卡相关工具类
 */
public final class SDCardUtils {

    private SDCardUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 判断SD卡是否可用
     *
     * @return true : 可用<br>false : 不可用
     */
    public static boolean isSDCardEnable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 获取SD卡路径
     * <p>先用shell，shell失败再普通方法获取，一般是/storage/emulated/0/</p>
     *
     * @return SD卡路径
     */
    public static String getSDCardPath() {
        if (!isSDCardEnable()) return null;
        String cmd = "cat /proc/mounts";
        Runtime run = Runtime.getRuntime();
        BufferedReader bufferedReader = null;
        try {
            Process p = run.exec(cmd);
            bufferedReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(p.getInputStream())));
            String lineStr;
            while ((lineStr = bufferedReader.readLine()) != null) {
                if (lineStr.contains("sdcard") && lineStr.contains(".android_secure")) {
                    String[] strArray = lineStr.split(" ");
                    if (strArray.length >= 5) {
                        return strArray[1].replace("/.android_secure", "") + File.separator;
                    }
                }
                if (p.waitFor() != 0 && p.exitValue() == 1) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeIO(bufferedReader);
        }
        return Environment.getExternalStorageDirectory().getPath() + File.separator;
    }

    /**
     * 获取SD卡data路径
     *
     * @return SD卡data路径
     */
    public static String getDataPath() {
        if (!isSDCardEnable()) return null;
        return Environment.getExternalStorageDirectory().getPath() + File.separator + "data" + File.separator;
    }

    /**
     * 获取SD卡剩余空间
     *
     * @return SD卡剩余空间
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getFreeSpace() {
        if (!isSDCardEnable()) return null;
        StatFs stat = new StatFs(getSDCardPath());
        long blockSize, availableBlocks;
        availableBlocks = stat.getAvailableBlocksLong();
        blockSize = stat.getBlockSizeLong();
        return ConvertUtils.byte2FitMemorySize(availableBlocks * blockSize);
    }

    /**
     * 获取SD卡信息
     *
     * @return SDCardInfo
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static String getSDCardInfo() {
        if (!isSDCardEnable()) return null;
        SDCardInfo sd = new SDCardInfo();
        sd.isExist = true;
        StatFs sf = new StatFs(Environment.getExternalStorageDirectory().getPath());
        sd.totalBlocks = sf.getBlockCountLong();
        sd.blockByteSize = sf.getBlockSizeLong();
        sd.availableBlocks = sf.getAvailableBlocksLong();
        sd.availableBytes = sf.getAvailableBytes();
        sd.freeBlocks = sf.getFreeBlocksLong();
        sd.freeBytes = sf.getFreeBytes();
        sd.totalBytes = sf.getTotalBytes();
        return sd.toString();
    }

    ///////////////////////////////////////////////////////////////////////////
    // 以下为合并自 com.ved.framework.utils.bland.code.SDCardUtils 的方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return whether sdcard is enabled by environment.
     *
     * @return {@code true}: enabled<br>{@code false}: disabled
     */
    public static boolean isSDCardEnableByEnvironment() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Return the path of sdcard by environment.
     *
     * @return the path of sdcard by environment
     */
    public static String getSDCardPathByEnvironment() {
        if (isSDCardEnableByEnvironment()) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return "";
    }

    /**
     * Return the ptah of mounted sdcard.
     *
     * @return the ptah of mounted sdcard.
     */
    public static List<String> getMountedSDCardPath() {
        List<String> path = new ArrayList<>();
        List<SDCardInfo> sdCardInfo = getSDCardInfoList();
        if (sdCardInfo == null || sdCardInfo.isEmpty()) return path;
        for (SDCardInfo cardInfo : sdCardInfo) {
            String state = cardInfo.state;
            if (state == null) continue;
            if ("mounted".equals(state.toLowerCase())) {
                path.add(cardInfo.path);
            }
        }
        return path;
    }

    /**
     * Return the total size of external storage
     *
     * @return the total size of external storage
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static long getExternalTotalSize() {
        return getFsTotalSize(getSDCardPathByEnvironment());
    }

    /**
     * Return the available size of external storage.
     *
     * @return the available size of external storage
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static long getExternalAvailableSize() {
        return getFsAvailableSize(getSDCardPathByEnvironment());
    }

    /**
     * Return the total size of internal storage
     *
     * @return the total size of internal storage
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static long getInternalTotalSize() {
        return getFsTotalSize(Environment.getDataDirectory().getAbsolutePath());
    }

    /**
     * Return the available size of internal storage.
     *
     * @return the available size of internal storage
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static long getInternalAvailableSize() {
        return getFsAvailableSize(Environment.getDataDirectory().getAbsolutePath());
    }

    ///////////////////////////////////////////////////////////////////////////
    // 以下为合并时从 UtilsBridge 内联实现的辅助方法
    ///////////////////////////////////////////////////////////////////////////

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static long getFsTotalSize(String path) {
        StatFs statFs = new StatFs(path);
        long blockSize = statFs.getBlockSizeLong();
        long totalBlocks = statFs.getBlockCountLong();
        return blockSize * totalBlocks;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static long getFsAvailableSize(String path) {
        StatFs statFs = new StatFs(path);
        long blockSize = statFs.getBlockSizeLong();
        long availableBlocks = statFs.getAvailableBlocksLong();
        return blockSize * availableBlocks;
    }

    /**
     * 获取SD卡信息列表（合并自 bland.code.SDCardUtils.getSDCardInfo()，因原方法名与 utils 版返回 String 的 getSDCardInfo() 冲突，故改名）
     *
     * @return SDCardInfo 列表
     */
    public static List<SDCardInfo> getSDCardInfoList() {
        List<SDCardInfo> paths = new ArrayList<>();
        StorageManager sm = (StorageManager) Utils.getContext().getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) return paths;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            List<StorageVolume> storageVolumes = sm.getStorageVolumes();
            try {
                //noinspection JavaReflectionMemberAccess
                Method getPathMethod = StorageVolume.class.getMethod("getPath");
                for (StorageVolume storageVolume : storageVolumes) {
                    boolean isRemovable = storageVolume.isRemovable();
                    String state = storageVolume.getState();
                    String path = (String) getPathMethod.invoke(storageVolume);
                    paths.add(new SDCardInfo(path, state, isRemovable));
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
                //noinspection JavaReflectionMemberAccess
                Method getPathMethod = storageVolumeClazz.getMethod("getPath");
                Method isRemovableMethod = storageVolumeClazz.getMethod("isRemovable");
                //noinspection JavaReflectionMemberAccess
                Method getVolumeStateMethod = StorageManager.class.getMethod("getVolumeState", String.class);
                //noinspection JavaReflectionMemberAccess
                Method getVolumeListMethod = StorageManager.class.getMethod("getVolumeList");
                Object result = getVolumeListMethod.invoke(sm);
                final int length = Array.getLength(result);
                for (int i = 0; i < length; i++) {
                    Object storageVolumeElement = Array.get(result, i);
                    String path = (String) getPathMethod.invoke(storageVolumeElement);
                    boolean isRemovable = (Boolean) isRemovableMethod.invoke(storageVolumeElement);
                    String state = (String) getVolumeStateMethod.invoke(sm, path);
                    paths.add(new SDCardInfo(path, state, isRemovable));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }

    public static class SDCardInfo {
        // utils 版字段
        boolean isExist;
        long    totalBlocks;
        long    freeBlocks;
        long    availableBlocks;
        long    blockByteSize;
        long    totalBytes;
        long    freeBytes;
        long    availableBytes;
        // bland 版字段
        private String  path;
        private String  state;
        private boolean isRemovable;
        private long    totalSize;
        private long    availableSize;

        public SDCardInfo() {
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        SDCardInfo(String path, String state, boolean isRemovable) {
            this.path = path;
            this.state = state;
            this.isRemovable = isRemovable;
            this.totalSize = getFsTotalSize(path);
            this.availableSize = getFsAvailableSize(path);
        }

        public String getPath() {
            return path;
        }

        public String getState() {
            return state;
        }

        public boolean isRemovable() {
            return isRemovable;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public long getAvailableSize() {
            return availableSize;
        }

        @Override
        public String toString() {
            return "isExist=" + isExist +
                    "\ntotalBlocks=" + totalBlocks +
                    "\nfreeBlocks=" + freeBlocks +
                    "\navailableBlocks=" + availableBlocks +
                    "\nblockByteSize=" + blockByteSize +
                    "\ntotalBytes=" + totalBytes +
                    "\nfreeBytes=" + freeBytes +
                    "\navailableBytes=" + availableBytes +
                    "\npath=" + path +
                    ", state=" + state +
                    ", isRemovable=" + isRemovable +
                    ", totalSize=" + Formatter.formatFileSize(Utils.getContext(), totalSize) +
                    ", availableSize=" + Formatter.formatFileSize(Utils.getContext(), availableSize);
        }
    }
}
