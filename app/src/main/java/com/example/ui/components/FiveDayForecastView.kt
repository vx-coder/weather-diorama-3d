package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecast

/**
 * Formats temperature in Celsius and Fahrenheit side-by-side or based on preference
 */
fun formatBothUnits(celsius: Double): String {
    val c = celsius.toInt()
    val f = ((celsius * 9.0 / 5.0) + 32.0).toInt()
    return "$c°C / $f°F"
}

fun formatTempC(celsius: Double): String = "${celsius.toInt()}°C"
fun formatTempF(celsius: Double): String = "${((celsius * 9.0 / 5.0) + 32.0).toInt()}°F"

/**
 * Full Dedicated 5-Day Weather Forecast Component
 */
@Composable
fun FiveDayForecastView(
    dailyList: List<DailyForecast>,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier,
    cityName: String = "Current Location"
) {
    // Take the 5-day forecast slice
    val fiveDays = dailyList.take(5)
    var expandedDayIndex by remember { mutableStateOf<Int?>(0) }

    val overallMin = fiveDays.minOfOrNull { it.minTemp } ?: 0.0
    val overallMax = fiveDays.maxOfOrNull { it.maxTemp } ?: 35.0
    val tempRange = (overallMax - overallMin).coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("five_day_forecast_view")
    ) {
        // Header Section
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "5-Day Weather Forecast",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Outlook for $cityName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = "5 Days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of 5 Forecast Day Cards
        fiveDays.forEachIndexed { index, day ->
            val isExpanded = expandedDayIndex == index

            FiveDayForecastCard(
                day = day,
                isFahrenheit = isFahrenheit,
                isExpanded = isExpanded,
                overallMin = overallMin,
                tempRange = tempRange,
                onToggleExpand = {
                    expandedDayIndex = if (isExpanded) null else index
                },
                modifier = Modifier.testTag("forecast_day_card_$index")
            )

            if (index < fiveDays.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun FiveDayForecastCard(
    day: DailyForecast,
    isFahrenheit: Boolean,
    isExpanded: Boolean,
    overallMin: Double,
    tempRange: Double,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isExpanded) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main Summary Row for the Day
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Day of Week & Date
                Column(modifier = Modifier.width(90.dp)) {
                    Text(
                        text = day.dayOfWeek,
                        fontWeight = if (day.dayOfWeek == "Today") FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (day.dayOfWeek == "Today") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = day.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Weather Icon + Condition Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getWeatherIcon(day.weatherCondition),
                                contentDescription = day.weatherCondition.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = day.weatherCondition.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Chance of Precipitation badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Precipitation Chance",
                                tint = if (day.precipitationChance > 20) Color(0xFF38BDF8) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${day.precipitationChance}% rain",
                                fontSize = 11.sp,
                                fontWeight = if (day.precipitationChance > 40) FontWeight.Bold else FontWeight.Normal,
                                color = if (day.precipitationChance > 40) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Predicted High & Low Temperatures
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "High Temp",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isFahrenheit) formatTempF(day.maxTemp) else formatTempC(day.maxTemp),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Low Temp",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isFahrenheit) formatTempF(day.minTemp) else formatTempC(day.minTemp),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Secondary units in brackets for complete clarity (both C and F)
                    Text(
                        text = if (isFahrenheit)
                            "${day.maxTemp.toInt()}° / ${day.minTemp.toInt()}°C"
                        else
                            "${((day.maxTemp * 9.0 / 5.0) + 32.0).toInt()}° / ${((day.minTemp * 9.0 / 5.0) + 32.0).toInt()}°F",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Expand / Collapse Chevron
                IconButtonChevron(isExpanded = isExpanded)
            }

            // Visual Temperature Gradient Bar
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${day.minTemp.toInt()}°",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val startRatio = ((day.minTemp - overallMin) / tempRange).toFloat().coerceIn(0f, 0.9f)
                    val endRatio = ((day.maxTemp - overallMin) / tempRange).toFloat().coerceIn(0.1f, 1f)
                    val spanRatio = (endRatio - startRatio).coerceAtLeast(0.1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(spanRatio)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFFEF4444))
                                )
                            )
                    )
                }
                Text(
                    text = "${day.maxTemp.toInt()}°",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(24.dp)
                )
            }

            // Expandable Detailed Breakdown for this Day
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Daily Atmosphere & Environmental Summary",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Detail 1: Dual Temp
                                DayDetailStat(
                                    icon = Icons.Default.Thermostat,
                                    label = "High / Low",
                                    value = formatBothUnits(day.maxTemp),
                                    subValue = "Min: ${formatBothUnits(day.minTemp)}"
                                )
                                // Detail 2: Precipitation
                                DayDetailStat(
                                    icon = Icons.Default.WaterDrop,
                                    label = "Precipitation",
                                    value = "${day.precipitationChance}% Chance",
                                    subValue = if (day.precipitationChance > 50) "Expect rainfall" else "Low rain risk"
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Detail 3: UV Index
                                DayDetailStat(
                                    icon = Icons.Default.WbSunny,
                                    label = "Max UV Index",
                                    value = String.format("%.1f", day.uvIndexMax),
                                    subValue = if (day.uvIndexMax > 6) "High sun protection" else "Moderate sun"
                                )
                                // Detail 4: Sun Times
                                DayDetailStat(
                                    icon = Icons.Default.WbTwilight,
                                    label = "Sunrise / Sunset",
                                    value = day.sunriseTime,
                                    subValue = "Set: ${day.sunsetTime}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconButtonChevron(isExpanded: Boolean) {
    Icon(
        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
        contentDescription = if (isExpanded) "Collapse" else "Expand",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(20.dp)
            .padding(start = 4.dp)
    )
}

@Composable
private fun DayDetailStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.width(150.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subValue,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
