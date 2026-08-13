package com.ved.framework.utils;

import static android.Manifest.permission.CALL_PHONE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.content.FileProvider;

import com.ved.framework.R;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hanlyjiang on 2018/5/16-16:53.
 * @version 1.0
 */

public class IntentUtils {

    static final String IS_DOCUMENT = "isDoc";

    /**
     * 通过文件后缀名来获取对应的Intent
     *
     * @param context  Context 对象
     * @param fileName 文件名称
     * @return 对应的Intent 或 null - 不支持的文件类型
     */
    public static Intent getFileViewIntent(Context context, String fileName) {
        String ext = FileViewerUtils.getExtension(fileName);
        boolean isDocument = false;
        if (TextUtils.isEmpty(ext)) {
            ToastUtils.showLong(context.getString(R.string.file_not_recognized));
            return null;
        }
        Intent intent = null;
        if (".jpg".equalsIgnoreCase(ext) || ".jpeg".equalsIgnoreCase(ext) || ".png".equalsIgnoreCase(ext) || ".gif".equalsIgnoreCase(ext)) {
            intent = getImageFileIntent(fileName);
        } else if (".xls".equalsIgnoreCase(ext) || ".xlsx".equalsIgnoreCase(ext) || ".et".equalsIgnoreCase(ext) || ".ett".equalsIgnoreCase(ext)) {
            intent = getExcelFileIntent(fileName);
            isDocument = true;
        } else if (".doc".equalsIgnoreCase(ext) || ".docx".equalsIgnoreCase(ext) || ".wps".equalsIgnoreCase(ext) || ".wpt".equalsIgnoreCase(ext)) {
            intent = getWordFileIntent(fileName);
            isDocument = true;
        } else if (".ppt".equalsIgnoreCase(ext) || ".pptx".equalsIgnoreCase(ext) || ".dps".equalsIgnoreCase(ext) || ".dpt".equalsIgnoreCase(ext)) {
            intent = getPptFileIntent(fileName);
            isDocument = true;
        } else if (".pdf".equalsIgnoreCase(ext)) {
            intent = getPdfFileIntent(fileName);
            isDocument = true;
        } else if (".htm".equalsIgnoreCase(ext) || ".html".equalsIgnoreCase(ext) || ".jsp".equalsIgnoreCase(ext) || ".css".equalsIgnoreCase(ext)) {
            intent = getHtmlFileIntent(fileName);
        } else if (".txt".equalsIgnoreCase(ext) || ".text".equalsIgnoreCase(ext)) {
            intent = getHtmlFileIntent(fileName);
        } else if (".mp3".equalsIgnoreCase(ext) || ".wma".equalsIgnoreCase(ext) || ".aar".equalsIgnoreCase(ext) || ".m4a".equalsIgnoreCase(ext)) {
            intent = getAudioFileIntent(fileName);
        } else if (".mp4".equalsIgnoreCase(ext) || ".avi".equalsIgnoreCase(ext) || ".flv".equalsIgnoreCase(ext)) {
            intent = getVideoFileIntent(fileName);
        } else {

        }
        if (intent != null) {
            intent.putExtra(IS_DOCUMENT, isDocument);
        }
        return intent;
    }

    static Intent getExcelFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "application/vnd.ms-excel");
        return intent;
    }

    static Intent getHtmlFileIntent(String param) {
        Uri uri = Uri.parse(param).buildUpon().encodedAuthority("com.android.htmlfileprovider").scheme("content").encodedPath(param).build();
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(uri, "text/html");
        return intent;
    }


    static Intent getImageFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "image/*");
        return intent;
    }


    static Intent getPdfFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "application/pdf");
        return intent;
    }


    static Intent getTextFileIntent(String paramString, boolean paramBoolean) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (paramBoolean) {
            Uri uri1 = Uri.parse(paramString);
            intent.setDataAndType(uri1, "text/plain");
        }
        return intent;
    }


    static Intent getAudioFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "audio/*");
        return intent;
    }


    static Intent getVideoFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "video/*");
        return intent;
    }

    static Intent getChmFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "application/x-chm");
        return intent;
    }

    static Intent getWordFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "application/msword");
        return intent;
    }

    static Intent getPptFileIntent(String param) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        return intent;
    }

    static Intent getApkFileIntent(String fileName) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(new File(fileName));
        intent.setDataAndType(uri, "application/vnd.android");
        return intent;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 以下为合并自 com.ved.framework.utils.bland.code.IntentUtils 的方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return whether the intent is available.
     *
     * @param intent The intent.
     * @return {@code true}: yes<br>{@code false}: no
     */
    public static boolean isIntentAvailable(final Intent intent) {
        return Utils.getApp()
                .getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .size() > 0;
    }

    /**
     * Return the intent of install app.
     * <p>Target APIs greater than 25 must hold
     * {@code <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />}</p>
     *
     * @param filePath The path of file.
     * @return the intent of install app
     */
    public static Intent getInstallAppIntent(final String filePath) {
        return getInstallAppIntent(getFileByPath(filePath));
    }

    /**
     * Return the intent of install app.
     * <p>Target APIs greater than 25 must hold
     * {@code <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />}</p>
     *
     * @param file The file.
     * @return the intent of install app
     */
    public static Intent getInstallAppIntent(final File file) {
        if (!isFileExists(file)) return null;
        Uri uri;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {
            String authority = Utils.getApp().getPackageName() + ".utilcode.fileprovider";
            uri = FileProvider.getUriForFile(Utils.getApp(), authority, file);
        }
        return getInstallAppIntent(uri);
    }

    /**
     * Return the intent of install app.
     * <p>Target APIs greater than 25 must hold
     * {@code <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />}</p>
     *
     * @param uri The uri.
     * @return the intent of install app
     */
    public static Intent getInstallAppIntent(final Uri uri) {
        if (uri == null) return null;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String type = "application/vnd.android.package-archive";
        intent.setDataAndType(uri, type);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Return the intent of uninstall app.
     * <p>Target APIs greater than 25 must hold
     * Must hold {@code <uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />}</p>
     *
     * @param pkgName The name of the package.
     * @return the intent of uninstall app
     */
    public static Intent getUninstallAppIntent(final String pkgName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + pkgName));
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Return the intent of launch app.
     *
     * @param pkgName The name of the package.
     * @return the intent of launch app
     */
    public static Intent getLaunchAppIntent(final String pkgName) {
        String launcherActivity = getLauncherActivity(pkgName);
        if (StringUtils.isSpace(launcherActivity)) return null;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName(pkgName, launcherActivity);
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Return the intent of launch app details settings.
     *
     * @param pkgName The name of the package.
     * @return the intent of launch app details settings
     */
    public static Intent getLaunchAppDetailsSettingsIntent(final String pkgName) {
        return getLaunchAppDetailsSettingsIntent(pkgName, false);
    }

    /**
     * Return the intent of launch app details settings.
     *
     * @param pkgName The name of the package.
     * @return the intent of launch app details settings
     */
    public static Intent getLaunchAppDetailsSettingsIntent(final String pkgName, final boolean isNewTask) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + pkgName));
        return getIntent(intent, isNewTask);
    }

    /**
     * Return the intent of share text.
     *
     * @param content The content.
     * @return the intent of share text
     */
    public static Intent getShareTextIntent(final String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent = Intent.createChooser(intent, "");
        return getIntent(intent, true);
    }

    /**
     * Return the intent of share image.
     *
     * @param imagePath The path of image.
     * @return the intent of share image
     */
    public static Intent getShareImageIntent(final String imagePath) {
        return getShareTextImageIntent("", imagePath);
    }

    /**
     * Return the intent of share image.
     *
     * @param imageFile The file of image.
     * @return the intent of share image
     */
    public static Intent getShareImageIntent(final File imageFile) {
        return getShareTextImageIntent("", imageFile);
    }

    /**
     * Return the intent of share image.
     *
     * @param imageUri The uri of image.
     * @return the intent of share image
     */
    public static Intent getShareImageIntent(final Uri imageUri) {
        return getShareTextImageIntent("", imageUri);
    }

    /**
     * Return the intent of share image.
     *
     * @param content   The content.
     * @param imagePath The path of image.
     * @return the intent of share image
     */
    public static Intent getShareTextImageIntent(@Nullable final String content, final String imagePath) {
        return getShareTextImageIntent(content, getFileByPath(imagePath));
    }

    /**
     * Return the intent of share image.
     *
     * @param content   The content.
     * @param imageFile The file of image.
     * @return the intent of share image
     */
    public static Intent getShareTextImageIntent(@Nullable final String content, final File imageFile) {
        return getShareTextImageIntent(content, file2Uri(imageFile));
    }

    /**
     * Return the intent of share image.
     *
     * @param content  The content.
     * @param imageUri The uri of image.
     * @return the intent of share image
     */
    public static Intent getShareTextImageIntent(@Nullable final String content, final Uri imageUri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, "");
        return getIntent(intent, true);
    }

    /**
     * Return the intent of share images.
     *
     * @param imagePaths The paths of images.
     * @return the intent of share images
     */
    public static Intent getShareImageIntent(final LinkedList<String> imagePaths) {
        return getShareTextImageIntent("", imagePaths);
    }

    /**
     * Return the intent of share images.
     *
     * @param images The files of images.
     * @return the intent of share images
     */
    public static Intent getShareImageIntent(final List<File> images) {
        return getShareTextImageIntent("", images);
    }

    /**
     * Return the intent of share images.
     *
     * @param uris The uris of image.
     * @return the intent of share image
     */
    public static Intent getShareImageIntent(final ArrayList<Uri> uris) {
        return getShareTextImageIntent("", uris);
    }

    /**
     * Return the intent of share images.
     *
     * @param content    The content.
     * @param imagePaths The paths of images.
     * @return the intent of share images
     */
    public static Intent getShareTextImageIntent(@Nullable final String content,
                                                 final LinkedList<String> imagePaths) {
        List<File> files = new ArrayList<>();
        if (imagePaths != null) {
            for (String imagePath : imagePaths) {
                File file = getFileByPath(imagePath);
                if (file != null) {
                    files.add(file);
                }
            }
        }
        return getShareTextImageIntent(content, files);
    }

    /**
     * Return the intent of share images.
     *
     * @param content The content.
     * @param images  The files of images.
     * @return the intent of share images
     */
    public static Intent getShareTextImageIntent(@Nullable final String content, final List<File> images) {
        ArrayList<Uri> uris = new ArrayList<>();
        if (images != null) {
            for (File image : images) {
                Uri uri = file2Uri(image);
                if (uri != null) {
                    uris.add(uri);
                }
            }
        }
        return getShareTextImageIntent(content, uris);
    }

    /**
     * Return the intent of share images.
     *
     * @param content The content.
     * @param uris    The uris of image.
     * @return the intent of share image
     */
    public static Intent getShareTextImageIntent(@Nullable final String content, final ArrayList<Uri> uris) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.setType("image/*");
        intent = Intent.createChooser(intent, "");
        return getIntent(intent, true);
    }

    /**
     * Return the intent of component.
     *
     * @param pkgName   The name of the package.
     * @param className The name of class.
     * @return the intent of component
     */
    public static Intent getComponentIntent(final String pkgName, final String className) {
        return getComponentIntent(pkgName, className, null, false);
    }

    /**
     * Return the intent of component.
     *
     * @param pkgName   The name of the package.
     * @param className The name of class.
     * @param isNewTask True to add flag of new task, false otherwise.
     * @return the intent of component
     */
    public static Intent getComponentIntent(final String pkgName,
                                            final String className,
                                            final boolean isNewTask) {
        return getComponentIntent(pkgName, className, null, isNewTask);
    }

    /**
     * Return the intent of component.
     *
     * @param pkgName   The name of the package.
     * @param className The name of class.
     * @param bundle    The Bundle of extras to add to this intent.
     * @return the intent of component
     */
    public static Intent getComponentIntent(final String pkgName,
                                            final String className,
                                            final Bundle bundle) {
        return getComponentIntent(pkgName, className, bundle, false);
    }

    /**
     * Return the intent of component.
     *
     * @param pkgName   The name of the package.
     * @param className The name of class.
     * @param bundle    The Bundle of extras to add to this intent.
     * @param isNewTask True to add flag of new task, false otherwise.
     * @return the intent of component
     */
    public static Intent getComponentIntent(final String pkgName,
                                            final String className,
                                            final Bundle bundle,
                                            final boolean isNewTask) {
        Intent intent = new Intent();
        if (bundle != null) intent.putExtras(bundle);
        ComponentName cn = new ComponentName(pkgName, className);
        intent.setComponent(cn);
        return getIntent(intent, isNewTask);
    }

    /**
     * Return the intent of shutdown.
     * <p>Requires root permission
     * or hold {@code android:sharedUserId="android.uid.system"},
     * {@code <uses-permission android:name="android.permission.SHUTDOWN" />}
     * in manifest.</p>
     *
     * @return the intent of shutdown
     */
    public static Intent getShutdownIntent() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
        } else {
            intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
        }
        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Return the intent of dial.
     *
     * @param phoneNumber The phone number.
     * @return the intent of dial
     */
    public static Intent getDialIntent(@NonNull final String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phoneNumber)));
        return getIntent(intent, true);
    }

    /**
     * Return the intent of call.
     * <p>Must hold {@code <uses-permission android:name="android.permission.CALL_PHONE" />}</p>
     *
     * @param phoneNumber The phone number.
     * @return the intent of call
     */
    @RequiresPermission(CALL_PHONE)
    public static Intent getCallIntent(@NonNull final String phoneNumber) {
        Intent intent = new Intent("android.intent.action.CALL", Uri.parse("tel:" + Uri.encode(phoneNumber)));
        return getIntent(intent, true);
    }

    /**
     * Return the intent of send SMS.
     *
     * @param phoneNumber The phone number.
     * @param content     The content of SMS.
     * @return the intent of send SMS
     */
    public static Intent getSendSmsIntent(@NonNull final String phoneNumber, final String content) {
        Uri uri = Uri.parse("smsto:" + Uri.encode(phoneNumber));
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra("sms_body", content);
        return getIntent(intent, true);
    }

    /**
     * Return the intent of capture.
     *
     * @param outUri The uri of output.
     * @return the intent of capture
     */
    public static Intent getCaptureIntent(final Uri outUri) {
        return getCaptureIntent(outUri, false);
    }

    /**
     * Return the intent of capture.
     *
     * @param outUri    The uri of output.
     * @param isNewTask True to add flag of new task, false otherwise.
     * @return the intent of capture
     */
    public static Intent getCaptureIntent(final Uri outUri, final boolean isNewTask) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return getIntent(intent, isNewTask);
    }

    private static Intent getIntent(final Intent intent, final boolean isNewTask) {
        return isNewTask ? intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) : intent;
    }

    ///////////////////////////////////////////////////////////////////////////
    // 以下为合并时从 UtilsBridge 内联实现的辅助方法
    ///////////////////////////////////////////////////////////////////////////

    private static File getFileByPath(final String filePath) {
        return StringUtils.isSpace(filePath) ? null : new File(filePath);
    }

    private static boolean isFileExists(final File file) {
        return file != null && file.exists();
    }

    private static String getLauncherActivity(final String pkgName) {
        if (StringUtils.isSpace(pkgName)) return "";
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(pkgName);
        List<ResolveInfo> lists = Utils.getApp().getPackageManager().queryIntentActivities(intent, 0);
        return lists.size() > 0 ? lists.get(0).activityInfo.name : "";
    }

    private static Uri file2Uri(final File file) {
        if (file == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = Utils.getApp().getPackageName() + ".utilcode.fileprovider";
            return FileProvider.getUriForFile(Utils.getApp(), authority, file);
        } else {
            return Uri.fromFile(file);
        }
    }
}
