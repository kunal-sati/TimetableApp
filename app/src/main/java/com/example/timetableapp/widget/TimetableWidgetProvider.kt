package com.example.timetableapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.timetableapp.MainActivity
import com.example.timetableapp.R
import com.example.timetableapp.data.Period
import com.example.timetableapp.data.TimetableDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TimetableWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Create an Intent to launch the main activity when widget is clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            // Set initial values
            views.setTextViewText(R.id.widget_current_period, "Current: Loading...")
            views.setTextViewText(R.id.widget_next_period, "Next: Loading...")

            // Update widget with current and next period
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val calendar = Calendar.getInstance()
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Adjust to 1-7 (Monday-Sunday)
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val currentTime = timeFormat.format(calendar.time)

                    Log.d("TimetableWidget", "Updating widget for day $dayOfWeek at time $currentTime")

                    // Get database instance
                    val db = TimetableDatabase.getDatabase(context)
                    val periodDao = db.periodDao()

                    // Get all periods and filter manually
                    // This is a workaround since we can't directly access LiveData value in a background thread
                    val allPeriods = db.periodDao().getAllPeriodsSync()
                    val periods = allPeriods.filter { it.dayOfWeek == dayOfWeek }

                    var currentPeriod: Period? = null
                    var nextPeriod: Period? = null

                    for (period in periods) {
                        if (currentTime >= period.startTime && currentTime <= period.endTime) {
                            currentPeriod = period
                        } else if (currentTime < period.startTime &&
                            (nextPeriod == null || period.startTime < nextPeriod.startTime)) {
                            nextPeriod = period
                        }
                    }

                    // Update the widget on the main thread
                    val currentText = if (currentPeriod != null) {
                        "Current: ${currentPeriod.subject} (${currentPeriod.startTime}-${currentPeriod.endTime})"
                    } else {
                        "Current: None"
                    }

                    val nextText = if (nextPeriod != null) {
                        "Next: ${nextPeriod.subject} (${nextPeriod.startTime}-${nextPeriod.endTime})"
                    } else {
                        "Next: None"
                    }

                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.widget_current_period, currentText)
                        views.setTextViewText(R.id.widget_next_period, nextText)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    Log.e("TimetableWidget", "Error updating widget: ${e.message}")
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.widget_current_period, "Current: Error")
                        views.setTextViewText(R.id.widget_next_period, "Next: Error")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }
}
