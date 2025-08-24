package com.roamtool.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if overlay permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            !Settings.canDrawOverlays(this)) {
            // Request overlay permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            // Permission already granted, start the bubble service
            startBubbleService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                Settings.canDrawOverlays(this)) {
                // Permission granted, start the bubble service
                startBubbleService();
            } else {
                // Permission denied, finish the app
                finish();
            }
        }
    }

    private void startBubbleService() {
        Intent serviceIntent = new Intent(this, BubbleOverlayService.class);
        startService(serviceIntent);
        // Finish the activity so the app becomes invisible
        finish();
    }
}
