package com.google.ar.core.examples.kotlin.helloar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.ar.core.examples.kotlin.helloar.R

val DisketFont = FontFamily(
    Font(R.font.disketregular, FontWeight.Normal),
    Font(R.font.disketbolt, FontWeight.Bold)
)

val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = DisketFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DisketFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    )
)
