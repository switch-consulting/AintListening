/*
 * Copyright 2026 Switch Consulting (https://switch-consulting.de/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.switchconsulting.aintlistening.di;

import android.content.Context;

import androidx.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import de.switchconsulting.aintlistening.data.AudioStorageManager;
import de.switchconsulting.aintlistening.data.ModelCatalogRepository;
import de.switchconsulting.aintlistening.data.PreferencesDataSource;
import de.switchconsulting.aintlistening.data.TranscriptionLocalDataSource;
import de.switchconsulting.aintlistening.data.db.AppDatabase;
import de.switchconsulting.aintlistening.data.db.TranscriptionDao;

/**
 * Hilt module for providing dependencies that have a singleton scope.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    private static final String DATABASE_NAME = "aint_listening.db";

    /**
     * Provides the singleton instance of PreferencesDataSource.
     *
     * @param context The application context.
     * @return The PreferencesDataSource instance.
     */
    @Provides
    @Singleton
    public static PreferencesDataSource providePreferencesDataSource(@ApplicationContext Context context) {
        return new PreferencesDataSource(context);
    }

    /**
     * Provides the singleton instance of AudioStorageManager.
     *
     * @param context The application context.
     * @return The AudioStorageManager instance.
     */
    @Provides
    @Singleton
    public static AudioStorageManager provideAudioStorageManager(@ApplicationContext Context context) {
        return new AudioStorageManager(context);
    }

    /**
     * Provides the singleton instance of AppDatabase.
     *
     * @param context The application context.
     * @return The AppDatabase instance.
     */
    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).build();
    }

    /**
     * Provides the singleton instance of TranscriptionDao.
     *
     * @param database The AppDatabase instance.
     * @return The TranscriptionDao instance.
     */
    @Provides
    @Singleton
    public static TranscriptionDao provideTranscriptionDao(AppDatabase database) {
        return database.transcriptionDao();
    }

    /**
     * Provides the singleton instance of TranscriptionLocalDataSource.
     *
     * @param transcriptionDao The TranscriptionDao instance.
     * @return The TranscriptionLocalDataSource instance.
     */
    @Provides
    @Singleton
    public static TranscriptionLocalDataSource provideTranscriptionLocalDataSource(TranscriptionDao transcriptionDao) {
        return new TranscriptionLocalDataSource(transcriptionDao);
    }

    /**
     * Provides the singleton instance of ModelCatalogRepository.
     *
     * @param context               The application context.
     * @param preferencesDataSource The PreferencesDataSource instance.
     * @return The ModelCatalogRepository instance.
     */
    @Provides
    @Singleton
    public static ModelCatalogRepository provideModelCatalogRepository(@ApplicationContext Context context, PreferencesDataSource preferencesDataSource) {
        return new ModelCatalogRepository(context, preferencesDataSource);
    }
}
