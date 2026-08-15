package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM saved_locations ORDER BY lastViewedTimestamp DESC")
    fun getAllSavedLocations(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocationEntity)

    @Update
    suspend fun updateLocation(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocation(id: String)

    @Query("UPDATE saved_locations SET cachedDioramaBase64 = :base64, cachedDioramaPrompt = :prompt, cachedDioramaStyle = :style WHERE id = :id")
    suspend fun updateDioramaCache(id: String, base64: String, prompt: String, style: String)
}
