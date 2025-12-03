package com.astro.storm.ui.chart

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import kotlin.math.sqrt

/**
 * Professional North Indian Style Vedic Chart Renderer (Enhanced)
 *
 * Authentic North Indian chart format matching traditional Vedic astrology software:
 * - SQUARE outer boundary
 * - Central diamond created by connecting midpoints of sides
 * - Full corner-to-corner diagonals creating proper 12-house divisions
 * - 12 houses in traditional layout
 * - Planet positions with degree superscripts
 * - Status indicators: (R) Retrograde, (C) Combust, (V) Vargottama, (Ex) Exalted, (Db) Debilitated
 *
 * Standard North Indian Chart Layout (Houses numbered 1-12):
 * Ascendant is ALWAYS in the top center diamond (House 1)
 * Houses flow COUNTER-CLOCKWISE from House 1
 *
 * Visual Reference:
 *
 *       ┌────────────────────────────────────────┐
 *       │\              2               /        │
 *       │  \                          /    1    │
 *       │ 3  \                      /           │
 *       │      \                  /         12  │
 *       │        \              /               │
 *       │─────────\────────────/────────────────│
 *       │           \        /                  │
 *       │    4       \      /       11          │
 *       │             \    /                    │
 *       │──────────────\  /─────────────────────│
 *       │               \/                      │
 *       │               /\                      │
 *       │    5        /    \        10          │
 *       │           /        \                  │
 *       │─────────/────────────\────────────────│
 *       │       /       7        \       9      │
 *       │  6  /                    \            │
 *       │   /                        \     8    │
 *       │ /                            \        │
 *       └────────────────────────────────────────┘
 */
class ChartRenderer {

    // ============================================================================
    // GEOMETRY CONFIGURATION
    // ============================================================================

    // Geometry ratios for house NUMBER placement
    private val DIAMOND_NUMBER_VERTICAL_FRACTION = 0.38f
    private val CORNER_NUMBER_OFFSET_FRACTION = 0.12f
    private val SIDE_NUMBER_HORIZONTAL_OFFSET_FRACTION = 0.08f
    private val SIDE_NUMBER_VERTICAL_OFFSET_FRACTION = 0.28f
    private val DIAMOND_NUMBER_HORIZONTAL_OFFSET_FRACTION = 0.12f
    private val DIAMOND_NUMBER_VERTICAL_OFFSET_FRACTION = 0.08f
    private val CORNER_NUMBER_HORIZONTAL_OFFSET_FRACTION = 0.18f

    // Offset multipliers for shifting house numbers when planets are present
    private val HOUSE_NUMBER_SHIFT_FRACTION = 0.06f

    // Minimum text size ratio (relative to base) to prevent text from becoming too small
    private val MIN_TEXT_SIZE_RATIO = 0.6f

    // ============================================================================
    // PAINT OBJECTS (Cached for performance)
    // ============================================================================

    private val textPaint = android.graphics.Paint().apply {
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isSubpixelText = true
        isLinearText = true
    }

    private val backgroundPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }

    private val glowPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
    }

    private val borderStroke = Stroke(width = 3f)
    private val lineStroke = Stroke(width = 2.5f)
    private val frameLinesPath = Path()

    // ============================================================================
    // COMPANION OBJECT - CONSTANTS
    // ============================================================================

    companion object {
        // Typefaces for text rendering
        private val TYPEFACE_NORMAL = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        private val TYPEFACE_BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        // Color palette
        private val BACKGROUND_COLOR = Color(0xFFD4C4A8)      // Warm parchment
        private val BORDER_COLOR = Color(0xFFB8860B)          // Dark goldenrod
        private val HOUSE_NUMBER_COLOR = Color(0xFF4A4A4A)
        private val HOUSE_NUMBER_FADED_COLOR = Color(0x664A4A4A) // Faded for when planets present
        private val TEXT_BACKGROUND_COLOR = Color(0xCCD4C4A8)    // Semi-transparent parchment

        // Planet-specific colors
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

        // Status indicator symbols (ASCII-safe alternatives)
        // These are guaranteed to render on all Android devices
        const val SYMBOL_RETROGRADE = "R"     // Retrograde
        const val SYMBOL_COMBUST = "C"        // Combust
        const val SYMBOL_VARGOTTAMA = "V"     // Vargottama
        const val SYMBOL_EXALTED = "Ex"       // Exalted
        const val SYMBOL_DEBILITATED = "Db"   // Debilitated

        // Navamsa constants
        private const val NAVAMSA_PART_DEGREES = 10.0 / 3.0

        // Layout constants
        private const val MAX_PLANETS_SINGLE_COLUMN = 3
        private const val MAX_ITEMS_PER_ROW_CORNER = 2
        private const val MAX_ITEMS_PER_ROW_SIDE = 3
        private const val MAX_ITEMS_PER_ROW_DIAMOND = 3
    }

    // ============================================================================
    // DATA CLASSES
    // ============================================================================

    private data class ChartFrame(
        val left: Float,
        val top: Float,
        val size: Float,
        val centerX: Float,
        val centerY: Float
    )

    private enum class HouseType {
        DIAMOND,
        SIDE,
        CORNER
    }

    /**
     * Represents an item to be drawn in a house (planet or Lagna marker)
     */
    private data class HouseItem(
        val displayText: String,
        val color: Color,
        val isLagna: Boolean = false
    )

    /**
     * House bounds information for dynamic sizing
     */
    private data class HouseBounds(
        val polygon: List<Offset>,
        val centroid: Offset,
        val approximateWidth: Float,
        val approximateHeight: Float,
        val usableWidth: Float,
        val usableHeight: Float
    )

    // ============================================================================
    // PLANET COLOR MAPPING
    // ============================================================================

    /**
     * Get color for a specific planet
     */
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

    // ============================================================================
    // TEXT FORMATTING UTILITIES
    // ============================================================================

    /**
     * Convert degree to superscript string
     * Uses Unicode superscript characters for compact display
     */
    private fun toSuperscript(degree: Int): String {
        val superscripts = mapOf(
            '0' to '\u2070',
            '1' to '\u00B9',
            '2' to '\u00B2',
            '3' to '\u00B3',
            '4' to '\u2074',
            '5' to '\u2075',
            '6' to '\u2076',
            '7' to '\u2077',
            '8' to '\u2078',
            '9' to '\u2079'
        )
        return degree.toString().map { superscripts[it] ?: it }.joinToString("")
    }

    /**
     * Build status indicator string for a planet
     * Uses ASCII-safe abbreviations wrapped in parentheses for clarity
     */
    private fun buildStatusIndicators(
        planet: PlanetPosition,
        chart: VedicChart?,
        sunPosition: PlanetPosition?
    ): String {
        val indicators = mutableListOf<String>()

        if (planet.isRetrograde) {
            indicators.add(SYMBOL_RETROGRADE)
        }
        if (isExalted(planet.planet, planet.sign)) {
            indicators.add(SYMBOL_EXALTED)
        } else if (isDebilitated(planet.planet, planet.sign)) {
            indicators.add(SYMBOL_DEBILITATED)
        }
        if (chart != null && isCombust(planet, sunPosition)) {
            indicators.add(SYMBOL_COMBUST)
        }
        if (chart != null && isVargottama(planet, chart)) {
            indicators.add(SYMBOL_VARGOTTAMA)
        }

        return if (indicators.isNotEmpty()) {
            "(${indicators.joinToString("")})"
        } else {
            ""
        }
    }

    // ============================================================================
    // ASTROLOGICAL CALCULATION FUNCTIONS
    // ============================================================================

    /**
     * Check if planet is exalted in its current sign
     */
    private fun isExalted(planet: Planet, sign: ZodiacSign): Boolean {
        return when (planet) {
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
    }

    /**
     * Check if planet is debilitated in its current sign
     */
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

    /**
     * Check if a planet is Vargottama (same sign in D1 Rashi and D9 Navamsa charts)
     */
    private fun isVargottama(planet: PlanetPosition, chart: VedicChart): Boolean {
        val navamsaLongitude = calculateNavamsaLongitude(planet.longitude)
        val navamsaSign = ZodiacSign.fromLongitude(navamsaLongitude)
        return planet.sign == navamsaSign
    }

    /**
     * Calculate Navamsa longitude for a given longitude
     * Each sign is divided into 9 navamsas of 3°20' each
     */
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

    /**
     * Check if a planet is combust (too close to the Sun)
     * Uses traditional Vedic combustion orbs
     */
    private fun isCombust(planet: PlanetPosition, sunPosition: PlanetPosition?): Boolean {
        if (planet.planet == Planet.SUN) return false
        if (planet.planet in listOf(Planet.RAHU, Planet.KETU, Planet.URANUS, Planet.NEPTUNE, Planet.PLUTO)) {
            return false
        }
        if (sunPosition == null) return false

        val angularDistance = calculateAngularDistance(planet.longitude, sunPosition.longitude)

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

    /**
     * Calculate the angular distance between two longitudes
     * Handles the 360° wrap-around correctly
     */
    private fun calculateAngularDistance(long1: Double, long2: Double): Double {
        val diff = abs(long1 - long2)
        return if (diff > 180.0) 360.0 - diff else diff
    }

    // ============================================================================
    // CHART FRAME DRAWING
    // ============================================================================

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
        drawRect(
            color = BACKGROUND_COLOR,
            size = Size(size, size)
        )

        // Outer square border
        drawRect(
            color = BORDER_COLOR,
            topLeft = Offset(left, top),
            size = Size(chartSize, chartSize),
            style = borderStroke
        )

        // Internal frame lines (diamond + diagonals)
        frameLinesPath.reset()

        // Central diamond
        frameLinesPath.moveTo(centerX, top)
        frameLinesPath.lineTo(right, centerY)
        frameLinesPath.lineTo(centerX, bottom)
        frameLinesPath.lineTo(left, centerY)
        frameLinesPath.close()

        // Corner-to-corner diagonals
        frameLinesPath.moveTo(left, top)
        frameLinesPath.lineTo(right, bottom)
        frameLinesPath.moveTo(right, top)
        frameLinesPath.lineTo(left, bottom)

        drawPath(frameLinesPath, BORDER_COLOR, style = lineStroke)

        return ChartFrame(left, top, chartSize, centerX, centerY)
    }

    // ============================================================================
    // PUBLIC CHART DRAWING FUNCTIONS
    // ============================================================================

    /**
     * Draw a professional North Indian style Vedic chart
     */
    fun drawNorthIndianChart(
        drawScope: DrawScope,
        chart: VedicChart,
        size: Float,
        chartTitle: String = "Lagna"
    ) {
        with(drawScope) {
            val frame = drawNorthIndianFrame(size)
            val ascendantSign = ZodiacSign.fromLongitude(chart.ascendant)

            drawAllHouseContents(
                left = frame.left,
                top = frame.top,
                chartSize = frame.size,
                centerX = frame.centerX,
                centerY = frame.centerY,
                ascendantSign = ascendantSign,
                planetPositions = chart.planetPositions,
                size = size,
                chart = chart
            )
        }
    }

    /**
     * Draw a divisional chart (D9, D10, etc.)
     */
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

            drawAllHouseContents(
                left = frame.left,
                top = frame.top,
                chartSize = frame.size,
                centerX = frame.centerX,
                centerY = frame.centerY,
                ascendantSign = ascendantSign,
                planetPositions = planetPositions,
                size = size,
                chart = originalChart
            )
        }
    }

    // ============================================================================
    // HOUSE TYPE UTILITIES
    // ============================================================================

    private fun getHouseType(houseNum: Int): HouseType {
        return when (houseNum) {
            1, 4, 7, 10 -> HouseType.DIAMOND
            3, 5, 9, 11 -> HouseType.SIDE
            2, 6, 8, 12 -> HouseType.CORNER
            else -> HouseType.DIAMOND
        }
    }

    /**
     * Converts a house index into a sign number, given the ascendant sign.
     */
    private fun signNumberForHouse(houseNum: Int, ascendantSign: ZodiacSign): Int {
        return ((ascendantSign.ordinal + houseNum - 1) % 12) + 1
    }

    // ============================================================================
    // HOUSE GEOMETRY CALCULATIONS
    // ============================================================================

    /**
     * Get the polygon vertices for a house
     */
    private fun getHousePolygon(
        houseNum: Int,
        left: Float,
        top: Float,
        chartSize: Float,
        centerX: Float,
        centerY: Float
    ): List<Offset> {
        val right = left + chartSize
        val bottom = top + chartSize

        // Outer corners
        val a = Offset(left, top)       // top-left
        val b = Offset(right, top)      // top-right
        val c = Offset(right, bottom)   // bottom-right
        val d = Offset(left, bottom)    // bottom-left

        // Midpoints of sides (diamond vertices)
        val e = Offset(centerX, top)    // top-mid
        val f = Offset(right, centerY)  // right-mid
        val g = Offset(centerX, bottom) // bottom-mid
        val h = Offset(left, centerY)   // left-mid

        val o = Offset(centerX, centerY) // center

        // Quarter points along the diagonals
        val quarter = chartSize * 0.25f
        val threeQuarter = chartSize * 0.75f

        val p = Offset(left + quarter, top + quarter)
        val q = Offset(left + threeQuarter, top + threeQuarter)
        val r = Offset(left + threeQuarter, top + quarter)
        val s = Offset(left + quarter, top + threeQuarter)

        return when (houseNum) {
            // Diamonds (1,4,7,10)
            1 -> listOf(e, p, o, r)
            4 -> listOf(h, s, o, p)
            7 -> listOf(g, s, o, q)
            10 -> listOf(f, r, o, q)

            // Corner triangles (2,6,8,12)
            2 -> listOf(a, e, p)
            6 -> listOf(d, g, s)
            8 -> listOf(c, g, q)
            12 -> listOf(b, e, r)

            // Side triangles (3,5,9,11)
            3 -> listOf(a, h, p)
            5 -> listOf(d, h, s)
            9 -> listOf(c, f, q)
            11 -> listOf(b, f, r)

            else -> emptyList()
        }
    }

    /**
     * Calculate house bounds including usable area for content
     */
    private fun getHouseBounds(
        houseNum: Int,
        left: Float,
        top: Float,
        chartSize: Float,
        centerX: Float,
        centerY: Float
    ): HouseBounds {
        val polygon = getHousePolygon(houseNum, left, top, chartSize, centerX, centerY)
        val centroid = polygonCentroid(polygon)
        val houseType = getHouseType(houseNum)

        // Calculate bounding box of polygon
        val minX = polygon.minOfOrNull { it.x } ?: 0f
        val maxX = polygon.maxOfOrNull { it.x } ?: 0f
        val minY = polygon.minOfOrNull { it.y } ?: 0f
        val maxY = polygon.maxOfOrNull { it.y } ?: 0f

        val width = maxX - minX
        val height = maxY - minY

        // Usable area is smaller for triangular houses to account for shape
        val usableMultiplier = when (houseType) {
            HouseType.CORNER -> 0.55f
            HouseType.SIDE -> 0.6f
            HouseType.DIAMOND -> 0.7f
        }

        return HouseBounds(
            polygon = polygon,
            centroid = centroid,
            approximateWidth = width,
            approximateHeight = height,
            usableWidth = width * usableMultiplier,
            usableHeight = height * usableMultiplier
        )
    }

    /**
     * Compute polygon centroid for accurate house centers
     */
    private fun getHousePlanetCenter(
        houseNum: Int,
        left: Float,
        top: Float,
        chartSize: Float,
        centerX: Float,
        centerY: Float
    ): Offset {
        val polygon = getHousePolygon(houseNum, left, top, chartSize, centerX, centerY)
        return if (polygon.isEmpty()) {
            Offset(centerX, centerY)
        } else {
            polygonCentroid(polygon)
        }
    }

    /**
     * Position for house number with dynamic adjustment based on planet presence
     */
    private fun getHouseNumberPosition(
        houseNum: Int,
        left: Float,
        top: Float,
        chartSize: Float,
        centerX: Float,
        centerY: Float,
        hasPlanets: Boolean
    ): Offset {
        val right = left + chartSize
        val bottom = top + chartSize

        // Base positions
        val basePosition = when (houseNum) {
            1 -> Offset(centerX, top + chartSize * DIAMOND_NUMBER_VERTICAL_FRACTION)
            2 -> Offset(
                centerX - chartSize * CORNER_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                top + chartSize * CORNER_NUMBER_OFFSET_FRACTION
            )
            3 -> Offset(
                left + chartSize * SIDE_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                centerY - chartSize * SIDE_NUMBER_VERTICAL_OFFSET_FRACTION
            )
            4 -> Offset(
                left + chartSize * DIAMOND_NUMBER_VERTICAL_FRACTION - chartSize * DIAMOND_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                centerY + chartSize * DIAMOND_NUMBER_VERTICAL_OFFSET_FRACTION
            )
            5 -> Offset(
                left + chartSize * SIDE_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                centerY + chartSize * SIDE_NUMBER_VERTICAL_OFFSET_FRACTION
            )
            6 -> Offset(
                centerX - chartSize * CORNER_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                bottom - chartSize * CORNER_NUMBER_OFFSET_FRACTION
            )
            7 -> Offset(centerX, bottom - chartSize * DIAMOND_NUMBER_VERTICAL_FRACTION)
            8 -> Offset(
                centerX + chartSize * CORNER_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                bottom - chartSize * CORNER_NUMBER_OFFSET_FRACTION
            )
            9 -> Offset(
                right - chartSize * SIDE_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                centerY + chartSize * SIDE_NUMBER_VERTICAL_OFFSET_FRACTION
            )
            10 -> Offset(
                right - chartSize * DIAMOND_NUMBER_VERTICAL_FRACTION + chartSize * DIAMOND_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                centerY + chartSize * DIAMOND_NUMBER_VERTICAL_OFFSET_FRACTION
            )
            11 -> Offset(
                right - chartSize * SIDE_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                centerY - chartSize * SIDE_NUMBER_VERTICAL_OFFSET_FRACTION
            )
            12 -> Offset(
                centerX + chartSize * CORNER_NUMBER_HORIZONTAL_OFFSET_FRACTION,
                top + chartSize * CORNER_NUMBER_OFFSET_FRACTION
            )
            else -> Offset(centerX, centerY)
        }

        // If planets are present, shift the house number toward the outer edge
        if (hasPlanets) {
            val shiftAmount = chartSize * HOUSE_NUMBER_SHIFT_FRACTION
            val shiftOffset = when (houseNum) {
                1 -> Offset(0f, -shiftAmount)           // Shift up
                2 -> Offset(-shiftAmount, -shiftAmount) // Shift top-left
                3 -> Offset(-shiftAmount, 0f)           // Shift left
                4 -> Offset(-shiftAmount, 0f)           // Shift left
                5 -> Offset(-shiftAmount, 0f)           // Shift left
                6 -> Offset(-shiftAmount, shiftAmount)  // Shift bottom-left
                7 -> Offset(0f, shiftAmount)            // Shift down
                8 -> Offset(shiftAmount, shiftAmount)   // Shift bottom-right
                9 -> Offset(shiftAmount, 0f)            // Shift right
                10 -> Offset(shiftAmount, 0f)           // Shift right
                11 -> Offset(shiftAmount, 0f)           // Shift right
                12 -> Offset(shiftAmount, -shiftAmount) // Shift top-right
                else -> Offset.Zero
            }
            return Offset(basePosition.x + shiftOffset.x, basePosition.y + shiftOffset.y)
        }

        return basePosition
    }

    /**
     * Polygon centroid calculation using the shoelace formula
     */
    private fun polygonCentroid(points: List<Offset>): Offset {
        if (points.isEmpty()) return Offset.Zero
        if (points.size == 1) return points[0]
        if (points.size == 2) {
            return Offset(
                (points[0].x + points[1].x) / 2f,
                (points[0].y + points[1].y) / 2f
            )
        }

        var crossSum = 0.0
        var cx = 0.0
        var cy = 0.0

        for (i in points.indices) {
            val j = (i + 1) % points.size
            val xi = points[i].x.toDouble()
            val yi = points[i].y.toDouble()
            val xj = points[j].x.toDouble()
            val yj = points[j].y.toDouble()

            val cross = xi * yj - xj * yi
            crossSum += cross
            cx += (xi + xj) * cross
            cy += (yi + yj) * cross
        }

        if (abs(crossSum) < 1e-10) {
            // Degenerate polygon, fallback to simple average
            val avgX = points.sumOf { it.x.toDouble() } / points.size
            val avgY = points.sumOf { it.y.toDouble() } / points.size
            return Offset(avgX.toFloat(), avgY.toFloat())
        }

        val factor = 1.0 / (3.0 * crossSum)
        return Offset((cx * factor).toFloat(), (cy * factor).toFloat())
    }

    // ============================================================================
    // HOUSE CONTENT DRAWING
    // ============================================================================

    /**
     * Draw all house contents including house numbers and planets
     */
    private fun DrawScope.drawAllHouseContents(
        left: Float,
        top: Float,
        chartSize: Float,
        centerX: Float,
        centerY: Float,
        ascendantSign: ZodiacSign,
        planetPositions: List<PlanetPosition>,
        size: Float,
        chart: VedicChart? = null,
        showSignNumbers: Boolean = true
    ) {
        val planetsByHouse = planetPositions.groupBy { it.house }
        val sunPosition = chart?.planetPositions?.find { it.planet == Planet.SUN }

        for (houseNum in 1..12) {
            val houseBounds = getHouseBounds(houseNum, left, top, chartSize, centerX, centerY)
            val planets = planetsByHouse[houseNum] ?: emptyList()
            val hasPlanets = planets.isNotEmpty()

            // Draw house number (before planets for proper z-order)
            val numberPos = getHouseNumberPosition(
                houseNum, left, top, chartSize, centerX, centerY, hasPlanets
            )
            val numberText = if (showSignNumbers) {
                signNumberForHouse(houseNum, ascendantSign).toString()
            } else {
                houseNum.toString()
            }

            // Use faded color if planets are present (watermark effect)
            val numberColor = if (hasPlanets) HOUSE_NUMBER_FADED_COLOR else HOUSE_NUMBER_COLOR

            drawTextCentered(
                text = numberText,
                position = numberPos,
                textSize = size * 0.035f,
                color = numberColor,
                isBold = false
            )

            // Build list of items to draw in this house
            val houseItems = mutableListOf<HouseItem>()

            // For House 1, add "La" as the first item in the flow
            if (houseNum == 1) {
                houseItems.add(
                    HouseItem(
                        displayText = "La",
                        color = LAGNA_COLOR,
                        isLagna = true
                    )
                )
            }

            // Add planets
            planets.forEach { planet ->
                val abbrev = planet.planet.symbol
                val degree = (planet.longitude % 30.0).toInt()
                val degreeSuper = toSuperscript(degree)
                val statusIndicators = buildStatusIndicators(planet, chart, sunPosition)
                val displayText = "$abbrev$degreeSuper$statusIndicators"

                houseItems.add(
                    HouseItem(
                        displayText = displayText,
                        color = getPlanetColor(planet.planet),
                        isLagna = false
                    )
                )
            }

            // Draw all items in the house
            if (houseItems.isNotEmpty()) {
                drawItemsInHouse(
                    items = houseItems,
                    houseBounds = houseBounds,
                    size = size,
                    houseNum = houseNum
                )
            }
        }
    }

    /**
     * Draw items (planets and Lagna marker) in a house with dynamic text sizing
     */
    private fun DrawScope.drawItemsInHouse(
        items: List<HouseItem>,
        houseBounds: HouseBounds,
        size: Float,
        houseNum: Int
    ) {
        val houseType = getHouseType(houseNum)
        val houseCenter = houseBounds.centroid

        // Calculate dynamic text size based on content and available space
        val layoutInfo = calculateDynamicLayout(
            itemCount = items.size,
            houseType = houseType,
            houseBounds = houseBounds,
            baseSize = size
        )

        val textSize = layoutInfo.textSize
        val lineHeight = layoutInfo.lineHeight
        val columns = layoutInfo.columns
        val columnSpacing = layoutInfo.columnSpacing

        val itemsPerColumn = ceil(items.size.toFloat() / columns).toInt()

        items.forEachIndexed { index, item ->
            val col = if (columns > 1) index % columns else 0
            val row = index / columns

            val xOffset = if (columns > 1) {
                (col - (columns - 1) / 2f) * columnSpacing
            } else {
                0f
            }

            val totalRows = if (columns > 1) itemsPerColumn else items.size
            val yOffset = (row - (totalRows - 1) / 2f) * lineHeight

            val position = Offset(houseCenter.x + xOffset, houseCenter.y + yOffset)

            // Draw text with background for better readability
            drawTextWithBackground(
                text = item.displayText,
                position = position,
                textSize = textSize,
                color = item.color,
                isBold = !item.isLagna, // Lagna is not bold, planets are
                backgroundColor = TEXT_BACKGROUND_COLOR
            )
        }
    }

    /**
     * Layout calculation result
     */
    private data class LayoutInfo(
        val textSize: Float,
        val lineHeight: Float,
        val columns: Int,
        val columnSpacing: Float
    )

    /**
     * Calculate dynamic layout parameters based on content and house type
     */
    private fun calculateDynamicLayout(
        itemCount: Int,
        houseType: HouseType,
        houseBounds: HouseBounds,
        baseSize: Float
    ): LayoutInfo {
        // Base values
        val baseTextSize = baseSize * 0.032f
        val baseLineHeight = baseSize * 0.042f

        // Determine maximum items per row based on house type
        val maxItemsPerRow = when (houseType) {
            HouseType.CORNER -> MAX_ITEMS_PER_ROW_CORNER
            HouseType.SIDE -> MAX_ITEMS_PER_ROW_SIDE
            HouseType.DIAMOND -> MAX_ITEMS_PER_ROW_DIAMOND
        }

        // Determine if we need multiple columns
        val columns = when {
            houseType == HouseType.CORNER && itemCount >= 3 -> 2
            houseType == HouseType.SIDE && itemCount >= 4 -> 2
            houseType == HouseType.DIAMOND && itemCount >= 5 -> 2
            itemCount > 6 -> min(3, (itemCount + 2) / 3)
            else -> 1
        }

        val itemsPerColumn = ceil(itemCount.toFloat() / columns).toInt()

        // Calculate the height needed for the text block
        var lineHeight = when (houseType) {
            HouseType.CORNER -> baseLineHeight * 0.85f
            HouseType.SIDE -> baseLineHeight * 0.9f
            HouseType.DIAMOND -> baseLineHeight
        }

        var textSize = when {
            itemCount > 4 && houseType == HouseType.CORNER -> baseTextSize * 0.8f
            itemCount > 3 && houseType == HouseType.CORNER -> baseTextSize * 0.85f
            itemCount > 5 -> baseTextSize * 0.9f
            else -> baseTextSize
        }

        // Dynamic sizing: measure if text block fits in usable area
        val estimatedBlockHeight = itemsPerColumn * lineHeight
        val usableHeight = houseBounds.usableHeight * 0.9f // 90% of usable height

        if (estimatedBlockHeight > usableHeight && itemsPerColumn > 1) {
            val scaleFactor = (usableHeight / estimatedBlockHeight).coerceIn(MIN_TEXT_SIZE_RATIO, 1f)
            textSize *= scaleFactor
            lineHeight *= scaleFactor
        }

        // Column spacing based on house type
        val columnSpacing = when (houseType) {
            HouseType.CORNER -> baseSize * 0.045f
            HouseType.SIDE -> baseSize * 0.055f
            HouseType.DIAMOND -> baseSize * 0.07f
        }

        return LayoutInfo(
            textSize = textSize,
            lineHeight = lineHeight,
            columns = columns,
            columnSpacing = columnSpacing
        )
    }

    // ============================================================================
    // TEXT DRAWING FUNCTIONS
    // ============================================================================

    /**
     * Draw centered text with optional background for better readability
     */
    private fun DrawScope.drawTextWithBackground(
        text: String,
        position: Offset,
        textSize: Float,
        color: Color,
        isBold: Boolean = false,
        backgroundColor: Color? = null
    ) {
        val typeface = if (isBold) TYPEFACE_BOLD else TYPEFACE_NORMAL

        // Configure paint
        textPaint.apply {
            this.color = color.toArgb()
            this.textSize = textSize
            this.typeface = typeface
        }

        val textWidth = textPaint.measureText(text)
        val textHeight = textPaint.descent() - textPaint.ascent()
        val textOffset = textHeight / 2 - textPaint.descent()

        // Draw background rectangle if specified
        if (backgroundColor != null) {
            val padding = textSize * 0.15f
            backgroundPaint.color = backgroundColor.toArgb()

            drawContext.canvas.nativeCanvas.drawRoundRect(
                position.x - textWidth / 2 - padding,
                position.y - textHeight / 2 - padding * 0.5f,
                position.x + textWidth / 2 + padding,
                position.y + textHeight / 2 + padding * 0.5f,
                padding,
                padding,
                backgroundPaint
            )
        }

        // Draw the text
        drawContext.canvas.nativeCanvas.drawText(
            text,
            position.x,
            position.y + textOffset,
            textPaint
        )
    }

    /**
     * Draw centered text (simple version without background)
     */
    private fun DrawScope.drawTextCentered(
        text: String,
        position: Offset,
        textSize: Float,
        color: Color,
        isBold: Boolean = false
    ) {
        val typeface = if (isBold) TYPEFACE_BOLD else TYPEFACE_NORMAL

        textPaint.apply {
            this.color = color.toArgb()
            this.textSize = textSize
            this.typeface = typeface
        }

        val textHeight = textPaint.descent() - textPaint.ascent()
        val textOffset = textHeight / 2 - textPaint.descent()

        drawContext.canvas.nativeCanvas.drawText(
            text,
            position.x,
            position.y + textOffset,
            textPaint
        )
    }

    // ============================================================================
    // CUSTOM SYMBOL DRAWING (Path-based for guaranteed rendering)
    // ============================================================================

    /**
     * Draw a small upward arrow (exaltation indicator)
     */
    private fun DrawScope.drawExaltationArrow(center: Offset, size: Float, color: Color) {
        val arrowPath = Path().apply {
            moveTo(center.x, center.y - size / 2)
            lineTo(center.x - size / 3, center.y + size / 2)
            lineTo(center.x, center.y + size / 4)
            lineTo(center.x + size / 3, center.y + size / 2)
            close()
        }
        drawPath(arrowPath, color)
    }

    /**
     * Draw a small downward arrow (debilitation indicator)
     */
    private fun DrawScope.drawDebilitationArrow(center: Offset, size: Float, color: Color) {
        val arrowPath = Path().apply {
            moveTo(center.x, center.y + size / 2)
            lineTo(center.x - size / 3, center.y - size / 2)
            lineTo(center.x, center.y - size / 4)
            lineTo(center.x + size / 3, center.y - size / 2)
            close()
        }
        drawPath(arrowPath, color)
    }

    /**
     * Draw a small star (vargottama indicator)
     */
    private fun DrawScope.drawVargottamaStar(center: Offset, size: Float, color: Color) {
        val points = 4
        val outerRadius = size / 2
        val innerRadius = size / 4
        val starPath = Path()

        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = Math.PI / 2 + (i * Math.PI / points)
            val x = center.x + (radius * kotlin.math.cos(angle)).toFloat()
            val y = center.y - (radius * kotlin.math.sin(angle)).toFloat()

            if (i == 0) {
                starPath.moveTo(x, y)
            } else {
                starPath.lineTo(x, y)
            }
        }
        starPath.close()
        drawPath(starPath, color)
    }

    // ============================================================================
    // BITMAP CREATION FUNCTIONS
    // ============================================================================

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
            drawDivisionalChart(
                this,
                planetPositions,
                ascendantLongitude,
                min(width, height).toFloat(),
                chartTitle
            )
        }

        return bitmap
    }

    // ============================================================================
    // SOUTH INDIAN CHART (Alias)
    // ============================================================================

    /**
     * South Indian chart is currently an alias to the North Indian renderer.
     * TODO: Implement proper South Indian layout
     */
    fun drawSouthIndianChart(
        drawScope: DrawScope,
        chart: VedicChart,
        size: Float
    ) {
        drawNorthIndianChart(drawScope, chart, size, "Lagna")
    }

    // ============================================================================
    // CHART LEGEND
    // ============================================================================

    fun DrawScope.drawChartLegend(
        chartBottom: Float,
        chartLeft: Float,
        chartWidth: Float,
        textSize: Float
    ) {
        val legendY = chartBottom + textSize * 1.5f
        val legendItems = listOf(
            Pair("($SYMBOL_RETROGRADE) Retrograde", HOUSE_NUMBER_COLOR),
            Pair("($SYMBOL_COMBUST) Combust", HOUSE_NUMBER_COLOR),
            Pair("($SYMBOL_VARGOTTAMA) Vargottama", HOUSE_NUMBER_COLOR),
            Pair("($SYMBOL_EXALTED) Exalted", HOUSE_NUMBER_COLOR),
            Pair("($SYMBOL_DEBILITATED) Debilitated", HOUSE_NUMBER_COLOR)
        )

        val totalItems = legendItems.size
        val itemWidth = chartWidth / totalItems

        legendItems.forEachIndexed { index, (text, color) ->
            val xPos = chartLeft + (index * itemWidth) + (itemWidth / 2)
            drawTextCentered(
                text = text,
                position = Offset(xPos, legendY),
                textSize = textSize * 0.75f,
                color = color,
                isBold = false
            )
        }
    }

    fun drawChartWithLegend(
        drawScope: DrawScope,
        chart: VedicChart,
        size: Float,
        chartTitle: String = "Lagna",
        showLegend: Boolean = true
    ) {
        with(drawScope) {
            val legendHeight = if (showLegend) size * 0.08f else 0f
            val chartSize = size - legendHeight

            drawNorthIndianChart(this, chart, chartSize, chartTitle)

            if (showLegend) {
                val padding = chartSize * 0.02f
                val chartWidth = chartSize - (padding * 2)

                drawRect(
                    color = BACKGROUND_COLOR,
                    topLeft = Offset(0f, chartSize),
                    size = Size(size, legendHeight)
                )

                drawChartLegend(
                    chartBottom = chartSize,
                    chartLeft = padding,
                    chartWidth = chartWidth,
                    textSize = chartSize * 0.028f
                )
            }
        }
    }
}