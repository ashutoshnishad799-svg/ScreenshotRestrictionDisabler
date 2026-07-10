package com.screenshot.disabler;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "secure_flag_prefs";
    private static final String KEY_ENABLED_APPS = "enabled_apps";
    private static final String KEY_MASTER_SWITCH = "master_switch";
    private static final String KEY_SHOW_SYSTEM_APPS = "show_system_apps";

    private AppPreferences preferences;

    private Switch masterSwitch;
    private EditText searchBox;
    private ListView appListView;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView emptyText;
    private Switch systemAppSwitch;

    private AppListAdapter adapter;
    private List<AppInfo> allApps;
    private List<AppInfo> visibleApps;
    private boolean showSystemApps = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = new AppPreferences(this);

        // Initialize views
        masterSwitch = findViewById(R.id.master_switch);
        searchBox = findViewById(R.id.search_box);
        appListView = findViewById(R.id.app_list);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);
        emptyText = findViewById(R.id.empty_text);
        systemAppSwitch = findViewById(R.id.system_app_switch);

        // Load saved preferences
        masterSwitch.setChecked(preferences.isMasterSwitchEnabled());
        showSystemApps = getSharedPreferences("ui_prefs", MODE_PRIVATE)
                .getBoolean(KEY_SHOW_SYSTEM_APPS, false);
        systemAppSwitch.setChecked(showSystemApps);

        // Master switch listener
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setMasterSwitch(isChecked);
            updateStatusText();
            Toast.makeText(this,
                    isChecked ? "Module activated! Select apps below." : "Module deactivated.",
                    Toast.LENGTH_SHORT).show();
        });

        // System app toggle
        systemAppSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showSystemApps = isChecked;
            getSharedPreferences("ui_prefs", MODE_PRIVATE)
                    .edit().putBoolean(KEY_SHOW_SYSTEM_APPS, isChecked).apply();
            filterAndDisplayApps();
        });

        // Search box listener
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        updateStatusText();
        loadInstalledApps();
    }

    private void loadInstalledApps() {
        progressBar.setVisibility(View.VISIBLE);
        appListView.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        AppLoader.loadApps(this, apps -> {
            allApps = apps;
            progressBar.setVisibility(View.GONE);

            visibleApps = new ArrayList<>();
            filterAndDisplayApps();
        });
    }

    private void filterAndDisplayApps() {
        if (allApps == null) return;

        visibleApps = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (!showSystemApps && app.isSystemApp()) {
                continue;
            }
            visibleApps.add(app);
        }

        if (visibleApps.isEmpty()) {
            appListView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText("No apps found.");
        } else {
            appListView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);

            if (adapter == null) {
                adapter = new AppListAdapter(this, visibleApps, preferences);
                appListView.setAdapter(adapter);
            } else {
                adapter.updateApps(visibleApps);
            }
        }
    }

    private void updateStatusText() {
        int count = preferences.getEnabledApps().size();
        if (preferences.isMasterSwitchEnabled()) {
            statusText.setText("✅ Active — " + count + " app(s) selected");
            statusText.setTextColor(getColor(R.color.green_active));
        } else {
            statusText.setText("⚠️ Module is OFF — Turn on master switch to activate");
            statusText.setTextColor(getColor(R.color.red_inactive));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusText();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadInstalledApps();
            return true;
        } else if (id == R.id.action_clear) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Selection")
                    .setMessage("Remove all apps from the list?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        Set<String> empty = new HashSet<>();
                        getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
                                .edit().putStringSet(KEY_ENABLED_APPS, empty).apply();
                        if (adapter != null) adapter.notifyDataSetChanged();
                        updateStatusText();
                        Toast.makeText(this, "Selection cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle("About")
                    .setMessage("Screenshot Restriction Disabler v1.0\n\n"
                            + "An Xposed/LSPosed module that disables FLAG_SECURE "
                            + "on selected apps, allowing screenshots and screen recording.\n\n"
                            + "Requirements:\n"
                            + "• Root with LSPosed/Xposed framework\n"
                            + "• Enable this module in LSPosed Manager\n"
                            + "• Select target apps in LSPosed scope\n"
                            + "• Turn on master switch and select apps here\n"
                            + "• Restart target apps for changes to take effect")
                    .setPositiveButton("OK", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
