package ru.mediasoft.datamanager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class DataManager {
     public static void init(Activity activity) {
        Intent intent = new Intent(activity, FloatingDBButtonService.class);
        activity.startService(intent);
    }

    public static void destroy(Activity activity) {
        Intent intent = new Intent(activity, FloatingDBButtonService.class);
        activity.stopService(intent);
    }
}
