package com.ved.framework.utils;

import static android.Manifest.permission.EXPAND_STATUS_BAR;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

public class NotificationUtils {

    public static Notification showNotification(Context mContext, @NonNull Class<?> cls, String title, String content, @DrawableRes int icon, int importance){

        NotificationManager notificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        //准备intent
        Intent intent = new Intent();
        intent.setClass(mContext, cls);

        //notification
        Notification notification = null;
        String contentText;
        // 构建 PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(mContext, 1, intent, flags);
        //版本兼容
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new NotificationCompat.Builder(mContext)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(icon)
                    .setContentIntent(pi)
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();

        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new Notification.Builder(mContext)
                    .setAutoCancel(true)//设置点击通知栏消息后，通知消息自动消失
                    .setContentIntent(pi)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String CHANNEL_ID = "patrol_work_channel_id";
            CharSequence name = "patrol_work_channel";
            String Description = "patrol work channel";
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 100, 200});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);

            notification = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(importance)
                    .setContentIntent(pi)
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();

        }

        notificationManager.notify(getNotifyId(), notification);
        return notification;
    }


    public static Notification showNotification(Context mContext,String title,String content, @DrawableRes int icon,int importance,Intent intentParam){
        NotificationManager notificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = null;
        String contentText;
        // 构建 PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(mContext, 1, intentParam, flags);
        //版本兼容
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new NotificationCompat.Builder(mContext)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(icon)
                    .setContentIntent(pi)
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();

        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new Notification.Builder(mContext)
                    .setAutoCancel(true)//设置点击通知栏消息后，通知消息自动消失
                    .setContentIntent(pi)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String CHANNEL_ID = "patrol_work_channel_id";
            CharSequence name = "patrol_work_channel";
            String Description = "patrol work channel";
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 100, 200});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);

            notification = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(importance)
                    .setContentIntent(pi)
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();

        }

        notificationManager.notify(getNotifyId(), notification);
        return notification;
    }


    public static Notification getNotification(Context mContext,String title,String content, @DrawableRes int icon,int importance,String channelId,Intent intent){

        NotificationManager notificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        //准备intent
//        intent.setClass(mContext, MessageActivity.class);

        //notification
        Notification notification = null;
        String contentText;
        // 构建 PendingIntent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(mContext, 1, intent, flags);
        //版本兼容
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new NotificationCompat.Builder(mContext)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(icon)
                    .setContentIntent(pi)
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();

        } else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            notification = new Notification.Builder(mContext)
                    .setAutoCancel(true)//设置点击通知栏消息后，通知消息自动消失
                    .setContentIntent(pi)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String CHANNEL_ID =channelId;
            CharSequence name = "patrol_work_channel";
            String Description = "patrol work channel";
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 100, 200});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);

            notification = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(importance)
                    .setContentIntent(pi)
                    .setVibrate(new long[]{0, 1000, 1000, 1000}) //通知栏消息震动
                    .setLights(Color.GREEN, 1000, 2000) //通知栏消息闪灯(亮一秒间隔两秒再亮)
                    .build();

        }
        return notification;
    }

    private static int getNotifyId(){
        return (int)(Math.random()*9+1)*1000+(int)(Math.random()*9+1)*100+(int)(Math.random()*9+1)*10+(int)(Math.random()*9+1);
    }

    ///////////////////////////////////////////////////////////////////////////
    // 以下为合并自 com.ved.framework.utils.bland.code.NotificationUtils 的方法
    ///////////////////////////////////////////////////////////////////////////

    public static final int IMPORTANCE_UNSPECIFIED = -1000;
    public static final int IMPORTANCE_NONE        = 0;
    public static final int IMPORTANCE_MIN         = 1;
    public static final int IMPORTANCE_LOW         = 2;
    public static final int IMPORTANCE_DEFAULT     = 3;
    public static final int IMPORTANCE_HIGH        = 4;

    @IntDef({IMPORTANCE_UNSPECIFIED, IMPORTANCE_NONE, IMPORTANCE_MIN, IMPORTANCE_LOW, IMPORTANCE_DEFAULT, IMPORTANCE_HIGH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Importance {
    }

    /**
     * Return whether the notifications enabled.
     *
     * @return {@code true}: yes<br>{@code false}: no
     */
    public static boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(Utils.getContext()).areNotificationsEnabled();
    }

    /**
     * Post a notification to be shown in the status bar.
     *
     * @param id       An identifier for this notification.
     * @param consumer The consumer of create the builder of notification.
     */
    public static void notify(int id, Utils.Consumer<NotificationCompat.Builder> consumer) {
        notify(null, id, ChannelConfig.DEFAULT_CHANNEL_CONFIG, consumer);
    }

    /**
     * Post a notification to be shown in the status bar.
     *
     * @param tag      A string identifier for this notification.  May be {@code null}.
     * @param id       An identifier for this notification.
     * @param consumer The consumer of create the builder of notification.
     */
    public static void notify(String tag, int id, Utils.Consumer<NotificationCompat.Builder> consumer) {
        notify(tag, id, ChannelConfig.DEFAULT_CHANNEL_CONFIG, consumer);
    }

    /**
     * Post a notification to be shown in the status bar.
     *
     * @param id            An identifier for this notification.
     * @param channelConfig The notification channel of config.
     * @param consumer      The consumer of create the builder of notification.
     */
    public static void notify(int id, ChannelConfig channelConfig, Utils.Consumer<NotificationCompat.Builder> consumer) {
        notify(null, id, channelConfig, consumer);
    }

    /**
     * Post a notification to be shown in the status bar.
     *
     * @param tag           A string identifier for this notification.  May be {@code null}.
     * @param id            An identifier for this notification.
     * @param channelConfig The notification channel of config.
     * @param consumer      The consumer of create the builder of notification.
     */
    public static void notify(String tag, int id, ChannelConfig channelConfig, Utils.Consumer<NotificationCompat.Builder> consumer) {
        NotificationManagerCompat.from(Utils.getContext()).notify(tag, id, getNotification(channelConfig, consumer));
    }


    public static Notification getNotification(ChannelConfig channelConfig, Utils.Consumer<NotificationCompat.Builder> consumer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) Utils.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            //noinspection ConstantConditions
            nm.createNotificationChannel(channelConfig.getNotificationChannel());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(Utils.getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(channelConfig.mNotificationChannel.getId());
        }
        if (consumer != null) {
            consumer.accept(builder);
        }

        return builder.build();
    }

    /**
     * Cancel The notification.
     *
     * @param tag The tag for the notification will be cancelled.
     * @param id  The identifier for the notification will be cancelled.
     */
    public static void cancel(String tag, final int id) {
        NotificationManagerCompat.from(Utils.getContext()).cancel(tag, id);
    }

    /**
     * Cancel The notification.
     *
     * @param id The identifier for the notification will be cancelled.
     */
    public static void cancel(final int id) {
        NotificationManagerCompat.from(Utils.getContext()).cancel(id);
    }

    /**
     * Cancel all of the notifications.
     */
    public static void cancelAll() {
        NotificationManagerCompat.from(Utils.getContext()).cancelAll();
    }

    /**
     * Set the notification bar's visibility.
     * <p>Must hold {@code <uses-permission android:name="android.permission.EXPAND_STATUS_BAR" />}</p>
     *
     * @param isVisible True to set notification bar visible, false otherwise.
     */
    @RequiresPermission(EXPAND_STATUS_BAR)
    public static void setNotificationBarVisibility(final boolean isVisible) {
        String methodName;
        if (isVisible) {
            methodName = (Build.VERSION.SDK_INT <= 16) ? "expand" : "expandNotificationsPanel";
        } else {
            methodName = (Build.VERSION.SDK_INT <= 16) ? "collapse" : "collapsePanels";
        }
        invokePanels(methodName);
    }

    private static void invokePanels(final String methodName) {
        try {
            @SuppressLint("WrongConstant")
            Object service = Utils.getContext().getSystemService("statusbar");
            @SuppressLint("PrivateApi")
            Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");
            Method expand = statusBarManager.getMethod(methodName);
            expand.invoke(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ChannelConfig {

        public static final ChannelConfig DEFAULT_CHANNEL_CONFIG = new ChannelConfig(
                Utils.getContext().getPackageName(), Utils.getContext().getPackageName(), IMPORTANCE_DEFAULT
        );

        private NotificationChannel mNotificationChannel;

        public ChannelConfig(String id, CharSequence name, @Importance int importance) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel = new NotificationChannel(id, name, importance);
            }
        }

        public NotificationChannel getNotificationChannel() {
            return mNotificationChannel;
        }

        /**
         * Sets whether or not notifications posted to this channel can interrupt the user in
         * {@link NotificationManager.Policy#INTERRUPTION_FILTER_PRIORITY} mode.
         * <p>
         * Only modifiable by the system and notification ranker.
         */
        public ChannelConfig setBypassDnd(boolean bypassDnd) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setBypassDnd(bypassDnd);
            }
            return this;
        }

        /**
         * Sets the user visible description of this channel.
         *
         * <p>The recommended maximum length is 300 characters; the value may be truncated if it is too
         * long.
         */
        public ChannelConfig setDescription(String description) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setDescription(description);
            }
            return this;
        }

        /**
         * Sets what group this channel belongs to.
         * <p>
         * Group information is only used for presentation, not for behavior.
         * <p>
         * Only modifiable before the channel is submitted to
         * {@link NotificationManager#createNotificationChannel(NotificationChannel)}, unless the
         * channel is not currently part of a group.
         *
         * @param groupId the id of a group created by
         *                {@link NotificationManager#createNotificationChannelGroup)}.
         */
        public ChannelConfig setGroup(String groupId) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setGroup(groupId);
            }
            return this;
        }

        /**
         * Sets the level of interruption of this notification channel.
         * <p>
         * Only modifiable before the channel is submitted to
         * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
         *
         * @param importance the amount the user should be interrupted by
         *                   notifications from this channel.
         */
        public ChannelConfig setImportance(@Importance int importance) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setImportance(importance);
            }
            return this;
        }

        /**
         * Sets the notification light color for notifications posted to this channel, if lights are
         * {@link NotificationChannel#enableLights(boolean) enabled} on this channel and the device supports that feature.
         * <p>
         * Only modifiable before the channel is submitted to
         * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
         */
        public ChannelConfig setLightColor(int argb) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setLightColor(argb);
            }
            return this;
        }

        /**
         * Sets whether notifications posted to this channel appear on the lockscreen or not, and if so,
         * whether they appear in a redacted form. See e.g. {@link Notification#VISIBILITY_SECRET}.
         * <p>
         * Only modifiable by the system and notification ranker.
         */
        public ChannelConfig setLockscreenVisibility(int lockscreenVisibility) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setLockscreenVisibility(lockscreenVisibility);
            }
            return this;
        }

        /**
         * Sets the user visible name of this channel.
         *
         * <p>The recommended maximum length is 40 characters; the value may be truncated if it is too
         * long.
         */
        public ChannelConfig setName(CharSequence name) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setName(name);
            }
            return this;
        }

        /**
         * Sets whether notifications posted to this channel can appear as application icon badges
         * in a Launcher.
         * <p>
         * Only modifiable before the channel is submitted to
         * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
         *
         * @param showBadge true if badges should be allowed to be shown.
         */
        public ChannelConfig setShowBadge(boolean showBadge) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setShowBadge(showBadge);
            }
            return this;
        }

        /**
         * Sets the sound that should be played for notifications posted to this channel and its
         * audio attributes. Notification channels with an {@link NotificationChannel#getImportance() importance} of at
         * least {@link NotificationManager#IMPORTANCE_DEFAULT} should have a sound.
         * <p>
         * Only modifiable before the channel is submitted to
         * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
         */
        public ChannelConfig setSound(Uri sound, AudioAttributes audioAttributes) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setSound(sound, audioAttributes);
            }
            return this;
        }

        /**
         * Sets the vibration pattern for notifications posted to this channel. If the provided
         * pattern is valid (non-null, non-empty), will {@link NotificationChannel#enableVibration(boolean)} enable
         * vibration} as well. Otherwise, vibration will be disabled.
         * <p>
         * Only modifiable before the channel is submitted to
         * {@link NotificationManager#createNotificationChannel(NotificationChannel)}.
         */
        public ChannelConfig setVibrationPattern(long[] vibrationPattern) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationChannel.setVibrationPattern(vibrationPattern);
            }
            return this;
        }
    }
}