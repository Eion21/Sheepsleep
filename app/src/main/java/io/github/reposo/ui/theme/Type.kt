package io.github.reposo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.reposo.R

private val Varela = FontFamily(Font(R.font.varela_round))

private val Nunito = FontFamily(
    Font(R.font.nunito_sans_400, FontWeight.Normal),
    Font(R.font.nunito_sans_600, FontWeight.SemiBold),
    Font(R.font.nunito_sans_700, FontWeight.Bold),
    Font(R.font.nunito_sans_800, FontWeight.ExtraBold),
)

private val base = Typography()

val ReposoTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Varela),
    displayMedium = base.displayMedium.copy(fontFamily = Varela),
    displaySmall = base.displaySmall.copy(fontFamily = Varela),
    headlineLarge = base.headlineLarge.copy(fontFamily = Varela),
    headlineMedium = base.headlineMedium.copy(fontFamily = Varela),
    headlineSmall = base.headlineSmall.copy(fontFamily = Varela),
    titleLarge = base.titleLarge.copy(fontFamily = Varela),
    titleMedium = base.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Nunito),
    bodyMedium = base.bodyMedium.copy(fontFamily = Nunito),
    bodySmall = base.bodySmall.copy(fontFamily = Nunito),
    labelLarge = base.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
)
