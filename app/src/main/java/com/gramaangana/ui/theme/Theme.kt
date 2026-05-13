package com.gramaangana.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
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
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
)

// Semantic colors for use in Composables
data class VillageCustomColors(
    val free: Color,
    val partial: Color,
    val full: Color,
    val progressPledged: Color,
    val progressActual: Color
)

val LocalVillageColors = staticCompositionLocalOf {
    VillageCustomColors(
        free = Color.Unspecified,
        partial = Color.Unspecified,
        full = Color.Unspecified,
        progressPledged = Color.Unspecified,
        progressActual = Color.Unspecified
    )
}

@Composable
fun GramaAnganaTheme(
    content: @Composable () -> Unit
) {
    val customColors = VillageCustomColors(
        free = VillageFree,
        partial = VillagePartial,
        full = VillageFull,
        progressPledged = ProgressPledged,
        progressActual = ProgressActual
    )

    CompositionLocalProvider(LocalVillageColors provides customColors) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            content = content
        )
    }
}
