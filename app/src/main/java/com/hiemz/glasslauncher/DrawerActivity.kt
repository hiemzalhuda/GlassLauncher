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

/** App drawer: all launchable apps, alphabetical, on glass squircles. */
class DrawerActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_drawer)
        findViewById<ImageView>(R.id.wallpaper_bg)
            .setImageBitmap(WallpaperBlurProvider.wallpaperBitmap)

        val grid = findViewById<RecyclerView>(R.id.app_grid)
        val span = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 4
        grid.layoutManager = GridLayoutManager(this, span)
        adapter = AppGridAdapter(emptyList()) { app -> launchApp(app) }
        grid.adapter = adapter

        findViewById<View>(R.id.close_button).setOnClickListener { finish() }
        registerReceiver(
            wallpaperChangedReceiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
        )
        adapter.updateApps(AppList.load(packageManager))
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(wallpaperChangedReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was never registered.
        }
        super.onDestroy()
    }

    private fun launchApp(app: AppInfo) {
        packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
    }
}
