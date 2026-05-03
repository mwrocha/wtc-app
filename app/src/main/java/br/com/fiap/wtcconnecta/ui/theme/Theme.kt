package br.com.fiap.wtcconnecta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    // Principais
    primary = PrimaryColor,       // #0B537B — barra, FAB, botões
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,   // #EEF6FB — fundo azul pálido
    onPrimaryContainer = TextPrimary,

    secondary = SecondaryColor,       // #1A6E9A
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,   // #D0E8F2
    onSecondaryContainer = TextPrimary,

    tertiary = AccentColor,          // #3FA7E3
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,    // #F0F8FC
    onTertiaryContainer = TextPrimary,

    // Fundo e superfícies
    background = BackgroundColor,  // #F5FAFD
    onBackground = TextPrimary,

    surface = SurfaceColor,   // #FFFFFF
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant, // #EEF6FB
    onSurfaceVariant = TextMuted,

    // Outline
    outline = SecondaryContainer,  // #D0E8F2
    outlineVariant = PrimaryContainer,    // #EEF6FB

    // Status
    error = ErrorColor,
    onError = Color.White,
    errorContainer = Color(0xFFFFEDED),
    onErrorContainer = ErrorColor,

    // Inverse (notificações do sistema)
    inverseSurface = PrimaryColor,
    inverseOnSurface = Color.White,
    inversePrimary = PrimaryContainer,

    // Scrim (overlay dos modais)
    scrim = Color(0xFF0D2B3E),

    // Surface containers — usados internamente pelo AlertDialog, BottomSheet, etc.
    // Sem esses, o Material3 usa o padrão roxo/rosa do sistema
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5FAFD),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEEF6FB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD0E8F2),
    surfaceBright = Color(0xFFFFFFFF),
)

@Composable
fun WTCConnectaTheme(
    content: @Composable () -> Unit
) {
    // Dynamic Color DESABILITADO — garante que o azul #0B537B
    // seja aplicado em todos os componentes independente do wallpaper
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}