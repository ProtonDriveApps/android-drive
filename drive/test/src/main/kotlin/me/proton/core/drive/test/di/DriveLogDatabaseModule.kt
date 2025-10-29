package me.proton.core.drive.test.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.log.data.db.DriveLogDatabase
import me.proton.core.drive.log.data.db.LogDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveLogDatabaseModule {
    @Provides
    @Singleton
    fun provideDriveLogDatabase(@ApplicationContext context: Context): DriveLogDatabase =
        Room.inMemoryDatabaseBuilder(context, DriveLogDatabase::class.java).build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DriveLogDatabaseBindsModule {

    @Binds
    abstract fun provideLogDatabase(db: DriveLogDatabase): LogDatabase
}
