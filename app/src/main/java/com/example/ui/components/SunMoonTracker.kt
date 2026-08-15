package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SunMoonTracker(
    sunrise: String,
    sunset: String,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val arcPath = remember { Path() }
    val arcColors = remember {
        listOf(Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0xFFFB923C))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WbTwilight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sun & Solar Ephemeris Arc",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Arc canvas with cached Path
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    arcPath.reset()
                    arcPath.moveTo(20f, h - 8f)
                    arcPath.quadraticTo(w * 0.5f, -10f, w - 20f, h - 8f)

                    // Horizon line
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(0f, h - 8f),
                        end = Offset(w, h - 8f),
                        strokeWidth = 1.5f
                    )

                    // Arc line
                    drawPath(
                        path = arcPath,
                        brush = Brush.horizontalGradient(arcColors),
                        style = Stroke(width = 2.5f)
                    )

                    // Sun or Moon position along arc
                    val sunRatio = if (isDay) 0.55f else 0.88f
                    val sunX = 20f + (w - 40f) * sunRatio
                    val t = sunRatio
                    val sunY = (1 - t) * (1 - t) * (h - 8f) + 2 * (1 - t) * t * (-10f) + t * t * (h - 8f)

                    if (isDay) {
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = 8f,
                            center = Offset(sunX, sunY)
                        )
                        drawCircle(
                            color = Color(0xFFFBBF24).copy(alpha = 0.4f),
                            radius = 16f,
                            center = Offset(sunX, sunY)
                        )
                    } else {
                        drawCircle(
                            color = Color(0xFF818CF8),
                            radius = 7f,
                            center = Offset(sunX, sunY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sunrise & Sunset Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Sunrise",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = sunrise,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isDay) "Daylight Active" else "Night Sky",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Sunset",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = sunset,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
