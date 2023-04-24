package ro.bankar.app.ui.theme

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color


private val lightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

private val darkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

class CustomColors(val accountDefault: Color, val negativeAmount: Color, val positiveAmount: Color)

private val lightCustomColors = CustomColors(accountDefault, light_negativeAmount, light_positiveAmount)
private val darkCustomColors = CustomColors(accountDefault, dark_negativeAmount, dark_positiveAmount)

val LocalCustomColors = compositionLocalOf { lightCustomColors }

@Suppress("UnusedReceiverParameter")
val MaterialTheme.customColors @Composable get() = LocalCustomColors.current

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        lightColors
    } else {
        darkColors
    }

    val transition = updateTransition(targetState = colors, "Theme Toggle")

    @Suppress("TransitionPropertiesLabel")
    val animatedColors = ColorScheme(
        transition.animateColor { it.primary }.value,
        transition.animateColor { it.onPrimary }.value,
        transition.animateColor { it.primaryContainer }.value,
        transition.animateColor { it.onPrimaryContainer }.value,
        transition.animateColor { it.inversePrimary }.value,
        transition.animateColor { it.secondary }.value,
        transition.animateColor { it.onSecondary }.value,
        transition.animateColor { it.secondaryContainer }.value,
        transition.animateColor { it.onSecondaryContainer }.value,
        transition.animateColor { it.tertiary }.value,
        transition.animateColor { it.onTertiary }.value,
        transition.animateColor { it.tertiaryContainer }.value,
        transition.animateColor { it.onTertiaryContainer }.value,
        transition.animateColor { it.background }.value,
        transition.animateColor { it.onBackground }.value,
        transition.animateColor { it.surface }.value,
        transition.animateColor { it.onSurface }.value,
        transition.animateColor { it.surfaceVariant }.value,
        transition.animateColor { it.onSurfaceVariant }.value,
        transition.animateColor { it.surfaceTint }.value,
        transition.animateColor { it.inverseSurface }.value,
        transition.animateColor { it.inverseOnSurface }.value,
        transition.animateColor { it.error }.value,
        transition.animateColor { it.onError }.value,
        transition.animateColor { it.errorContainer }.value,
        transition.animateColor { it.onErrorContainer }.value,
        transition.animateColor { it.outline }.value,
        transition.animateColor { it.outlineVariant }.value,
        transition.animateColor { it.scrim }.value,
    )

    CompositionLocalProvider(LocalCustomColors provides if (useDarkTheme) darkCustomColors else lightCustomColors) {
        MaterialTheme(
            colorScheme = animatedColors,
            content = content,
            typography = Typography
        )
    }
}