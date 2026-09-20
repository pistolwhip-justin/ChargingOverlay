package com.pistolwhip.chargingoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.service.quicksettings.TileService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class ChargingOverlayService extends Service {
    public static final String ACTION_SET_ENABLED = "com.pistolwhip.chargingoverlay.SET_ENABLED";
    public static final String EXTRA_ENABLED = "enabled";
    private static final String PREFS = "charging_overlay";
    private static final String KEY_ENABLED = "enabled";
    private static final String CHANNEL_ID = "charging_overlay_service";
    private static final int NOTIFICATION_ID = 100;

    private WindowManager windowManager;
    private ImageView dot;
    private BroadcastReceiver powerReceiver;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dot)
                .setContentTitle("ChargingOverlay")
                .setContentText("Charging indicator overlay is active")
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createDot();
        updateChargingState();

        powerReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                updateChargingState();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(powerReceiver, filter);
    }

    private void createDot() {
        dot = new ImageView(this);
        dot.setImageResource(R.drawable.ic_dot);
        dot.setColorFilter(Color.rgb(0, 200, 83));
        dot.setVisibility(View.GONE);

        int size = dp(8);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(7);
        params.y = dp(6);
        if (Build.VERSION.SDK_INT >= 28) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }

        try {
            windowManager.addView(dot, params);
        } catch (Exception ignored) {
        }
    }

    private void updateChargingState() {
        if (!Settings.canDrawOverlays(this)) {
            if (dot != null) dot.setVisibility(View.GONE);
            return;
        }

        boolean enabled = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true);

        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null || dot == null) return;

        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;

        dot.setVisibility(enabled && charging ? View.VISIBLE : View.GONE);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();

        if (enabled && Settings.canDrawOverlays(context)) {
            Intent service = new Intent(context, ChargingOverlayService.class);
            context.startForegroundService(service);
        } else {
            context.stopService(new Intent(context, ChargingOverlayService.class));
        }

        if (Build.VERSION.SDK_INT >= 24) {
            TileService.requestListeningState(
                    context,
                    new ComponentName(context, ChargingOverlayTileService.class));
        }
    }

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "ChargingOverlay", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the charging overlay service running");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SET_ENABLED.equals(intent.getAction())) {
            boolean enabled = intent.getBooleanExtra(EXTRA_ENABLED, true);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(KEY_ENABLED, enabled).apply();

            if (!enabled) {
                stopSelf();
                return START_NOT_STICKY;
            }
            updateChargingState();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (powerReceiver != null) {
            try { unregisterReceiver(powerReceiver); } catch (Exception ignored) {}
        }
        if (dot != null) {
            try { windowManager.removeView(dot); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}