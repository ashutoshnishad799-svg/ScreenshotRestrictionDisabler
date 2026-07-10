package com.screenshot.disabler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed hook module — runs inside each target app's process.
 * Intercepts Window.setFlags() and Window.addFlags() calls to strip FLAG_SECURE.
 */
public class SecureFlagHook implements IXposedHookLoadPackage {

    private static final String TAG = "SecureFlagDisabler";
    private static final String PREFS_NAME = "secure_flag_prefs";
    private static final String KEY_ENABLED_APPS = "enabled_apps";
    private static final String KEY_MASTER_SWITCH = "master_switch";
    private static final String MODULE_PACKAGE = "com.screenshot.disabler";

    // Window flags
    private static final int FLAG_SECURE = 0x00002000;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Don't hook our own module
        if (lpparam.packageName.equals(MODULE_PACKAGE)) {
            return;
        }

        final String targetPackage = lpparam.packageName;

        // Hook Window.setFlags(int, int)
        XposedHelpers.findAndHookMethod(
                Window.class,
                "setFlags",
                int.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!shouldDisableSecureFlag(targetPackage)) {
                            return;
                        }
                        int flags = (int) param.args[0];
                        int mask = (int) param.args[1];

                        // Remove FLAG_SECURE from both flags and mask
                        flags &= ~FLAG_SECURE;
                        mask &= ~FLAG_SECURE;

                        param.args[0] = flags;
                        param.args[1] = mask;
                    }
                }
        );

        // Hook Window.addFlags(int)
        XposedHelpers.findAndHookMethod(
                Window.class,
                "addFlags",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!shouldDisableSecureFlag(targetPackage)) {
                            return;
                        }
                        int flags = (int) param.args[0];
                        flags &= ~FLAG_SECURE;
                        param.args[0] = flags;
                    }
                }
        );

        // Hook WindowManager.LayoutParams.flags field set via reflection
        // Some apps set flags directly on LayoutParams
        try {
            XposedHelpers.findAndHookMethod(
                    WindowManager.LayoutParams.class,
                    "setTitle",
                    CharSequence.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            // LayoutParams is often accessed after setTitle
                            // We can't easily intercept field sets, but setFlags/addFlags covers most apps
                        }
                    }
            );
        } catch (Exception ignored) {}

        // Hook SurfaceView or TextureView secure surface if needed
        // Some apps use Surface.setSecure() (hidden API)
        try {
            Class<?> surfaceClass = Class.forName("android.view.Surface");
            Method setSecureMethod = surfaceClass.getDeclaredMethod("setSecure", boolean.class);
            XposedBridge.hookMethod(setSecureMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (shouldDisableSecureFlag(targetPackage)) {
                        param.args[0] = false;
                    }
                }
            });
        } catch (Exception ignored) {
            // Not all Android versions have this method
        }

        XposedBridge.log(TAG + ": Hooked package: " + targetPackage);
    }

    /**
     * Reads the module's SharedPreferences from the target app's process.
     * The prefs file is WORLD_READABLE so the target process can access it.
     */
    private boolean shouldDisableSecureFlag(String packageName) {
        try {
            Context targetContext = (Context) XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", null),
                    "currentApplication"
            );

            if (targetContext == null) {
                return false;
            }

            Context moduleContext = targetContext.createPackageContext(
                    MODULE_PACKAGE,
                    Context.CONTEXT_IGNORE_SECURITY
            );

            SharedPreferences prefs = moduleContext.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_WORLD_READABLE
            );

            // Check master switch
            if (!prefs.getBoolean(KEY_MASTER_SWITCH, false)) {
                return false;
            }

            Set<String> enabledApps = prefs.getStringSet(KEY_ENABLED_APPS, new HashSet<>());
            return enabledApps.contains(packageName);

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error reading prefs: " + e.getMessage());
            return false;
        }
    }
}
