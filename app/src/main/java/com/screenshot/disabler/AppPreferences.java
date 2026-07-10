package com.screenshot.disabler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages the list of selected app package names.
 * Uses a SharedPreferences file that is WORLD_READABLE so the Xposed hook
 * (running in the target app's process) can also read it.
 */
public class AppPreferences {

    private static final String PREFS_NAME = "secure_flag_prefs";
    private static final String KEY_ENABLED_APPS = "enabled_apps";
    private static final String KEY_MASTER_SWITCH = "master_switch";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        // MODE_WORLD_READABLE so Xposed hook in target app process can read
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);
    }

    public boolean isMasterSwitchEnabled() {
        return prefs.getBoolean(KEY_MASTER_SWITCH, false);
    }

    public void setMasterSwitch(boolean enabled) {
        prefs.edit().putBoolean(KEY_MASTER_SWITCH, enabled).apply();
    }

    public Set<String> getEnabledApps() {
        return prefs.getStringSet(KEY_ENABLED_APPS, new HashSet<>());
    }

    public boolean isAppEnabled(String packageName) {
        return getEnabledApps().contains(packageName);
    }

    public void setAppEnabled(String packageName, boolean enabled) {
        Set<String> current = new HashSet<>(getEnabledApps());
        if (enabled) {
            current.add(packageName);
        } else {
            current.remove(packageName);
        }
        prefs.edit().putStringSet(KEY_ENABLED_APPS, current).apply();
    }

    /**
     * Static method for the Xposed hook to read prefs from target app's context.
     * The target app process can read our WORLD_READABLE SharedPreferences.
     */
    public static boolean shouldDisableSecureFlag(Context context, String packageName) {
        try {
            Context ourContext = context.createPackageContext(
                    "com.screenshot.disabler",
                    Context.CONTEXT_IGNORE_SECURITY
            );
            SharedPreferences prefs = ourContext.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);

            // Check master switch first
            if (!prefs.getBoolean(KEY_MASTER_SWITCH, false)) {
                return false;
            }

            Set<String> enabledApps = prefs.getStringSet(KEY_ENABLED_APPS, new HashSet<>());
            return enabledApps.contains(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
