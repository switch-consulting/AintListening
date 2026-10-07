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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Repository for accessing speech model metadata, checking model installation status,
 * resolving active transcribers, and deleting model files from device storage.
 */
@Singleton
public class ModelCatalogRepository {
    private final Context context;
    private final PreferencesDataSource preferencesDataSource;

    @Inject
    public ModelCatalogRepository(@ApplicationContext Context context, PreferencesDataSource preferencesDataSource) {
        this.context = context;
        this.preferencesDataSource = preferencesDataSource;
    }

    /**
     * Returns the list of supported languages.
     *
     * @return The list of supported languages.
     */
    public List<LanguageSupport> getSupportedLanguages() {
        return List.of(ModelCatalog.SUPPORTED_LANGUAGES);
    }

    /**
     * Resolves the LanguageSupport instance for a given locale.
     *
     * @param locale The target locale.
     * @return The matching LanguageSupport instance, or null if not found.
     */
    @Nullable
    public LanguageSupport getLanguageSupport(@Nullable Locale locale) {
        return ModelCatalog.getLanguageSupport(locale);
    }

    /**
     * Checks if a model is downloaded and present on device storage.
     *
     * @param info The model metadata.
     * @return True if present on disk, false otherwise.
     */
    public boolean isModelDownloaded(@Nullable ModelInfo info) {
        if (info == null) return false;
        File modelFile = new File(context.getFilesDir(), info.name());
        if (info.isZip()) {
            return modelFile.exists() && modelFile.isDirectory();
        } else {
            return modelFile.exists() && modelFile.isFile();
        }
    }

    /**
     * Checks if the model for a specific transcriber type is downloaded for a language.
     *
     * @param language The language support object.
     * @param type     The transcriber type.
     * @return True if downloaded, false otherwise.
     */
    public boolean isDownloaded(@Nullable LanguageSupport language, @Nullable TranscriberType type) {
        if (language == null || type == null) return false;
        ModelInfo info = language.getModel(type);
        return info != null && isModelDownloaded(info);
    }

    /**
     * Checks if the formatting model for a language is downloaded.
     *
     * @param language The language support object.
     * @return True if downloaded, false otherwise.
     */
    public boolean isFormattingDownloaded(@Nullable LanguageSupport language) {
        if (language == null) return false;
        return isModelDownloaded(language.getFormattingModel());
    }

    /**
     * Checks if at least one transcription model is downloaded for a language.
     *
     * @param language The language support object.
     * @return True if downloaded.
     */
    public boolean hasTranscriptionModelDownloaded(@Nullable LanguageSupport language) {
        if (language == null) return false;
        for (TranscriberType type : TranscriberType.values()) {
            if (isDownloaded(language, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the active transcriber type for a language based on user preference and model availability.
     *
     * @param language The language support object.
     * @return The active TranscriberType.
     */
    @NonNull
    public TranscriberType getActiveTranscriberType(@NonNull LanguageSupport language) {
        TranscriberType preferred = preferencesDataSource.getTranscriberType(language.getLocale());
        if (isDownloaded(language, preferred)) {
            return preferred;
        }

        for (ModelInfo info : language.getTranscriptionModels()) {
            if (info != null && isModelDownloaded(info)) {
                return info.type();
            }
        }

        return preferencesDataSource.getDefaultTranscriberType();
    }

    /**
     * Returns a list of language display names for all languages where at least one transcription model is downloaded
     * and the language is enabled in preferences.
     *
     * @return A list of available language display names.
     */
    public List<String> getAvailableLanguageNames() {
        List<String> available = new ArrayList<>();
        for (LanguageSupport language : ModelCatalog.SUPPORTED_LANGUAGES) {
            if (!preferencesDataSource.isLanguageEnabled(language.getLocale())) {
                continue;
            }
            if (hasTranscriptionModelDownloaded(language)) {
                available.add(language.getLocale().getDisplayName());
            }
        }
        return available;
    }

    /**
     * Deletes the model files for the specified model.
     *
     * @param info The model information to delete.
     * @return True if successfully deleted.
     */
    public boolean deleteModel(@Nullable ModelInfo info) {
        if (info == null) return false;
        File modelDir = new File(context.getFilesDir(), info.name());
        return deleteRecursive(modelDir);
    }

    /**
     * Recursively deletes a file or directory and all its contents.
     *
     * @param fileOrDirectory The file or directory to delete.
     * @return True if deletion was successful, false otherwise.
     */
    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    /**
     * Returns the string resource ID for the human-readable engine name of the specified transcriber type.
     *
     * @param type The transcriber type.
     * @return The string resource ID.
     */
    public int getEngineNameResId(TranscriberType type) {
        return ModelCatalog.getEngineNameResId(type);
    }
}
