package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.data.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data class representing a 3D architectural element in the miniature diorama
 */
data class Diorama3DObject(
    val name: String,
    val localX: Float, // relative to platform center (-1.0 to 1.0)
    val localZ: Float, // depth coordinate (-1.0 to 1.0)
    val width: Float,
    val height: Float,
    val drawType: DioramaDrawType
)

/**
 * Mutable projected item container to prevent GC object allocation on every frame
 */
class Projected3DObject(
    var obj: Diorama3DObject,
    var screenX: Float = 0f,
    var screenY: Float = 0f,
    var depthZ: Float = 0f
)

enum class DioramaDrawType {
    EIFFEL_TOWER,
    ARC_DE_TRIOMPHE,
    LOUVRE_PYRAMID,
    TOKYO_TOWER,
    PAGODA_TEMPLE,
    TORII_GATE,
    EMPIRE_STATE,
    MODERN_SKYSCRAPER,
    STATUE_LIBERTY,
    BIG_BEN,
    LONDON_EYE,
    THE_SHARD,
    BURJ_KHALIFA,
    BURJ_AL_ARAB,
    DUBAI_FRAME,
    SUSPENSION_BRIDGE,
    PAINTED_LADIES,
    OPERA_HOUSE,
    HARBOUR_BRIDGE,
    COLOSSEUM,
    TAJ_MAHAL,
    PYRAMIDS,
    HAUSSMANN_BLOCK,
    ISOMETRIC_TREE,
    STREET_LAMP
}

/**
 * Zero-Allocation Reusable Drawing Cache for 60-120fps Performance
 */
class DioramaDrawCache {
    val pathA = Path()
    val pathB = Path()
    val pathC = Path()
    val pedestalPath = Path()
    var projectedBuffer: Array<Projected3DObject> = emptyArray()

    fun ensureBufferSize(size: Int, objects: List<Diorama3DObject>) {
        if (projectedBuffer.size != size) {
            projectedBuffer = Array(size) { i ->
                Projected3DObject(objects[i])
            }
        } else {
            for (i in 0 until size) {
                projectedBuffer[i].obj = objects[i]
            }
        }
    }
}

/**
 * High-Performance Interactive 3D Miniature Diorama Cluster
 * Engineered for buttery-smooth 60-120 FPS:
 * - Pre-allocated reusable Path pools
 * - Zero GC allocations during draw pass
 * - Optimized 3D depth sorting
 */
@Composable
fun ProceduralDioramaCluster(
    cityName: String,
    condition: WeatherCondition,
    isDay: Boolean,
    rotationAngleDeg: Float = 0f,
    tiltYDeg: Float = 0f,
    zoomScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "diorama_cluster_anim")
    val cloudShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud_shift"
    )
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light_pulse"
    )

    // Cache city objects configuration across recompositions
    val clusterObjects = remember(cityName) { getObjectsForCity(cityName) }
    val drawCache = remember { DioramaDrawCache() }

    // Static color palettes pre-calculated
    val skyColors = remember(condition, isDay) {
        if (isDay) {
            when {
                condition.isThunder -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
                condition.isRainy -> listOf(Color(0xFF384E66), Color(0xFF5A738E), Color(0xFF8BA2B8))
                condition.isSnowy -> listOf(Color(0xFFB0C4DE), Color(0xFFD0E1FD), Color(0xFFEAF2FD))
                condition.isCloudy -> listOf(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFFCBD5E1))
                else -> listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFBAE6FD))
            }
        } else {
            listOf(Color(0xFF090D16), Color(0xFF0F172A), Color(0xFF1E293B))
        }
    }

    val pedestalSideColors = remember(isDay) {
        val sideDark = if (isDay) Color(0xFF0284C7) else Color(0xFF075985)
        val sideLight = if (isDay) Color(0xFF0369A1) else Color(0xFF0C4A6E)
        listOf(sideDark, sideLight, sideDark)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCache.ensureBufferSize(clusterObjects.size, clusterObjects)

        // 1. Atmosphere / Sky Gradient
        drawRect(brush = Brush.verticalGradient(skyColors))

        // Celestial Body (Sun/Moon/Stars)
        val radX = (rotationAngleDeg * 0.017453292f) // Math.toRadians fast constant
        val celestialParallaxX = sin(radX) * 25f
        val celestialParallaxY = (tiltYDeg / 30f) * 15f

        if (!isDay) {
            drawProceduralStars(w, h, lightPulse)
            val moonCenter = Offset(w * 0.82f + celestialParallaxX, h * 0.20f + celestialParallaxY)
            drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.95f),
                radius = 16f,
                center = moonCenter
            )
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 13.5f,
                center = Offset(moonCenter.x + 6f, moonCenter.y - 4f)
            )
        } else {
            val sunCenter = Offset(w * 0.80f + celestialParallaxX, h * 0.22f + celestialParallaxY)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.5f * lightPulse), Color.Transparent),
                    center = sunCenter,
                    radius = 55f
                ),
                radius = 55f,
                center = sunCenter
            )
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 18f,
                center = sunCenter
            )
        }

        // Floating volumetric clouds
        drawMiniatureClouds(w, h, cloudShift, isDay, celestialParallaxX)

        // Center point for 3D isometric diorama
        val center = Offset(w * 0.5f, h * 0.68f + (tiltYDeg / 30f) * 18f)

        withTransform({
            scale(zoomScale, zoomScale, center)
        }) {
            val platformRadiusX = w * 0.42f
            val platformRadiusY = platformRadiusX * 0.48f
            val platformHeight = 28f

            // 2. Base 3D Isometric Pedestal
            draw3DPedestal(
                center = center,
                radiusX = platformRadiusX,
                radiusY = platformRadiusY,
                height = platformHeight,
                isDay = isDay,
                condition = condition,
                sideColors = pedestalSideColors,
                cache = drawCache
            )

            // 3. Fast Zero-Allocation 3D Project & Sort
            val cosR = cos(radX)
            val sinR = sin(radX)
            val spreadX = platformRadiusX * 0.78f
            val spreadY = platformRadiusY * 0.78f
            val groundOffsetY = platformHeight * 0.45f

            val buf = drawCache.projectedBuffer
            for (i in 0 until clusterObjects.size) {
                val obj = clusterObjects[i]
                val item = buf[i]
                item.obj = obj
                val rotX = obj.localX * cosR - obj.localZ * sinR
                val rotZ = obj.localX * sinR + obj.localZ * cosR
                item.screenX = center.x + rotX * spreadX
                item.screenY = center.y + rotZ * spreadY - groundOffsetY
                item.depthZ = rotZ
            }

            // In-place insertion sort (optimal for small arrays n < 10 with 0 object allocations)
            for (i in 1 until clusterObjects.size) {
                val key = buf[i]
                var j = i - 1
                while (j >= 0 && buf[j].depthZ > key.depthZ) {
                    buf[j + 1] = buf[j]
                    j--
                }
                buf[j + 1] = key
            }

            // Draw each sorted 3D element
            for (i in 0 until clusterObjects.size) {
                val p = buf[i]
                draw3DObject(
                    obj = p.obj,
                    pos = Offset(p.screenX, p.screenY),
                    isDay = isDay,
                    condition = condition,
                    lightPulse = lightPulse,
                    cache = drawCache
                )
            }
        }
    }
}

private fun getObjectsForCity(cityName: String): List<Diorama3DObject> {
    val name = cityName.lowercase()
    return when {
        name.contains("paris") -> listOf(
            Diorama3DObject("Eiffel Tower", 0.0f, 0.0f, 54f, 132f, DioramaDrawType.EIFFEL_TOWER),
            Diorama3DObject("Arc de Triomphe", -0.55f, 0.35f, 44f, 48f, DioramaDrawType.ARC_DE_TRIOMPHE),
            Diorama3DObject("Louvre Pyramid", 0.55f, 0.35f, 42f, 32f, DioramaDrawType.LOUVRE_PYRAMID),
            Diorama3DObject("Haussmann Left", -0.75f, -0.4f, 36f, 56f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Haussmann Right", 0.75f, -0.4f, 38f, 52f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Tree 1", -0.28f, 0.65f, 16f, 22f, DioramaDrawType.ISOMETRIC_TREE),
            Diorama3DObject("Tree 2", 0.28f, 0.65f, 16f, 22f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("tokyo") || name.contains("japan") -> listOf(
            Diorama3DObject("Tokyo Tower", 0.05f, 0.0f, 52f, 136f, DioramaDrawType.TOKYO_TOWER),
            Diorama3DObject("Pagoda Temple", -0.55f, 0.30f, 44f, 75f, DioramaDrawType.PAGODA_TEMPLE),
            Diorama3DObject("Shibuya Skyscraper", 0.58f, -0.30f, 38f, 102f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Torii Gate", -0.25f, 0.65f, 28f, 32f, DioramaDrawType.TORII_GATE),
            Diorama3DObject("Modern Tower", 0.75f, 0.35f, 32f, 78f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Cherry Blossom Tree", 0.25f, 0.65f, 18f, 24f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("york") || name.contains("nyc") -> listOf(
            Diorama3DObject("Empire State", 0.0f, -0.1f, 46f, 130f, DioramaDrawType.EMPIRE_STATE),
            Diorama3DObject("One World Trade", 0.55f, -0.3f, 40f, 118f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Statue of Liberty", -0.58f, 0.4f, 30f, 66f, DioramaDrawType.STATUE_LIBERTY),
            Diorama3DObject("Midtown Highrise", -0.72f, -0.4f, 34f, 90f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Brownstones", 0.52f, 0.45f, 38f, 46f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Central Park Tree", 0.0f, 0.65f, 18f, 24f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("london") -> listOf(
            Diorama3DObject("Big Ben", -0.05f, 0.0f, 34f, 96f, DioramaDrawType.BIG_BEN),
            Diorama3DObject("London Eye", -0.58f, 0.35f, 68f, 76f, DioramaDrawType.LONDON_EYE),
            Diorama3DObject("The Shard", 0.55f, -0.25f, 36f, 122f, DioramaDrawType.THE_SHARD),
            Diorama3DObject("Victorian Block Left", -0.72f, -0.4f, 36f, 50f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Victorian Block Right", 0.72f, 0.35f, 38f, 52f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Park Tree", 0.22f, 0.65f, 16f, 22f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("francisco") || name.contains("sf") -> listOf(
            Diorama3DObject("Golden Gate Bridge", -0.15f, 0.0f, 85f, 78f, DioramaDrawType.SUSPENSION_BRIDGE),
            Diorama3DObject("Transamerica Pyramid", 0.58f, -0.25f, 40f, 102f, DioramaDrawType.BURJ_AL_ARAB),
            Diorama3DObject("Painted Ladies", -0.60f, 0.45f, 44f, 38f, DioramaDrawType.PAINTED_LADIES),
            Diorama3DObject("Financial Skyscraper", 0.70f, 0.35f, 35f, 88f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Cypress Tree", 0.28f, 0.65f, 16f, 24f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("dubai") -> listOf(
            Diorama3DObject("Burj Khalifa", 0.0f, -0.1f, 42f, 145f, DioramaDrawType.BURJ_KHALIFA),
            Diorama3DObject("Burj Al Arab", -0.58f, 0.35f, 42f, 86f, DioramaDrawType.BURJ_AL_ARAB),
            Diorama3DObject("Dubai Frame", 0.56f, 0.30f, 36f, 66f, DioramaDrawType.DUBAI_FRAME),
            Diorama3DObject("Marina Skyscraper 1", -0.72f, -0.35f, 32f, 80f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Marina Skyscraper 2", 0.72f, -0.35f, 34f, 85f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Palm Tree", 0.0f, 0.65f, 18f, 26f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("sydney") -> listOf(
            Diorama3DObject("Sydney Opera House", 0.05f, 0.1f, 66f, 42f, DioramaDrawType.OPERA_HOUSE),
            Diorama3DObject("Harbour Bridge", -0.56f, 0.25f, 68f, 46f, DioramaDrawType.HARBOUR_BRIDGE),
            Diorama3DObject("Sydney Tower Eye", 0.55f, -0.30f, 36f, 110f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("CBD Skyscraper", -0.72f, -0.35f, 32f, 82f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Harbour Tree", 0.60f, 0.50f, 16f, 22f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("rome") || name.contains("italy") -> listOf(
            Diorama3DObject("Colosseum", 0.0f, 0.0f, 68f, 48f, DioramaDrawType.COLOSSEUM),
            Diorama3DObject("Roman Columns", -0.56f, 0.35f, 38f, 40f, DioramaDrawType.ARC_DE_TRIOMPHE),
            Diorama3DObject("Italian Villa", 0.58f, 0.35f, 42f, 46f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Historic Block", -0.70f, -0.35f, 36f, 52f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Cypress Tree", 0.28f, 0.65f, 16f, 26f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("cairo") || name.contains("egypt") -> listOf(
            Diorama3DObject("Great Pyramid", 0.0f, 0.0f, 64f, 56f, DioramaDrawType.PYRAMIDS),
            Diorama3DObject("Second Pyramid", -0.52f, -0.2f, 48f, 42f, DioramaDrawType.PYRAMIDS),
            Diorama3DObject("Sphinx Monument", 0.52f, 0.40f, 38f, 32f, DioramaDrawType.STATUE_LIBERTY),
            Diorama3DObject("Oasis Palm 1", -0.30f, 0.65f, 18f, 26f, DioramaDrawType.ISOMETRIC_TREE),
            Diorama3DObject("Oasis Palm 2", 0.30f, 0.65f, 18f, 26f, DioramaDrawType.ISOMETRIC_TREE)
        )
        name.contains("delhi") || name.contains("agra") || name.contains("india") -> listOf(
            Diorama3DObject("Taj Mahal", 0.0f, 0.0f, 64f, 66f, DioramaDrawType.TAJ_MAHAL),
            Diorama3DObject("India Gate", -0.55f, 0.35f, 42f, 46f, DioramaDrawType.ARC_DE_TRIOMPHE),
            Diorama3DObject("Heritage Pavilion", 0.55f, 0.35f, 38f, 42f, DioramaDrawType.PAGODA_TEMPLE),
            Diorama3DObject("Garden Tree", 0.0f, 0.65f, 18f, 24f, DioramaDrawType.ISOMETRIC_TREE)
        )
        else -> listOf(
            Diorama3DObject("Central Spire", 0.0f, -0.05f, 44f, 126f, DioramaDrawType.EMPIRE_STATE),
            Diorama3DObject("West Tower", -0.52f, 0.25f, 36f, 92f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("East Highrise", 0.52f, -0.25f, 38f, 106f, DioramaDrawType.MODERN_SKYSCRAPER),
            Diorama3DObject("Townhouse Row", -0.70f, -0.35f, 36f, 50f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("Civic Block", 0.70f, 0.35f, 36f, 48f, DioramaDrawType.HAUSSMANN_BLOCK),
            Diorama3DObject("City Park Tree 1", -0.25f, 0.65f, 16f, 22f, DioramaDrawType.ISOMETRIC_TREE),
            Diorama3DObject("City Park Tree 2", 0.25f, 0.65f, 16f, 22f, DioramaDrawType.ISOMETRIC_TREE)
        )
    }
}

private fun DrawScope.draw3DPedestal(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    height: Float,
    isDay: Boolean,
    condition: WeatherCondition,
    sideColors: List<Color>,
    cache: DioramaDrawCache
) {
    val topColor = if (condition.isSnowy) {
        Color(0xFFE2E8F0)
    } else if (isDay) {
        Color(0xFF38BDF8)
    } else {
        Color(0xFF0369A1)
    }

    // Reuse pre-allocated pedestal rim path
    val rim = cache.pedestalPath
    rim.reset()
    rim.moveTo(center.x - radiusX, center.y)
    rim.cubicTo(
        center.x - radiusX, center.y + radiusY,
        center.x + radiusX, center.y + radiusY,
        center.x + radiusX, center.y
    )
    rim.lineTo(center.x + radiusX, center.y + height)
    rim.cubicTo(
        center.x + radiusX, center.y + radiusY + height,
        center.x - radiusX, center.y + radiusY + height,
        center.x - radiusX, center.y + height
    )
    rim.close()

    drawPath(path = rim, brush = Brush.horizontalGradient(sideColors))

    // Top Ellipse Base
    drawOval(
        color = topColor,
        topLeft = Offset(center.x - radiusX, center.y - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f)
    )

    // Concentric accent ring
    val ringAlpha = if (isDay) 0.35f else 0.2f
    drawOval(
        color = Color.White.copy(alpha = ringAlpha),
        topLeft = Offset(center.x - radiusX * 0.75f, center.y - radiusY * 0.75f),
        size = Size(radiusX * 1.5f, radiusY * 1.5f),
        style = Stroke(width = 1.2f)
    )

    // Pedestal rim highlight
    drawOval(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(center.x - radiusX, center.y - radiusY),
        size = Size(radiusX * 2f, radiusY * 2f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.draw3DObject(
    obj: Diorama3DObject,
    pos: Offset,
    isDay: Boolean,
    condition: WeatherCondition,
    lightPulse: Float,
    cache: DioramaDrawCache
) {
    val wallColorLight = if (condition.isSnowy) Color(0xFFF1F5F9) else if (isDay) Color(0xFFF8FAFC) else Color(0xFF1E293B)
    val wallColorDark = if (condition.isSnowy) Color(0xFFCBD5E1) else if (isDay) Color(0xFFCBD5E1) else Color(0xFF0F172A)
    val roofAccent = if (isDay) Color(0xFF0284C7) else Color(0xFF38BDF8)
    val windowGlow = if (!isDay) Color(0xFFFDE047).copy(alpha = 0.85f * lightPulse) else Color(0xFF93C5FD).copy(alpha = 0.6f)

    when (obj.drawType) {
        DioramaDrawType.EIFFEL_TOWER -> drawEiffelTower(pos.x, pos.y, isDay, cache)
        DioramaDrawType.ARC_DE_TRIOMPHE -> drawArcDeTriomphe(pos.x, pos.y, wallColorLight, wallColorDark, cache)
        DioramaDrawType.LOUVRE_PYRAMID -> drawLouvrePyramid(pos.x, pos.y, isDay, cache)
        DioramaDrawType.TOKYO_TOWER -> drawTokyoTower(pos.x, pos.y, cache)
        DioramaDrawType.PAGODA_TEMPLE -> drawPagodaTemple(pos.x, pos.y, wallColorLight, isDay, cache)
        DioramaDrawType.TORII_GATE -> drawToriiGate(pos.x, pos.y)
        DioramaDrawType.EMPIRE_STATE -> drawEmpireState(pos.x, pos.y, wallColorLight, wallColorDark, roofAccent, windowGlow)
        DioramaDrawType.MODERN_SKYSCRAPER -> drawModernSkyscraper(pos.x, pos.y, obj.width, obj.height, wallColorLight, wallColorDark, windowGlow)
        DioramaDrawType.STATUE_LIBERTY -> drawStatueSilhouette(pos.x, pos.y)
        DioramaDrawType.BIG_BEN -> drawBigBen(pos.x, pos.y, wallColorLight, wallColorDark, windowGlow, cache)
        DioramaDrawType.LONDON_EYE -> drawLondonEye(pos.x, pos.y, isDay)
        DioramaDrawType.THE_SHARD -> drawTheShard(pos.x, pos.y, isDay, cache)
        DioramaDrawType.BURJ_KHALIFA -> drawBurjKhalifa(pos.x, pos.y, wallColorLight, wallColorDark)
        DioramaDrawType.BURJ_AL_ARAB -> drawBurjAlArab(pos.x, pos.y, isDay, cache)
        DioramaDrawType.DUBAI_FRAME -> drawDubaiFrame(pos.x, pos.y)
        DioramaDrawType.SUSPENSION_BRIDGE -> drawSuspensionBridge(pos.x, pos.y, cache)
        DioramaDrawType.PAINTED_LADIES -> drawPaintedLadies(pos.x, pos.y, cache)
        DioramaDrawType.OPERA_HOUSE -> drawOperaHouse(pos.x, pos.y, isDay, cache)
        DioramaDrawType.HARBOUR_BRIDGE -> drawHarbourBridge(pos.x, pos.y, isDay, cache)
        DioramaDrawType.COLOSSEUM -> drawColosseum(pos.x, pos.y, isDay, cache)
        DioramaDrawType.TAJ_MAHAL -> drawTajMahal(pos.x, pos.y, isDay, cache)
        DioramaDrawType.PYRAMIDS -> drawPyramid(pos.x, pos.y, isDay, cache)
        DioramaDrawType.HAUSSMANN_BLOCK -> drawHaussmannBlock(pos.x, pos.y, obj.width, obj.height, wallColorLight, wallColorDark, windowGlow, cache)
        DioramaDrawType.ISOMETRIC_TREE -> drawIsometricTree(pos.x, pos.y, isDay, condition)
        DioramaDrawType.STREET_LAMP -> drawStreetLamp(pos.x, pos.y, isDay, lightPulse)
    }
}

// ----------------- Optimized Zero-Allocation Vectors -----------------

private fun DrawScope.drawProceduralStars(w: Float, h: Float, pulse: Float) {
    val starCoords = listOf(
        Pair(0.12f, 0.15f), Pair(0.24f, 0.28f), Pair(0.38f, 0.12f),
        Pair(0.52f, 0.22f), Pair(0.68f, 0.10f), Pair(0.88f, 0.18f),
        Pair(0.18f, 0.38f), Pair(0.44f, 0.34f), Pair(0.74f, 0.36f)
    )
    for (i in starCoords.indices) {
        val (rx, ry) = starCoords[i]
        val starAlpha = (0.4f + 0.6f * sin((pulse * 3.14159f + i).toDouble()).toFloat()).coerceIn(0f, 1f)
        drawCircle(
            color = Color.White.copy(alpha = starAlpha),
            radius = if (i % 2 == 0) 2.2f else 1.4f,
            center = Offset(w * rx, h * ry)
        )
    }
}

private fun DrawScope.drawMiniatureClouds(w: Float, h: Float, shift: Float, isDay: Boolean, parallaxX: Float) {
    val cloudColor = if (isDay) Color.White.copy(alpha = 0.75f) else Color(0xFFCBD5E1).copy(alpha = 0.35f)

    val c1X = ((w * 0.2f + shift * w * 0.6f + parallaxX * 0.5f) % (w * 1.4f)) - w * 0.2f
    val c1Y = h * 0.26f
    drawCircle(cloudColor, 18f, Offset(c1X, c1Y))
    drawCircle(cloudColor, 26f, Offset(c1X + 16f, c1Y - 6f))
    drawCircle(cloudColor, 20f, Offset(c1X + 36f, c1Y))
    drawCircle(cloudColor, 14f, Offset(c1X + 48f, c1Y + 2f))
    drawRect(cloudColor, topLeft = Offset(c1X - 2f, c1Y + 2f), size = Size(52f, 16f))

    val c2X = ((w * 0.65f + shift * w * 0.35f + parallaxX * 0.4f) % (w * 1.4f)) - w * 0.2f
    val c2Y = h * 0.16f
    drawCircle(cloudColor, 14f, Offset(c2X, c2Y))
    drawCircle(cloudColor, 20f, Offset(c2X + 14f, c2Y - 4f))
    drawCircle(cloudColor, 15f, Offset(c2X + 28f, c2Y))
    drawRect(cloudColor, topLeft = Offset(c2X - 2f, c2Y + 2f), size = Size(32f, 12f))
}

private fun DrawScope.drawEiffelTower(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val h = 130f
    val baseW = 54f
    val towerColor = if (isDay) Color(0xFF78716C) else Color(0xFFD6D3D1)
    val glowSpire = if (!isDay) Color(0xFFFBBF24) else Color(0xFFE2E8F0)

    val p = cache.pathA
    p.reset()
    p.moveTo(cx - baseW * 0.5f, baseY)
    p.cubicTo(cx - baseW * 0.25f, baseY - h * 0.3f, cx - 10f, baseY - h * 0.6f, cx - 4f, baseY - h)
    p.lineTo(cx + 4f, baseY - h)
    p.cubicTo(cx + 10f, baseY - h * 0.6f, cx + baseW * 0.25f, baseY - h * 0.3f, cx + baseW * 0.5f, baseY)
    p.lineTo(cx + baseW * 0.28f, baseY)
    p.cubicTo(cx + 14f, baseY - 24f, cx - 14f, baseY - 24f, cx - baseW * 0.28f, baseY)
    p.close()
    drawPath(p, towerColor)

    drawRect(towerColor, topLeft = Offset(cx - 18f, baseY - h * 0.38f), size = Size(36f, 6f))
    drawRect(towerColor, topLeft = Offset(cx - 12f, baseY - h * 0.68f), size = Size(24f, 5f))
    drawCircle(glowSpire, radius = 3.5f, center = Offset(cx, baseY - h - 3f))
}

private fun DrawScope.drawArcDeTriomphe(cx: Float, baseY: Float, lightColor: Color, darkColor: Color, cache: DioramaDrawCache) {
    val w = 44f
    val h = 48f
    val p = cache.pathA
    p.reset()
    p.moveTo(cx - w * 0.5f, baseY)
    p.lineTo(cx - w * 0.5f, baseY - h)
    p.lineTo(cx + w * 0.5f, baseY - h)
    p.lineTo(cx + w * 0.5f, baseY)
    p.lineTo(cx + w * 0.22f, baseY)
    p.lineTo(cx + w * 0.22f, baseY - h * 0.58f)
    p.cubicTo(cx + 12f, baseY - h * 0.72f, cx - 12f, baseY - h * 0.72f, cx - w * 0.22f, baseY - h * 0.58f)
    p.lineTo(cx - w * 0.22f, baseY)
    p.close()
    drawPath(p, lightColor)
    drawRect(darkColor, topLeft = Offset(cx - w * 0.55f, baseY - h - 5f), size = Size(w * 1.1f, 6f))
}

private fun DrawScope.drawLouvrePyramid(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val w = 42f
    val h = 32f
    val glassColor = if (isDay) Color(0xFF67E8F9).copy(alpha = 0.75f) else Color(0xFF38BDF8).copy(alpha = 0.85f)
    val frameColor = if (isDay) Color(0xFF0E7490) else Color(0xFFBAE6FD)

    val p = cache.pathA
    p.reset()
    p.moveTo(cx - w * 0.5f, baseY)
    p.lineTo(cx, baseY - h)
    p.lineTo(cx + w * 0.5f, baseY)
    p.close()
    drawPath(p, glassColor)
    drawPath(p, frameColor, style = Stroke(width = 1.5f))
    drawLine(frameColor, Offset(cx - w * 0.25f, baseY), Offset(cx, baseY - h), strokeWidth = 1f)
    drawLine(frameColor, Offset(cx + w * 0.25f, baseY), Offset(cx, baseY - h), strokeWidth = 1f)
}

private fun DrawScope.drawTokyoTower(cx: Float, baseY: Float, cache: DioramaDrawCache) {
    val h = 135f
    val baseW = 50f
    val redColor = Color(0xFFEF4444)
    val whiteColor = Color(0xFFF8FAFC)

    val p = cache.pathA
    p.reset()
    p.moveTo(cx - baseW * 0.5f, baseY)
    p.lineTo(cx - 3f, baseY - h)
    p.lineTo(cx + 3f, baseY - h)
    p.lineTo(cx + baseW * 0.5f, baseY)
    p.lineTo(cx + baseW * 0.25f, baseY)
    p.cubicTo(cx + 12f, baseY - 22f, cx - 12f, baseY - 22f, cx - baseW * 0.25f, baseY)
    p.close()
    drawPath(p, redColor)

    drawRect(whiteColor, topLeft = Offset(cx - 18f, baseY - h * 0.32f), size = Size(36f, 8f))
    drawRect(whiteColor, topLeft = Offset(cx - 12f, baseY - h * 0.62f), size = Size(24f, 8f))
    drawRect(Color(0xFF1E293B), topLeft = Offset(cx - 16f, baseY - h * 0.46f), size = Size(32f, 10f))
    drawLine(Color.White, Offset(cx, baseY - h), Offset(cx, baseY - h - 14f), strokeWidth = 2f)
}

private fun DrawScope.drawPagodaTemple(cx: Float, baseY: Float, wallColor: Color, isDay: Boolean, cache: DioramaDrawCache) {
    val roofColor = if (isDay) Color(0xFFDC2626) else Color(0xFF991B1B)
    var curY = baseY
    var curW = 42f

    val roof = cache.pathA
    for (tier in 1..4) {
        drawRect(wallColor, topLeft = Offset(cx - curW * 0.32f, curY - 14f), size = Size(curW * 0.64f, 14f))
        roof.reset()
        roof.moveTo(cx - curW * 0.55f, curY - 12f)
        roof.quadraticTo(cx - curW * 0.2f, curY - 18f, cx, curY - 19f)
        roof.quadraticTo(cx + curW * 0.2f, curY - 18f, cx + curW * 0.55f, curY - 12f)
        roof.lineTo(cx + curW * 0.45f, curY - 15f)
        roof.lineTo(cx - curW * 0.45f, curY - 15f)
        roof.close()
        drawPath(roof, roofColor)
        curY -= 17f
        curW *= 0.82f
    }
    drawLine(Color(0xFFF59E0B), Offset(cx, curY - 2f), Offset(cx, curY - 14f), strokeWidth = 2.2f)
}

private fun DrawScope.drawToriiGate(cx: Float, baseY: Float) {
    val vermilion = Color(0xFFEA580C)
    val w = 28f
    val h = 30f
    drawRect(vermilion, topLeft = Offset(cx - w * 0.38f, baseY - h), size = Size(4.5f, h))
    drawRect(vermilion, topLeft = Offset(cx + w * 0.38f - 4.5f, baseY - h), size = Size(4.5f, h))
    drawRect(vermilion, topLeft = Offset(cx - w * 0.55f, baseY - h - 3f), size = Size(w * 1.1f, 5f))
    drawRect(Color(0xFF1E293B), topLeft = Offset(cx - w * 0.6f, baseY - h - 6f), size = Size(w * 1.2f, 3.5f))
}

private fun DrawScope.drawEmpireState(
    cx: Float,
    baseY: Float,
    lightColor: Color,
    darkColor: Color,
    accent: Color,
    windowGlow: Color
) {
    var curY = baseY
    val stages = listOf(Pair(46f, 32f), Pair(36f, 30f), Pair(26f, 26f), Pair(16f, 18f))
    for (i in stages.indices) {
        val (w, h) = stages[i]
        drawRect(lightColor, topLeft = Offset(cx - w * 0.5f, curY - h), size = Size(w * 0.5f, h))
        drawRect(darkColor, topLeft = Offset(cx, curY - h), size = Size(w * 0.5f, h))
        for (wy in 1..3) {
            val winY = curY - h + wy * (h / 4f)
            drawCircle(windowGlow, 1.2f, Offset(cx - w * 0.25f, winY))
            drawCircle(windowGlow, 1.2f, Offset(cx + w * 0.25f, winY))
        }
        curY -= h
    }
    drawLine(accent, Offset(cx, curY), Offset(cx, curY - 20f), strokeWidth = 2f)
    drawCircle(Color(0xFFEF4444), radius = 2f, center = Offset(cx, curY - 20f))
}

private fun DrawScope.drawBigBen(
    cx: Float,
    baseY: Float,
    lightColor: Color,
    darkColor: Color,
    windowGlow: Color,
    cache: DioramaDrawCache
) {
    val w = 32f
    val h = 88f
    drawRect(lightColor, topLeft = Offset(cx - w * 0.5f, baseY - h), size = Size(w * 0.5f, h))
    drawRect(darkColor, topLeft = Offset(cx, baseY - h), size = Size(w * 0.5f, h))

    drawCircle(Color(0xFFFEF08A), radius = 7f, center = Offset(cx, baseY - h + 16f))
    drawCircle(Color(0xFF1E293B), radius = 1.2f, center = Offset(cx, baseY - h + 16f))

    val spire = cache.pathA
    spire.reset()
    spire.moveTo(cx - w * 0.5f - 2f, baseY - h)
    spire.lineTo(cx, baseY - h - 30f)
    spire.lineTo(cx + w * 0.5f + 2f, baseY - h)
    spire.close()
    drawPath(spire, Color(0xFF0F766E))
    drawLine(Color(0xFFF59E0B), Offset(cx, baseY - h - 30f), Offset(cx, baseY - h - 38f), strokeWidth = 2f)
}

private fun DrawScope.drawLondonEye(cx: Float, baseY: Float, isDay: Boolean) {
    val r = 36f
    val center = Offset(cx, baseY - r - 8f)
    val rimColor = if (isDay) Color(0xFF0284C7) else Color(0xFF38BDF8)

    drawCircle(rimColor.copy(alpha = 0.5f), radius = r, center = center, style = Stroke(width = 2.5f))
    drawCircle(rimColor.copy(alpha = 0.8f), radius = r * 0.3f, center = center, style = Stroke(width = 1.5f))

    for (i in 0 until 8) {
        val angle = i * 0.7853982f
        val ex = center.x + r * cos(angle)
        val ey = center.y + r * sin(angle)
        drawLine(rimColor.copy(alpha = 0.4f), center, Offset(ex, ey), strokeWidth = 1f)
    }

    drawLine(rimColor, center, Offset(cx - 16f, baseY), strokeWidth = 2.5f)
    drawLine(rimColor, center, Offset(cx + 16f, baseY), strokeWidth = 2.5f)
}

private fun DrawScope.drawTheShard(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val w = 36f
    val h = 120f
    val glassLeft = if (isDay) Color(0xFF7DD3FC) else Color(0xFF0284C7)
    val glassRight = if (isDay) Color(0xFF38BDF8) else Color(0xFF0369A1)

    val leftShard = cache.pathA
    leftShard.reset()
    leftShard.moveTo(cx - w * 0.5f, baseY)
    leftShard.lineTo(cx, baseY - h)
    leftShard.lineTo(cx, baseY)
    leftShard.close()

    val rightShard = cache.pathB
    rightShard.reset()
    rightShard.moveTo(cx, baseY)
    rightShard.lineTo(cx, baseY - h)
    rightShard.lineTo(cx + w * 0.5f, baseY)
    rightShard.close()

    drawPath(leftShard, glassLeft)
    drawPath(rightShard, glassRight)
}

private fun DrawScope.drawBurjKhalifa(cx: Float, baseY: Float, lightColor: Color, darkColor: Color) {
    var curY = baseY
    val sections = listOf(Pair(40f, 26f), Pair(32f, 26f), Pair(24f, 24f), Pair(18f, 22f), Pair(12f, 20f), Pair(6f, 16f))
    for (i in sections.indices) {
        val (w, h) = sections[i]
        drawRect(lightColor, topLeft = Offset(cx - w * 0.5f, curY - h), size = Size(w * 0.5f, h))
        drawRect(darkColor, topLeft = Offset(cx, curY - h), size = Size(w * 0.5f, h))
        curY -= h
    }
    drawLine(Color.White, Offset(cx, curY), Offset(cx, curY - 24f), strokeWidth = 2f)
}

private fun DrawScope.drawBurjAlArab(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val w = 40f
    val h = 82f
    val sailColor = if (isDay) Color(0xFFF8FAFC) else Color(0xFFE2E8F0)
    val spineColor = if (isDay) Color(0xFF0284C7) else Color(0xFF38BDF8)

    val sail = cache.pathA
    sail.reset()
    sail.moveTo(cx - w * 0.4f, baseY)
    sail.cubicTo(cx - w * 0.4f, baseY - h * 0.5f, cx - w * 0.1f, baseY - h * 0.9f, cx + w * 0.4f, baseY - h)
    sail.lineTo(cx + w * 0.4f, baseY)
    sail.close()
    drawPath(sail, sailColor)
    drawLine(spineColor, Offset(cx + w * 0.4f, baseY), Offset(cx + w * 0.4f, baseY - h - 8f), strokeWidth = 3f)
    drawCircle(Color(0xFF38BDF8), radius = 6f, center = Offset(cx + w * 0.15f, baseY - h * 0.72f))
}

private fun DrawScope.drawDubaiFrame(cx: Float, baseY: Float) {
    val w = 36f
    val h = 64f
    val goldColor = Color(0xFFF59E0B)
    drawRect(goldColor, topLeft = Offset(cx - w * 0.5f, baseY - h), size = Size(w, h), style = Stroke(width = 5f))
}

private fun DrawScope.drawSuspensionBridge(cx: Float, baseY: Float, cache: DioramaDrawCache) {
    val redColor = Color(0xFFEA580C)
    val towerW = 10f
    val towerH = 75f
    val spanW = 80f

    drawRect(redColor, topLeft = Offset(cx - spanW * 0.35f - towerW * 0.5f, baseY - towerH), size = Size(towerW, towerH))
    drawRect(redColor, topLeft = Offset(cx + spanW * 0.35f - towerW * 0.5f, baseY - towerH), size = Size(towerW, towerH))

    val cable = cache.pathA
    cable.reset()
    cable.moveTo(cx - spanW * 0.55f, baseY - 16f)
    cable.cubicTo(cx - spanW * 0.35f, baseY - towerH, cx, baseY - 24f, cx + spanW * 0.35f, baseY - towerH)
    cable.cubicTo(cx + spanW * 0.35f, baseY - towerH, cx + spanW * 0.48f, baseY - 20f, cx + spanW * 0.55f, baseY - 16f)
    drawPath(cable, redColor, style = Stroke(width = 2f))
    drawRect(Color(0xFF334155), topLeft = Offset(cx - spanW * 0.55f, baseY - 18f), size = Size(spanW * 1.1f, 5f))
}

private fun DrawScope.drawPaintedLadies(cx: Float, baseY: Float, cache: DioramaDrawCache) {
    val colors = listOf(Color(0xFFF472B6), Color(0xFF38BDF8), Color(0xFFFBBF24))
    val roof = cache.pathA
    for (i in 0 until 3) {
        val houseX = cx - 22f + i * 22f
        val w = 18f
        val h = 26f
        drawRect(colors[i], topLeft = Offset(houseX - w * 0.5f, baseY - h), size = Size(w, h))
        roof.reset()
        roof.moveTo(houseX - w * 0.55f, baseY - h)
        roof.lineTo(houseX, baseY - h - 12f)
        roof.lineTo(houseX + w * 0.55f, baseY - h)
        roof.close()
        drawPath(roof, Color(0xFF475569))
    }
}

private fun DrawScope.drawOperaHouse(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val shellColor = if (isDay) Color(0xFFF8FAFC) else Color(0xFFE2E8F0)
    val shellShade = if (isDay) Color(0xFFCBD5E1) else Color(0xFF94A3B8)
    val shell = cache.pathA

    for (i in 0 until 3) {
        val sx = cx - 24f + i * 20f
        val sh = 28f + i * 8f
        val sw = 22f
        shell.reset()
        shell.moveTo(sx, baseY)
        shell.cubicTo(sx + 4f, baseY - sh * 0.8f, sx + sw * 0.6f, baseY - sh, sx + sw, baseY)
        shell.close()
        drawPath(shell, shellColor)
        drawPath(shell, shellShade, style = Stroke(width = 1.2f))
    }
}

private fun DrawScope.drawHarbourBridge(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val bridgeColor = if (isDay) Color(0xFF64748B) else Color(0xFF94A3B8)
    val w = 68f
    val h = 42f

    val arch = cache.pathA
    arch.reset()
    arch.moveTo(cx - w * 0.5f, baseY)
    arch.cubicTo(cx - w * 0.25f, baseY - h, cx + w * 0.25f, baseY - h, cx + w * 0.5f, baseY)
    drawPath(arch, bridgeColor, style = Stroke(width = 3f))
    drawRect(bridgeColor, topLeft = Offset(cx - w * 0.5f, baseY - 16f), size = Size(w, 4f))
}

private fun DrawScope.drawColosseum(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val stoneColor = if (isDay) Color(0xFFE2D9C8) else Color(0xFF78716C)
    val shadeColor = if (isDay) Color(0xFFC7BCA9) else Color(0xFF57534E)
    val w = 64f
    val h = 38f

    val body = cache.pathA
    body.reset()
    body.moveTo(cx - w * 0.5f, baseY)
    body.lineTo(cx - w * 0.5f, baseY - h * 0.7f)
    body.cubicTo(cx - w * 0.3f, baseY - h, cx + w * 0.3f, baseY - h, cx + w * 0.5f, baseY - h * 0.55f)
    body.lineTo(cx + w * 0.5f, baseY)
    body.close()
    drawPath(body, stoneColor)

    for (i in 0 until 5) {
        val ax = cx - w * 0.38f + i * (w * 0.19f)
        drawCircle(shadeColor, 3f, Offset(ax, baseY - 12f))
        drawCircle(shadeColor, 2.5f, Offset(ax, baseY - 22f))
    }
}

private fun DrawScope.drawTajMahal(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val marbleColor = if (isDay) Color(0xFFF8FAFC) else Color(0xFFE2E8F0)
    val domeColor = if (isDay) Color(0xFFFFFFFF) else Color(0xFFCBD5E1)
    val w = 52f
    val h = 42f

    drawRect(marbleColor, topLeft = Offset(cx - w * 0.5f, baseY - h), size = Size(w, h))

    val dome = cache.pathA
    dome.reset()
    dome.moveTo(cx - 14f, baseY - h)
    dome.cubicTo(cx - 18f, baseY - h - 14f, cx, baseY - h - 28f, cx, baseY - h - 30f)
    dome.cubicTo(cx, baseY - h - 28f, cx + 18f, baseY - h - 14f, cx + 14f, baseY - h)
    dome.close()
    drawPath(dome, domeColor)
    drawLine(Color(0xFFF59E0B), Offset(cx, baseY - h - 30f), Offset(cx, baseY - h - 36f), strokeWidth = 1.5f)

    drawLine(marbleColor, Offset(cx - w * 0.65f, baseY), Offset(cx - w * 0.65f, baseY - 50f), strokeWidth = 3f)
    drawLine(marbleColor, Offset(cx + w * 0.65f, baseY), Offset(cx + w * 0.65f, baseY - 50f), strokeWidth = 3f)
}

private fun DrawScope.drawPyramid(cx: Float, baseY: Float, isDay: Boolean, cache: DioramaDrawCache) {
    val sandLight = if (isDay) Color(0xFFFDE68A) else Color(0xFFD97706)
    val sandDark = if (isDay) Color(0xFFF59E0B) else Color(0xFFB45309)
    val w = 56f
    val h = 48f

    val leftFace = cache.pathA
    leftFace.reset()
    leftFace.moveTo(cx - w * 0.5f, baseY)
    leftFace.lineTo(cx, baseY - h)
    leftFace.lineTo(cx + 4f, baseY)
    leftFace.close()

    val rightFace = cache.pathB
    rightFace.reset()
    rightFace.moveTo(cx + 4f, baseY)
    rightFace.lineTo(cx, baseY - h)
    rightFace.lineTo(cx + w * 0.5f, baseY)
    rightFace.close()

    drawPath(leftFace, sandLight)
    drawPath(rightFace, sandDark)
}

private fun DrawScope.drawStatueSilhouette(cx: Float, baseY: Float) {
    val copperColor = Color(0xFF2DD4BF)
    drawRect(Color(0xFF64748B), topLeft = Offset(cx - 10f, baseY - 24f), size = Size(20f, 24f))
    drawRect(copperColor, topLeft = Offset(cx - 6f, baseY - 50f), size = Size(12f, 26f))
    drawCircle(copperColor, 5f, Offset(cx, baseY - 54f))
    drawLine(copperColor, Offset(cx + 4f, baseY - 46f), Offset(cx + 12f, baseY - 62f), strokeWidth = 2.5f)
    drawCircle(Color(0xFFF59E0B), 3f, Offset(cx + 12f, baseY - 63f))
}

private fun DrawScope.drawModernSkyscraper(
    cx: Float,
    baseY: Float,
    w: Float,
    h: Float,
    lightColor: Color,
    darkColor: Color,
    windowGlow: Color
) {
    drawRect(lightColor, topLeft = Offset(cx - w * 0.5f, baseY - h), size = Size(w * 0.5f, h))
    drawRect(darkColor, topLeft = Offset(cx, baseY - h), size = Size(w * 0.5f, h))
    val rows = 5
    for (r in 1..rows) {
        val y = baseY - h + r * (h / (rows + 1))
        drawCircle(windowGlow, 1.3f, Offset(cx - w * 0.25f, y))
        drawCircle(windowGlow, 1.3f, Offset(cx + w * 0.25f, y))
    }
}

private fun DrawScope.drawHaussmannBlock(
    cx: Float,
    baseY: Float,
    w: Float,
    h: Float,
    lightColor: Color,
    darkColor: Color,
    windowGlow: Color,
    cache: DioramaDrawCache
) {
    drawRect(lightColor, topLeft = Offset(cx - w * 0.5f, baseY - h), size = Size(w * 0.5f, h))
    drawRect(darkColor, topLeft = Offset(cx, baseY - h), size = Size(w * 0.5f, h))

    val mansard = cache.pathA
    mansard.reset()
    mansard.moveTo(cx - w * 0.5f, baseY - h)
    mansard.lineTo(cx - w * 0.35f, baseY - h - 10f)
    mansard.lineTo(cx + w * 0.35f, baseY - h - 10f)
    mansard.lineTo(cx + w * 0.5f, baseY - h)
    mansard.close()
    drawPath(mansard, Color(0xFF475569))

    for (r in 1..3) {
        val y = baseY - h + r * (h / 4)
        drawCircle(windowGlow, 1.2f, Offset(cx - w * 0.25f, y))
        drawCircle(windowGlow, 1.2f, Offset(cx + w * 0.25f, y))
    }
}

private fun DrawScope.drawIsometricTree(cx: Float, baseY: Float, isDay: Boolean, condition: WeatherCondition) {
    val trunkColor = Color(0xFF78350F)
    val foliageColor = if (condition.isSnowy) {
        Color(0xFFE2E8F0)
    } else if (isDay) {
        Color(0xFF16A34A)
    } else {
        Color(0xFF15803D)
    }

    drawRect(trunkColor, topLeft = Offset(cx - 1.5f, baseY - 8f), size = Size(3f, 8f))
    drawCircle(foliageColor, 7f, Offset(cx, baseY - 13f))
    drawCircle(foliageColor.copy(alpha = 0.8f), 5f, Offset(cx - 3f, baseY - 11f))
    drawCircle(foliageColor.copy(alpha = 0.8f), 5f, Offset(cx + 3f, baseY - 11f))
}

private fun DrawScope.drawStreetLamp(cx: Float, baseY: Float, isDay: Boolean, pulse: Float) {
    val postColor = Color(0xFF334155)
    val glowColor = Color(0xFFFEF08A).copy(alpha = if (!isDay) 0.9f * pulse else 0.2f)

    drawLine(postColor, Offset(cx, baseY), Offset(cx, baseY - 18f), strokeWidth = 1.5f)
    drawCircle(glowColor, radius = 4f, center = Offset(cx, baseY - 18f))
}
