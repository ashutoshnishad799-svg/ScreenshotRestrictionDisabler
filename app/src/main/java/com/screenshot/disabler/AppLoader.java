package com.screenshot.disabler;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads installed apps in a background thread.
 */
public class AppLoader {

    public interface OnAppsLoadedListener {
        void onAppsLoaded(List<AppInfo> apps);
    }

    public static void loadApps(Context context, OnAppsLoadedListener listener) {
        new LoadAppsTask(context, listener).execute();
    }

    private static class LoadAppsTask extends AsyncTask<Void, Void, List<AppInfo>> {
        private final Context context;
        private final OnAppsLoadedListener listener;

        LoadAppsTask(Context context, OnAppsLoadedListener listener) {
            this.context = context.getApplicationContext();
            this.listener = listener;
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfo> appList = new ArrayList<>();

            for (ApplicationInfo info : packages) {
                String name = pm.getApplicationLabel(info).toString();
                Drawable icon = pm.getApplicationIcon(info);
                boolean isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                appList.add(new AppInfo(info.packageName, name, icon, isSystem));
            }

            Collections.sort(appList);
            return appList;
        }

        @Override
        protected void onPostExecute(List<AppInfo> apps) {
            if (listener != null) {
                listener.onAppsLoaded(apps);
            }
        }
    }
}
