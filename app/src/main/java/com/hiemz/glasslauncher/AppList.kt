package com.hiemz.glasslauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** One launchable app shown in the grid. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

object AppList {
    /** All launchable apps, sorted alphabetically by label. */
    fun load(pm: PackageManager): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = ArrayList<AppInfo>()
        for (ri in pm.queryIntentActivities(intent, 0)) {
            val label = ri.loadLabel(pm)?.toString() ?: continue
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pm.getLaunchIntentForPackage(pkg) == null) continue
            apps += AppInfo(label, pkg, ri.loadIcon(pm))
        }
        return apps.sortedBy { it.label.lowercase() }
    }
}
