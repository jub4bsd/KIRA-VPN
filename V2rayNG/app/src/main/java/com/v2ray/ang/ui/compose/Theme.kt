package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFF6E22C3), // KIRA violet
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFEBDDFF), // Pale violet
    onPrimaryContainer = Color(0xFF260456), // Deep violet
    secondary = Color(0xFF9633C3), // Magenta violet
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFF5D7FF), // Pale magenta
    onSecondaryContainer = Color(0xFF330A4B), // Deep plum
    tertiary = Color(0xFF009966), // Green
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFA0F2D0), // Light Green
    onTertiaryContainer = Color(0xFF00201A), // Dark Teal
    error = Color(0xFFBA1A1A), // Red
    errorContainer = Color(0xFFFFDAD6), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFF8F3FF), // Pale violet
    onBackground = Color(0xFF1C1B1F), // Near Black
    surface = Color(0xFFFFFBFF), // Warm white
    onSurface = Color(0xFF1C1B1F), // Near Black
    surfaceVariant = Color(0xFFF0E5FA), // Light violet gray
    onSurfaceVariant = Color(0xFF4D4058), // Dark plum gray
    outline = Color(0xFF806C8C), // Muted violet
    outlineVariant = Color(0xFFD7C5E6), // Pale violet
    inverseSurface = Color(0xFF313033), // Dark Gray
    inverseOnSurface = Color(0xFFF4EFF4), // Very Light Gray
    inversePrimary = Color(0xFFCBA7FF), // Bright violet
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF6E22C3), // KIRA violet
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFFAF4FF), // Very light violet
    surfaceContainer = Color(0xFFF4EBFC), // Light violet
    surfaceContainerHigh = Color(0xFFEFE5F8), // Light violet
    surfaceContainerHighest = Color(0xFFE9DEF3), // Light violet
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFCBA7FF), // KIRA violet
    onPrimary = Color(0xFF200039), // Deep plum
    primaryContainer = Color(0xFF531E8F), // Purple
    onPrimaryContainer = Color(0xFFF4DFFF), // Pale violet
    secondary = Color(0xFFE574FF), // Magenta violet
    onSecondary = Color(0xFF380038), // Deep magenta
    secondaryContainer = Color(0xFF5A145D), // Plum
    onSecondaryContainer = Color(0xFFFFD6FE), // Pale magenta
    tertiary = Color(0xFF83D6B5), // Mint Green
    onTertiary = Color(0xFF00382E), // Dark Teal
    tertiaryContainer = Color(0xFF005143), // Teal
    onTertiaryContainer = Color(0xFFA0F2D0), // Light Green
    error = Color(0xFFFFB4AB), // Light Red
    errorContainer = Color(0xFF93000A), // Dark Red
    onError = Color(0xFF690005), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    background = Color(0xFF080512), // Near black with violet tint
    onBackground = Color(0xFFF4ECFF), // Pale violet
    surface = Color(0xFF0D0819), // Dark plum
    onSurface = Color(0xFFF4ECFF), // Pale violet
    surfaceVariant = Color(0xFF241733), // Dark violet
    onSurfaceVariant = Color(0xFFDBC7F4), // Muted pale violet
    outline = Color(0xFFA886C2), // Muted violet
    outlineVariant = Color(0xFF53396B), // Dark violet
    inverseSurface = Color(0xFFE6E1E5), // Light Gray
    inverseOnSurface = Color(0xFF1C1B1F), // Near Black
    inversePrimary = Color(0xFF6E22C3), // KIRA violet
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFFCBA7FF), // KIRA violet
    surfaceContainerLowest = Color(0xFF05030B), // Near black
    surfaceContainerLow = Color(0xFF100A1D), // Dark violet
    surfaceContainer = Color(0xFF160E27), // Dark violet
    surfaceContainerHigh = Color(0xFF201430), // Plum
    surfaceContainerHighest = Color(0xFF2B1B3D), // Light plum
)

// Semantic Colors
val colorPing = Color(0xFF009966) // Green
val colorPingRed = Color(0xFFFF0099) // Pink Red
val colorConfigType = Color(0xFFC57BFF) // KIRA violet
val colorFabActive = Color(0xFF8A2BE2) // Violet
val colorFabInactiveLight = Color(0xFFA76BDE) // Muted violet
val colorFabInactiveDark = Color(0xFF61427A) // Dark violet
val dividerColorLight = Color(0xFFE4D2FF) // Pale violet
val dividerColorDark = Color(0xFF39234D) // Dark violet

// Toast Colors 70%
val toastNormalBgLight = Color(0xB3353A3E) // Dark Gray
val toastNormalBgDark = Color(0xB34A4F54) // Darker Gray
val toastSuccessBg = Color(0xB3388E3C) // Green
val toastErrorBg = Color(0xB3D50000) // Red
val toastInfoBg = Color(0xB33F51B5) // Indigo Blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val systemColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val colorScheme = if (darkTheme) {
        systemColorScheme.copy(
            primary = Color(0xFFCBA7FF),
            onPrimary = Color(0xFF200039),
            primaryContainer = Color(0xFF531E8F),
            onPrimaryContainer = Color(0xFFF4DFFF),
            secondary = Color(0xFFE574FF),
            onSecondary = Color(0xFF380038),
            background = Color(0xFF080512),
            onBackground = Color(0xFFF4ECFF),
            surface = Color(0xFF0D0819),
            onSurface = Color(0xFFF4ECFF),
            surfaceVariant = Color(0xFF241733),
            onSurfaceVariant = Color(0xFFDBC7F4),
            outline = Color(0xFFA886C2),
            outlineVariant = Color(0xFF53396B),
            surfaceTint = Color(0xFFCBA7FF),
            surfaceContainerLowest = Color(0xFF05030B),
            surfaceContainerLow = Color(0xFF100A1D),
            surfaceContainer = Color(0xFF160E27),
            surfaceContainerHigh = Color(0xFF201430),
            surfaceContainerHighest = Color(0xFF2B1B3D),
        )
    } else {
        systemColorScheme.copy(
            primary = Color(0xFF6E22C3),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEBDDFF),
            onPrimaryContainer = Color(0xFF260456),
            secondary = Color(0xFF9633C3),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFFF8F3FF),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFF0E5FA),
            onSurfaceVariant = Color(0xFF4D4058),
            outline = Color(0xFF806C8C),
            outlineVariant = Color(0xFFD7C5E6),
            surfaceTint = Color(0xFF6E22C3),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFAF4FF),
            surfaceContainer = Color(0xFFF4EBFC),
            surfaceContainerHigh = Color(0xFFEFE5F8),
            surfaceContainerHighest = Color(0xFFE9DEF3),
        )
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
