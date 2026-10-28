package com.hiemz.glasslauncher

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Home screen: app grid over the wallpaper, every icon on a glass tile. */
class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperBlurProvider.prepare(this)
        setContentView(R.layout.activity_main)

        val grid = findViewById<RecyclerView>(R.id.app_grid)
        val span = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 6 else 4
        grid.layoutManager = GridLayoutManager(this, span)
        adapter = AppGridAdapter(emptyList()) { app -> launchApp(app) }
        grid.adapter = adapter
        grid.addOnScrollListener(refreshTilesOnScrollIdle(grid))

        findViewById<View>(R.id.drawer_button).setOnClickListener {
            startActivity(Intent(this, DrawerActivity::class.java))
        }
        refreshApps()
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

    /**
     * Tiles blur the wallpaper region behind them, so after scrolling their
     * backgrounds must be recomputed once the grid stops moving.
     */
    private fun refreshTilesOnScrollIdle(grid: RecyclerView) =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = rv.layoutManager as GridLayoutManager
                    val first = lm.findFirstVisibleItemPosition()
                    val last = lm.findLastVisibleItemPosition()
                    if (first >= 0 && last >= first) {
                        adapter.notifyItemRangeChanged(first, last - first + 1)
                    }
                }
            }
        }
}
