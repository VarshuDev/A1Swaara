package com.varshudev.a1swaara.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.media3.common.MediaItem
import com.varshudev.a1swaara.MainActivity
import com.varshudev.a1swaara.R

object SwaaraWidgetManager {
    const val ACTION_PLAY_PAUSE = "com.varshudev.a1swaara.widget.PLAY_PAUSE"
    const val ACTION_NEXT = "com.varshudev.a1swaara.widget.NEXT"
    const val ACTION_PREV = "com.varshudev.a1swaara.widget.PREV"
    const val ACTION_LIKE = "com.varshudev.a1swaara.widget.LIKE"

    var lastTitle: String = "A1 Swaara"
    var lastArtist: String = "Studio Audio"
    var lastIsPlaying: Boolean = false
    var lastIsLiked: Boolean = false
    var lastArtwork: Bitmap? = null

    fun updateWidgets(
        context: Context,
        mediaItem: MediaItem?,
        isPlaying: Boolean,
        isLiked: Boolean,
        artwork: Bitmap?
    ) {
        val title = mediaItem?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() } ?: "A1 Swaara"
        val artist = mediaItem?.mediaMetadata?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Studio Audio"

        lastTitle = title
        lastArtist = artist
        lastIsPlaying = isPlaying
        lastIsLiked = isLiked
        lastArtwork = artwork

        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        // 1. Update Studio Widget (4x2)
        val studioComponent = ComponentName(context, SwaaraStudioWidgetProvider::class.java)
        val studioIds = appWidgetManager.getAppWidgetIds(studioComponent)
        if (studioIds != null && studioIds.isNotEmpty()) {
            for (widgetId in studioIds) {
                val views = buildStudioRemoteViews(context, title, artist, isPlaying, isLiked, artwork)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        // 2. Update Compact Widget (4x1)
        val compactComponent = ComponentName(context, SwaaraCompactWidgetProvider::class.java)
        val compactIds = appWidgetManager.getAppWidgetIds(compactComponent)
        if (compactIds != null && compactIds.isNotEmpty()) {
            for (widgetId in compactIds) {
                val views = buildCompactRemoteViews(context, title, artist, isPlaying, artwork)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun buildStudioRemoteViews(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        isLiked: Boolean,
        artwork: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.swaara_widget_studio)
        views.setTextViewText(R.id.widget_studio_title, title)
        views.setTextViewText(R.id.widget_studio_artist, artist)
        views.setImageViewResource(
            R.id.widget_studio_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )
        views.setImageViewResource(
            R.id.widget_studio_like,
            if (isLiked) R.drawable.ic_widget_heart else R.drawable.ic_widget_heart_outline
        )

        if (artwork != null) {
            val rounded = createRoundedBitmap(context, artwork, 16f)
            views.setImageViewBitmap(R.id.widget_studio_art, rounded)
        } else {
            views.setImageViewResource(R.id.widget_studio_art, R.drawable.app_icon)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_studio_art, contentIntent)
        views.setOnClickPendingIntent(R.id.widget_studio_info_container, contentIntent)

        views.setOnClickPendingIntent(
            R.id.widget_studio_play_pause,
            getBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 101)
        )
        views.setOnClickPendingIntent(
            R.id.widget_studio_prev,
            getBroadcastPendingIntent(context, ACTION_PREV, 102)
        )
        views.setOnClickPendingIntent(
            R.id.widget_studio_next,
            getBroadcastPendingIntent(context, ACTION_NEXT, 103)
        )
        views.setOnClickPendingIntent(
            R.id.widget_studio_like,
            getBroadcastPendingIntent(context, ACTION_LIKE, 104)
        )

        return views
    }

    private fun buildCompactRemoteViews(
        context: Context,
        title: String,
        artist: String,
        isPlaying: Boolean,
        artwork: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.swaara_widget_compact)
        views.setTextViewText(R.id.widget_compact_title, title)
        views.setTextViewText(R.id.widget_compact_artist, artist)
        views.setImageViewResource(
            R.id.widget_compact_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        if (artwork != null) {
            val rounded = createRoundedBitmap(context, artwork, 12f)
            views.setImageViewBitmap(R.id.widget_compact_art, rounded)
        } else {
            views.setImageViewResource(R.id.widget_compact_art, R.drawable.app_icon)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_compact_art, contentIntent)
        views.setOnClickPendingIntent(R.id.widget_compact_info_container, contentIntent)

        views.setOnClickPendingIntent(
            R.id.widget_compact_play_pause,
            getBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 201)
        )
        views.setOnClickPendingIntent(
            R.id.widget_compact_prev,
            getBroadcastPendingIntent(context, ACTION_PREV, 202)
        )
        views.setOnClickPendingIntent(
            R.id.widget_compact_next,
            getBroadcastPendingIntent(context, ACTION_NEXT, 203)
        )

        return views
    }

    fun handleWidgetAction(context: Context, action: String) {
        when (action) {
            ACTION_PLAY_PAUSE -> {
                val broadcastAction = if (lastIsPlaying) "com.varshudev.a1swaara.pause" else "com.varshudev.a1swaara.play"
                context.sendBroadcast(Intent(broadcastAction).setPackage(context.packageName))
            }
            ACTION_NEXT -> {
                context.sendBroadcast(Intent("com.varshudev.a1swaara.next").setPackage(context.packageName))
            }
            ACTION_PREV -> {
                context.sendBroadcast(Intent("com.varshudev.a1swaara.previous").setPackage(context.packageName))
            }
            ACTION_LIKE -> {
                context.sendBroadcast(Intent("com.varshudev.a1swaara.like").setPackage(context.packageName))
            }
        }
    }

    private fun getBroadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, SwaaraStudioWidgetProvider::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createRoundedBitmap(context: Context, src: Bitmap, radiusDp: Float): Bitmap {
        val density = context.resources.displayMetrics.density
        val radiusPx = radiusDp * density
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, src.width, src.height)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        return output
    }
}
