package com.hiemz.glasslauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Home screen: app grid over the wallpaper, every icon a glass squircle.
 * The window draws edge-to-edge behind the system bars and shows its own
 * wallpaper bitmap (the same bitmap the glass blur is cropped from), so
 * glass and background can never misalign.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppGridAdapter

    private val wallpaperChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            WallpaperBlurProvider.invalidate()
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperBlurProvider.prepare(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_main)
        findViewById<ImageView>(R.id.wallpaper_bg)
            .setImageBitmap(WallpaperBlurProvider.wallpaperBitmap)

        val grid = findViewById<RecyclerView>(R.id.app_grid)
        val span = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 4
        grid.layoutManager = GridLayoutManager(this, span)
        adapter = AppGridAdapter(emptyList()) { app -> launchApp(app) }
        grid.adapter = adapter

        findViewById<View>(R.id.drawer_button).setOnClickListener {
            startActivity(Intent(this, DrawerActivity::class.java))
        }
        registerReceiver(
            wallpaperChangedReceiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
        )
        refreshApps()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(wallpaperChangedReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was never registered.
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        refreshApps()
    }

    private fun refreshApps() {
        adapter.updateApps(AppList.load(packageManager))
    }

    private fun launchApp(app: AppInfo) {
        packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
    }
}
