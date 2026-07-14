package com.vibecode.ide.data.local.dao

import androidx.room.*
import com.vibecode.ide.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastOpenedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE rootPath = :path LIMIT 1")
    suspend fun getByPath(path: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("UPDATE projects SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: String, timestamp: Long)

    @Query("UPDATE projects SET selectedProviderId = :providerId, selectedModelId = :modelId WHERE id = :id")
    suspend fun updateSelection(id: String, providerId: String?, modelId: String?)

    @Delete
    suspend fun delete(project: ProjectEntity)
}
