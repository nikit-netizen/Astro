package com.astro.storm.ui.chart

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.PlanetPosition
import com.astro.storm.data.model.Quality
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.model.ZodiacSign
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

class ChartRenderer {

    private val textPaint = android.graphics.Paint().apply {
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isSubpixelText = true
    }

    private val borderStroke = Stroke(width = 3f)
    private val lineStroke = Stroke(width = 2.5f)
    private val frameLinesPath = Path()

    companion object {
        private val TYPEFACE_NORMAL = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        private val TYPEFACE_BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        private val BACKGROUND_COLOR = Color(0xFFD4C4A8)
        private val BORDER_COLOR = Color(0xFFB8860B)
        private val HOUSE_NUMBER_COLOR = Color(0xFF5D4E37)

        private val SUN_COLOR = Color(0xFFD2691E)
        private val MOON_COLOR = Color(0xFFDC143C)
        private val MARS_COLOR = Color(0xFFB22222)
        private val MERCURY_COLOR = Color(0xFF228B22)
        private val JUPITER_COLOR = Color(0xFFDAA520)
        private val VENUS_COLOR = Color(0xFF9370DB)
        private val SATURN_COLOR = Color(0xFF4169E1)
        private val RAHU_COLOR = Color(0xFF8B0000)
        private val KETU_COLOR = Color(0xFF8B0000)
        private val URANUS_COLOR = Color(0xFF20B2AA)
        private val NEPTUNE_COLOR = Color(0xFF4682B4)
        private val PLUTO_COLOR = Color(0xFF800080)
        private val LAGNA_COLOR = Color(0xFF8B4513)

        private const val NAVAMSA_PART_DEGREES = 10.0 / 3.0

        private val SUPERSCRIPT_DIGITS = charArrayOf('⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹')
    }

    private data class ChartFrame(
        val left: Float,
        val top: Float,
        val size: Float,
        val centerX: Float,
        val centerY: Float
    )

    private enum class HouseType { DIAMOND, SIDE, CORNER }

    private data class HouseItem(val text: String, val color: Color)

    private fun getPlanetColor(planet: Planet): Color = when (planet) {
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

    private fun toSuperscript(degree: Int): String =
        degree.toString().map { SUPERSCRIPT_DIGITS[it - '0'] }.joinToString("")

    private fun isExalted(planet: Planet, sign: ZodiacSign): Boolean = when (planet) {
        Planet.SUN -> sign == ZodiacSign.ARIES
        Planet.MOON -> sign == ZodiacSign.TAURUS
        Planet.MARS -> sign == ZodiacSign.CAPRICORN
        Planet.MERCURY -> sign == ZodiacSign.VIRGO
        Planet.JUPITER -> sign == ZodiacSign.CANCER
        Planet.VENUS -> sign == ZodiacSign.PISCES
        Planet.SATURN -> sign == ZodiacSign.LIBRA
        Planet.RAHU -> sign == ZodiacSign.TAURUS || sign == ZodiacSign.GEMINI
        Planet.KETU -> sign == ZodiacSign.SCORPIO || sign == ZodiacSign.SAGITTARIUS
        else -> false
    }

    private fun isDebilitated(planet: Planet, sign: ZodiacSign): Boolean = when (planet) {
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

    private fun isVargottama(planet: PlanetPosition, chart: VedicChart): Boolean {
        val navamsaSign = ZodiacSign.fromLongitude(calculateNavamsaLongitude(planet.longitude))
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
        return (navamsaSignIndex * 30.0) + (positionInNavamsa / NAVAMSA_PART_DEGREES) * 30.0
    }

    private fun isCombust(planet: PlanetPosition, sunPosition: PlanetPosition?): Boolean {
        if (planet.planet == Planet.SUN || sunPosition == null) return false
        if (planet.planet in listOf(Planet.RAHU, Planet.KETU, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO)) return false

        val diff = abs(planet.longitude - sunPosition.longitude)
        val angularDistance = if (diff > 180.0) 360.0 - diff else diff

        val combustionOrb = when (planet.planet) {
            Planet.MOON -> 12.0
            Planet.MARS -> 17.0
            Planet.MERCURY -> if (planet.isRetrograde) 12.0 else 14.0
            Planet.JUPITER -> 11.0
            Planet.VENUS -> if (planet.isRetrograde) 8.0 else 10.0
            Planet.SATURN -> 15.0
            else -> 0.0
        }

        return angularDistance <= combustionOrb
    }

    private fun buildStatusIndicators(planet: PlanetPosition, chart: VedicChart?, sunPosition: PlanetPosition?): String {
        val indicators = StringBuilder()
        if (planet.isRetrograde) indicators.append("ᴿ")
        if (isExalted(planet.planet, planet.sign)) indicators.append("⁺")
        else if (isDebilitated(planet.planet, planet.sign)) indicators.append("⁻")
        if (chart != null && isCombust(planet, sunPosition)) indicators.append("ᶜ")
        if (chart != null && isVargottama(planet, chart)) indicators.append("ᵛ")
        return indicators.toString()
    }

    private fun DrawScope.drawNorthIndianFrame(size: Float): ChartFrame {
        val padding = size * 0.02f
        val chartSize = size - (padding * 2)
        val left = padding
        val top = padding
        val right = left + chartSize
        val bottom = top + chartSize
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        drawRect(color = BACKGROUND_COLOR, size = Size(size, size))
        drawRect(color = BORDER_COLOR, topLeft = Offset(left, top), size = Size(chartSize, chartSize), style = borderStroke)

        frameLinesPath.reset()
        frameLinesPath.moveTo(centerX, top)
        frameLinesPath.lineTo(right, centerY)
        frameLinesPath.lineTo(centerX, bottom)
        frameLinesPath.lineTo(left, centerY)
        frameLinesPath.close()
        frameLinesPath.moveTo(left, top)
        frameLinesPath.lineTo(right, bottom)
        frameLinesPath.moveTo(right, top)
        frameLinesPath.lineTo(left, bottom)
        drawPath(frameLinesPath, BORDER_COLOR, style = lineStroke)

        return ChartFrame(left, top, chartSize, centerX, centerY)
    }

    fun drawNorthIndianChart(drawScope: DrawScope, chart: VedicChart, size: Float, chartTitle: String = "Lagna") {
        with(drawScope) {
            val frame = drawNorthIndianFrame(size)
            drawAllHouseContents(frame, ZodiacSign.fromLongitude(chart.ascendant), chart.planetPositions, size, chart)
        }
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
            drawAllHouseContents(frame, ZodiacSign.fromLongitude(ascendantLongitude), planetPositions, size, originalChart)
        }
    }

    private fun getHouseType(houseNum: Int): HouseType = when (houseNum) {
        1, 4, 7, 10 -> HouseType.DIAMOND
        3, 5, 9, 11 -> HouseType.SIDE
        else -> HouseType.CORNER
    }

    private fun signNumberForHouse(houseNum: Int, ascendantSign: ZodiacSign): Int =
        ((ascendantSign.ordinal + houseNum - 1) % 12) + 1

    private fun getHousePolygon(houseNum: Int, frame: ChartFrame): List<Offset> {
        val left = frame.left
        val top = frame.top
        val chartSize = frame.size
        val centerX = frame.centerX
        val centerY = frame.centerY
        val right = left + chartSize
        val bottom = top + chartSize

        val a = Offset(left, top)
        val b = Offset(right, top)
        val c = Offset(right, bottom)
        val d = Offset(left, bottom)
        val e = Offset(centerX, top)
        val f = Offset(right, centerY)
        val g = Offset(centerX, bottom)
        val h = Offset(left, centerY)
        val o = Offset(centerX, centerY)

        val quarter = chartSize * 0.25f
        val threeQuarter = chartSize * 0.75f
        val p = Offset(left + quarter, top + quarter)
        val q = Offset(left + threeQuarter, top + threeQuarter)
        val r = Offset(left + threeQuarter, top + quarter)
        val s = Offset(left + quarter, top + threeQuarter)

        return when (houseNum) {
            1 -> listOf(e, p, o, r)
            4 -> listOf(h, s, o, p)
            7 -> listOf(g, s, o, q)
            10 -> listOf(f, r, o, q)
            2 -> listOf(a, e, p)
            6 -> listOf(d, g, s)
            8 -> listOf(c, g, q)
            12 -> listOf(b, e, r)
            3 -> listOf(a, h, p)
            5 -> listOf(d, h, s)
            9 -> listOf(c, f, q)
            11 -> listOf(b, f, r)
            else -> emptyList()
        }
    }

    private fun polygonCentroid(points: List<Offset>): Offset {
        if (points.size < 3) {
            return if (points.isEmpty()) Offset.Zero
            else Offset(
                points.sumOf { it.x.toDouble() }.toFloat() / points.size,
                points.sumOf { it.y.toDouble() }.toFloat() / points.size
            )
        }

        var crossSum = 0.0
        var cx = 0.0
        var cy = 0.0

        for (i in points.indices) {
            val j = (i + 1) % points.size
            val cross = points[i].x.toDouble() * points[j].y - points[j].x.toDouble() * points[i].y
            crossSum += cross
            cx += (points[i].x + points[j].x) * cross
            cy += (points[i].y + points[j].y) * cross
        }

        if (abs(crossSum) < 1e-10) {
            return Offset(
                points.sumOf { it.x.toDouble() }.toFloat() / points.size,
                points.sumOf { it.y.toDouble() }.toFloat() / points.size
            )
        }

        val factor = 1.0 / (3.0 * crossSum)
        return Offset((cx * factor).toFloat(), (cy * factor).toFloat())
    }

    private fun getHouseNumberPosition(houseNum: Int, frame: ChartFrame): Offset {
        val s = frame.size
        val l = frame.left
        val t = frame.top
        val cx = frame.centerX
        val cy = frame.centerY
        val r = l + s
        val b = t + s

        return when (houseNum) {
            1 -> Offset(cx, t + s * 0.07f)
            2 -> Offset(l + s * 0.07f, t + s * 0.07f)
            3 -> Offset(l + s * 0.05f, cy - s * 0.18f)
            4 -> Offset(l + s * 0.07f, cy)
            5 -> Offset(l + s * 0.05f, cy + s * 0.18f)
            6 -> Offset(l + s * 0.07f, b - s * 0.07f)
            7 -> Offset(cx, b - s * 0.07f)
            8 -> Offset(r - s * 0.07f, b - s * 0.07f)
            9 -> Offset(r - s * 0.05f, cy + s * 0.18f)
            10 -> Offset(r - s * 0.07f, cy)
            11 -> Offset(r - s * 0.05f, cy - s * 0.18f)
            12 -> Offset(r - s * 0.07f, t + s * 0.07f)
            else -> Offset(cx, cy)
        }
    }

    private fun DrawScope.drawAllHouseContents(
        frame: ChartFrame,
        ascendantSign: ZodiacSign,
        planetPositions: List<PlanetPosition>,
        size: Float,
        chart: VedicChart?
    ) {
        val planetsByHouse = planetPositions.groupBy { it.house }
        val sunPosition = chart?.planetPositions?.find { it.planet == Planet.SUN }

        for (houseNum in 1..12) {
            val polygon = getHousePolygon(houseNum, frame)
            val centroid = polygonCentroid(polygon)
            val planets = planetsByHouse[houseNum] ?: emptyList()

            drawTextCentered(
                text = signNumberForHouse(houseNum, ascendantSign).toString(),
                position = getHouseNumberPosition(houseNum, frame),
                textSize = size * 0.03f,
                color = HOUSE_NUMBER_COLOR,
                isBold = false
            )

            val items = mutableListOf<HouseItem>()
            if (houseNum == 1) items.add(HouseItem("Lg", LAGNA_COLOR))

            planets.forEach { planet ->
                val degree = (planet.longitude % 30.0).toInt()
                val text = "${planet.planet.symbol}${toSuperscript(degree)}${buildStatusIndicators(planet, chart, sunPosition)}"
                items.add(HouseItem(text, getPlanetColor(planet.planet)))
            }

            if (items.isNotEmpty()) {
                drawItemsInHouse(items, centroid, polygon, size, getHouseType(houseNum))
            }
        }
    }

    private fun DrawScope.drawItemsInHouse(
        items: List<HouseItem>,
        centroid: Offset,
        polygon: List<Offset>,
        size: Float,
        houseType: HouseType
    ) {
        val houseHeight = polygon.maxOf { it.y } - polygon.minOf { it.y }

        val usableRatio = when (houseType) {
            HouseType.CORNER -> 0.50f
            HouseType.SIDE -> 0.55f
            HouseType.DIAMOND -> 0.65f
        }
        val usableHeight = houseHeight * usableRatio

        val columns = when {
            houseType == HouseType.CORNER && items.size >= 3 -> 2
            houseType == HouseType.SIDE && items.size >= 4 -> 2
            houseType == HouseType.DIAMOND && items.size >= 5 -> 2
            else -> 1
        }

        val rows = ceil(items.size.toFloat() / columns).toInt()
        val baseTextSize = size * 0.028f
        val baseLineHeight = size * 0.036f

        val requiredHeight = rows * baseLineHeight
        val scale = if (requiredHeight > usableHeight) (usableHeight / requiredHeight).coerceIn(0.7f, 1f) else 1f

        val textSize = baseTextSize * scale
        val lineHeight = baseLineHeight * scale
        val colSpacing = size * 0.045f * scale

        items.forEachIndexed { index, item ->
            val col = if (columns > 1) index % columns else 0
            val row = index / columns
            val xOffset = if (columns > 1) (col - 0.5f) * colSpacing else 0f
            val yOffset = (row - (rows - 1) / 2f) * lineHeight

            drawTextCentered(
                text = item.text,
                position = Offset(centroid.x + xOffset, centroid.y + yOffset),
                textSize = textSize,
                color = item.color,
                isBold = true
            )
        }
    }

    private fun DrawScope.drawTextCentered(text: String, position: Offset, textSize: Float, color: Color, isBold: Boolean) {
        textPaint.color = color.toArgb()
        textPaint.textSize = textSize
        textPaint.typeface = if (isBold) TYPEFACE_BOLD else TYPEFACE_NORMAL

        val metrics = textPaint.fontMetrics
        val yOffset = -(metrics.ascent + metrics.descent) / 2
        drawContext.canvas.nativeCanvas.drawText(text, position.x, position.y + yOffset, textPaint)
    }

    fun createChartBitmap(chart: VedicChart, width: Int, height: Int, density: Density): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(android.graphics.Canvas(bitmap)), Size(width.toFloat(), height.toFloat())) {
            drawNorthIndianChart(this, chart, min(width, height).toFloat())
        }
        return bitmap
    }

    fun createDivisionalChartBitmap(
        planetPositions: List<PlanetPosition>,
        ascendantLongitude: Double,
        chartTitle: String,
        width: Int,
        height: Int,
        density: Density
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(android.graphics.Canvas(bitmap)), Size(width.toFloat(), height.toFloat())) {
            drawDivisionalChart(this, planetPositions, ascendantLongitude, min(width, height).toFloat(), chartTitle)
        }
        return bitmap
    }

    fun drawSouthIndianChart(drawScope: DrawScope, chart: VedicChart, size: Float) {
        drawNorthIndianChart(drawScope, chart, size)
    }

    fun DrawScope.drawChartLegend(chartBottom: Float, chartLeft: Float, chartWidth: Float, textSize: Float) {
        val legendY = chartBottom + textSize * 1.5f
        val items = listOf("ᴿ Retro", "⁺ Exalt", "⁻ Debil", "ᶜ Comb", "ᵛ Vargo")
        val itemWidth = chartWidth / items.size
        items.forEachIndexed { i, text ->
            drawTextCentered(text, Offset(chartLeft + i * itemWidth + itemWidth / 2, legendY), textSize * 0.8f, HOUSE_NUMBER_COLOR, false)
        }
    }

    fun drawChartWithLegend(drawScope: DrawScope, chart: VedicChart, size: Float, chartTitle: String = "Lagna", showLegend: Boolean = true) {
        with(drawScope) {
            val legendHeight = if (showLegend) size * 0.08f else 0f
            val chartSize = size - legendHeight
            drawNorthIndianChart(this, chart, chartSize, chartTitle)
            if (showLegend) {
                val padding = chartSize * 0.02f
                drawRect(BACKGROUND_COLOR, Offset(0f, chartSize), Size(size, legendHeight))
                drawChartLegend(chartSize, padding, chartSize - padding * 2, chartSize * 0.028f)
            }
        }
    }
}