package com.example.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.AvailableWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OwlAppWidgetHost(private val context: Context) {

    companion object {
        const val HOST_ID = 1024
        const val REQUEST_CONFIGURE_APPWIDGET = 9002
    }

    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context.applicationContext, HOST_ID)
    private val hostViewCache = HashMap<Int, AppWidgetHostView>()

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {
        }
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {
        }
    }

    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteWidgetId(appWidgetId: Int) {
        hostViewCache.remove(appWidgetId)
        if (appWidgetId > 0) {
            try {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            } catch (_: Exception) {
            }
        }
    }

    fun findProviderInfo(providerPackage: String, providerClass: String): AppWidgetProviderInfo? {
        val target = ComponentName(providerPackage, providerClass)
        return appWidgetManager.installedProviders.firstOrNull { it.provider == target }
    }

    fun getProviderInfoForWidgetId(appWidgetId: Int): AppWidgetProviderInfo? {
        return appWidgetManager.getAppWidgetInfo(appWidgetId)
    }

    fun tryBindWidget(appWidgetId: Int, provider: AppWidgetProviderInfo): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)
        } catch (_: Exception) {
            false
        }
    }

    fun createBindPermissionIntent(appWidgetId: Int, provider: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
        }
    }

    fun needsConfiguration(provider: AppWidgetProviderInfo): Boolean {
        return provider.configure != null
    }

    fun startConfigureActivity(activity: Activity, appWidgetId: Int) {
        try {
            appWidgetHost.startAppWidgetConfigureActivityForResult(
                activity,
                appWidgetId,
                0,
                REQUEST_CONFIGURE_APPWIDGET,
                null
            )
        } catch (_: Exception) {
        }
    }

    fun createHostView(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): AppWidgetHostView {
        val existing = hostViewCache[appWidgetId]
        if (existing != null) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }
        val view = appWidgetHost.createView(context, appWidgetId, providerInfo)
        view.setAppWidget(appWidgetId, providerInfo)
        view.setPadding(0, 0, 0, 0)
        hostViewCache[appWidgetId] = view
        return view
    }

    fun updateWidgetSize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        try {
            val safeW = widthDp.coerceAtLeast(48)
            val safeH = heightDp.coerceAtLeast(48)
            val opts = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, safeW)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, safeW)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, safeH)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, safeH)
            }
            view.updateAppWidgetOptions(opts)
        } catch (_: Exception) {
        }
    }

    suspend fun loadAvailableProviders(): List<AvailableWidgetProvider> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        appWidgetManager.installedProviders
            .filter { it.provider.packageName != context.packageName }
            .map { info ->
                val appLabel = try {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(
                            info.provider.packageName,
                            PackageManagerFlagHelper.zero()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(info.provider.packageName, 0)
                    }
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    info.provider.packageName
                }

                val widgetLabel = info.loadLabel(pm)?.takeIf { it.isNotBlank() } ?: appLabel
                val previewDrawable = try {
                    info.loadPreviewImage(context, DisplayMetrics.DENSITY_HIGH)
                        ?: info.loadIcon(context, DisplayMetrics.DENSITY_HIGH)
                } catch (_: Exception) {
                    null
                }

                AvailableWidgetProvider(
                    providerPackage = info.provider.packageName,
                    providerClass = info.provider.className,
                    appLabel = appLabel,
                    widgetLabel = widgetLabel,
                    minWidthDp = (info.minWidth / density).toInt().coerceAtLeast(60),
                    minHeightDp = (info.minHeight / density).toInt().coerceAtLeast(60),
                    previewBitmap = previewDrawable?.let { drawableToBitmap(it) }
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel })
    }

    private fun drawableToBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            val w = drawable.intrinsicWidth.takeIf { it > 0 }?.coerceIn(64, 360) ?: 160
            val h = drawable.intrinsicHeight.takeIf { it > 0 }?.coerceIn(64, 240) ?: 120
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

private object PackageManagerFlagHelper {
    fun zero(): android.content.pm.PackageManager.ApplicationInfoFlags {
        return android.content.pm.PackageManager.ApplicationInfoFlags.of(0L)
    }
}
