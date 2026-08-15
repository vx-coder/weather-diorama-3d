package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String, // e.g. "Paris_France" or "35.68_139.76"
    val name: String,
    val country: String,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?,
    val isFavorite: Boolean = false,
    val lastViewedTimestamp: Long = System.currentTimeMillis(),
    val cachedDioramaBase64: String? = null,
    val cachedDioramaPrompt: String? = null,
    val cachedDioramaStyle: String? = null
)
