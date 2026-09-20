package com.pistolwhip.chargingoverlay;

import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class ChargingOverlayTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override public void onClick() {
        super.onClick();
        boolean newState = !ChargingOverlayService.isEnabled(this);

        if (newState && !Settings.canDrawOverlays(this)) {
            startActivityAndCollapse(new android.content.Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            updateTile();
            return;
        }

        ChargingOverlayService.setEnabled(this, newState);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean enabled = ChargingOverlayService.isEnabled(this);
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("ChargingOverlay");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_plug));
        tile.updateTile();
    }
}