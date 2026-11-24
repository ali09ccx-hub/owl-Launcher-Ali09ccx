package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.StormMode
import com.example.model.WallpaperConfig
import com.example.model.WallpaperScaleMode
import com.example.ui.theme.MidnightDeep
import com.example.ui.theme.NightCareTint
import com.example.ui.theme.OwlAmberSoft
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class RainDropSeed(
    val xFraction: Float,
    val yOffset: Float,
    val speed: Float,
    val length: Float,
    val alpha: Float,
    val thickness: Float
)

@Composable
fun WallpaperCanvas(
    isLandscape: Boolean,
    portraitConfig: WallpaperConfig,
    landscapeConfig: WallpaperConfig,
    nightOwlMode: Boolean,
    stormMode: StormMode = StormMode.THUNDER_STORM,
    rainDensity: Float = 0.65f,
    lightningEnabled: Boolean = true,
    stormFogEnabled: Boolean = true,
    pageParallaxFraction: Float = 0f,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val stormActive = stormMode != StormMode.OFF && !isPaused && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    val activeConfig = if (isLandscape) landscapeConfig else portraitConfig
    val dimOverlayColor by animateColorAsState(
        targetValue = if (nightOwlMode) {
            NightCareTint.copy(alpha = (activeConfig.dimAlpha + 0.24f).coerceIn(0.20f, 0.85f))
        } else {
            MidnightDeep.copy(alpha = activeConfig.dimAlpha)
        },
        animationSpec = tween(250),
        label = "wallpaper_dim"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val cfg = if (isLandscape) landscapeConfig else portraitConfig
        val contentScale = when (cfg.scaleMode) {
            WallpaperScaleMode.CROP -> ContentScale.Crop
            WallpaperScaleMode.FIT -> ContentScale.Fit
            WallpaperScaleMode.STRETCH -> ContentScale.FillBounds
        }
        val parallaxShift = -pageParallaxFraction * 28f
        val transformModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = cfg.zoom * 1.03f
                scaleY = cfg.zoom * 1.03f
                translationX = cfg.offsetX * widthPx * 0.35f + parallaxShift
                translationY = cfg.offsetY * heightPx * 0.35f
            }

        val customFile = cfg.customFilePath?.let { File(it) }?.takeIf { it.exists() }
        if (customFile != null) {
            val targetW = widthPx.toInt().coerceIn(720, 1440)
            val targetH = heightPx.toInt().coerceIn(720, 2560)
            val cacheKey = "${customFile.absolutePath}_${cfg.version}"
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(customFile)
                    .size(targetW, targetH)
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = contentScale,
                alignment = Alignment.Center,
                modifier = transformModifier
            )
        } else {
            val resId = if (isLandscape) {
                R.drawable.img_wallpaper_landscape_owl
            } else {
                R.drawable.img_wallpaper_portrait_owl
            }
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = contentScale,
                alignment = Alignment.Center,
                modifier = transformModifier
            )
        }

        // Subtle atmospheric legibility gradient + user dim + Night Owl warmth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MidnightDeep.copy(alpha = 0.24f),
                            Color.Transparent,
                            MidnightDeep.copy(alpha = 0.40f)
                        )
                    )
                )
                .background(dimOverlayColor)
        )

        // Living Owl Storm Engine (Rain, Drifting Storm Mist, Thunder & Lightning)
        if (stormMode != StormMode.OFF) {
            LivingOwlStormLayer(
                stormMode = stormMode,
                rainDensity = rainDensity,
                lightningEnabled = lightningEnabled,
                stormFogEnabled = stormFogEnabled,
                pageParallaxFraction = pageParallaxFraction,
                animateFrames = stormActive
            )
        }
    }
}

@Composable
private fun LivingOwlStormLayer(
    stormMode: StormMode,
    rainDensity: Float,
    lightningEnabled: Boolean,
    stormFogEnabled: Boolean,
    pageParallaxFraction: Float,
    animateFrames: Boolean
) {
    var frameTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(stormMode, animateFrames) {
        if (!animateFrames) return@LaunchedEffect
        val baseOffsetMs = frameTimeMs
        val start = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                frameTimeMs = baseOffsetMs + (now - start) / 1_000_000L
            }
        }
    }

    val dropCount = remember(stormMode, rainDensity) {
        val base = if (stormMode == StormMode.THUNDER_STORM) 125 else 55
        (base * rainDensity.coerceIn(0.15f, 1.0f)).toInt().coerceAtLeast(16)
    }

    val rainSeeds = remember(dropCount) {
        val rng = Random(202607)
        List(dropCount) {
            RainDropSeed(
                xFraction = rng.nextFloat(),
                yOffset = rng.nextFloat(),
                speed = 0.55f + rng.nextFloat() * 0.75f,
                length = 18f + rng.nextFloat() * 34f,
                alpha = 0.14f + rng.nextFloat() * 0.28f,
                thickness = 1.1f + rng.nextFloat() * 1.4f
            )
        }
    }

    val boltSeeds = remember {
        listOf(0.18f, 0.72f, 0.38f, 0.84f, 0.52f)
    }
    val boltPath = remember { Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val tSec = frameTimeMs / 1000f

        // 1. Drifting storm mist / dark nocturnal clouds
        if (stormFogEnabled) {
            val driftA = sin(tSec * 0.35f) * w * 0.08f - pageParallaxFraction * 42f
            val driftB = cos(tSec * 0.25f) * w * 0.10f - pageParallaxFraction * 24f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1F2B4D).copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.28f + driftA, h * 0.16f),
                    radius = w * 0.58f
                ),
                center = Offset(w * 0.28f + driftA, h * 0.16f),
                radius = w * 0.58f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF141C34).copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.74f + driftB, h * 0.22f),
                    radius = w * 0.62f
                ),
                center = Offset(w * 0.74f + driftB, h * 0.22f),
                radius = w * 0.62f
            )
            // Ground mist near perched/patient owl horizon
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF18243E).copy(alpha = 0.22f + 0.06f * sin(tSec * 0.7f))
                    ),
                    startY = h * 0.68f,
                    endY = h
                )
            )
        }

        // 2. Thunder & Lightning Flashes (periodic dramatic sky flashes while owl stays calm)
        var flashIntensity = 0f
        if (lightningEnabled && stormMode == StormMode.THUNDER_STORM) {
            val cycle = tSec % 6.8f
            flashIntensity = when {
                cycle in 1.20f..1.32f -> 0.55f
                cycle in 1.38f..1.52f -> 0.85f
                cycle in 1.52f..1.85f -> (1.85f - cycle) / 0.33f * 0.25f
                else -> 0f
            }

            if (flashIntensity > 0.01f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD8E8FF).copy(alpha = flashIntensity * 0.34f),
                            Color(0xFF9AB8FF).copy(alpha = flashIntensity * 0.14f),
                            Color.Transparent
                        )
                    )
                )

                val strikeIndex = ((tSec / 6.8f).toInt()) % boltSeeds.size
                val startX = w * boltSeeds[strikeIndex]
                if (flashIntensity > 0.30f) {
                    boltPath.reset()
                    boltPath.moveTo(startX, 0f)
                    boltPath.lineTo(startX - w * 0.04f, h * 0.09f)
                    boltPath.lineTo(startX + w * 0.025f, h * 0.16f)
                    boltPath.lineTo(startX - w * 0.055f, h * 0.26f)
                    boltPath.lineTo(startX - w * 0.01f, h * 0.35f)
                    // Outer bolt glow
                    drawPath(
                        path = boltPath,
                        color = Color(0xFF9EC5FF).copy(alpha = flashIntensity * 0.45f),
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                    // Core crisp lightning bolt
                    drawPath(
                        path = boltPath,
                        color = Color(0xFFF2F7FF).copy(alpha = flashIntensity * 0.92f),
                        style = Stroke(width = 2.6f, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // 3. Multi-depth slanted storm rain streaks
        val windTiltX = if (stormMode == StormMode.THUNDER_STORM) -0.22f else -0.10f
        val baseRainColor = Color(0xFFD2E2FF)
        rainSeeds.forEachIndexed { idx, drop ->
            val progress = (drop.yOffset + tSec * drop.speed) % 1.0f
            val y = progress * (h + drop.length) - drop.length
            val xDrift = progress * h * windTiltX
            val x = ((drop.xFraction * w + xDrift) % w + w) % w
            val endX = x + drop.length * windTiltX
            val endY = y + drop.length

            val boostAlpha = (drop.alpha + flashIntensity * 0.18f).coerceAtMost(0.65f)
            drawLine(
                color = baseRainColor.copy(alpha = boostAlpha),
                start = Offset(x, y),
                end = Offset(endX, endY),
                strokeWidth = drop.thickness,
                cap = StrokeCap.Round
            )

            // Subtle raindrop splash ripple in the lower mist
            if (idx % 7 == 0 && progress > 0.88f) {
                val rippleProgress = (progress - 0.88f) / 0.12f
                drawCircle(
                    color = baseRainColor.copy(alpha = (1f - rippleProgress) * 0.22f),
                    radius = 4f + rippleProgress * 12f,
                    center = Offset(endX, h * 0.92f - (idx % 5) * 14f),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // 4. Subtle patient golden firefly / ember glints in the dark canopy
        if (stormMode == StormMode.DRIZZLE || flashIntensity < 0.1f) {
            for (i in 0..5) {
                val angle = tSec * 0.5f + i * (PI.toFloat() / 3f)
                val fx = w * (0.15f + i * 0.14f) + cos(angle) * 18f
                val fy = h * (0.62f + (i % 3) * 0.09f) + sin(angle * 1.3f) * 12f
                val glowAlpha = 0.18f + 0.18f * sin(tSec * 2.1f + i).coerceAtLeast(0f)
                drawCircle(
                    color = OwlAmberSoft.copy(alpha = glowAlpha),
                    radius = 2.6f,
                    center = Offset(fx, fy)
                )
            }
        }
    }
}
