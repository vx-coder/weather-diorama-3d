package com.example.data.repository

import com.example.data.local.SavedLocationEntity
import com.example.data.local.WeatherDao
import com.example.data.model.CurrentWeather
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyForecast
import com.example.data.model.LocationInfo
import com.example.data.model.WeatherCondition
import com.example.data.network.NetworkClient
import com.example.data.network.OpenMeteoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WeatherRepository(
    private val api: OpenMeteoService = NetworkClient.openMeteoService,
    private val dao: WeatherDao
) {
    val savedLocations: Flow<List<SavedLocationEntity>> = dao.getAllSavedLocations()

    suspend fun getWeatherData(location: LocationInfo): Result<CurrentWeather> = withContext(Dispatchers.IO) {
        try {
            val response = api.getForecast(location.latitude, location.longitude)
            val currentDto = response.current ?: throw IllegalStateException("Current weather data unavailable")
            val hourlyDto = response.hourly
            val dailyDto = response.daily

            val condition = WeatherCondition.fromWmoCode(currentDto.weatherCode)
            val isDay = currentDto.isDay == 1

            // Parse Hourly Forecast (take next 24 hours)
            val hourlyList = mutableListOf<HourlyForecast>()
            if (hourlyDto != null && hourlyDto.time.isNotEmpty()) {
                val size = minOf(hourlyDto.time.size, 24)
                for (i in 0 until size) {
                    val rawTime = hourlyDto.time[i]
                    val formattedTime = formatHourlyTime(rawTime)
                    val temp = hourlyDto.temperature.getOrNull(i) ?: 0.0
                    val code = hourlyDto.weatherCode.getOrNull(i) ?: 0
                    val rainProb = hourlyDto.precipitationProbability?.getOrNull(i) ?: 0
                    val dayFlag = (hourlyDto.isDay?.getOrNull(i) ?: 1) == 1
                    val wind = hourlyDto.windSpeed?.getOrNull(i) ?: 0.0

                    hourlyList.add(
                        HourlyForecast(
                            timeFormatted = formattedTime,
                            timestamp = rawTime,
                            temperature = temp,
                            weatherCondition = WeatherCondition.fromWmoCode(code),
                            precipitationProbability = rainProb,
                            isDay = dayFlag,
                            windSpeed = wind
                        )
                    )
                }
            }

            // Parse Daily Forecast (7 days)
            val dailyList = mutableListOf<DailyForecast>()
            if (dailyDto != null && dailyDto.time.isNotEmpty()) {
                val size = minOf(dailyDto.time.size, 7)
                for (i in 0 until size) {
                    val rawDate = dailyDto.time[i]
                    val dayOfWeek = formatDayOfWeek(rawDate, i == 0)
                    val minT = dailyDto.temperatureMin.getOrNull(i) ?: 0.0
                    val maxT = dailyDto.temperatureMax.getOrNull(i) ?: 0.0
                    val code = dailyDto.weatherCode.getOrNull(i) ?: 0
                    val rainChance = dailyDto.precipitationProbabilityMax?.getOrNull(i) ?: 0
                    val sunriseRaw = dailyDto.sunrise?.getOrNull(i) ?: ""
                    val sunsetRaw = dailyDto.sunset?.getOrNull(i) ?: ""
                    val uvMax = dailyDto.uvIndexMax?.getOrNull(i) ?: 5.0

                    dailyList.add(
                        DailyForecast(
                            date = rawDate,
                            dayOfWeek = dayOfWeek,
                            minTemp = minT,
                            maxTemp = maxT,
                            weatherCondition = WeatherCondition.fromWmoCode(code),
                            precipitationChance = rainChance,
                            sunriseTime = formatSunTime(sunriseRaw),
                            sunsetTime = formatSunTime(sunsetRaw),
                            uvIndexMax = uvMax
                        )
                    )
                }
            }

            val todaySunrise = dailyList.firstOrNull()?.sunriseTime ?: "06:00 AM"
            val todaySunset = dailyList.firstOrNull()?.sunsetTime ?: "07:30 PM"
            val todayMin = dailyList.firstOrNull()?.minTemp ?: (currentDto.temperature - 4)
            val todayMax = dailyList.firstOrNull()?.maxTemp ?: (currentDto.temperature + 4)

            val currentWeather = CurrentWeather(
                location = location,
                temperature = currentDto.temperature,
                feelsLike = currentDto.apparentTemperature,
                condition = condition,
                isDay = isDay,
                humidity = currentDto.relativeHumidity,
                windSpeed = currentDto.windSpeed,
                windDirection = currentDto.windDirection,
                pressureHpa = currentDto.pressureMsl ?: currentDto.surfacePressure ?: 1013.25,
                uvIndex = currentDto.uvIndex ?: 4.5,
                cloudCover = currentDto.cloudCover ?: 20.0,
                precipitationMm = currentDto.precipitation,
                sunrise = todaySunrise,
                sunset = todaySunset,
                minTempToday = todayMin,
                maxTempToday = todayMax,
                hourlyList = hourlyList,
                dailyList = dailyList
            )

            // Update database with latest timestamp
            val locId = "${location.name}_${location.country}"
            dao.insertLocation(
                SavedLocationEntity(
                    id = locId,
                    name = location.name,
                    country = location.country,
                    admin1 = location.admin1,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    countryCode = location.countryCode,
                    lastViewedTimestamp = System.currentTimeMillis()
                )
            )

            Result.success(currentWeather)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCities(query: String): List<LocationInfo> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        try {
            val res = api.searchLocations(query.trim())
            res.results?.map {
                LocationInfo(
                    name = it.name,
                    country = it.country ?: "",
                    admin1 = it.admin1,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    countryCode = it.countryCode,
                    timezone = it.timezone
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCachedLocation(id: String): SavedLocationEntity? = withContext(Dispatchers.IO) {
        dao.getLocationById(id)
    }

    suspend fun saveDioramaCache(id: String, base64: String, prompt: String, style: String) = withContext(Dispatchers.IO) {
        dao.updateDioramaCache(id, base64, prompt, style)
    }

    private fun formatHourlyTime(isoTime: String): String {
        return try {
            // ISO format e.g. "2026-08-14T15:00"
            val parts = isoTime.split("T")
            if (parts.size >= 2) {
                val hourMin = parts[1].split(":")
                val hour = hourMin[0].toIntOrNull() ?: 12
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$displayHour $amPm"
            } else {
                isoTime
            }
        } catch (e: Exception) {
            isoTime
        }
    }

    private fun formatDayOfWeek(isoDate: String, isToday: Boolean): String {
        if (isToday) return "Today"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(isoDate)
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            date?.let { dayFormat.format(it) } ?: isoDate
        } catch (e: Exception) {
            isoDate
        }
    }

    private fun formatSunTime(isoDateTime: String): String {
        return try {
            val parts = isoDateTime.split("T")
            if (parts.size >= 2) {
                val timeParts = parts[1].split(":")
                val hour = timeParts[0].toIntOrNull() ?: 6
                val min = timeParts.getOrNull(1) ?: "00"
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$displayHour:$min $amPm"
            } else {
                isoDateTime
            }
        } catch (e: Exception) {
            isoDateTime
        }
    }
}
