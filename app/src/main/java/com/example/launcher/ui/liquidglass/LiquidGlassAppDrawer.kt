package com.example.launcher.ui.liquidglass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LauncherAppitem(
    val packageName: String,
    val label: String,
    val category: String = "All",
    val icon: ImageBitmap? = null
)

private val LIQUID_GLASS_AGSL = """
    uniform float2 uResolution;
    uniform float uTime;

    half4 main(float2 fragCoord) {
        float2 safeRes = max(uResolution, float2(1.0, 1.0));
        float2 uv = fragCoord / safeRes;
        float wave1 = sin(uv.x * 6.2831 + uTime * 1.1) * cos(uv.y * 5.0 - uTime * 0.8);
        float wave2 = sin((uv.x + uv.y) * 4.5 - uTime * 0.6);
        float mixFactor = clamp(0.5 + 0.25 * wave1 + 0.15 * wave2, 0.0, 1.0);

        float3 deepTeal = float3(0.06, 0.14, 0.22);
        float3 glassCyan = float3(0.16, 0.44, 0.58);
        float3 softViolet = float3(0.32, 0.24, 0.52);

        float3 baseColor = mix(deepTeal, glassCyan, mixFactor);
        float3 finalColor = mix(baseColor, softViolet, clamp(wave2 * 0.35 + 0.25, 0.0, 1.0));
        return half4(finalColor, 1.0);
    }
""".trimIndent()

@Composable
fun LiquidGlassAppdrawer(
    apps: List<LauncherAppitem>,
    onAppClick: (LauncherAppitem) -> Unit,
    onAppLongClick: (LauncherAppitem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_anim")
    val uTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "uTime_anim"
    )

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.label.contains(searchQuery.trim(), ignoreCase = true) ||
                    app.packageName.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    val fallbackBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F2438),
                Color(0xFF1D4E66),
                Color(0xFF3A2D66)
            )
        )
    }

    val shaderBrush: Brush = remember(uTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val shader = RuntimeShader(LIQUID_GLASS_AGSL)
                shader.setFloatUniform("uResolution", 1080f, 2400f)
                shader.setFloatUniform("uTime", uTime)
                ShaderBrush(shader)
            }.getOrElse { fallbackBrush }
        } else {
            fallbackBrush
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shaderBrush)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12
