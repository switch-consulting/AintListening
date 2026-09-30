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

package de.switchconsulting.aintlistening.data;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Repository for managing transcription state persistence, temporary audio cache files,
 * audio chunk storage, and user preferences.
 */
@Singleton
public class TranscriptionRepository {

    private final TranscriptionLocalDataSource localDataSource;
    private final AudioStorageManager audioStorageManager;
    private final PreferencesDataSource preferencesDataSource;

    /**
     * Constructs a new TranscriptionRepository with injected data sources.
     *
     * @param localDataSource       The transcription local data source.
     * @param audioStorageManager    The audio storage manager.
     * @param preferencesDataSource The preferences data source.
     */
    @Inject
    public TranscriptionRepository(
            TranscriptionLocalDataSource localDataSource,
            AudioStorageManager audioStorageManager,
            PreferencesDataSource preferencesDataSource
    ) {
        this.localDataSource = localDataSource;
        this.audioStorageManager = audioStorageManager;
        this.preferencesDataSource = preferencesDataSource;
    }

    /**
     * Loads the last saved transcription paragraphs.
     *
     * @return List of transcription paragraphs, or null if none exist.
     */
    public List<TranscriptionParagraph> loadLastMessage() {
        return localDataSource.loadLastMessage();
    }

    /**
     * Persists the completed list of transcription paragraphs and associated language locale.
     *
     * @param paragraphs The paragraphs to save.
     * @param locale     The language locale used for transcription.
     */
    public void saveLastMessage(List<TranscriptionParagraph> paragraphs, Locale locale) {
        localDataSource.saveLastMessage(paragraphs, locale);
    }

    /**
     * Returns the temporary incoming WAV file location.
     *
     * @return The File object for the incoming audio.
     */
    public File getIncomingWavFile() {
        return audioStorageManager.getIncomingWavFile();
    }

    /**
     * Clears temporary audio files and chunk directories.
     */
    public void clearTemporaryFiles() {
        audioStorageManager.clearTemporaryFiles();
    }

    /**
     * Saves raw PCM data as an audio chunk WAV file.
     *
     * @param pcmData The PCM data bytes.
     * @param index   The chunk index.
     * @return Absolute path of the saved chunk file.
     * @throws IOException If saving fails.
     */
    public String saveAudioChunk(byte[] pcmData, int index) throws IOException {
        return audioStorageManager.saveAudioChunk(pcmData, index);
    }

    /**
     * Returns the underlying PreferencesDataSource instance for UI and configuration checks.
     *
     * @return The preferences data source.
     */
    public PreferencesDataSource getPreferencesDataSource() {
        return preferencesDataSource;
    }
}
