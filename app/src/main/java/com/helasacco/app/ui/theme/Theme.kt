package com.helasacco.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand colors (mirrors Python constants.py palette) ───────────────────────

object HelaColors {
    // Primary - SACCO Green
    val Primary = Color(0xFF006C4C)
    val PrimaryLight = Color(0xFF4C9979)
    val PrimaryDark = Color(0xFF004D36)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF89F8C8)
    val OnPrimaryContainer = Color(0xFF002114)

    // Secondary - Purple
    val Secondary = Color(0xFF6750A4)
    val SecondaryLight = Color(0xFF9A82DB)
    val SecondaryDark = Color(0xFF4F378B)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE9DDFF)
    val OnSecondaryContainer = Color(0xFF21005D)

    // Tertiary - Coral
    val Tertiary = Color(0xFF984061)
    val TertiaryContainer = Color(0xFFFFD9E2)
    val OnTertiary = Color(0xFFFFFFFF)
    val OnTertiaryContainer = Color(0xFF3E001D)

    // Status colors
    val Success = Color(0xFF2E7D32)
    val Warning = Color(0xFFE65100)
    val Error = Color(0xFFB00020)
    val Info = Color(0xFF006780)

    // Surface
    val Surface = Color(0xFFF8FBF7)
    val SurfaceDark = Color(0xFF0F1F19)
    val SurfaceVariant = Color(0xFFDCE5DC)
    val SurfaceVariantDark = Color(0xFF3F4F42)

    // Background
    val Background = Color(0xFFF6F9F4)
    val BackgroundDark = Color(0xFF0D1B15)

    // Neutral
    val Outline = Color(0xFF6F7F6E)
    val OutlineDark = Color(0xFF899688)
}

// ── Light color scheme ────────────────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary = HelaColors.Primary,
    onPrimary = HelaColors.OnPrimary,
    primaryContainer = HelaColors.PrimaryContainer,
    onPrimaryContainer = HelaColors.OnPrimaryContainer,
    secondary = HelaColors.Secondary,
    onSecondary = HelaColors.OnSecondary,
    secondaryContainer = HelaColors.SecondaryContainer,
    onSecondaryContainer = HelaColors.OnSecondaryContainer,
    tertiary = HelaColors.Tertiary,
    onTertiary = HelaColors.OnTertiary,
    tertiaryContainer = HelaColors.TertiaryContainer,
    onTertiaryContainer = HelaColors.OnTertiaryContainer,
    error = HelaColors.Error,
    background = HelaColors.Background,
    surface = HelaColors.Surface,
    surfaceVariant = HelaColors.SurfaceVariant,
    outline = HelaColors.Outline,
)

// ── Dark color scheme ─────────────────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary = HelaColors.PrimaryContainer,
    onPrimary = HelaColors.PrimaryDark,
    primaryContainer = HelaColors.PrimaryDark,
    onPrimaryContainer = HelaColors.PrimaryContainer,
    secondary = HelaColors.SecondaryLight,
    onSecondary = HelaColors.SecondaryDark,
    secondaryContainer = HelaColors.SecondaryDark,
    onSecondaryContainer = HelaColors.SecondaryContainer,
    tertiary = HelaColors.TertiaryContainer,
    onTertiary = HelaColors.Tertiary,
    background = HelaColors.BackgroundDark,
    surface = HelaColors.SurfaceDark,
    surfaceVariant = HelaColors.SurfaceVariantDark,
    outline = HelaColors.OutlineDark,
)

// ── Typography ────────────────────────────────────────────────────────────────

val HelaTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

// ── Extended colors (accessible via LocalHelaExtColors) ──────────────────────

data class HelaExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
)

val LocalHelaExtColors = staticCompositionLocalOf {
    HelaExtendedColors(
        success = HelaColors.Success,
        onSuccess = Color.White,
        successContainer = Color(0xFFC8E6C9),
        warning = HelaColors.Warning,
        onWarning = Color.White,
        warningContainer = Color(0xFFFFE0B2),
        info = HelaColors.Info,
        onInfo = Color.White,
        infoContainer = Color(0xFFB8EAFF),
    )
}

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun HelaSaccoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val extColors = if (darkTheme) HelaExtendedColors(
        success = Color(0xFF66BB6A),
        onSuccess = Color(0xFF003909),
        successContainer = Color(0xFF005312),
        warning = Color(0xFFFFB74D),
        onWarning = Color(0xFF4A2800),
        warningContainer = Color(0xFF6B3C00),
        info = Color(0xFF4DD0E1),
        onInfo = Color(0xFF003641),
        infoContainer = Color(0xFF004E5E),
    ) else HelaExtendedColors(
        success = HelaColors.Success,
        onSuccess = Color.White,
        successContainer = Color(0xFFC8E6C9),
        warning = HelaColors.Warning,
        onWarning = Color.White,
        warningContainer = Color(0xFFFFE0B2),
        info = HelaColors.Info,
        onInfo = Color.White,
        infoContainer = Color(0xFFB8EAFF),
    )

    CompositionLocalProvider(LocalHelaExtColors provides extColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HelaTypography,
            content = content,
        )
    }
}

// Convenience accessor
val MaterialTheme.extendedColors: HelaExtendedColors
    @Composable get() = LocalHelaExtColors.current
