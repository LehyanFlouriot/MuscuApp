package com.lehyanflouriot.muscuapp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Material 3 scheme generated from the brand orange #FF5722, with calm containers: the vivid orange
// is kept, as "highlight", for what celebrates (a streak, a session done) or runs (the rest).

internal val LightColors = lightColorScheme(
    primary = Color(0xFFB02F00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3B0900),
    inversePrimary = Color(0xFFFFB5A0),
    secondary = Color(0xFF9F4125),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFC8866),
    onSecondaryContainer = Color(0xFF722107),
    tertiary = Color(0xFF00628C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF007CAF),
    onTertiaryContainer = Color(0xFFFCFCFF),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF271813),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF271813),
    surfaceVariant = Color(0xFFFFDBD1),
    onSurfaceVariant = Color(0xFF5B4039),
    surfaceTint = Color(0xFFB02F00),
    inverseSurface = Color(0xFF3E2C27),
    inverseOnSurface = Color(0xFFFFEDE8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF907067),
    outlineVariant = Color(0xFFE4BEB4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F6),
    surfaceContainer = Color(0xFFFFE9E4),
    surfaceContainerHigh = Color(0xFFFFE2DA),
    surfaceContainerHighest = Color(0xFFFADCD4),
    surfaceContainerLow = Color(0xFFFFF1ED),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF1D4CC),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB5A0),
    onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFF862200),
    onPrimaryContainer = Color(0xFFFFDBD1),
    inversePrimary = Color(0xFFB02F00),
    secondary = Color(0xFFFFB5A0),
    onSecondary = Color(0xFF5F1500),
    secondaryContainer = Color(0xFF7F2A10),
    onSecondaryContainer = Color(0xFFFF9B7E),
    tertiary = Color(0xFF86CFFF),
    onTertiary = Color(0xFF00344C),
    tertiaryContainer = Color(0xFF019AD8),
    onTertiaryContainer = Color(0xFF002D42),
    background = Color(0xFF1E100C),
    onBackground = Color(0xFFFADCD4),
    surface = Color(0xFF1E100C),
    onSurface = Color(0xFFFADCD4),
    surfaceVariant = Color(0xFF5B4039),
    onSurfaceVariant = Color(0xFFE4BEB4),
    surfaceTint = Color(0xFFFFB5A0),
    inverseSurface = Color(0xFFFADCD4),
    inverseOnSurface = Color(0xFF3E2C27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFAB8980),
    outlineVariant = Color(0xFF5B4039),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF483530),
    surfaceContainer = Color(0xFF2C1C17),
    surfaceContainerHigh = Color(0xFF372621),
    surfaceContainerHighest = Color(0xFF43302B),
    surfaceContainerLow = Color(0xFF271813),
    surfaceContainerLowest = Color(0xFF180B07),
    surfaceDim = Color(0xFF1E100C),
)

/** Colors that Material 3 does not provide, used to show progress. */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    /** The brand orange, for what celebrates or runs. */
    val highlight: Color,
    val onHighlight: Color,
)

internal val LightExtendedColors = ExtendedColors(
    success = Color(0xFF396A00),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFB4F577),
    onSuccessContainer = Color(0xFF0D2000),
    highlight = Color(0xFFFF5722),
    onHighlight = Color(0xFF541200),
)

internal val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF99D85E),
    onSuccess = Color(0xFF1B3700),
    successContainer = Color(0xFF2A5000),
    onSuccessContainer = Color(0xFFB4F577),
    highlight = Color(0xFFFF5722),
    onHighlight = Color(0xFF541200),
)
