package com.example.dailybite.di

import android.content.Context
import androidx.room.Room
import com.example.dailybite.data.local.AppDatabase
import com.example.dailybite.data.local.PostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "dailybite.db")
            .fallbackToDestructiveMigration() // לפיתוח
            .build()

    @Provides
    fun providePostDao(db: AppDatabase): PostDao = db.postDao()
}