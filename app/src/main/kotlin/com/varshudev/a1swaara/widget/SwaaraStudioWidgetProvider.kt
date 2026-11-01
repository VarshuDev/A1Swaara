package com.varshudev.a1swaara.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class SwaaraStudioWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        SwaaraWidgetManager.updateWidgets(
            context = context,
            mediaItem = null,
            isPlaying = SwaaraWidgetManager.lastIsPlaying,
            isLiked = SwaaraWidgetManager.lastIsLiked,
            artwork = SwaaraWidgetManager.lastArtwork
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action.startsWith("com.varshudev.a1swaara.widget.")) {
            SwaaraWidgetManager.handleWidgetAction(context, action)
        }
    }
}
