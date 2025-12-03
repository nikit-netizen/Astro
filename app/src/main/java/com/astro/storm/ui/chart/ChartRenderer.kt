package com.astro.storm.ui.chart

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.PlanetPosition
import com.astro.storm.data.model.Quality
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.model.ZodiacSign
import kotlin.math.abs
import kotlin.math.min

/**
 * Professional North Indian Style Vedic Chart Renderer
 *
 * Fixes:
 * - Vector path drawing syntax errors resolved.
 * - Null safety for Divisional Charts (removed invalid Constructor calls).
 * - Optimized text vs icon layout.
 */
class ChartRenderer {

    // --- Configuration Constants ---
    
    private val textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isSubpixelText = true
    }

    private val borderStroke = Stroke(width = 3f, cap = StrokeCap.Square)
    private val lineStroke = Stroke(width = 2.0f, cap = StrokeCap.Round)
    private val symbolPath = Path() // Reusable path for drawing icons

    companion object {
        private val TYPEFACE_NORMAL = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        private val TYPEFACE_BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        // Colors
        private val BACKGROUND_COLOR = Color(0xFFD4C4A8) 
        private val BORDER_COLOR = Color(0xFFB8860B)
        private val HOUSE_NUMBER_COLOR = Color(0x805D4037) // Semi-transparent brown
        private val LAGNA_COLOR = Color(0xFF8B4513)

        // Planet Colors
        private val SUN_COLOR = Color(0xFFD2691E)
        private val MOON_COLOR = Color(0xFFB22222)
        private val MARS_COLOR = Color(0xFFCC0000)
        private val MERCURY_COLOR = Color(0xFF006400)
        private val JUPITER_COLOR = Color(0xFFDAA520)
        private val VENUS_COLOR = Color(0xFF800080)
        private val SATURN_COLOR = Color(0xFF00008B)
        private val RAHU_COLOR = Color(0xFF4A4A4A)
        private val KETU_COLOR = Color(0xFF4A4A4A)
        private val URANUS_COLOR = Color(0xFF008080)
        private val NEPTUNE_COLOR = Color(0xFF4682B4)
        private val PLUTO_COLOR = Color(0xFF4B0082)

        private const val NAVAMSA_PART_DEGREES = 10.0 / 3.0
    }

    // --- Core Rendering Functions ---

    fun createChartBitmap(chart: VedicChart, width: Int, height: Int, density: Density): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density,
            LayoutDirection.Ltr,
            Canvas(canvas),
            Size(width.toFloat(), height.toFloat())
        ) {
            drawNorthIndianChart(this, chart, min(width, height).toFloat())
        }
        return bitmap
    }
    
    fun drawNorthIndianChart(
        drawScope: DrawScope,
        chart: VedicChart,
        size: Float,
        chartTitle: String = "Lagna"
    ) {
        with(drawScope) {
            // 1. Draw Frame & Background
            val frame = drawNorthIndianFrame(size)
            
            // 2. Prepare Data
            val ascendantSign = ZodiacSign.fromLongitude(chart.ascendant)
            val planetsByHouse = chart.planetPositions.groupBy { it.house }
            val sunPosition = chart.planetPositions.find { it.planet == Planet.SUN }

            // 3. Draw Content for each House
            for (houseNum in 1..12) {
                // Calculate separate zones for Number and Planets to avoid overlap
                val layout = getHouseLayout(houseNum, frame)
                
                // Draw House Number (Sign Number)
                val signNum = signNumberForHouse(houseNum, ascendantSign)
                drawHouseNumber(signNum.toString(), layout.numberAnchor, size)

                // Draw Lagna Marker (Only in House 1)
                if (houseNum == 1) {
                    drawLagnaHeader(layout.planetCenter, size)
                }

                // Draw Planets
                val planets = planetsByHouse[houseNum] ?: emptyList()
                if (planets.isNotEmpty()) {
                    drawPlanetsInHouse(
                        planets = planets,
                        center = layout.planetCenter,
                        size = size,
                        houseNum = houseNum,
                        chart = chart, // Pass full chart for Vargottama check
                        sunPosition = sunPosition,
                        hasLagnaHeader = (houseNum == 1)
                    )
                }
            }
        }
    }

    // --- Geometry & Layout Logic ---

    data class ChartFrame(
        val left: Float, val top: Float, 
        val right: Float, val bottom: Float,
        val size: Float,
        val centerX: Float, val centerY: Float
    )

    data class HouseLayout(
        val numberAnchor: Offset, // Position for the House Number
        val planetCenter: Offset  // Center point for the Planet Stack
    )

    private fun DrawScope.drawNorthIndianFrame(size: Float): ChartFrame {
        val padding = size * 0.02f
        val chartSize = size - (padding * 2)
        val left = padding
        val top = padding
        val right = left + chartSize
        val bottom = top + chartSize
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        // Background
        drawRect(color = BACKGROUND_COLOR, size = Size(size, size))

        // Outer Border
        drawRect(
            color = BORDER_COLOR,
            topLeft = Offset(left, top),
            size = Size(chartSize, chartSize),
            style = borderStroke
        )

        // Inner Lines (Diagonals and Diamond)
        val path = Path().apply {
            // Diamond
            moveTo(centerX, top); lineTo(right, centerY); lineTo(centerX, bottom); lineTo(left, centerY); close()
            // Diagonals
            moveTo(left, top); lineTo(right, bottom)
            moveTo(right, top); lineTo(left, bottom)
        }
        drawPath(path, BORDER_COLOR, style = lineStroke)

        return ChartFrame(left, top, right, bottom, chartSize, centerX, centerY)
    }

    private fun getHouseLayout(houseNum: Int, f: ChartFrame): HouseLayout {
        val s = f.size
        val cx = f.centerX
        val cy = f.centerY
        val cOffset = 0.10f // Corner offset
        
        return when (houseNum) {
            // DIAMONDS (1, 4, 7, 10)
            1 -> HouseLayout(Offset(cx, f.top + s * 0.12f), Offset(cx, f.top + s * 0.28f))
            4 -> HouseLayout(Offset(f.left + s * 0.12f, cy), Offset(f.left + s * 0.25f, cy))
            7 -> HouseLayout(Offset(cx, f.bottom - s * 0.12f), Offset(cx, f.bottom - s * 0.25f))
            10 -> HouseLayout(Offset(f.right - s * 0.12f, cy), Offset(f.right - s * 0.25f, cy))

            // CORNER TRIANGLES (2, 6, 8, 12)
            2 -> HouseLayout(Offset(f.left + s * cOffset, f.top + s * cOffset), Offset(f.left + s * 0.20f, f.top + s * 0.20f))
            6 -> HouseLayout(Offset(f.left + s * cOffset, f.bottom - s * cOffset), Offset(f.left + s * 0.20f, f.bottom - s * 0.20f))
            8 -> HouseLayout(Offset(f.right - s * cOffset, f.bottom - s * cOffset), Offset(f.right - s * 0.20f, f.bottom - s * 0.20f))
            12 -> HouseLayout(Offset(f.right - s * cOffset, f.top + s * cOffset), Offset(f.right - s * 0.20f, f.top + s * 0.20f))

            // SIDE TRIANGLES (3, 5, 9, 11)
            3 -> HouseLayout(Offset(f.left + s * 0.22f, f.top + s * 0.22f), Offset(f.left + s * 0.10f, cy - s * 0.12f))
            5 -> HouseLayout(Offset(f.left + s * 0.22f, f.bottom - s * 0.22f), Offset(f.left + s * 0.10f, cy + s * 0.12f))
            9 -> HouseLayout(Offset(f.right - s * 0.22f, f.bottom - s * 0.22f), Offset(f.right - s * 0.10f, cy + s * 0.12f))
            11 -> HouseLayout(Offset(f.right - s * 0.22f, f.top + s * 0.22f), Offset(f.right - s * 0.10f, cy - s * 0.12f))
            else -> HouseLayout(Offset(cx, cy), Offset(cx, cy))
        }
    }

    // --- Drawing Implementations ---

    private fun DrawScope.drawHouseNumber(text: String, pos: Offset, size: Float) {
        drawTextWithPaint(text, pos.x, pos.y, size * 0.035f, HOUSE_NUMBER_COLOR, TYPEFACE_NORMAL)
    }

    private fun DrawScope.drawLagnaHeader(center: Offset, size: Float) {
        drawTextWithPaint("La", center.x, center.y - (size * 0.065f), size * 0.038f, LAGNA_COLOR, TYPEFACE_BOLD)
    }

    private fun DrawScope.drawPlanetsInHouse(
        planets: List<PlanetPosition>,
        center: Offset,
        size: Float,
        houseNum: Int,
        chart: VedicChart?,
        sunPosition: PlanetPosition?,
        hasLagnaHeader: Boolean
    ) {
        val isCorner = houseNum in listOf(2, 3, 5, 6, 8, 9, 11, 12)
        val useDoubleColumn = (planets.size >= 4) || (isCorner && planets.size >= 3)
        
        val baseTextSize = size * 0.032f
        val textSize = if (planets.size > 4) baseTextSize * 0.85f else baseTextSize
        val lineHeight = textSize * 1.2f
        
        val rows = if (useDoubleColumn) (planets.size + 1) / 2 else planets.size
        val colWidth = size * 0.07f
        val totalHeight = rows * lineHeight
        
        val startY = if (hasLagnaHeader) center.y - (totalHeight / 2) + (size * 0.02f) 
                     else center.y - (totalHeight / 2) + (lineHeight / 2)

        planets.forEachIndexed { index, planet ->
            val colIndex = if (useDoubleColumn) index % 2 else 0
            val rowIndex = if (useDoubleColumn) index / 2 else index
            
            val xPos = if (useDoubleColumn) center.x + (if (colIndex == 0) -colWidth/2 else colWidth/2) else center.x
            val yPos = startY + (rowIndex * lineHeight)

            val isRetro = planet.isRetrograde
            val isCombust = isCombust(planet, sunPosition)
            val isExalted = isExalted(planet.planet, planet.sign)
            val isDebilitated = isDebilitated(planet.planet, planet.sign)
            // Safely check vargottama only if chart is present
            val isVargottama = chart != null && isVargottama(planet, chart)

            drawPlanetEntry(this, planet, xPos, yPos, textSize, isRetro, isCombust, isExalted, isDebilitated, isVargottama)
        }
    }

    private fun drawPlanetEntry(
        drawScope: DrawScope,
        planet: PlanetPosition,
        x: Float,
        y: Float,
        textSize: Float,
        isRetro: Boolean,
        isCombust: Boolean,
        isExalted: Boolean,
        isDebilitated: Boolean,
        isVargottama: Boolean
    ) {
        val color = getPlanetColor(planet.planet)
        val symbol = planet.planet.symbol
        val degree = (planet.longitude % 30.0).toInt()
        val degreeStr = toSuperscript(degree)
        val mainText = "$symbol$degreeStr"

        textPaint.textSize = textSize
        textPaint.typeface = TYPEFACE_BOLD
        val textWidth = textPaint.measureText(mainText)
        
        drawScope.drawTextWithPaint(mainText, x, y, textSize, color, TYPEFACE_BOLD)

        val iconStartX = x + (textWidth / 2) + (textSize * 0.1f)
        val iconSize = textSize * 0.6f
        
        with(drawScope) {
            var currentX = iconStartX
            
            if (isRetro) {
                drawStatusIcon(StatusIcon.RETRO, currentX, y, iconSize, color)
                currentX += iconSize
            }
            
            if (isExalted) {
                drawStatusIcon(StatusIcon.UP_ARROW, currentX, y, iconSize, Color(0xFF2E7D32))
                currentX += iconSize
            } else if (isDebilitated) {
                drawStatusIcon(StatusIcon.DOWN_ARROW, currentX, y, iconSize, Color(0xFFC62828))
                currentX += iconSize
            }

            if (isCombust) {
                drawStatusIcon(StatusIcon.COMBUST, currentX, y, iconSize, Color.DarkGray)
                currentX += iconSize
            }
            
            if (isVargottama) {
                drawStatusIcon(StatusIcon.VARGOTTAMA, currentX, y, iconSize, color)
            }
        }
    }
    
    private enum class StatusIcon { RETRO, UP_ARROW, DOWN_ARROW, COMBUST, VARGOTTAMA }

    private fun DrawScope.drawStatusIcon(icon: StatusIcon, x: Float, y: Float, size: Float, color: Color) {
        symbolPath.reset()
        val half = size / 2
        val cy = y - (size * 0.6f) 
        
        when (icon) {
            StatusIcon.UP_ARROW -> {
                symbolPath.moveTo(x, cy + half)
                symbolPath.lineTo(x + size, cy + half)
                symbolPath.lineTo(x + half, cy - half)
                symbolPath.close()
            }
            StatusIcon.DOWN_ARROW -> {
                symbolPath.moveTo(x, cy - half)
                symbolPath.lineTo(x + size, cy - half)
                symbolPath.lineTo(x + half, cy + half)
                symbolPath.close()
            }
            StatusIcon.RETRO -> {
                // Corrected lineTo calls
                symbolPath.moveTo(x + half, cy - half); symbolPath.lineTo(x + half, cy + half)
                symbolPath.moveTo(x, cy); symbolPath.lineTo(x + size, cy)
                symbolPath.moveTo(x, cy - half); symbolPath.lineTo(x + size, cy + half)
                symbolPath.moveTo(x + size, cy - half); symbolPath.lineTo(x, cy + half)
            }
            StatusIcon.COMBUST -> {
                symbolPath.moveTo(x, cy + half)
                symbolPath.lineTo(x + half, cy - half)
                symbolPath.lineTo(x + size, cy + half)
            }
            StatusIcon.VARGOTTAMA -> {
                symbolPath.moveTo(x + half, cy - half)
                symbolPath.lineTo(x + size, cy)
                symbolPath.lineTo(x + half, cy + half)
                symbolPath.lineTo(x, cy)
                symbolPath.close()
            }
        }
        
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = color.toArgb()
                style = if (icon == StatusIcon.RETRO || icon == StatusIcon.COMBUST) Paint.Style.STROKE else Paint.Style.FILL
                strokeWidth = if (icon == StatusIcon.RETRO) 2f else 3f
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawPath(symbolPath.asAndroidPath(), paint)
        }
    }

    private fun DrawScope.drawTextWithPaint(
        text: String, x: Float, y: Float, 
        textSize: Float, color: Color, typeface: Typeface
    ) {
        if (textPaint.color != color.toArgb()) textPaint.color = color.toArgb()
        if (textPaint.textSize != textSize) textPaint.textSize = textSize
        if (textPaint.typeface != typeface) textPaint.typeface = typeface

        drawContext.canvas.nativeCanvas.drawText(text, x, y, textPaint)
    }

    // --- Astrology Logic Utils ---

    private fun signNumberForHouse(houseNum: Int, ascendantSign: ZodiacSign): Int {
        return ((ascendantSign.ordinal + houseNum - 1) % 12) + 1
    }

    private fun toSuperscript(degree: Int): String {
        val supers = arrayOf("\u2070", "\u00B9", "\u00B2", "\u00B3", "\u2074", "\u2075", "\u2076", "\u2077", "\u2078", "\u2079")
        return degree.toString().map { supers.getOrElse(it.digitToInt()) { "" } }.joinToString("")
    }

    private fun getPlanetColor(planet: Planet): Color {
        return when (planet) {
            Planet.SUN -> SUN_COLOR
            Planet.MOON -> MOON_COLOR
            Planet.MARS -> MARS_COLOR
            Planet.MERCURY -> MERCURY_COLOR
            Planet.JUPITER -> JUPITER_COLOR
            Planet.VENUS -> VENUS_COLOR
            Planet.SATURN -> SATURN_COLOR
            Planet.RAHU -> RAHU_COLOR
            Planet.KETU -> KETU_COLOR
            Planet.URANUS -> URANUS_COLOR
            Planet.NEPTUNE -> NEPTUNE_COLOR
            Planet.PLUTO -> PLUTO_COLOR
        }
    }

    private fun isExalted(planet: Planet, sign: ZodiacSign): Boolean {
        return when (planet) {
            Planet.SUN -> sign == ZodiacSign.ARIES
            Planet.MOON -> sign == ZodiacSign.TAURUS
            Planet.MARS -> sign == ZodiacSign.CAPRICORN
            Planet.MERCURY -> sign == ZodiacSign.VIRGO
            Planet.JUPITER -> sign == ZodiacSign.CANCER
            Planet.VENUS -> sign == ZodiacSign.PISCES
            Planet.SATURN -> sign == ZodiacSign.LIBRA
            Planet.RAHU -> sign == ZodiacSign.TAURUS
            Planet.KETU -> sign == ZodiacSign.SCORPIO
            else -> false
        }
    }

    private fun isDebilitated(planet: Planet, sign: ZodiacSign): Boolean {
        return when (planet) {
            Planet.SUN -> sign == ZodiacSign.LIBRA
            Planet.MOON -> sign == ZodiacSign.SCORPIO
            Planet.MARS -> sign == ZodiacSign.CANCER
            Planet.MERCURY -> sign == ZodiacSign.PISCES
            Planet.JUPITER -> sign == ZodiacSign.CAPRICORN
            Planet.VENUS -> sign == ZodiacSign.VIRGO
            Planet.SATURN -> sign == ZodiacSign.ARIES
            Planet.RAHU -> sign == ZodiacSign.SCORPIO || sign == ZodiacSign.SAGITTARIUS
            Planet.KETU -> sign == ZodiacSign.TAURUS || sign == ZodiacSign.GEMINI
            else -> false
        }
    }

    private fun isVargottama(planet: PlanetPosition, chart: VedicChart): Boolean {
        val navamsaLongitude = calculateNavamsaLongitude(planet.longitude)
        val navamsaSign = ZodiacSign.fromLongitude(navamsaLongitude)
        return planet.sign == navamsaSign
    }

    private fun calculateNavamsaLongitude(longitude: Double): Double {
        val normalizedLong = ((longitude % 360.0) + 360.0) % 360.0
        val sign = ZodiacSign.fromLongitude(normalizedLong)
        val degreeInSign = normalizedLong % 30.0
        val navamsaPart = (degreeInSign / NAVAMSA_PART_DEGREES).toInt().coerceIn(0, 8)

        val startingSignIndex = when (sign.quality) {
            Quality.CARDINAL -> sign.ordinal
            Quality.FIXED -> (sign.ordinal + 8) % 12
            Quality.MUTABLE -> (sign.ordinal + 4) % 12
        }
        val navamsaSignIndex = (startingSignIndex + navamsaPart) % 12
        val positionInNavamsa = degreeInSign % NAVAMSA_PART_DEGREES
        val navamsaDegree = (positionInNavamsa / NAVAMSA_PART_DEGREES) * 30.0
        return (navamsaSignIndex * 30.0) + navamsaDegree
    }

    private fun isCombust(planet: PlanetPosition, sunPosition: PlanetPosition?): Boolean {
        if (planet.planet == Planet.SUN || sunPosition == null) return false
        if (planet.planet in listOf(Planet.RAHU, Planet.KETU, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO)) return false

        val dist = calculateAngularDistance(planet.longitude, sunPosition.longitude)
        val limit = when (planet.planet) {
            Planet.MOON -> 12.0
            Planet.MARS -> 17.0
            Planet.MERCURY -> if (planet.isRetrograde) 12.0 else 14.0
            Planet.JUPITER -> 11.0
            Planet.VENUS -> if (planet.isRetrograde) 8.0 else 10.0
            Planet.SATURN -> 15.0
            else -> 0.0
        }
        return dist <= limit
    }

    private fun calculateAngularDistance(long1: Double, long2: Double): Double {
        val diff = abs(long1 - long2)
        return if (diff > 180.0) 360.0 - diff else diff
    }

    // --- Divisional Chart Support ---

    fun createDivisionalChartBitmap(
        planetPositions: List<PlanetPosition>,
        ascendantLongitude: Double,
        chartTitle: String,
        width: Int,
        height: Int,
        density: Density
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density,
            LayoutDirection.Ltr,
            Canvas(canvas),
            Size(width.toFloat(), height.toFloat())
        ) {
            drawDivisionalChart(this, planetPositions, ascendantLongitude, min(width, height).toFloat(), chartTitle)
        }
        return bitmap
    }

    fun drawDivisionalChart(
        drawScope: DrawScope,
        planetPositions: List<PlanetPosition>,
        ascendantLongitude: Double,
        size: Float,
        chartTitle: String,
        originalChart: VedicChart? = null
    ) {
        with(drawScope) {
            val frame = drawNorthIndianFrame(size)
            val ascendantSign = ZodiacSign.fromLongitude(ascendantLongitude)
            val planetsByHouse = planetPositions.groupBy { it.house }

            for (houseNum in 1..12) {
                val layout = getHouseLayout(houseNum, frame)
                
                val signNum = signNumberForHouse(houseNum, ascendantSign)
                drawHouseNumber(signNum.toString(), layout.numberAnchor, size)

                if (houseNum == 1) drawLagnaHeader(layout.planetCenter, size)

                val planets = planetsByHouse[houseNum] ?: emptyList()
                if (planets.isNotEmpty()) {
                    drawPlanetsInHouse(
                        planets = planets, 
                        center = layout.planetCenter, 
                        size = size, 
                        houseNum = houseNum,
                        chart = originalChart, // Passed safely (nullable)
                        sunPosition = null, 
                        hasLagnaHeader = (houseNum == 1)
                    )
                }
            }
        }
    }
}