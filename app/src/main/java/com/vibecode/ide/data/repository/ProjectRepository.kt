package com.vibecode.ide.data.repository

import com.vibecode.ide.data.local.dao.ProjectDao
import com.vibecode.ide.data.local.entity.ProjectEntity
import com.vibecode.ide.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
) {
    fun observeRecents(): Flow<List<Project>> =
        projectDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getProject(id: String): Project? = projectDao.getById(id)?.toDomain()

    /** Registers a folder on disk as a project (creating it if it doesn't already exist). */
    suspend fun openOrCreateProject(name: String, rootPath: String): Project {
        val dir = File(rootPath)
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (!created && !dir.exists()) {
                throw java.io.IOException(
                    "Could not create '$rootPath'. This usually means the app doesn't have " +
                        "storage access yet — grant it from the banner on the Home screen.",
                )
            }
        }

        val existing = projectDao.getByPath(rootPath)
        val now = System.currentTimeMillis()
        val entity = existing?.copy(lastOpenedAt = now) ?: ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            rootPath = rootPath,
            createdAt = now,
            lastOpenedAt = now,
        )
        projectDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun touch(id: String) = projectDao.touch(id, System.currentTimeMillis())

    suspend fun updateSelectedModel(id: String, providerId: String?, modelId: String?) =
        projectDao.updateSelection(id, providerId, modelId)

    suspend fun deleteProject(project: Project, alsoDeleteFiles: Boolean) {
        projectDao.getById(project.id)?.let { projectDao.delete(it) }
        if (alsoDeleteFiles) {
            File(project.rootPath).deleteRecursively()
        }
    }

    private fun ProjectEntity.toDomain() = Project(
        id = id, name = name, rootPath = rootPath, createdAt = createdAt,
        lastOpenedAt = lastOpenedAt, selectedProviderId = selectedProviderId, selectedModelId = selectedModelId,
    )
}
