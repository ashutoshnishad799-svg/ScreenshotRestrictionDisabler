package com.screenshot.disabler;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * ListView adapter showing installed apps with toggle switches.
 */
public class AppListAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private final AppPreferences preferences;
    private List<AppInfo> originalList;
    private List<AppInfo> filteredList;
    private final LayoutInflater inflater;

    public AppListAdapter(Context context, List<AppInfo> apps, AppPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
        this.originalList = new ArrayList<>(apps);
        this.filteredList = new ArrayList<>(apps);
        this.inflater = LayoutInflater.from(context);
    }

    public void updateApps(List<AppInfo> apps) {
        this.originalList = new ArrayList<>(apps);
        this.filteredList = new ArrayList<>(apps);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_app, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.app_icon);
            holder.name = convertView.findViewById(R.id.app_name);
            holder.packageName = convertView.findViewById(R.id.app_package);
            holder.toggle = convertView.findViewById(R.id.app_toggle);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo app = getItem(position);
        holder.icon.setImageDrawable(app.getIcon());
        holder.name.setText(app.getAppName());
        holder.packageName.setText(app.getPackageName());

        // Set tag to position so we can identify which switch was toggled
        holder.toggle.setTag(position);
        holder.toggle.setChecked(preferences.isAppEnabled(app.getPackageName()));

        holder.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = (Integer) buttonView.getTag();
            AppInfo clickedApp = filteredList.get(pos);
            preferences.setAppEnabled(clickedApp.getPackageName(), isChecked);
        });

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<AppInfo> results = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    results.addAll(originalList);
                } else {
                    String filterText = constraint.toString().toLowerCase();
                    for (AppInfo app : originalList) {
                        if (app.getAppName().toLowerCase().contains(filterText) ||
                                app.getPackageName().toLowerCase().contains(filterText)) {
                            results.add(app);
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = results;
                filterResults.count = results.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList = (List<AppInfo>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView packageName;
        Switch toggle;
    }
}
