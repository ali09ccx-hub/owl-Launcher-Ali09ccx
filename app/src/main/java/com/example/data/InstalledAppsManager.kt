package com.example.data

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.AppCategory
import com.example.model.AppInfo
import com.example.model.DeviceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class InstalledAppsManager(private val context: Context) {

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun loadInstalledApps(): List<AppInfo> = loadInstalledAppsTwoPhase(emptySet()) {}

    suspend fun loadInstalledAppsTwoPhase(
        priorityKeys: Set<String>,
        onPriorityLoaded: suspend (List<AppInfo>) -> Unit
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }
            .filter { it.activityInfo.packageName != context.packageName }

        val (priorityInfos, otherInfos) = if (priorityKeys.isNotEmpty()) {
            resolveInfos.partition { info ->
                "${info.activityInfo.packageName}/${info.activityInfo.name}" in priorityKeys
            }
        } else {
            emptyList<ResolveInfo>() to resolveInfos
        }

        val priorityApps = priorityInfos.map { buildAppInfo(pm, it) }
        if (priorityApps.isNotEmpty()) {
            onPriorityLoaded(
                priorityApps
                    .distinctBy { it.componentKey }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            )
        }

        val remainingApps = coroutineScope {
            otherInfos.chunked(16).map { chunk ->
                async(Dispatchers.IO) {
                    chunk.map { buildAppInfo(pm, it) }
                }
            }.awaitAll().flatten()
        }

        (priorityApps + remainingApps)
            .distinctBy { it.componentKey }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun buildAppInfo(pm: PackageManager, info: ResolveInfo): AppInfo {
        val pkg = info.activityInfo.packageName
        val cls = info.activityInfo.name
        val key = "$pkg/$cls"
        val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
        val iconBitmap = iconCache[key] ?: run {
            val drawable = info.loadIcon(pm)
            drawableToUnalteredBitmap(drawable)?.also { iconCache[key] = it }
        }
        val appInfo = info.activityInfo.applicationInfo
        val category = mapCategory(appInfo, pkg, label)
        val installTime = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L)).firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0).firstInstallTime
            }
        } catch (_: Exception) {
            0L
        }

        return AppInfo(
            packageName = pkg,
            activityName = cls,
            label = label,
            category = category,
            icon = iconBitmap,
            installTime = installTime
        )
    }

    fun clearIconCacheForPackage(packageName: String) {
        iconCache.keys.filter { it.startsWith("$packageName/") }.forEach { iconCache.remove(it) }
    }

    fun launchApp(app: AppInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                val fallback = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                    true
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openSystemSettings(action: String = Settings.ACTION_SETTINGS) {
        try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }

    @Suppress("PrivateApi", "WrongConstant")
    fun expandNotificationsShade(): Boolean {
        return try {
            val service = context.getSystemService("statusbar") ?: return false
            val clazz = Class.forName("android.app.StatusBarManager")
            val method = clazz.getMethod("expandNotificationsPanel")
            method.invoke(service)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isDefaultLauncher(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
            } else {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                @Suppress("DEPRECATION")
                val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                resolved?.activityInfo?.packageName == context.packageName
            }
        } catch (_: Exception) {
            false
        }
    }

    fun createSetDefaultLauncherIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun readDeviceStatus(currentTorchState: Boolean): DeviceStatus {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 100
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val freeBytes = try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBytes
        } catch (_: Exception) {
            0L
        }
        val freeText = Formatter.formatShortFileSize(context, freeBytes)

        val torchSupported = findFlashCameraId() != null

        return DeviceStatus(
            batteryPercent = percent.coerceIn(0, 100),
            isCharging = charging,
            freeStorageText = freeText,
            torchSupported = torchSupported,
            torchEnabled = currentTorchState
        )
    }

    fun setFlashlight(enable: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return false
            val cameraId = findFlashCameraId() ?: return false
            cameraManager.setTorchMode(cameraId, enable)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findFlashCameraId(): String? {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return null
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (_: Exception) {
            null
        }
    }

    fun registerPackageReceiver(onPackagesChanged: (String?) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val pkg = intent?.data?.schemeSpecificPart
                if (pkg != null) clearIconCacheForPackage(pkg)
                onPackagesChanged(pkg)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        return receiver
    }

    private fun mapCategory(appInfo: ApplicationInfo, pkg: String, label: String): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.MEDIA
                ApplicationInfo.CATEGORY_SOCIAL,
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.COMMUNICATION
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_MAPS,
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> return AppCategory.UTILITIES
            }
        }
        val lower = "$pkg $label".lowercase()
        return when {
            listOf("dialer", "phone", "contacts", "messaging", "sms", "whatsapp", "telegram", "mail", "gmail", "chat").any { lower.contains(it) } -> AppCategory.COMMUNICATION
            listOf("camera", "gallery", "photos", "music", "video", "youtube", "spotify", "media").any { lower.contains(it) } -> AppCategory.MEDIA
            listOf("docs", "notes", "calendar", "drive", "sheets", "office", "clock", "calculator").any { lower.contains(it) } -> AppCategory.PRODUCTIVITY
            listOf("settings", "files", "browser", "chrome", "maps", "store").any { lower.contains(it) } -> AppCategory.UTILITIES
            else -> AppCategory.OTHER
        }
    }

    private fun drawableToUnalteredBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val bmp = drawable.bitmap
                if (bmp.width in 48..320 && bmp.height in 48..320) {
                    return bmp.asImageBitmap()
                }
            }
            val size = 144
            val width = drawable.intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(size) ?: size
            val height = drawable.intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(size) ?: size
            val targetW = maxOf(width, 128)
            val targetH = maxOf(height, 128)
            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
