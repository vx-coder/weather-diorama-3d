package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeather
import kotlin.math.ln

@Composable
fun WeatherMetricsGrid(
    weather: CurrentWeather,
    isMph: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("weather_metrics_grid")
    ) {
        Text(
            text = "Atmospheric Conditions & Telemetry",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // 2x2 Metric Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: UV Index Gauge
            UvIndexCard(uvIndex = weather.uvIndex, modifier = Modifier.weight(1f))
            // Card 2: Wind Speed & Direction + Compass
            WindCompassCard(
                speedKmh = weather.windSpeed,
                directionDeg = weather.windDirection,
                isMph = isMph,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: Humidity & Precipitation Details
            HumidityMetricCard(
                humidity = weather.humidity,
                temperatureC = weather.temperature,
                precipitationMm = weather.precipitationMm,
                modifier = Modifier.weight(1f)
            )
            // Card 4: Pressure & Cloud Cover
            MetricDetailCard(
                icon = Icons.Default.Compress,
                iconTint = Color(0xFF8B5CF6),
                title = "PRESSURE",
                value = "${weather.pressureHpa.toInt()} hPa",
                subtitle = "Cloud cover: ${weather.cloudCover.toInt()}%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UvIndexCard(
    uvIndex: Double,
    modifier: Modifier = Modifier
) {
    val uvCategory = when {
        uvIndex < 3.0 -> "Low"
        uvIndex < 6.0 -> "Moderate"
        uvIndex < 8.0 -> "High"
        uvIndex < 11.0 -> "Very High"
        else -> "Extreme"
    }
    val uvColor = when {
        uvIndex < 3.0 -> Color(0xFF10B981)
        uvIndex < 6.0 -> Color(0xFFF59E0B)
        uvIndex < 8.0 -> Color(0xFFF97316)
        uvIndex < 11.0 -> Color(0xFFEF4444)
        else -> Color(0xFFA855F7)
    }

    Surface(
        modifier = modifier.height(138.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = uvColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "UV INDEX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                Text(
                    text = String.format("%.1f", uvIndex),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = uvCategory,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = uvColor
                )
            }

            // Horizontal gauge
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                val w = size.width
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, 2f),
                    end = Offset(w, 2f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                val progress = (uvIndex.toFloat() / 11f).coerceIn(0.05f, 1f)
                drawLine(
                    color = uvColor,
                    start = Offset(0f, 2f),
                    end = Offset(w * progress, 2f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun WindCompassCard(
    speedKmh: Double,
    directionDeg: Double,
    isMph: Boolean,
    modifier: Modifier = Modifier
) {
    val speedMph = speedKmh * 0.621371
    val cardinal = getWindDirectionCardinal(directionDeg)
    val animatedRotation by animateFloatAsState(targetValue = directionDeg.toFloat(), label = "compass_rot")

    Surface(
        modifier = modifier.height(138.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "WIND & DIRECTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isMph) "${speedMph.toInt()} mph" else "${speedKmh.toInt()} km/h",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isMph) "${speedKmh.toInt()} km/h" else "${speedMph.toInt()} mph",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini Compass dial
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Wind Direction",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(animatedRotation)
                    )
                }
            }

            Text(
                text = "$cardinal (${directionDeg.toInt()}° heading)",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HumidityMetricCard(
    humidity: Double,
    temperatureC: Double,
    precipitationMm: Double,
    modifier: Modifier = Modifier
) {
    val comfortLevel = when {
        humidity < 30.0 -> "Dry Air"
        humidity <= 60.0 -> "Comfortable"
        humidity <= 80.0 -> "Humid"
        else -> "Very High Humidity"
    }

    // Simple dew point approximation Magnus formula
    val a = 17.27
    val b = 237.7
    val alpha = ((a * temperatureC) / (b + temperatureC)) + ln(humidity.coerceIn(1.0, 100.0) / 100.0)
    val dewPointC = ((b * alpha) / (a - alpha)).toInt()

    Surface(
        modifier = modifier.height(138.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HUMIDITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                Text(
                    text = "${humidity.toInt()}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = comfortLevel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )
            }

            Text(
                text = "Dew point ~${dewPointC}°C • ${precipitationMm}mm",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricDetailCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(138.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
