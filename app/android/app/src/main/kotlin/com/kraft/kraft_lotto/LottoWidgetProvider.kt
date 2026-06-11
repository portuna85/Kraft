package com.kraft.kraft_lotto

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LottoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }
}

private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
    val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
    val round   = prefs.getString("flutter.widget_round",   "--회") ?: "--회"
    val date    = prefs.getString("flutter.widget_date",    "") ?: ""
    val dday    = prefs.getString("flutter.widget_dday",    "D-?") ?: "D-?"
    val numStr  = prefs.getString("flutter.widget_numbers", "") ?: ""
    val bonusStr = prefs.getString("flutter.widget_bonus",  "0") ?: "0"

    val numbers = numStr.split(",").mapNotNull { it.trim().toIntOrNull() }
    val bonus   = bonusStr.toIntOrNull() ?: 0

    val views = RemoteViews(context.packageName, R.layout.lotto_widget)

    val roundLabel = if (date.isNotBlank()) "$round · $date" else round
    views.setTextViewText(R.id.widget_round, roundLabel)
    views.setTextViewText(R.id.widget_dday, dday)

    val ballViewIds = listOf(R.id.ball1, R.id.ball2, R.id.ball3, R.id.ball4, R.id.ball5, R.id.ball6)
    ballViewIds.forEachIndexed { i, viewId ->
        val num = numbers.getOrNull(i) ?: 0
        views.setTextViewText(viewId, if (num > 0) num.toString() else "?")
        views.setInt(viewId, "setBackgroundResource", ballDrawable(num))
    }
    views.setTextViewText(R.id.ball_bonus, if (bonus > 0) bonus.toString() else "?")
    views.setInt(R.id.ball_bonus, "setBackgroundResource", ballDrawable(bonus))

    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

    manager.updateAppWidget(id, views)
}

private fun ballDrawable(num: Int): Int = when (num) {
    in 1..10  -> R.drawable.ball_bg_yellow
    in 11..20 -> R.drawable.ball_bg_blue
    in 21..30 -> R.drawable.ball_bg_red
    in 31..40 -> R.drawable.ball_bg_gray
    in 41..45 -> R.drawable.ball_bg_green
    else      -> R.drawable.ball_bg_gray
}
