package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecast

@Composable
fun DailyForecastSection(
    dailyList: List<DailyForecast>,
    isFahrenheit: Boolean,
    onViewFullForecast: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Show 5-day forecast
    val displayDays = dailyList.take(5)
    val overallMin = displayDays.minOfOrNull { it.minTemp } ?: 0.0
    val overallMax = displayDays.maxOfOrNull { it.maxTemp } ?: 30.0
    val tempRange = (overallMax - overallMin).coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("daily_forecast_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "5-Day Weather Forecast",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (onViewFullForecast != null) {
                TextButton(
                    onClick = onViewFullForecast,
                    modifier = Modifier.testTag("expand_forecast_button")
                ) {
                    Text(
                        text = "Full Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                displayDays.forEachIndexed { index, day ->
                    DailyRowItem(
                        day = day,
                        isFahrenheit = isFahrenheit,
                        overallMin = overallMin,
                        tempRange = tempRange
                    )
                    if (index < displayDays.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRowItem(
    day: DailyForecast,
    isFahrenheit: Boolean,
    overallMin: Double,
    tempRange: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Weekday Name
        Column(modifier = Modifier.width(65.dp)) {
            Text(
                text = day.dayOfWeek,
                fontSize = 14.sp,
                fontWeight = if (day.dayOfWeek == "Today") FontWeight.Bold else FontWeight.Medium,
                color = if (day.dayOfWeek == "Today") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = day.weatherCondition.title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Weather Icon + Precipitation Chance Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(75.dp)
        ) {
            Icon(
                imageVector = getWeatherIcon(day.weatherCondition),
                contentDescription = day.weatherCondition.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Chance of rain",
                    tint = if (day.precipitationChance > 20) Color(0xFF38BDF8) else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "${day.precipitationChance}%",
                    fontSize = 11.sp,
                    fontWeight = if (day.precipitationChance > 30) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.precipitationChance > 30) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Predicted Low Temp Label
        Text(
            text = formatTemp(day.minTemp, isFahrenheit),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(34.dp)
        )

        // Dynamic Temperature Gradient Range Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
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

        // Predicted High Temp Label
        Text(
            text = formatTemp(day.maxTemp, isFahrenheit),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(34.dp)
        )
    }
}
