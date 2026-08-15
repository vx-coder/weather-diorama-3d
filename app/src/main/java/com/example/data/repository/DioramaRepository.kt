package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.AiLocationAdvisory
import com.example.data.model.DioramaStyle
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerateContentRequest
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiImageConfig
import com.example.data.model.GeminiPart
import com.example.data.model.LocationInfo
import com.example.data.model.WeatherCondition
import com.example.data.network.GeminiService
import com.example.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class DioramaRepository(
    private val geminiService: GeminiService = NetworkClient.geminiService
) {

    /**
     * Map of famous city landmark clusters to enrich prompts
     */
    private val famousLandmarks = mapOf(
        "Tokyo" to "Tokyo Tower, Tokyo Skytree, Shibuya Scramble miniature buildings, Senso-ji temple pagoda, stylized Mount Fuji backdrop, torii gate",
        "Paris" to "Eiffel Tower, Arc de Triomphe, Louvre glass pyramid, Notre-Dame cathedral, Sacré-Cœur basilica, Haussmannian apartment blocks",
        "New York" to "Empire State Building, Chrysler Building, One World Trade, Statue of Liberty, Brooklyn Bridge suspension cables, brownstone walk-ups",
        "London" to "Big Ben & Elizabeth Tower, London Eye ferris wheel, Tower Bridge, The Shard glass spire, St Paul's dome, red double-decker bus",
        "San Francisco" to "Golden Gate Bridge red towers, Transamerica Pyramid, Salesforce Tower, Painted Ladies Victorian houses, cable car on steep hill",
        "Dubai" to "Burj Khalifa spire, Burj Al Arab sail hotel, Dubai Frame, Museum of the Future torus ring, luxury desert marina skyscrapers",
        "Rome" to "Colosseum amphitheater, St. Peter's Basilica dome, Pantheon columns, Trevi Fountain stone carvings, Roman Forum marble ruins",
        "Sydney" to "Sydney Opera House white shells, Sydney Harbour Bridge arch, Sydney Tower Eye, circular quay miniature ferry boats",
        "Cairo" to "Great Pyramids of Giza, Great Sphinx stone carving, Cairo Tower lotus design, historic minarets of Old Cairo, Nile riverbank palms",
        "Rio de Janeiro" to "Christ the Redeemer statue on Corcovado peak, Sugarloaf mountain cableway, Copacabana promenade waves, vibrant favela clusters",
        "Singapore" to "Marina Bay Sands cantilever roof, Supertree Grove bio-domes, Singapore Flyer wheel, Merlion statue fountain, futuristic glass towers",
        "Toronto" to "CN Tower needle, Rogers Centre dome, modern waterfront financial towers, historic Gooderham Flatiron building",
        "Berlin" to "Brandenburg Gate columns, Berlin TV Tower sphere, Reichstag glass dome, historic city palace facades, modern Potsdamer Platz",
        "Seoul" to "N Seoul Tower on Namsan, Lotte World Tower skyscraper, Gyeongbokgung Palace gates and eaves, Han River bridges, Bukchon Hanok roofs",
        "Amsterdam" to "Historic canal step-gabled townhouses, historic drawbridge over canal, windmill, quaint bicycles lined up along cobblestone quay"
    )

    fun getLandmarksForCity(cityName: String): String {
        return famousLandmarks.entries.firstOrNull {
            cityName.contains(it.key, ignoreCase = true) || it.key.contains(cityName, ignoreCase = true)
        }?.value ?: "iconic local architectural landmarks, distinctive municipal monuments, central skyline skyscrapers, and signature heritage facades"
    }

    /**
     * Generates a 3D miniature horizontal cluster diorama using Gemini 2.5 Flash Image.
     */
    suspend fun generateDioramaImage(
        location: LocationInfo,
        condition: WeatherCondition,
        isDay: Boolean,
        style: DioramaStyle
    ): Result<Pair<Bitmap, String>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in the AI Studio Secrets panel.")
            )
        }

        val landmarks = getLandmarksForCity(location.name)
        val timeAtmosphere = if (isDay) {
            "bright daylight atmosphere with soft sunlight casting crisp miniature isometric shadows"
        } else {
            "nocturnal twilight atmosphere, starry sky backdrop, illuminated glowing warm windows and micro street lanterns"
        }

        val weatherAtmosphere = when {
            condition.isThunder -> "thunderstorm mood with dramatic dark miniature storm clouds and subtle electric lightning flash illumination"
            condition.isSnowy -> "winter snow wonderland with delicate powdery white snow resting on tiny rooftops, frost crystals, and soft cool ambient light"
            condition.isRainy -> "rainy mood with glossy wet reflective miniature cobblestone streets, micro water puddles with reflections, and delicate rain streaks"
            condition.isFoggy -> "mystical atmospheric mist and translucent low fog drifting around the base of the miniature architectural structures"
            condition.isCloudy -> "soft overcast diffusion with fluffy volumetric miniature cumulus clouds suspended playfully around tower spires"
            else -> "clear vibrant sky with warm radiant sun glow illuminating the micro architectural details"
        }

        val prompt = buildString {
            append("A ${style.promptModifier} of ${location.name}, ${location.country}. ")
            append("Featuring a horizontal clustered layout on a clean display pedestal showcasing $landmarks. ")
            append("Atmosphere: $weatherAtmosphere, $timeAtmosphere. ")
            append("Wide horizontal landscape aspect ratio, ultra high definition miniature world, masterpiece diorama composition.")
        }

        try {
            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = prompt)
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = GeminiImageConfig(
                        aspectRatio = "16:9",
                        imageSize = "1K"
                    )
                )
            )

            // Primary model for image generation as specified in gemini-api SKILL.md
            val response = geminiService.generateContent(
                model = "gemini-2.5-flash-image",
                apiKey = apiKey,
                request = request
            )

            val candidate = response.candidates?.firstOrNull()
            val imagePart = candidate?.content?.parts?.firstOrNull { it.inlineData != null }

            if (imagePart?.inlineData != null) {
                val base64Data = imagePart.inlineData.data
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = false
                }
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                if (bitmap != null) {
                    Result.success(Pair(bitmap, prompt))
                } else {
                    Result.failure(IllegalStateException("Failed to decode generated diorama image"))
                }
            } else {
                // If text was returned or no image part
                val textPart = candidate?.content?.parts?.firstOrNull { it.text != null }?.text
                Result.failure(
                    IllegalStateException(
                        textPart ?: "No image content returned from Gemini model"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates an AI Weather Story, Landmark guide, and Travel advisory using Gemini 3.5 Flash.
     */
    suspend fun generateLocationAdvisory(
        location: LocationInfo,
        condition: WeatherCondition,
        tempCelsius: Double,
        isDay: Boolean
    ): Result<AiLocationAdvisory> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Return fallback advisory without failing the UI
            return@withContext Result.success(
                generateFallbackAdvisory(location, condition, tempCelsius, isDay)
            )
        }

        val prompt = """
            You are an expert architectural historian and meteorological travel advisor.
            Location: ${location.name}, ${location.country}
            Current Weather: ${condition.title} (${tempCelsius.toInt()}°C / ${(tempCelsius * 9 / 5 + 32).toInt()}°F), ${if (isDay) "Daytime" else "Night"}
            
            Provide a short, structured JSON response with these exact keys:
            {
              "summary": "1-2 sentence atmospheric summary of ${location.name} in this weather",
              "landmarkOverview": "A description of the iconic landmarks in this city's skyline diorama",
              "outfitRecommendation": "Practical clothing recommendation for this exact weather condition",
              "photographyTip": "Lighting and angle tip for photographing these landmarks in today's weather",
              "bestOutdoorTime": "Best time of day to enjoy outdoor sights today",
              "miniatureFact": "One fun trivia fact about the city's architectural landmarks"
            }
            Respond ONLY with the JSON object.
        """.trimIndent()

        try {
            val request = GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.4f
                )
            )

            val response = geminiService.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val jsonClean = rawText.substringAfter("{").substringBeforeLast("}")
            if (jsonClean.isNotBlank()) {
                val fullJson = "{$jsonClean}"
                val advisory = parseAdvisoryJson(fullJson, location.name)
                Result.success(advisory)
            } else {
                Result.success(generateFallbackAdvisory(location, condition, tempCelsius, isDay))
            }
        } catch (e: Exception) {
            Result.success(generateFallbackAdvisory(location, condition, tempCelsius, isDay))
        }
    }

    private fun parseAdvisoryJson(json: String, cityName: String): AiLocationAdvisory {
        fun extractKey(key: String): String {
            val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            return regex.find(json)?.groupValues?.get(1) ?: ""
        }

        val summary = extractKey("summary").ifBlank { "$cityName is experiencing distinctive weather across its iconic architectural landscape." }
        val landmarkOverview = extractKey("landmarkOverview").ifBlank { getLandmarksForCity(cityName) }
        val outfit = extractKey("outfitRecommendation").ifBlank { "Dress comfortably in layered clothing." }
        val photo = extractKey("photographyTip").ifBlank { "Capture the skyline with low-angle warm light." }
        val bestTime = extractKey("bestOutdoorTime").ifBlank { "Mid-afternoon for golden highlights." }
        val fact = extractKey("miniatureFact").ifBlank { "$cityName features architectural marvels recognized worldwide." }

        return AiLocationAdvisory(
            cityName = cityName,
            summary = summary,
            landmarkOverview = landmarkOverview,
            outfitRecommendation = outfit,
            photographyTip = photo,
            bestOutdoorTime = bestTime,
            miniatureFact = fact
        )
    }

    private fun generateFallbackAdvisory(
        location: LocationInfo,
        condition: WeatherCondition,
        tempCelsius: Double,
        isDay: Boolean
    ): AiLocationAdvisory {
        val landmarks = getLandmarksForCity(location.name)
        val outfit = when {
            condition.isRainy -> "Waterproof jacket, compact umbrella, and water-resistant footwear."
            condition.isSnowy -> "Insulated winter coat, thermal layers, woolen scarf, and snow boots."
            tempCelsius > 25.0 -> "Breathable cotton/linen clothing, sunglasses, and UV protection."
            tempCelsius < 10.0 -> "Warm fleece jacket, cozy knit sweater, and windproof outer layer."
            else -> "Comfortable seasonal layers, light cardigan or windbreaker."
        }

        val photoTip = if (isDay) {
            "Aim for Golden Hour just before sunset for warm isometric shadows highlighting landmark facades."
        } else {
            "Use a steady support to capture illuminated glowing spires and night sky reflections."
        }

        return AiLocationAdvisory(
            cityName = location.name,
            summary = "Atmospheric ${condition.title.lowercase()} over the miniature skyline of ${location.name} at ${tempCelsius.toInt()}°C.",
            landmarkOverview = "The diorama cluster highlights $landmarks.",
            outfitRecommendation = outfit,
            photographyTip = photoTip,
            bestOutdoorTime = if (condition.isRainy) "Indoor museums and covered historic arcades" else "Morning stroll between 9 AM and 11 AM",
            miniatureFact = "${location.name}'s distinctive architectural silhouette is one of the most recognizable skyline clusters in ${location.country}."
        )
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            null
        }
    }
}
