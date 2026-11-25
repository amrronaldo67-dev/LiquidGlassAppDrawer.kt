package com.example.launcher.ui.liquidglass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

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

        float2 p = uv * 2.0 - 1.0;
        float w1 = sin(p.x * 3.2 + uTime * 0.9) * cos(p.y * 2.8 - uTime * 0.7);
        float w2 = sin((p.x - p.y) * 4.0 + uTime * 0.65);
        float caustic = pow(clamp(0.5 + 0.5 * sin((uv.x + w1 * 0.08) * 10.0 + uTime) * cos((uv.y + w2 * 0.08) * 10.0 - uTime * 0.8), 0.0, 1.0), 3.0);

        float3 abyssBlue = float3(0.04, 0.09, 0.18);
        float3 liquidCyan = float3(0.12, 0.38, 0.56);
        float3 glassViolet = float3(0.28, 0.22, 0.50);
        float3 specularHighlight = float3(0.55, 0.82, 0.96);

        float blendA = clamp(0.5 + 0.3 * w1 + 0.2 * w2, 0.0, 1.0);
        float3 baseGrad = mix(abyssBlue, liquidCyan, blendA);
        baseGrad = mix(baseGrad, glassViolet, clamp(uv.y * 0.55 + w2 * 0.2, 0.0, 1.0));
        float3 finalColor = baseGrad + specularHighlight * (caustic * 0.22);

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
    var isDrawerOpen by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val timeText = remember(currentTime) {
        SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(currentTime))
    }
    val dateText = remember(currentTime) {
        SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(currentTime))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_anim")
    val uTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
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

    val homeApps = remember(apps) { apps.take(8) }
    val dockApps = remember(apps) { apps.drop(8).take(4).ifEmpty { apps.take(4) } }

    val fallbackBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A172E),
                Color(0xFF164366),
                Color(0xFF2E2759)
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
            .pointerInput(isDrawerOpen) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -24f && !isDrawerOpen) {
                        isDrawerOpen = true
                    } else if (dragAmount > 28f && isDrawerOpen) {
                        isDrawerOpen = false
                    }
                }
            }
    ) {
        // 1. Home Screen Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Glass Clock & Weather Widget
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    fillAlpha = 0.14f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = timeText,
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = dateText,
                                color = Color.White.copy(alpha = 0.80f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        LiquidGlassSurface(
                            cornerRadius = 20.dp,
                            fillAlpha = 0.16f
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Liquid Glass",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${apps.size} Apps",
                                    color = Color(0xFFA8E6FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Home Screen Pinned Squircle Grid (4x2)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(
                        items = homeApps,
                        key = { "home_${it.packageName}" }
                    ) { app ->
                        SquircleGlassAppTile(
                            app = app,
                            showLabel = true,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
                    }
                }
            }

            // Bottom Search Pill + Floating Glass Dock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bottom Search Pill (tapping opens drawer & search)
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .clip(CircleShape)
                        .clickable { isDrawerOpen = true },
                    cornerRadius = 50.dp,
                    fillAlpha = 0.16f
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Search • Swipe up",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Floating Liquid Glass Dock
                LiquidGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 30.dp,
                    fillAlpha = 0.18f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dockApps.forEach { app ->
                            SquircleGlassAppTile(
                                app = app,
                                showLabel = false,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }

                        // App Library Button in Dock
                        SquircleGlassActionTile(
                            onClick = { isDrawerOpen = true }
                        )
                    }
                }
            }
        }

        // 2. Full Liquid Glass App Drawer Overlay
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF071120).copy(alpha = 0.72f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Library",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { isDrawerOpen = false }) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Close Drawer",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        fillAlpha = 0.15f
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = "Search ${apps.size} apps...",
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(
                            items = filteredApps,
                            key = { "drawer_${it.packageName}" }
                        ) { app ->
                            SquircleGlassAppTile(
                                app = app,
                                showLabel = true,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    fillAlpha: Float = 0.14f,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassFill = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = fillAlpha + 0.08f),
            Color.White.copy(alpha = fillAlpha * 0.55f)
        )
    )
    val specularRim = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.55f),
            Color(0xFF9EE8FF).copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.35f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassFill)
            .border(width = 1.dp, brush = specularRim, shape = shape)
    ) {
        content()
    }
}

@Composable
private fun SquircleGlassAppTile(
    app: LauncherAppitem,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val squircleShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LiquidGlassSurface(
            modifier = Modifier.size(60.dp),
            cornerRadius = 18.dp,
            fillAlpha = 0.18f
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(squircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = app.label,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLongClick)
            )
        }
    }
}

@Composable
private fun SquircleGlassActionTile(
    onClick: () -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        fillAlpha = 0.24f
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = "All Apps",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
