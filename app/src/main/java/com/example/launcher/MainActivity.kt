package com.example.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.launcher.ui.liquidglass.LauncherAppitem
import com.example.launcher.ui.liquidglass.LiquidGlassAppdrawer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val installedApps = loadInstalledApps()

        setContent {
            LiquidGlassAppdrawer(
                apps = installedApps,
                onAppClick = { app ->
                    packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                },
                onAppLongClick = { app ->
                    packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            )
        }
    }

    private fun loadInstalledApps(): List<LauncherAppitem> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        val selfPackage = packageName

        return resolveInfos
            .mapNotNull { info ->
                val pkgName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkgName == selfPackage) return@mapNotNull null

                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null

                val bitmap = runCatching {
                    info.loadIcon(packageManager).toBitmap(width = 128, height = 128).asImageBitmap()
                }.getOrNull()

                LauncherAppitem(
                    packageName = pkgName,
                    label = label,
                    icon = bitmap
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
