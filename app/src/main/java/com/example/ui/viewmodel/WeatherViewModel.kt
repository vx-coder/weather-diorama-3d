package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SavedLocationEntity
import com.example.data.model.AiLocationAdvisory
import com.example.data.model.CurrentWeather
import com.example.data.model.DioramaState
import com.example.data.model.DioramaStyle
import com.example.data.model.LocationInfo
import com.example.data.repository.DioramaRepository
import com.example.data.repository.WeatherRepository
import com.example.ui.components.FamousPresetCities
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeatherUiState(
    val currentWeather: CurrentWeather? = null,
    val isLoadingWeather: Boolean = false,
    val weatherErrorMessage: String? = null,
    val dioramaState: DioramaState = DioramaState.Initial,
    val selectedStyle: DioramaStyle = DioramaStyle.TILT_SHIFT_ISOMETRIC,
    val aiAdvisory: AiLocationAdvisory? = null,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<LocationInfo> = emptyList(),
    val isFahrenheit: Boolean = false,
    val isMph: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val weatherRepository = WeatherRepository(dao = db.weatherDao())
    private val dioramaRepository = DioramaRepository()

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    val savedLocations: StateFlow<List<SavedLocationEntity>> = weatherRepository.savedLocations
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var searchJob: Job? = null
    private var dioramaJob: Job? = null

    init {
        // Default initial city: Tokyo or Paris
        val defaultCity = FamousPresetCities.firstOrNull { it.name == "Tokyo" } ?: FamousPresetCities[0]
        loadWeatherForLocation(defaultCity)
    }

    fun loadWeatherForLocation(location: LocationInfo) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingWeather = true,
                weatherErrorMessage = null
            )

            // Check if cached diorama exists for this city
            val locId = "${location.name}_${location.country}"
            val cachedLoc = weatherRepository.getCachedLocation(locId)
            if (cachedLoc?.cachedDioramaBase64 != null) {
                val cachedBmp = dioramaRepository.base64ToBitmap(cachedLoc.cachedDioramaBase64)
                if (cachedBmp != null) {
                    val style = DioramaStyle.entries.find { it.name == cachedLoc.cachedDioramaStyle }
                        ?: _uiState.value.selectedStyle
                    _uiState.value = _uiState.value.copy(
                        dioramaState = DioramaState.Success(
                            bitmap = cachedBmp,
                            promptUsed = cachedLoc.cachedDioramaPrompt ?: "",
                            style = style
                        ),
                        selectedStyle = style
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(dioramaState = DioramaState.Initial)
            }

            val result = weatherRepository.getWeatherData(location)
            result.onSuccess { weather ->
                _uiState.value = _uiState.value.copy(
                    currentWeather = weather,
                    isLoadingWeather = false,
                    weatherErrorMessage = null
                )

                // Load AI Location advisory in background
                fetchAiAdvisory(weather)

                // If no diorama yet, trigger generation with Gemini
                if (_uiState.value.dioramaState !is DioramaState.Success) {
                    generateDiorama(_uiState.value.selectedStyle)
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingWeather = false,
                    weatherErrorMessage = error.message ?: "Failed to load weather forecast"
                )
            }
        }
    }

    fun generateDiorama(style: DioramaStyle) {
        val weather = _uiState.value.currentWeather ?: return
        dioramaJob?.cancel()

        dioramaJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedStyle = style,
                dioramaState = DioramaState.Generating(promptUsed = "Generating 3D Miniature...", style = style)
            )

            val result = dioramaRepository.generateDioramaImage(
                location = weather.location,
                condition = weather.condition,
                isDay = weather.isDay,
                style = style
            )

            result.onSuccess { (bitmap, prompt) ->
                _uiState.value = _uiState.value.copy(
                    dioramaState = DioramaState.Success(
                        bitmap = bitmap,
                        promptUsed = prompt,
                        style = style
                    )
                )
                // Cache to database
                val base64 = dioramaRepository.bitmapToBase64(bitmap)
                val locId = "${weather.location.name}_${weather.location.country}"
                weatherRepository.saveDioramaCache(locId, base64, prompt, style.name)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    dioramaState = DioramaState.Error(
                        message = error.message ?: "Could not generate diorama with Gemini"
                    )
                )
            }
        }
    }

    private fun fetchAiAdvisory(weather: CurrentWeather) {
        viewModelScope.launch {
            val result = dioramaRepository.generateLocationAdvisory(
                location = weather.location,
                condition = weather.condition,
                tempCelsius = weather.temperature,
                isDay = weather.isDay
            )
            result.onSuccess { adv ->
                _uiState.value = _uiState.value.copy(aiAdvisory = adv)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            delay(350) // Debounce typing
            val results = weatherRepository.searchCities(query)
            _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun setStyle(style: DioramaStyle) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun setFahrenheit(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isFahrenheit = enabled)
    }

    fun setMph(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isMph = enabled)
    }

    fun refreshCurrentWeather() {
        val loc = _uiState.value.currentWeather?.location
        if (loc != null) {
            loadWeatherForLocation(loc)
        }
    }
}
