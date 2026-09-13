package com.nextos.launcher.util;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Thin Usage Stats API wrapper for App Library "Terbaru" suggestions.
 */
public final class UsageStatsHelper {

    private UsageStatsHelper() {
    }

    public static List<String> recentPackages(Context context, int limit) {
        List<String> out = new ArrayList<>();
        if (!PermissionHelper.hasUsageStats(context)) {
            return out;
        }
        UsageStatsManager usm =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) {
            return out;
        }
        long end = System.currentTimeMillis();
        long start = end - TimeUnit.DAYS.toMillis(2);
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
        if (stats == null || stats.isEmpty()) {
            return out;
        }
        Collections.sort(stats, new Comparator<UsageStats>() {
            @Override
            public int compare(UsageStats a, UsageStats b) {
                return Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed());
            }
        });
        String self = context.getPackageName();
        for (UsageStats s : stats) {
            String pkg = s.getPackageName();
            if (pkg == null || pkg.equals(self) || out.contains(pkg)) {
                continue;
            }
            if (s.getLastTimeUsed() <= 0) {
                continue;
            }
            out.add(pkg);
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }
}
