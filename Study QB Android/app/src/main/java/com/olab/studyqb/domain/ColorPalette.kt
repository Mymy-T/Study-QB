package com.olab.studyqb.domain

import androidx.compose.ui.graphics.Color

/**
 * The 10 fixed palette colors, one per [Category] (indexed by ordinal), approximating the
 * iOS app's [.red, .orange, .yellow, .green, .teal, .blue, .indigo, .purple, .pink, .brown]
 * system-color palette. Cards are colored with these at ~0.4 alpha, snapshotted into the
 * card's `rgba` field at creation/edit time rather than derived live from category.
 */
object ColorPalette {
    val colors: List<Color> = listOf(
        Color(0xFFFF3B30), // red
        Color(0xFFFF9500), // orange
        Color(0xFFFFCC00), // yellow
        Color(0xFF34C759), // green
        Color(0xFF30B0C7), // teal
        Color(0xFF007AFF), // blue
        Color(0xFF5856D6), // indigo
        Color(0xFFAF52DE), // purple
        Color(0xFFFF2D55), // pink
        Color(0xFFA2845E)  // brown
    )

    const val DEFAULT_ALPHA = 0.4f

    fun colorForCategory(category: Category, alpha: Float = DEFAULT_ALPHA): Color =
        colors[category.ordinal].copy(alpha = alpha)

    /** Matches iOS's `convertRGBAToColor([Double]) -> Color`. */
    fun rgbaToColor(rgba: List<Double>): Color {
        if (rgba.size < 4) return Color.White
        return Color(
            red = rgba[0].toFloat(),
            green = rgba[1].toFloat(),
            blue = rgba[2].toFloat(),
            alpha = rgba[3].toFloat()
        )
    }

    /** Matches iOS's `convertColorToRGBA(Color) -> [Double]`. */
    fun colorToRgba(color: Color): List<Double> =
        listOf(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble(), color.alpha.toDouble())

    fun rgbaToString(rgba: List<Double>): String = rgba.joinToString(",")

    fun stringToRgba(value: String): List<Double> =
        value.split(",").mapNotNull { it.trim().toDoubleOrNull() }.let {
            if (it.size == 4) it else listOf(1.0, 1.0, 1.0, 1.0)
        }

    /**
     * Matches iOS's `colorForNormalizedScore(Double) -> Color`: hue 0.33 (green) at 0 down to
     * 0.0 (red) at 1, passing through yellow/orange. [value] is expected in 0..1.
     */
    fun colorForNormalizedScore(value: Double): Color {
        val clamped = value.coerceIn(0.0, 1.0).toFloat()
        val hueDegrees = 0.33f * (1f - clamped) * 360f
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hueDegrees, 0.85f, 0.85f)))
    }
}
