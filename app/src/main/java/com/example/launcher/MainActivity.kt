package com.example.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.launcher.ui.liquidglass.LauncherAppItem
import com.example.launcher.ui.liquidglass.LiquidGlassAppDrawer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var apps by remember { mutableStateOf<List<LauncherAppItem>>(emptyList()) }

                    LaunchedEffect(Unit) {
                        apps = loadInstalledApps()
                    }

                    LiquidGlassAppDrawer(
                        apps = apps,
                        onAppClick = { app ->
                            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "تعذر فتح ${app.label}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onAppLongClick = { app ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${app.packageName}")
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    private fun loadInstalledApps(): List<LauncherAppItem> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(mainIntent, 0)
        }

        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == packageName) return@mapNotNull null
                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isEmpty()) return@mapNotNull null
                val iconBitmap = drawableToImageBitmap(info.loadIcon(packageManager))
                val category = guessCategory(pkg, label)
                LauncherAppItem(
                    packageName = pkg,
                    label = label,
                    category = category,
                    icon = iconBitmap
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun guessCategory(pkg: String, label: String): String {
        val text = "$pkg $label".lowercase()
        return when {
            listOf("camera", "photo", "gallery", "video", "music", "spotify", "youtube").any { it in text } -> "الوسائط"
            listOf("whatsapp", "telegram", "chat", "mail", "gmail", "message", "sms", "phone").any { it in text } -> "التواصل"
            listOf("game", "play").any { it in text } -> "الألعاب"
            listOf("browser", "chrome", "maps", "settings", "clock", "calc", "files").any { it in text } -> "الأدوات"
            else -> "عام"
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
