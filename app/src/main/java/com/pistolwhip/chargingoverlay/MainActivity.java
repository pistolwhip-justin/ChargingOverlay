package com.pistolwhip.chargingoverlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class MainActivity extends Activity {
    private static final int OVERLAY_REQUEST = 1001;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            try { startActivityForResult(intent, OVERLAY_REQUEST); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        } else {
            startOverlayService();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST && Settings.canDrawOverlays(this)) startOverlayService();
    }

    private void startOverlayService() {
        Intent service = new Intent(this, ChargingOverlayService.class);
        startForegroundService(service);
        finish();
    }
}
