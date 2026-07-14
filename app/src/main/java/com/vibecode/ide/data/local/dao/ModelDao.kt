package com.vibecode.ide.data.local.dao

import androidx.room.*
import com.vibecode.ide.data.local.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models WHERE providerId = :providerId ORDER BY isFavorite DESC, displayName ASC")
    fun observeForProvider(providerId: String): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models ORDER BY isFavorite DESC, displayName ASC")
    fun observeAll(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getById(id: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: ModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(models: List<ModelEntity>)

    @Delete
    suspend fun delete(model: ModelEntity)

    @Query("DELETE FROM models WHERE providerId = :providerId AND source = 'DISCOVERED'")
    suspend fun clearDiscovered(providerId: String)
}
