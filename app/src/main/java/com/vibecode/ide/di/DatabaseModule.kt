package com.vibecode.ide.di

import android.content.Context
import androidx.room.Room
import com.vibecode.ide.data.local.AppDatabase
import com.vibecode.ide.data.local.dao.ChatDao
import com.vibecode.ide.data.local.dao.ModelDao
import com.vibecode.ide.data.local.dao.ProjectDao
import com.vibecode.ide.data.local.dao.ProviderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()
    @Provides fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
    @Provides fun provideProviderDao(db: AppDatabase): ProviderDao = db.providerDao()
    @Provides fun provideModelDao(db: AppDatabase): ModelDao = db.modelDao()
}
