package com.lillesand.badetemp.ui.theme

import androidx.compose.ui.graphics.Color

// Lillesand coastal blue palette
val LillesandBlue = Color(0xFF1B5E99)
val LillesandBlueDark = Color(0xFF0D3F6E)
val LillesandBlueLight = Color(0xFF4A8DC4)
val LillesandOcean = Color(0xFF2E86AB)
val LillesandSky = Color(0xFFD1E9FF)
val LillesandSkyLight = Color(0xFFF0F6FB)
val LillesandWhite = Color(0xFFFFFFFF)
val LillesandText = Color(0xFF1A1A2E)
val LillesandTextSecondary = Color(0xFF4A5568)
val LillesandDivider = Color(0xFFCDE0F0)

// Dark mode palette
val DarkBackground = Color(0xFF0A1929)
val DarkSurface = Color(0xFF0F2438)
val DarkSurfaceVariant = Color(0xFF152D45)
val DarkText = Color(0xFFE0EAF4)
val DarkTextSecondary = Color(0xFF8BA3BC)
val DarkDivider = Color(0xFF1E3A55)

// Temperature indicator colors
val TempVeryCold = Color(0xFF1565C0)   // < 8°C  — deep blue
val TempCold = Color(0xFF0097A7)       // 8–14°C — teal
val TempCool = Color(0xFF2E7D32)       // 14–18°C — green
val TempWarm = Color(0xFFF57F17)       // 18–22°C — amber
val TempHot = Color(0xFFB71C1C)        // > 22°C  — red

fun temperatureColor(temp: Double): Color = when {
    temp < 8.0  -> TempVeryCold
    temp < 14.0 -> TempCold
    temp < 18.0 -> TempCool
    temp < 22.0 -> TempWarm
    else        -> TempHot
}
