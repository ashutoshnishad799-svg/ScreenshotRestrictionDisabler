package com.screenshot.disabler;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

/**
 * Simple data class representing an installed app for the list UI.
 */
public class AppInfo implements Comparable<AppInfo> {

    private final String packageName;
    private final String appName;
    private final Drawable icon;
    private final boolean isSystemApp;

    public AppInfo(String packageName, String appName, Drawable icon, boolean isSystemApp) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.isSystemApp = isSystemApp;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    @Override
    public int compareTo(AppInfo other) {
        return this.appName.compareToIgnoreCase(other.appName);
    }
}
