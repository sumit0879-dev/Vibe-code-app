package com.vibecode.ide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vibecode.ide.data.local.dao.ChatDao
import com.vibecode.ide.data.local.dao.ModelDao
import com.vibecode.ide.data.local.dao.ProjectDao
import com.vibecode.ide.data.local.dao.ProviderDao
import com.vibecode.ide.data.local.entity.ChatMessageEntity
import com.vibecode.ide.data.local.entity.ChatSessionEntity
import com.vibecode.ide.data.local.entity.ModelEntity
import com.vibecode.ide.data.local.entity.ProjectEntity
import com.vibecode.ide.data.local.entity.ProviderEntity

@Database(
    entities = [
        ProjectEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        ProviderEntity::class,
        ModelEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chatDao(): ChatDao
    abstract fun providerDao(): ProviderDao
    abstract fun modelDao(): ModelDao

    companion object {
        const val DATABASE_NAME = "vibecode_ai.db"
    }
}
