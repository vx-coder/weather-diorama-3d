package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CurrentWeather
import com.example.data.model.DioramaState
import com.example.data.model.DioramaStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DioramaViewport(
    weather: CurrentWeather,
    dioramaState: DioramaState,
    selectedStyle: DioramaStyle,
    onStyleSelected: (DioramaStyle) -> Unit,
    onGenerateDiorama: (DioramaStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    var rawRotationAngle by remember { mutableFloatStateOf(0f) }
    var rawTiltY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var isAutoRotating by remember { mutableStateOf(false) }
    var showFullscreenModal by remember { mutableStateOf(false) }

    // Auto-rotation engine
    val infiniteTransition = rememberInfiniteTransition(label = "auto_spin_trans")
    val autoSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_spin_angle"
    )

    val effectiveAngle = if (isAutoRotating) {
        (rawRotationAngle + autoSpinAngle) % 360f
    } else {
        rawRotationAngle
    }

    val animatedAngle by animateFloatAsState(
        targetValue = effectiveAngle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animated_angle"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = rawTiltY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animated_tilt_y"
    )
    val animatedZoom by animateFloatAsState(
        targetValue = zoomScale,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "animated_zoom"
    )

    val activeBitmap: Bitmap? = (dioramaState as? DioramaState.Success)?.bitmap
    val landmarkHighlights = remember(weather.location.name) { getLandmarkHighlights(weather.location.name) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Hero 3D Miniature Diorama Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10.5f)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (isAutoRotating) {
                            isAutoRotating = false
                        }
                        // Horizontal Orbit
                        rawRotationAngle = (rawRotationAngle - pan.x * 0.45f) % 360f
                        if (rawRotationAngle < 0) rawRotationAngle += 360f

                        // Vertical Pitch Tilt
                        rawTiltY = (rawTiltY + pan.y * 0.25f).coerceIn(-28f, 28f)

                        // Pinch Zoom
                        zoomScale = (zoomScale * zoom).coerceIn(0.75f, 2.2f)
                    }
                }
                .testTag("diorama_viewport")
        ) {
            // Layer 1: Background Content (AI Generated Diorama Image OR 3D Procedural Cluster)
            Crossfade(
                targetState = activeBitmap,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "diorama_crossfade"
            ) { bitmap ->
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Gemini 3D Miniature Landmark Diorama of ${weather.location.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = (animatedAngle / 360f - 0.5f) * 30f
                                translationY = animatedTiltY * 0.8f
                                scaleX = animatedZoom * 1.05f
                                scaleY = animatedZoom * 1.05f
                            }
                    )
                } else {
                    ProceduralDioramaCluster(
                        cityName = weather.location.name,
                        condition = weather.condition,
                        isDay = weather.isDay,
                        rotationAngleDeg = animatedAngle,
                        tiltYDeg = animatedTiltY,
                        zoomScale = animatedZoom,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Layer 2: Atmospheric Weather Particle Engine
            AtmosphericParticles(
                condition = weather.condition,
                windSpeed = weather.windSpeed,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 3: Top Badges & Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Diorama Model Source Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    modifier = Modifier.border(
                        0.8.dp,
                        Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (activeBitmap != null) "Gemini 3D Diorama" else "Interactive 3D Cluster",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Action buttons: Fullscreen, Reset Camera, Auto-Rotate & Generate
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reset View / Center
                    IconButton(
                        onClick = {
                            rawRotationAngle = 0f
                            rawTiltY = 0f
                            zoomScale = 1.0f
                            isAutoRotating = false
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("reset_3d_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Reset 3D Camera",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Auto-Rotate 360° Toggle
                    IconButton(
                        onClick = { isAutoRotating = !isAutoRotating },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isAutoRotating) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .testTag("auto_rotate_3d_button")
                    ) {
                        Icon(
                            imageVector = if (isAutoRotating) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoRotating) "Stop 3D rotation" else "Auto rotate 3D",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (activeBitmap != null) {
                        IconButton(
                            onClick = { showFullscreenModal = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .testTag("fullscreen_diorama_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "View full size diorama",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Quick AI Generate Button
                    Surface(
                        onClick = { onGenerateDiorama(selectedStyle) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("generate_diorama_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (dioramaState is DioramaState.Generating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Generate 3D Miniature with Gemini",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Layer 4: Floating Zoom Controls Overlay on right side
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale + 0.2f).coerceIn(0.75f, 2.2f) },
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("zoom_in_3d_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { zoomScale = (zoomScale - 0.2f).coerceIn(0.75f, 2.2f) },
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("zoom_out_3d_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Layer 5: Bottom Location & Interactive Hint Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${weather.location.name} 3D Miniature",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drag to orbit 360° • Pinch to zoom (${String.format("%.1fx", animatedZoom)})",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                    }

                    // Style indicator badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = selectedStyle.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Error notice banner inside viewport if generation failed
            if (dioramaState is DioramaState.Error) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dioramaState.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { onGenerateDiorama(selectedStyle) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry Gemini Generation", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Architectural Landmark Feature Badges
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            landmarkHighlights.forEach { landmark ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = landmark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Style Selector Horizontal Chip Bar
        Text(
            text = "Aesthetic Generation Style",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DioramaStyle.entries) { style ->
                val isSelected = style == selectedStyle
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        onStyleSelected(style)
                        onGenerateDiorama(style)
                    },
                    label = {
                        Text(
                            text = style.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("style_chip_${style.name}")
                )
            }
        }
    }

    // Fullscreen Diorama Modal Dialog
    if (showFullscreenModal && activeBitmap != null) {
        Dialog(onDismissRequest = { showFullscreenModal = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${weather.location.name} Diorama",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rendered with Gemini 2.5 Flash Image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = activeBitmap.asImageBitmap(),
                        contentDescription = "Full Diorama",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { showFullscreenModal = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

private fun getLandmarkHighlights(cityName: String): List<String> {
    val name = cityName.lowercase()
    return when {
        name.contains("paris") -> listOf("🗼 Eiffel Tower", "🏛️ Arc de Triomphe", "💎 Louvre Pyramid", "🏘️ Haussmann Quarters")
        name.contains("tokyo") || name.contains("japan") -> listOf("🗼 Tokyo Tower", "⛩️ Torii Gate", "🏯 Pagoda Temple", "🌸 Cherry Blossoms")
        name.contains("york") || name.contains("nyc") -> listOf("🏙️ Empire State", "🗽 Statue of Liberty", "🏢 One World Trade", "🌳 Central Park")
        name.contains("london") -> listOf("🕰️ Big Ben", "🎡 London Eye", "🏙️ The Shard", "🏛️ Westminster")
        name.contains("francisco") || name.contains("sf") -> listOf("🌉 Golden Gate", "🔺 Transamerica Pyramid", "🏡 Painted Ladies")
        name.contains("dubai") -> listOf("🏙️ Burj Khalifa", "⛵ Burj Al Arab", "🖼️ Dubai Frame")
        name.contains("sydney") -> listOf("🎭 Opera House", "🌉 Harbour Bridge", "🗼 Sydney Tower")
        name.contains("rome") || name.contains("italy") -> listOf("🏛️ Colosseum", "🏺 Roman Columns", "🏡 Italian Villa")
        name.contains("cairo") || name.contains("egypt") -> listOf("🔺 Great Pyramids", "🦁 Sphinx", "🌴 Oasis Palms")
        name.contains("delhi") || name.contains("agra") || name.contains("india") -> listOf("🕌 Taj Mahal", "🏛️ India Gate", "🌺 Heritage Gardens")
        else -> listOf("🏙️ Central Spire", "🏢 Modern Towers", "🏘️ Townhouse Blocks", "🌳 Isometric Foliage")
    }
}
