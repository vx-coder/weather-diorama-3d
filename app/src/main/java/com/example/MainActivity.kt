package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.LocationInfo
import com.example.ui.components.AiAdvisoryCard
import com.example.ui.components.CitySearchSheet
import com.example.ui.components.DailyForecastSection
import com.example.ui.components.DioramaViewport
import com.example.ui.components.FiveDayForecastView
import com.example.ui.components.HourlyForecastStrip
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SunMoonTracker
import com.example.ui.components.WeatherMetricsGrid
import com.example.ui.components.WeatherSummaryHero
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeatherViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                val savedLocations by viewModel.savedLocations.collectAsState()

                var showSearchSheet by remember { mutableStateOf(false) }
                var showSettingsDialog by remember { mutableStateOf(false) }

                val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val pullRefreshState = rememberPullToRefreshState()
                val coroutineScope = rememberCoroutineScope()
                val pagerState = rememberPagerState(pageCount = { 3 })

                val context = LocalContext.current
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    ) {
                        fetchDeviceLocation()
                    }
                }

                Scaffold(
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Diorama Weather",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "3D Miniature & 5-Day Forecast",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(1)
                                            }
                                        },
                                        modifier = Modifier.testTag("open_5day_forecast_topbar_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "5-Day Forecast",
                                            tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    IconButton(
                                        onClick = { showSearchSheet = true },
                                        modifier = Modifier.testTag("open_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search Location"
                                        )
                                    }
                                    IconButton(
                                        onClick = { showSettingsDialog = true },
                                        modifier = Modifier.testTag("open_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )

                            // Navigation Tabs with swipe & tap support
                            PrimaryTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("main_navigation_tabs")
                            ) {
                                Tab(
                                    selected = pagerState.currentPage == 0,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    text = {
                                        Text(
                                            text = "Overview",
                                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 1,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    text = {
                                        Text(
                                            text = "5-Day Forecast",
                                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                Tab(
                                    selected = pagerState.currentPage == 2,
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                    },
                                    text = {
                                        Text(
                                            text = "Telemetry",
                                            fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Air,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        PullToRefreshBox(
                            isRefreshing = uiState.isLoadingWeather,
                            onRefresh = { viewModel.refreshCurrentWeather() },
                            state = pullRefreshState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.currentWeather != null) {
                                val weather = uiState.currentWeather!!

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("weather_horizontal_pager")
                                ) { page ->
                                    when (page) {
                                        0 -> {
                                            // Page 0: Overview & Live 3D Miniature Landmark Diorama
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("weather_overview_list"),
                                                contentPadding = PaddingValues(bottom = 32.dp)
                                            ) {
                                                // 1. 3D Miniature Landmark Diorama Viewport (Hero)
                                                item {
                                                    DioramaViewport(
                                                        weather = weather,
                                                        dioramaState = uiState.dioramaState,
                                                        selectedStyle = uiState.selectedStyle,
                                                        onStyleSelected = { viewModel.setStyle(it) },
                                                        onGenerateDiorama = { viewModel.generateDiorama(it) }
                                                    )
                                                }

                                                // 2. Weather Summary Card with augmented telemetry & dual temps
                                                item {
                                                    WeatherSummaryHero(
                                                        weather = weather,
                                                        isFahrenheit = uiState.isFahrenheit,
                                                        onOpenSearch = { showSearchSheet = true },
                                                        onOpen5DayForecast = {
                                                            coroutineScope.launch {
                                                                pagerState.animateScrollToPage(1)
                                                            }
                                                        }
                                                    )
                                                }

                                                // 3. Hourly Forecast Timeline
                                                item {
                                                    HourlyForecastStrip(
                                                        hourlyList = weather.hourlyList,
                                                        isFahrenheit = uiState.isFahrenheit
                                                    )
                                                }

                                                // 4. Gemini AI Location & Architectural Briefing
                                                item {
                                                    AiAdvisoryCard(advisory = uiState.aiAdvisory)
                                                }

                                                // 5. Daily 5-Day Forecast Section
                                                item {
                                                    DailyForecastSection(
                                                        dailyList = weather.dailyList,
                                                        isFahrenheit = uiState.isFahrenheit,
                                                        onViewFullForecast = {
                                                            coroutineScope.launch {
                                                                pagerState.animateScrollToPage(1)
                                                            }
                                                        }
                                                    )
                                                }

                                                // 6. Atmospheric Metrics 2x2 Grid (UV, Wind, Humidity, Pressure)
                                                item {
                                                    WeatherMetricsGrid(
                                                        weather = weather,
                                                        isMph = uiState.isMph
                                                    )
                                                }

                                                // 7. Sun & Solar Arc Tracker
                                                item {
                                                    SunMoonTracker(
                                                        sunrise = weather.sunrise,
                                                        sunset = weather.sunset,
                                                        isDay = weather.isDay
                                                    )
                                                }
                                            }
                                        }

                                        1 -> {
                                            // Page 1: Dedicated 5-Day Weather Forecast Screen
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("dedicated_5day_forecast_list"),
                                                contentPadding = PaddingValues(bottom = 32.dp)
                                            ) {
                                                item {
                                                    FiveDayForecastView(
                                                        dailyList = weather.dailyList,
                                                        isFahrenheit = uiState.isFahrenheit,
                                                        cityName = weather.location.name
                                                    )
                                                }

                                                // AI Advisory for the week
                                                item {
                                                    AiAdvisoryCard(advisory = uiState.aiAdvisory)
                                                }
                                            }
                                        }

                                        2 -> {
                                            // Page 2: Atmospheric Telemetry & Solar Arc Tracker
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("telemetry_screen_list"),
                                                contentPadding = PaddingValues(bottom = 32.dp)
                                            ) {
                                                item {
                                                    WeatherMetricsGrid(
                                                        weather = weather,
                                                        isMph = uiState.isMph
                                                    )
                                                }

                                                item {
                                                    SunMoonTracker(
                                                        sunrise = weather.sunrise,
                                                        sunset = weather.sunset,
                                                        isDay = weather.isDay
                                                    )
                                                }

                                                item {
                                                    HourlyForecastStrip(
                                                        hourlyList = weather.hourlyList,
                                                        isFahrenheit = uiState.isFahrenheit
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (uiState.isLoadingWeather) {
                                // Loading Screen
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Crafting 3D Miniature Atmosphere & 5-Day Outlook...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else if (uiState.weatherErrorMessage != null) {
                                // Error State
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = uiState.weatherErrorMessage ?: "Failed to load weather",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = { viewModel.refreshCurrentWeather() }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // City Search Bottom Sheet
                if (showSearchSheet) {
                    CitySearchSheet(
                        sheetState = searchSheetState,
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        savedLocations = savedLocations,
                        onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        onLocationSelected = { loc ->
                            viewModel.loadWeatherForLocation(loc)
                        },
                        onCurrentLocationClicked = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                fetchDeviceLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onDismiss = { showSearchSheet = false }
                    )
                }

                // Settings Dialog
                if (showSettingsDialog) {
                    SettingsDialog(
                        isFahrenheit = uiState.isFahrenheit,
                        isMph = uiState.isMph,
                        onToggleFahrenheit = { viewModel.setFahrenheit(it) },
                        onToggleMph = { viewModel.setMph(it) },
                        onDismiss = { showSettingsDialog = false }
                    )
                }
            }
        }
    }

    private fun fetchDeviceLocation() {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val lat = loc.latitude
                    val lon = loc.longitude
                    val geocoder = Geocoder(this, Locale.getDefault())

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            geocoder.getFromLocation(lat, lon, 1) { addresses ->
                                var cityName = "Current Location"
                                var countryName = ""
                                var admin = ""
                                var countryCode = ""

                                if (!addresses.isNullOrEmpty()) {
                                    val addr = addresses[0]
                                    cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                                    countryName = addr.countryName ?: ""
                                    admin = addr.adminArea ?: ""
                                    countryCode = addr.countryCode ?: ""
                                }

                                runOnUiThread {
                                    viewModel.loadWeatherForLocation(
                                        LocationInfo(
                                            name = cityName,
                                            country = countryName,
                                            admin1 = admin,
                                            latitude = lat,
                                            longitude = lon,
                                            countryCode = countryCode
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            viewModel.loadWeatherForLocation(
                                LocationInfo(
                                    name = "Current Location",
                                    country = "",
                                    admin1 = "",
                                    latitude = lat,
                                    longitude = lon,
                                    countryCode = ""
                                )
                            )
                        }
                    } else {
                        Thread {
                            var cityName = "Current Location"
                            var countryName = ""
                            var admin = ""
                            var countryCode = ""

                            try {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(lat, lon, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val addr = addresses[0]
                                    cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "My Location"
                                    countryName = addr.countryName ?: ""
                                    admin = addr.adminArea ?: ""
                                    countryCode = addr.countryCode ?: ""
                                }
                            } catch (e: Exception) {
                                // Fallback to coordinates
                            }

                            runOnUiThread {
                                viewModel.loadWeatherForLocation(
                                    LocationInfo(
                                        name = cityName,
                                        country = countryName,
                                        admin1 = admin,
                                        latitude = lat,
                                        longitude = lon,
                                        countryCode = countryCode
                                    )
                                )
                            }
                        }.start()
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
