package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Standard WMO Weather Conditions
 */
enum class WeatherCondition(
    val title: String,
    val description: String,
    val iconName: String,
    val isRainy: Boolean = false,
    val isSnowy: Boolean = false,
    val isThunder: Boolean = false,
    val isCloudy: Boolean = false,
    val isClear: Boolean = false,
    val isFoggy: Boolean = false
) {
    CLEAR_SKY("Clear Sky", "Sunny and cloudless", "wb_sunny", isClear = true),
    MAINLY_CLEAR("Mainly Clear", "Mostly sunny", "wb_sunny", isClear = true),
    PARTLY_CLOUDY("Partly Cloudy", "Scattered clouds", "partly_cloudy_day", isCloudy = true),
    OVERCAST("Overcast", "Dense cloud cover", "cloud", isCloudy = true),
    FOG("Fog", "Misty & reduced visibility", "foggy", isFoggy = true),
    DEPOSITING_RIME_FOG("Rime Fog", "Freezing fog", "foggy", isFoggy = true),
    LIGHT_DRIZZLE("Light Drizzle", "Slight fine drizzle", "grain", isRainy = true),
    MODERATE_DRIZZLE("Drizzle", "Steady drizzle", "grain", isRainy = true),
    DENSE_DRIZZLE("Heavy Drizzle", "Dense drizzle", "grain", isRainy = true),
    LIGHT_RAIN("Light Rain", "Gentle rain showers", "rainy", isRainy = true),
    MODERATE_RAIN("Moderate Rain", "Steady rainfall", "rainy", isRainy = true),
    HEAVY_RAIN("Heavy Rain", "Intense downpour", "rainy", isRainy = true),
    FREEZING_RAIN("Freezing Rain", "Icy precipitation", "ac_unit", isRainy = true, isSnowy = true),
    LIGHT_SNOW("Light Snow", "Gentle snow flurries", "ac_unit", isSnowy = true),
    MODERATE_SNOW("Moderate Snow", "Steady snowfall", "ac_unit", isSnowy = true),
    HEAVY_SNOW("Heavy Snow", "Blizzard-like snow", "ac_unit", isSnowy = true),
    SNOW_GRAINS("Snow Grains", "Fine frozen grains", "ac_unit", isSnowy = true),
    RAIN_SHOWERS("Rain Showers", "Passing showers", "water_drop", isRainy = true),
    HEAVY_RAIN_SHOWERS("Violent Showers", "Heavy scattered rain", "water_drop", isRainy = true),
    SNOW_SHOWERS("Snow Showers", "Intermittent snowfall", "ac_unit", isSnowy = true),
    THUNDERSTORM("Thunderstorm", "Thunder & lightning", "thunderstorm", isThunder = true, isRainy = true),
    THUNDERSTORM_HAIL("Severe Thunderstorm", "Hail and thunder", "thunderstorm", isThunder = true, isRainy = true);

    companion object {
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR_SKY
            1 -> MAINLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45 -> FOG
            48 -> DEPOSITING_RIME_FOG
            51 -> LIGHT_DRIZZLE
            53 -> MODERATE_DRIZZLE
            55 -> DENSE_DRIZZLE
            56, 57 -> FREEZING_RAIN
            61 -> LIGHT_RAIN
            63 -> MODERATE_RAIN
            65 -> HEAVY_RAIN
            66, 67 -> FREEZING_RAIN
            71 -> LIGHT_SNOW
            73 -> MODERATE_SNOW
            75 -> HEAVY_SNOW
            77 -> SNOW_GRAINS
            80, 81 -> RAIN_SHOWERS
            82 -> HEAVY_RAIN_SHOWERS
            85, 86 -> SNOW_SHOWERS
            95 -> THUNDERSTORM
            96, 99 -> THUNDERSTORM_HAIL
            else -> if (code in 50..69) MODERATE_RAIN else if (code in 70..79) MODERATE_SNOW else CLEAR_SKY
        }
    }
}

/**
 * Open-Meteo Weather API Response Data Classes
 */
@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val timezone: String? = null,
    @Json(name = "current") val current: CurrentWeatherDto? = null,
    @Json(name = "hourly") val hourly: HourlyWeatherDto? = null,
    @Json(name = "daily") val daily: DailyWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    val time: String,
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity: Double,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "is_day") val isDay: Int,
    @Json(name = "precipitation") val precipitation: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "cloud_cover") val cloudCover: Double? = null,
    @Json(name = "pressure_msl") val pressureMsl: Double? = null,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed: Double,
    @Json(name = "wind_direction_10m") val windDirection: Double,
    @Json(name = "uv_index") val uvIndex: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperature: List<Double>,
    @Json(name = "relative_humidity_2m") val relativeHumidity: List<Double>? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>? = null,
    @Json(name = "precipitation") val precipitation: List<Double>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "wind_speed_10m") val windSpeed: List<Double>? = null,
    @Json(name = "uv_index") val uvIndex: List<Double>? = null,
    @Json(name = "is_day") val isDay: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double>,
    @Json(name = "sunrise") val sunrise: List<String>? = null,
    @Json(name = "sunset") val sunset: List<String>? = null,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>? = null,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>? = null,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
    @Json(name = "wind_speed_10m_max") val windSpeedMax: List<Double>? = null
)

/**
 * Domain Models for UI Layer
 */
data class LocationInfo(
    val name: String,
    val country: String,
    val admin1: String? = null,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String? = null,
    val timezone: String? = null
) {
    val displayTitle: String get() = name
    val displaySubtitle: String get() = listOfNotNull(admin1, country).joinToString(", ")
    val fullLocationQuery: String get() = "$name, ${country}"
}

data class HourlyForecast(
    val timeFormatted: String,
    val timestamp: String,
    val temperature: Double,
    val weatherCondition: WeatherCondition,
    val precipitationProbability: Int,
    val isDay: Boolean,
    val windSpeed: Double
)

data class DailyForecast(
    val date: String,
    val dayOfWeek: String,
    val minTemp: Double,
    val maxTemp: Double,
    val weatherCondition: WeatherCondition,
    val precipitationChance: Int,
    val sunriseTime: String,
    val sunsetTime: String,
    val uvIndexMax: Double
)

data class CurrentWeather(
    val location: LocationInfo,
    val temperature: Double,
    val feelsLike: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val humidity: Double,
    val windSpeed: Double,
    val windDirection: Double,
    val pressureHpa: Double,
    val uvIndex: Double,
    val cloudCover: Double,
    val precipitationMm: Double,
    val sunrise: String,
    val sunset: String,
    val minTempToday: Double,
    val maxTempToday: Double,
    val hourlyList: List<HourlyForecast>,
    val dailyList: List<DailyForecast>,
    val timestamp: Long = System.currentTimeMillis()
)
