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
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import de.switchconsulting.aintlistening.data.AudioStorageManager;
import de.switchconsulting.aintlistening.data.ModelCatalogRepository;
import de.switchconsulting.aintlistening.data.PreferencesDataSource;
import de.switchconsulting.aintlistening.data.TranscriptionLocalDataSource;
import de.switchconsulting.aintlistening.transcription.TranscriberRegistry;
import de.switchconsulting.aintlistening.transcription.VoskTranscriber;
import de.switchconsulting.aintlistening.transcription.WhisperTranscriber;

import javax.inject.Singleton;

/**
 * Hilt module for providing dependencies that have a singleton scope.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

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
     * Provides the singleton instance of TranscriptionLocalDataSource.
     *
     * @param context The application context.
     * @return The TranscriptionLocalDataSource instance.
     */
    @Provides
    @Singleton
    public static TranscriptionLocalDataSource provideTranscriptionLocalDataSource(@ApplicationContext Context context) {
        return new TranscriptionLocalDataSource(context);
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

    /**
     * Provides the singleton instance of the TranscriberRegistry.
     *
     * @param voskTranscriber    The Vosk transcriber instance.
     * @param whisperTranscriber The Whisper transcriber instance.
     * @return The TranscriberRegistry instance.
     */
    @Provides
    @Singleton
    public static TranscriberRegistry provideTranscriberRegistry(VoskTranscriber voskTranscriber, WhisperTranscriber whisperTranscriber) {
        return new TranscriberRegistry(voskTranscriber, whisperTranscriber);
    }
}
