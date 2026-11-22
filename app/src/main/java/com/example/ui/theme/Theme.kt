package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.OwlAccentTheme

private val OwlShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun OwlLauncherTheme(
    accentTheme: OwlAccentTheme = OwlAccentTheme.AMBER,
    nightOwlMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        accentTheme == OwlAccentTheme.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        accentTheme == OwlAccentTheme.INDIGO -> darkColorScheme(
            primary = OwlIndigo,
            onPrimary = MidnightDeep,
            primaryContainer = OwlIndigoDark,
            onPrimaryContainer = OwlIndigoSoft,
            secondary = OwlAmberSoft,
            onSecondary = MidnightDeep,
            tertiary = OwlSage,
            background = MidnightDeep,
            onBackground = IvoryText,
            surface = MidnightSurface,
            onSurface = IvoryText,
            surfaceVariant = MidnightElevated,
            onSurfaceVariant = MutedMoonText,
            outline = SlateBorder,
            error = OwlCoralError
        )
        accentTheme == OwlAccentTheme.SAGE -> darkColorScheme(
            primary = OwlSage,
            onPrimary = MidnightDeep,
            primaryContainer = OwlSageDark,
            onPrimaryContainer = OwlSageSoft,
            secondary = OwlAmber,
            onSecondary = MidnightDeep,
            tertiary = OwlIndigo,
            background = MidnightDeep,
            onBackground = IvoryText,
            surface = MidnightSurface,
            onSurface = IvoryText,
            surfaceVariant = MidnightElevated,
            onSurfaceVariant = MutedMoonText,
            outline = SlateBorder,
            error = OwlCoralError
        )
        else -> darkColorScheme(
            primary = OwlAmber,
            onPrimary = MidnightDeep,
            primaryContainer = OwlAmberDark,
            onPrimaryContainer = OwlAmberSoft,
            secondary = OwlIndigo,
            onSecondary = MidnightDeep,
            tertiary = OwlSage,
            background = MidnightDeep,
            onBackground = IvoryText,
            surface = MidnightSurface,
            onSurface = IvoryText,
            surfaceVariant = MidnightElevated,
            onSurfaceVariant = MutedMoonText,
            outline = SlateBorder,
            error = OwlCoralError
        )
    }

    val finalScheme = if (nightOwlMode) {
        baseScheme.copy(
            primary = OwlAmberSoft,
            surface = MidnightDeep,
            surfaceVariant = NightCareTint
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = OwlTypography,
        shapes = OwlShapes,
        content = content
    )
}
