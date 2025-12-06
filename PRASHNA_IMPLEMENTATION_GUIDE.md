# AstroStorm Codebase Analysis - Comprehensive Report
## A Complete Guide for Implementing the Prashna (Horary Astrology) Feature

---

## EXECUTIVE SUMMARY

AstroStorm is a **production-grade Android Vedic astrology application** built entirely in **Kotlin** with **Jetpack Compose** and **Material 3**. The app uses the **Swiss Ephemeris engine** for ultra-precise astronomical calculations and follows **Clean Architecture** with **MVVM** pattern.

**Current Status**: The codebase is fully functional with 11 implemented features. Prashna (Horary Astrology) is currently marked as "Coming Soon" in the UI.

---

## 1. PROJECT STRUCTURE & ARCHITECTURE

### Overall Directory Structure
```
app/
├── src/main/java/com/astro/storm/
│   ├── data/
│   │   ├── local/              # Room Database Layer (4 files)
│   │   │   ├── ChartDatabase.kt
│   │   │   ├── ChartDao.kt
│   │   │   ├── ChartEntity.kt
│   │   │   └── Converters.kt
│   │   ├── model/              # Domain Models (7 files)
│   │   │   ├── BirthData.kt
│   │   │   ├── Planet.kt
│   │   │   ├── Nakshatra.kt
│   │   │   ├── ZodiacSign.kt
│   │   │   ├── PlanetPosition.kt
│   │   │   ├── VedicChart.kt
│   │   │   └── HouseSystem.kt
│   │   └── repository/         # Data Access Layer (1 file)
│   │       └── ChartRepository.kt
│   ├── ephemeris/              # Calculation Engines (15 files!)
│   │   ├── SwissEphemerisEngine.kt     (Main engine - 300+ lines)
│   │   ├── PanchangaCalculator.kt
│   │   ├── DashaCalculator.kt
│   │   ├── YogaCalculator.kt
│   │   ├── AspectCalculator.kt
│   │   ├── DivisionalChartCalculator.kt
│   │   ├── AshtakavargaCalculator.kt
│   │   ├── ShadbalaCalculator.kt
│   │   ├── TransitAnalyzer.kt
│   │   ├── RetrogradeCombustionCalculator.kt
│   │   ├── MatchmakingCalculator.kt
│   │   ├── MuhurtaCalculator.kt
│   │   ├── RemediesCalculator.kt
│   │   ├── VarshaphalaCalculator.kt
│   │   └── HoroscopeCalculator.kt
│   ├── ui/
│   │   ├── chart/              # Chart Rendering (1 file)
│   │   │   └── ChartRenderer.kt        (700+ lines, produces 2048x2048 PNG)
│   │   ├── navigation/         # Navigation (1 file)
│   │   │   └── Navigation.kt           (Type-safe routing)
│   │   ├── screen/             # Main Screens (15+ files)
│   │   │   ├── main/
│   │   │   │   ├── MainScreen.kt       (Tab-based main screen)
│   │   │   │   ├── HomeTab.kt          (Features grid - Prashna listed here!)
│   │   │   │   ├── InsightsTab.kt
│   │   │   │   └── SettingsTab.kt
│   │   │   ├── ChartInputScreen.kt     (Birth data form)
│   │   │   ├── ChartAnalysisScreen.kt  (Chart details with tabs)
│   │   │   ├── ChartDetailScreen.kt
│   │   │   ├── MuhurtaScreen.kt        (Example feature screen)
│   │   │   ├── MatchmakingScreen.kt
│   │   │   ├── RemediesScreen.kt
│   │   │   └── VarshaphalaScreen.kt
│   │   ├── theme/              # Material 3 Theme (4 files)
│   │   │   ├── Color.kt                (68 colors defined)
│   │   │   ├── Theme.kt                (Dark theme always on)
│   │   │   ├── Type.kt                 (Typography)
│   │   │   └── AppTheme.kt             (Theme object)
│   │   ├── components/         # Reusable Components (10+ files)
│   │   │   ├── dialogs/
│   │   │   │   ├── PlanetDetailDialog.kt
│   │   │   │   ├── FullScreenChartDialog.kt
│   │   │   │   └── DialogComponents.kt
│   │   │   ├── ProfileSwitcherBottomSheet.kt
│   │   │   └── ChartDialogs.kt
│   │   └── viewmodel/          # ViewModels (3 files)
│   │       ├── ChartViewModel.kt
│   │       ├── ChartDetailViewModel.kt
│   │       └── InsightsViewModel.kt
│   ├── util/                   # Utilities (4 files)
│   │   ├── AstrologicalUtils.kt
│   │   ├── ChartExporter.kt
│   │   ├── ExportUtils.kt
│   │   └── PlanetaryRelationships.kt
│   ├── MainActivity.kt
│   └── AstroStormApplication.kt
└── res/                        # Resources (drawable, values, xml)
```

### Architecture Pattern: **Clean MVVM**
```
Presentation Layer (UI)
    ↓ (Uses)
Business Logic Layer (ViewModels)
    ↓ (Uses)
Domain Layer (Calculations + Models)
    ↓ (Uses)
Data Layer (Repository + Database)
```

---

## 2. THEME, COLORS & DESIGN PATTERNS

### AppTheme Object (Primary Design System)
Located in: `/ui/theme/AppTheme.kt`

**Color Palette** (Warm earth tones with cosmic accents):
```kotlin
// Primary Colors
ScreenBackground = Color(0xFF1C1410)      // Deep brown
CardBackground = Color(0xFF2A201A)        // Card brown
AccentPrimary = Color(0xFFB8A99A)         // Warm beige
AccentGold = Color(0xFFD4AF37)            // Gold highlights

// Text Colors
TextPrimary = Color(0xFFE8DFD6)           // Light beige
TextSecondary = Color(0xFFB8A99A)         // Muted beige
TextMuted = Color(0xFF8A7A6A)             // Very muted
TextSubtle = Color(0xFF6A5A4A)            // Subtle gray

// Status Colors
SuccessColor = Color(0xFF81C784)          // Green
WarningColor = Color(0xFFFFB74D)          // Orange
ErrorColor = Color(0xFFCF6679)            // Red
InfoColor = Color(0xFF64B5F6)             // Blue

// Life Area Colors (for category icons)
LifeAreaCareer = Color(0xFFFFB74D)
LifeAreaLove = Color(0xFFE57373)
LifeAreaHealth = Color(0xFF81C784)
LifeAreaGrowth = Color(0xFF64B5F6)
LifeAreaFinance = Color(0xFFFFD54F)
LifeAreaSpiritual = Color(0xFFBA68C8)
```

### Material 3 Theme Configuration
- **Always Dark Mode**: The app forces dark theme (astronomy-appropriate)
- **Status/Navigation Bars**: Colored to match background
- **Typography**: System font with clear hierarchy
- **Corner Radius**: 12.dp for cards, 10.dp for icon containers

### Design Patterns Used

1. **Feature Grid Layout**
   - 2-column grid for feature cards
   - Implemented in `HomeTab.kt`
   - Cards are 40x60dp with 12dp spacing

2. **Card Component Pattern**
   ```kotlin
   Card(
       modifier = Modifier.clip(RoundedCornerShape(12.dp)),
       colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
       elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
   ) {
       Column(modifier = Modifier.padding(16.dp)) {
           // Content here
       }
   }
   ```

3. **Section Headers**
   - Title text with optional muted state
   - Padding: 16.dp horizontal, 12.dp bottom

4. **Icon Container Pattern**
   ```kotlin
   Box(
       modifier = Modifier
           .size(40.dp)
       .clip(RoundedCornerShape(10.dp))
       .background(iconBackgroundColor),
       contentAlignment = Alignment.Center
   ) {
       Icon(imageVector = feature.icon, ...)
   }
   ```

---

## 3. NAVIGATION & ROUTING STRUCTURE

### Navigation Graph (Type-Safe)
Located in: `/ui/navigation/Navigation.kt`

**Routes Enum**:
```kotlin
sealed class Screen(val route: String) {
    object Main : Screen("main")                                    // Entry point
    object ChartInput : Screen("chart_input")
    object ChartAnalysis : Screen("chart_analysis/{chartId}/{feature}")
    object Matchmaking : Screen("matchmaking")
    object Muhurta : Screen("muhurta")
    object Remedies : Screen("remedies/{chartId}")
    object Varshaphala : Screen("varshaphala/{chartId}")
    // Prashna would go here: object Prashna : Screen("prashna")
}
```

### Navigation Flow
```
Main Screen
├── Home Tab
│   └── Feature Grid (2 columns)
│       ├── Implemented Features (clickable)
│       └── Coming Soon Features (disabled, badge)
├── Insights Tab
└── Settings Tab

Chart Input Screen
└── Calculates chart, saves to database

Chart Analysis Screen
└── Horizontal scrolling tabs for:
    - Full Chart
    - Planets
    - Yogas
    - Dashas
    - Transits
    - Ashtakavarga
    - Panchanga
    - Divisional Charts

Feature-Specific Screens
├── Matchmaking Screen
├── Muhurta Screen
├── Remedies Screen
└── Varshaphala Screen
```

### Navigation Implementation Example
```kotlin
// From HomeTab
onFeatureClick = { feature ->
    if (feature.isImplemented) {
        when (feature) {
            InsightFeature.MUHURTA -> onNavigateToMuhurta()
            InsightFeature.PRASHNA -> onNavigateToPrashna()  // TODO
            else -> onNavigateToChartAnalysis(feature)
        }
    }
}
```

---

## 4. EXISTING ASTROLOGY CALCULATIONS

### Primary Calculation Engine: SwissEphemerisEngine
Located in: `/ephemeris/SwissEphemerisEngine.kt` (~300 lines)

**Capabilities**:
```kotlin
class SwissEphemerisEngine {
    fun calculateVedicChart(
        birthData: BirthData,
        houseSystem: HouseSystem
    ): VedicChart
    
    // Returns:
    // - Planetary positions (9 planets)
    // - House cusps (12 houses)
    // - Ascendant, Midheaven
    // - Julian Day
    // - Ayanamsa (Lahiri)
}
```

**Key Parameters**:
- **Ayanamsa**: Lahiri (Vedic sidereal zodiac)
- **Ephemeris Mode**: JPL (most accurate)
- **Time Conversion**: Local → UTC → Julian Day
- **Retrograde Detection**: Via planetary speed

### Available Calculation Modules

| Calculator | Purpose | Lines |
|-----------|---------|-------|
| **SwissEphemerisEngine** | Core calculations | 300+ |
| **PanchangaCalculator** | Vedic calendar (Tithi, Yoga, Karana) | 300+ |
| **DashaCalculator** | Vimshottari Dasha periods | 800+ |
| **YogaCalculator** | Auspicious planetary combinations | 1500+ |
| **AspectCalculator** | Planetary aspects & strengths | 600+ |
| **DivisionalChartCalculator** | D9, D10, D24, etc. charts | 900+ |
| **AshtakavargaCalculator** | 8-fold strength analysis | 700+ |
| **ShadbalaCalculator** | 6-fold strength (Rupa, Cheshta, etc.) | 700+ |
| **TransitAnalyzer** | Current planetary transits | 700+ |
| **RetrogradeCombustionCalculator** | Retrograde & combustion detection | 700+ |
| **MatchmakingCalculator** | Kundli Milan (gun score) | 2000+ |
| **MuhurtaCalculator** | Auspicious timings | 1500+ |
| **RemediesCalculator** | Personalized remedies | 2500+ |
| **VarshaphalaCalculator** | Solar return horoscope | 2000+ |
| **HoroscopeCalculator** | Daily/weekly predictions | 1000+ |

### Data Models (All Immutable)
```kotlin
data class BirthData(
    val name: String,
    val dateTime: LocalDateTime,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val location: String
)

data class VedicChart(
    val birthData: BirthData,
    val julianDay: Double,
    val ayanamsa: Double,
    val ayanamsaName: String,
    val ascendant: Double,
    val midheaven: Double,
    val planetPositions: List<PlanetPosition>,    // 9 planets
    val houseCusps: List<Double>,                 // 12 houses
    val houseSystem: HouseSystem,
    val calculationTime: Long
)

data class PlanetPosition(
    val planet: Planet,
    val longitude: Double,
    val latitude: Double,
    val distance: Double,
    val speed: Double,
    val sign: ZodiacSign,
    val degree: Double,
    val minutes: Double,
    val seconds: Double,
    val isRetrograde: Boolean,
    val nakshatra: Nakshatra,
    val nakshatraPada: Int,    // 1-4
    val house: Int             // 1-12
)
```

### Planets Supported
```kotlin
enum class Planet(val swissEphId: Int, val displayName: String) {
    // Traditional 9 Vedic planets
    SUN(0, "Sun")
    MOON(1, "Moon")
    MERCURY(2, "Mercury")
    VENUS(3, "Venus")
    MARS(4, "Mars")
    JUPITER(5, "Jupiter")
    SATURN(6, "Saturn")
    RAHU(10, "Rahu")      // North Node
    KETU(-1, "Ketu")      // South Node (180° from Rahu)
    
    // Modern outer planets (optional)
    URANUS(7, "Uranus")
    NEPTUNE(8, "Neptune")
    PLUTO(9, "Pluto")
}
```

---

## 5. CHART RENDERING & VISUALIZATION

### ChartRenderer
Located in: `/ui/chart/ChartRenderer.kt` (~700 lines)

**Renders**: South Indian Diamond Chart (Rashi Chart)

**Features**:
- Canvas-based rendering for quality
- Outputs: 2048x2048 PNG bitmap
- Shows planets in houses with colors
- Retrograde indicators (*)
- Ascendant marking
- House numbers
- Cosmic dark theme

**Color Mapping**:
```kotlin
fun getPlanetColor(planet: Planet): Color = when (planet) {
    Planet.SUN -> Color(0xFFD2691E)      // Orange-brown
    Planet.MOON -> Color(0xFFDC143C)     // Crimson
    Planet.MARS -> Color(0xFFB22222)     // Red
    Planet.MERCURY -> Color(0xFF228B22)  // Green
    Planet.JUPITER -> Color(0xFFDAA520)  // Goldenrod
    Planet.VENUS -> Color(0xFF9370DB)    // Orchid
    Planet.SATURN -> Color(0xFF4169E1)   // Royal Blue
    Planet.RAHU -> Color(0xFF8B0000)     // Dark red
    Planet.KETU -> Color(0xFF8B0000)     // Dark red
}
```

**How to Use Renderer**:
```kotlin
val chartRenderer = ChartRenderer()
val bitmap = chartRenderer.renderChart(
    vedicChart = chart,
    density = LocalDensity.current
)
// bitmap is 2048x2048 PNG ready for export
```

---

## 6. STATE MANAGEMENT & VIEW MODELS

### ChartViewModel (Main ViewModel)
Located in: `/ui/viewmodel/ChartViewModel.kt` (~300 lines)

**State Flow Management**:
```kotlin
class ChartViewModel(application: Application) : AndroidViewModel(application) {
    // UI State
    private val _uiState = MutableStateFlow<ChartUiState>(ChartUiState.Initial)
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()
    
    // Saved Charts
    private val _savedCharts = MutableStateFlow<List<SavedChart>>(emptyList())
    val savedCharts: StateFlow<List<SavedChart>> = _savedCharts.asStateFlow()
    
    // Selected Chart
    private val _selectedChartId = MutableStateFlow<Long?>(null)
    val selectedChartId: StateFlow<Long?> = _selectedChartId.asStateFlow()
    
    // Core functions
    fun calculateChart(birthData: BirthData, houseSystem: HouseSystem)
    fun loadChart(chartId: Long)
    fun saveChart(chart: VedicChart)
    fun deleteChart(chartId: Long)
    fun exportChartToImage(chart: VedicChart, density: Density)
    fun exportChartToJson(chart: VedicChart)
    fun copyChartToClipboard(chart: VedicChart)
}
```

**UI State Sealed Class**:
```kotlin
sealed class ChartUiState {
    object Initial : ChartUiState()
    object Loading : ChartUiState()
    object Calculating : ChartUiState()
    data class Success(val chart: VedicChart) : ChartUiState()
    data class Error(val message: String) : ChartUiState()
    object Saved : ChartUiState()
}
```

### InsightsViewModel
Located in: `/ui/viewmodel/InsightsViewModel.kt`

Handles caching and loading of insights data (Dashas, Horoscopes, etc.)

---

## 7. DATABASE & PERSISTENCE

### Room Database Structure
Located in: `/data/local/`

**ChartEntity** (Table):
```kotlin
@Entity(tableName = "charts")
data class ChartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dateTime: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val location: String,
    val julianDay: Double,
    val ayanamsa: Double,
    val ayanamsaName: String,
    val ascendant: Double,
    val midheaven: Double,
    val planetPositionsJson: String,     // JSON array
    val houseCuspsJson: String,          // JSON array
    val houseSystem: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

**ChartDao** (Data Access):
```kotlin
@Dao
interface ChartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChart(chart: ChartEntity): Long
    
    @Query("SELECT * FROM charts WHERE id = :id")
    suspend fun getChartById(id: Long): ChartEntity?
    
    @Query("SELECT * FROM charts ORDER BY createdAt DESC")
    fun getAllCharts(): Flow<List<ChartEntity>>
    
    @Query("DELETE FROM charts WHERE id = :id")
    suspend fun deleteChartById(id: Long)
    
    @Query("SELECT * FROM charts WHERE name LIKE '%' || :query || '%'")
    fun searchCharts(query: String): Flow<List<ChartEntity>>
}
```

**Repository Pattern** (`ChartRepository`):
- Single source of truth
- Handles serialization/deserialization
- Uses JSON for complex objects

---

## 8. COMING SOON FEATURES (HomeTab Implementation)

### InsightFeature Enum
Located in: `/ui/screen/main/HomeTab.kt` (Line 422-428)

```kotlin
enum class InsightFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isImplemented: Boolean
) {
    // ... 11 implemented features ...
    
    PRASHNA(
        title = "Prashna",
        description = "Horary astrology",
        icon = Icons.Outlined.HelpOutline,
        color = AppTheme.AccentTeal,
        isImplemented = false  // ← Currently not implemented
    ),
    CHART_COMPARISON(
        title = "Synastry",
        description = "Chart comparison",
        icon = Icons.Outlined.CompareArrows,
        color = AppTheme.LifeAreaFinance,
        isImplemented = false
    ),
    // ... other coming soon features ...
}
```

### Feature Grid Display Logic
Located in: `/ui/screen/main/HomeTab.kt` (Lines 84-132)

```kotlin
@Composable
fun HomeTab(chart: VedicChart?, onFeatureClick: (InsightFeature) -> Unit) {
    val implementedFeatures = remember { InsightFeature.implementedFeatures }
    val comingSoonFeatures = remember { InsightFeature.comingSoonFeatures }
    
    LazyColumn {
        // Section 1: Chart Analysis (Implemented)
        item { SectionHeader("Chart Analysis") }
        item {
            FeatureGrid(
                features = implementedFeatures,
                onFeatureClick = onFeatureClick,
                isDisabled = false
            )
        }
        
        // Section 2: Coming Soon
        item { SectionHeader("Coming Soon", isMuted = true) }
        item {
            FeatureGrid(
                features = comingSoonFeatures,
                onFeatureClick = {},  // No-op for coming soon
                isDisabled = true      // Shows "Soon" badge
            )
        }
    }
}
```

### Coming Soon Card Styling
- Reduced opacity (0.5x)
- "Soon" badge in top-right corner
- Disabled interaction state
- Muted text colors

---

## 9. FEATURE IMPLEMENTATION PATTERNS

### Example: How Muhurta Screen is Implemented

**Navigation Integration** (`Navigation.kt`):
```kotlin
composable(Screen.Muhurta.route) {
    MuhurtaScreen(
        chart = currentChart,
        onBack = { navController.popBackStack() }
    )
}
```

**Screen File** (`MuhurtaScreen.kt`):
```kotlin
@Composable
fun MuhurtaScreen(
    chart: VedicChart?,
    onBack: () -> Unit
) {
    // 1. State management
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // 2. Calculate on mount
    val muhurtaTimes = remember(chart, selectedDate) {
        if (chart != null) {
            MuhurtaCalculator(context).calculateMuhurta(
                chart, selectedDate
            )
        } else null
    }
    
    // 3. UI Display
    Scaffold(
        topBar = { TopAppBar(...) },
        containerColor = AppTheme.ScreenBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            // Display results
            items(muhurtaTimes) { muhurta ->
                MuhurtaCard(muhurta)
            }
        }
    }
}
```

### Screen Template Pattern

Every feature screen follows this pattern:
```kotlin
@Composable
fun FeatureScreen(
    chart: VedicChart?,
    onBack: () -> Unit
) {
    // 1. Local state for UI
    var selectedFilter by remember { mutableStateOf(...) }
    var expandedItem by remember { mutableStateOf<Int?>(null) }
    
    // 2. Derived calculations (if needed)
    val calculatedData = remember(chart, selectedFilter) {
        if (chart != null) {
            Calculator.calculate(chart, selectedFilter)
        } else null
    }
    
    // 3. UI Structure
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feature Name") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, ...)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.ScreenBackground
                )
            )
        },
        containerColor = AppTheme.ScreenBackground
    ) { padding ->
        // 4. Content in LazyColumn
        LazyColumn(modifier = Modifier.padding(padding)) {
            // Items here
        }
    }
}
```

---

## 10. BUILD CONFIGURATION & DEPENDENCIES

### Gradle Build File
Located in: `app/build.gradle.kts`

**Key Dependencies**:
```kotlin
// Core Android
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
androidx.activity:activity-compose:1.8.2

// Compose
androidx.compose.ui:ui
androidx.compose.ui:ui-graphics
androidx.compose.material3:material3
androidx.compose.material:material-icons-extended

// Navigation
androidx.navigation:navigation-compose:2.7.7

// Room Database
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1

// Coroutines
kotlinx-coroutines-android:1.7.3

// Swiss Ephemeris (external JAR)
swisseph-2.10.03.jar (in app/libs/)

// Permissions
accompanist-permissions:0.34.0
```

**SDK Versions**:
- compileSdk: 34
- targetSdk: 34
- minSdk: 26 (Android 8.0+)
- Kotlin: 1.9.22
- Java: 17

---

## 11. EXISTING SCREENS QUICK REFERENCE

### Home Tab (Main Entry Point)
File: `HomeTab.kt` - 503 lines
- Displays 2 grids of features
- Implemented features (clickable)
- Coming soon features (disabled, with badge)
- Prashna is listed as "Coming Soon" here

### Chart Input Screen
File: `ChartInputScreen.kt` - 300+ lines
- Birth data form with validation
- Date/time picker
- Location with coordinates
- Timezone selector
- Form validation with error display

### Chart Analysis Screen
File: `ChartAnalysisScreen.kt` - 400+ lines
- Tab-based navigation (horizontal scroll)
- 7 analysis tabs:
  - Full Chart (visual rendering)
  - Planets (detailed positions)
  - Yogas (planetary combinations)
  - Dashas (period timelines)
  - Transits (current movements)
  - Ashtakavarga (strength analysis)
  - Panchanga (calendar elements)
- Dialog management for details

### Muhurta Screen
File: `MuhurtaScreen.kt` - 300+ lines
- **BEST REFERENCE FOR PRASHNA**
- Date selection with calendar
- Displays auspicious timings
- Expandable time slots
- Filter/search capability

### Matchmaking Screen
File: `MatchmakingScreen.kt` - 500+ lines
- Dual chart selection
- Gun Milan compatibility calculation
- Expandable detail sections
- Visual score display

---

## 12. COMPONENT LIBRARY

### Reusable Dialog Components

**FullScreenChartDialog** (`dialogs/FullScreenChartDialog.kt`):
```kotlin
@Composable
fun FullScreenChartDialog(
    chart: VedicChart,
    chartRenderer: ChartRenderer,
    chartTitle: String,
    divisionalChartData: DivisionalChartData?,
    onDismiss: () -> Unit
)
```

**PlanetDetailDialog** (`dialogs/PlanetDetailDialog.kt`):
```kotlin
@Composable
fun PlanetDetailDialog(
    planetPosition: PlanetPosition,
    chart: VedicChart,
    onDismiss: () -> Unit
)
```

### Common Patterns

**Card with Icon**:
```kotlin
Card(
    modifier = Modifier.padding(16.dp),
    colors = CardDefaults.cardColors(
        containerColor = AppTheme.CardBackground
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
) {
    Row(modifier = Modifier.padding(16.dp)) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

**Expandable Section**:
```kotlin
var expanded by remember { mutableStateOf(false) }
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = !expanded }
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            Icon(
                imageVector = if (expanded)
                    Icons.Default.ExpandLess
                else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
        if (expanded) {
            Content()
        }
    }
}
```

---

## 13. EXPORT & UTILITIES

### ChartExporter
File: `/util/ChartExporter.kt`

Exports charts to:
- **PNG** (2048x2048, high quality)
- **JSON** (complete data)
- **Plaintext** (LLM-friendly format)
- **Clipboard** (formatted text)

### Plaintext Export Format
Used for LLM integration:
```
═══════════════════════════════════════════════════════════
                  VEDIC BIRTH CHART
═══════════════════════════════════════════════════════════

BIRTH INFORMATION
─────────────────────────────────────────────────────────
Name          : [Name]
Date & Time   : [ISO 8601 format]
Location      : [City, Country]
Coordinates   : [Lat/Long with N/S/E/W]
Timezone      : [IANA timezone]

ASTRONOMICAL DATA
─────────────────────────────────────────────────────────
Julian Day    : [JD value]
Ayanamsa      : Lahiri
Ayanamsa Value: [Value in degrees]
Ascendant     : [Degree] (Zodiac Sign)
Midheaven     : [Degree] (Zodiac Sign)

PLANETARY POSITIONS (Sidereal)
─────────────────────────────────────────────────────────
[Detailed planet data with nakshatra]

HOUSE CUSPS
─────────────────────────────────────────────────────────
[House 1-12 with degrees and signs]

[More sections...]
```

---

## 14. KEY PATTERNS FOR PRASHNA IMPLEMENTATION

### Data Model Pattern
```kotlin
data class PrashnaChart(
    val questionData: QuestionData,
    val prashnaTime: LocalDateTime,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val lagna: Double,
    val ascendant: Double,
    val planetPositions: List<PlanetPosition>,
    val houseCusps: List<Double>,
    val prashnaHouses: Map<Int, String>,  // Significances
    val significator: Planet?,             // Quesited planet
    val judgment: String                   // Astrological interpretation
)

data class QuestionData(
    val question: String,
    val category: PrashnaCategory,  // Yes/No, When, Who, etc.
    val userLocation: String
)

enum class PrashnaCategory {
    YES_NO,
    TIMING,
    IDENTIFICATION,
    OUTCOME,
    GENERAL
}
```

### Calculator Pattern
```kotlin
class PrashnaCalculator(context: Context) {
    private val swissEph: SwissEph = SwissEph()
    
    fun calculatePrashna(
        questionData: QuestionData,
        latitude: Double,
        longitude: Double,
        timezone: String
    ): PrashnaChart {
        // 1. Use current time as question time
        val now = LocalDateTime.now()
        
        // 2. Calculate main chart using SwissEphemerisEngine
        val mainChart = engine.calculateVedicChart(
            BirthData(
                name = "Prashna: ${questionData.question}",
                dateTime = now,
                latitude = latitude,
                longitude = longitude,
                timezone = timezone,
                location = questionData.userLocation
            )
        )
        
        // 3. Apply Prashna-specific rules
        // 4. Determine significators
        // 5. Judge question based on houses and planets
        // 6. Return interpretation
    }
}
```

### UI Screen Pattern
```kotlin
@Composable
fun PrashnaScreen(
    onBack: () -> Unit,
    viewModel: ChartViewModel
) {
    var question by remember { mutableStateOf("") }
    var selectedCategory by remember { 
        mutableStateOf(PrashnaCategory.YES_NO) 
    }
    var useCurrentLocation by remember { mutableStateOf(true) }
    var selectedLocation by remember { mutableStateOf("") }
    
    var calculating by remember { mutableStateOf(false) }
    var prashnaChart by remember { mutableStateOf<PrashnaChart?>(null) }
    
    // ... form UI ...
    
    // On calculate
    LaunchedEffect(triggerCalculate) {
        calculating = true
        try {
            prashnaChart = PrashnaCalculator(context).calculatePrashna(
                questionData = QuestionData(question, selectedCategory),
                latitude = userLat,
                longitude = userLon,
                timezone = userTz
            )
        } finally {
            calculating = false
        }
    }
    
    // Display results if available
    if (prashnaChart != null) {
        PrashnaResultsSection(prashnaChart)
    }
}
```

---

## 15. INTEGRATION CHECKLIST FOR PRASHNA FEATURE

To implement Prashna (Horary Astrology), follow these steps:

### Phase 1: Data Models & Calculations
- [ ] Create `PrashnaChart.kt` data model
- [ ] Create `QuestionData.kt` data model
- [ ] Create `PrashnaCategory.kt` enum
- [ ] Create `PrashnaCalculator.kt` calculation engine
- [ ] Add Prashna calculation methods to calculator
- [ ] Add house significances for Prashna
- [ ] Add planet significators mapping

### Phase 2: Database & Repository
- [ ] Create `PrashnaEntity.kt` for Room
- [ ] Add `prashnas` table to database schema
- [ ] Update `ChartDao` with Prashna queries
- [ ] Update `ChartRepository` to handle Prashna data
- [ ] Add serialization/deserialization for Prashna data

### Phase 3: UI Components
- [ ] Create `PrashnaScreen.kt` (main screen)
- [ ] Create question input form component
- [ ] Create category selector component
- [ ] Create location picker component
- [ ] Create results display component
- [ ] Create interpretation card component

### Phase 4: Navigation & Integration
- [ ] Add `Prashna` screen route to `Navigation.kt`
- [ ] Update `HomeTab.kt`: Set `PRASHNA.isImplemented = true`
- [ ] Add navigation handler in `MainScreen.kt`
- [ ] Update `MainScreen` composable to handle Prashna navigation
- [ ] Add Prashna to feature list

### Phase 5: ViewModel & State Management
- [ ] Create `PrashnaViewModel.kt` (or extend `ChartViewModel`)
- [ ] Add Prashna calculation state management
- [ ] Add question history state
- [ ] Add caching for calculations

### Phase 6: Testing & Polish
- [ ] Test calculation accuracy
- [ ] Test UI responsiveness
- [ ] Add error handling
- [ ] Add loading states
- [ ] Polish animations and transitions
- [ ] Add accessibility labels

---

## 16. FILE LOCATIONS REFERENCE

### Critical Files for Prashna Implementation
```
Key Files to Reference:
├── Data Models
│   └── app/src/main/java/com/astro/storm/data/model/BirthData.kt
│   └── app/src/main/java/com/astro/storm/data/model/VedicChart.kt
│   └── app/src/main/java/com/astro/storm/data/model/Planet.kt
│
├── Calculations (Reference these patterns)
│   └── app/src/main/java/com/astro/storm/ephemeris/SwissEphemerisEngine.kt
│   └── app/src/main/java/com/astro/storm/ephemeris/PanchangaCalculator.kt
│   └── app/src/main/java/com/astro/storm/ephemeris/DashaCalculator.kt
│
├── UI Screens (Reference these implementations)
│   └── app/src/main/java/com/astro/storm/ui/screen/MuhurtaScreen.kt
│   └── app/src/main/java/com/astro/storm/ui/screen/main/HomeTab.kt
│   └── app/src/main/java/com/astro/storm/ui/screen/ChartInputScreen.kt
│
├── Navigation
│   └── app/src/main/java/com/astro/storm/ui/navigation/Navigation.kt
│   └── app/src/main/java/com/astro/storm/ui/screen/main/MainScreen.kt
│
├── ViewModels
│   └── app/src/main/java/com/astro/storm/ui/viewmodel/ChartViewModel.kt
│
├── Theme
│   └── app/src/main/java/com/astro/storm/ui/theme/AppTheme.kt
│   └── app/src/main/java/com/astro/storm/ui/theme/Color.kt
│
├── Database
│   └── app/src/main/java/com/astro/storm/data/local/ChartEntity.kt
│   └── app/src/main/java/com/astro/storm/data/local/ChartRepository.kt
│
└── WHERE TO ADD PRASHNA
    ├── NEW: app/src/main/java/com/astro/storm/data/model/PrashnaChart.kt
    ├── NEW: app/src/main/java/com/astro/storm/ephemeris/PrashnaCalculator.kt
    ├── NEW: app/src/main/java/com/astro/storm/ui/screen/PrashnaScreen.kt
    ├── UPDATE: app/src/main/java/com/astro/storm/ui/navigation/Navigation.kt
    ├── UPDATE: app/src/main/java/com/astro/storm/ui/screen/main/HomeTab.kt
    └── UPDATE: app/src/main/java/com/astro/storm/ui/screen/main/MainScreen.kt
```

---

## 17. PRASHNA-SPECIFIC ASTROLOGICAL CONCEPTS

### Prashna Rules (for reference)
1. **Question Time = Chart Time**: Use exact moment question is asked
2. **Lagna Significator**: Determine significator for question type
3. **Houses**:
   - House 1 (Lagna): Querent
   - House 7 (Descendant): Quesited
   - House 10: Outcome/Result
   - House 11: Fulfillment
4. **Planets**:
   - Significators based on question type
   - Aspect relationships indicate answers
   - Retrograde planets = delays
5. **Nakshatras**: Additional confirmation
6. **Moon Position**: Critical for timing questions

### Categories Supported
- **Yes/No**: Binary outcomes
- **Timing**: When will something happen
- **Identification**: Who/What
- **Outcome**: Prediction of result
- **General**: Any question

---

## CONCLUSION

The AstroStorm codebase is well-structured, follows best practices, and provides all the infrastructure needed to implement Prashna seamlessly:

1. **Calculation Engine**: Swiss Ephemeris is production-ready
2. **UI Framework**: Jetpack Compose with consistent styling
3. **Navigation**: Type-safe routing in place
4. **State Management**: MVVM with Flow/StateFlow
5. **Database**: Room with proper serialization
6. **Component Library**: Rich set of reusable components
7. **Design System**: Unified theming throughout

**Estimated Implementation Time**: 5-7 days for a complete, production-grade Prashna feature following this codebase's patterns.

