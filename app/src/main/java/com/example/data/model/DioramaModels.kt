package com.example.data.model

import android.graphics.Bitmap

/**
 * Aesthetic Diorama Art Styles for Gemini Image Generation
 */
enum class DioramaStyle(
    val displayName: String,
    val description: String,
    val promptModifier: String
) {
    TILT_SHIFT_ISOMETRIC(
        displayName = "Tilt-Shift 3D",
        description = "Hyper-detailed isometric clay & resin architectural diorama",
        promptModifier = "stunning 3D miniature horizontal isometric diorama cluster, tilt-shift macro lens photography, miniature architectural model on a polished sleek base pedestal, ultra-detailed micro buildings and iconic landmarks, cute volumetric clay and acrylic materials, octane 3D render, soft studio rim lighting, vivid textures, horizontal panoramic composition, 8k resolution, photorealistic depth of field"
    ),
    CYBERPUNK_NEON(
        displayName = "Cyberpunk Hologram",
        description = "Glowing neon holographic diorama with glass skyscrapers",
        promptModifier = "futuristic cyberpunk 3D miniature horizontal diorama, glowing neon cyan and magenta lights, transparent acrylic and frosted glass skyscrapers, holographic miniature weather displays, rainy reflective micro streets with neon reflections, dark synthwave aesthetic, intricate diorama cluster, highly detailed 3D octane render"
    ),
    COZY_CLAYMATION(
        displayName = "Cozy Claymation",
        description = "Warm handcrafted stop-motion clay style diorama",
        promptModifier = "cozy handcrafted stop-motion claymation miniature horizontal diorama, tactile polymer clay textures, cute rounded architectural landmarks, warm golden ambient sunlight, miniature trees and cozy micro street details, charming tactile handmade feel, soft shadows, macro diorama photograph"
    ),
    ARCHITECTURAL_GLASS(
        displayName = "Crystal & Brass",
        description = "Modern luxury architectural model in frosted crystal & brass",
        promptModifier = "luxury architectural exhibition model, horizontal diorama cluster of landmarks made of frosted crystal, frosted glass, brushed brass, and polished dark walnut wood, sleek minimalist architectural design, soft spotlight illumination, museum exhibition quality render"
    ),
    VOXEL_WORLD(
        displayName = "Voxel Art",
        description = "Charming 3D cubic voxel miniature city skyline",
        promptModifier = "gorgeous 3D voxel art miniature horizontal city cluster, cute micro voxel landmarks and cubic buildings, dynamic isometric voxel lighting, vibrant color palette, magicavoxel style, tilt-shift voxel diorama, ambient occlusion, crisp 3D render"
    ),
    WATERCOLOR_PAPERCRAFT(
        displayName = "Origami Papercraft",
        description = "Layered pop-up 3D papercraft and watercolor diorama",
        promptModifier = "exquisite 3D layered papercraft pop-up miniature diorama, textured watercolor paper layers forming iconic architectural landmarks, soft paper shadows, gentle pastel lighting, origami craft style, depth between horizontal paper cut layers, whimsical aesthetic"
    )
}

/**
 * Diorama Generation State
 */
sealed interface DioramaState {
    data object Initial : DioramaState
    data class Generating(val promptUsed: String, val style: DioramaStyle) : DioramaState
    data class Success(
        val bitmap: Bitmap,
        val promptUsed: String,
        val style: DioramaStyle,
        val landmarkHighlights: List<String> = emptyList(),
        val timestamp: Long = System.currentTimeMillis()
    ) : DioramaState
    data class Error(val message: String, val canRetry: Boolean = true) : DioramaState
}

/**
 * AI Weather Story & Location Briefing
 */
data class AiLocationAdvisory(
    val cityName: String,
    val summary: String,
    val landmarkOverview: String,
    val outfitRecommendation: String,
    val photographyTip: String,
    val bestOutdoorTime: String,
    val miniatureFact: String
)
