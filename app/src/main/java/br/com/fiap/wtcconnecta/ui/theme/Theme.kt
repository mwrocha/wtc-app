package br.com.fiap.wtcconnecta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightColorScheme = lightColorScheme(
    // Principais
    primary = PrimaryColor,            // Azul-petróleo (barra superior, FAB)
    onPrimary = Color.White,           // Ícones e texto em botões principais

    secondary = SecondaryColor,        // Azul médio (destaques, links)
    onSecondary = Color.White,

    tertiary = AccentColor,            // Azul claro (botões secundários, ícones)
    onTertiary = TextPrimary,

    // Fundo e superfícies
    background = BackgroundColor,      // Branco principal
    onBackground = TextPrimary,

    surface = CardBackground,          // Cinza claro dos cards
    onSurface = TextSecondary,

    surfaceVariant = SurfaceVariant,   // Tons neutros (borda, sombra suave)
    onSurfaceVariant = TextMuted,

    // Containers — agora correspondem ao Color.kt
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = TextPrimary,

    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = TextPrimary,

    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TextPrimary,

    // Status
    error = ErrorColor,
    onError = Color.White,


)

@Composable
fun WTCConnectaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography, // mantém sua tipografia atual
        content = content
    )
}
