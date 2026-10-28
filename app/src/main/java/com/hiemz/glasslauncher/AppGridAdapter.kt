package com.hiemz.glasslauncher

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/** Grid adapter: every item is a [GlassAppView] (glass tile + icon + label). */
class AppGridAdapter(
    private var apps: List<AppInfo>,
    private val onAppClicked: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.Holder>() {

    class Holder(val view: GlassAppView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = GlassAppView(parent.context)
        // Tighter cell than v1's 112dp: 64dp squircle + label below.
        val height = (96 * parent.context.resources.displayMetrics.density).toInt()
        view.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, height
        )
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.view.setApp(app)
        holder.view.setOnClickListener { onAppClicked(app) }
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
