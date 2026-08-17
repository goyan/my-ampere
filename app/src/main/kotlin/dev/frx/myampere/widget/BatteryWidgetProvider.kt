package dev.frx.myampere.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetPusher.pushStale(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
