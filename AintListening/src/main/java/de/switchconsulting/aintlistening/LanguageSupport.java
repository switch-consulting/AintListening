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

package de.switchconsulting.aintlistening;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

/**
 * Container class that groups all model information for a specific language.
 * It includes the transcription model and an optional formatting model.
 * It uses a {@link ModelManager} to check for model availability.
 */
public class LanguageSupport {
    /** The language identifier, serving as the primary key. */
    private final String language;
    /** The metadata for the speech-to-text transcription model. */
    private final ModelInfo transcriptionModel;
    /** The metadata for the smart formatting (punctuation) model. */
    private final ModelInfo formattingModel;
    /** The manager used to check for model presence. */
    private final ModelManager modelManager;

    /**
     * Constructs a new LanguageSupport instance.
     *
     * @param language           The language identifier (primary key).
     * @param transcriptionModel The transcription model information.
     * @param formattingModel    The formatting model information, or null if not supported.
     * @param modelManager       The manager to delegate model checks to.
     */
    LanguageSupport(@NonNull String language,
                    @NonNull ModelInfo transcriptionModel,
                    @Nullable ModelInfo formattingModel,
                    @NonNull ModelManager modelManager) {
        this.language = language;
        this.transcriptionModel = transcriptionModel;
        this.formattingModel = formattingModel;
        this.modelManager = modelManager;
    }

    /**
     * @return The language identifier.
     */
    @NonNull
    public String getLanguage() {
        return language;
    }

    /**
     * @return The transcription model information.
     */
    @NonNull
    public ModelInfo getTranscriptionModel() {
        return transcriptionModel;
    }

    /**
     * @return The formatting model information, or null if not supported for this language.
     */
    @Nullable
    public ModelInfo getFormattingModel() {
        return formattingModel;
    }

    /**
     * Checks if the transcription model for this language is downloaded.
     *
     * @param context The context.
     * @return True if downloaded, false otherwise.
     */
    public boolean isTranscriptionDownloaded(@NonNull Context context) {
        return modelManager.isModelDownloaded(context, transcriptionModel);
    }

    /**
     * Checks if the formatting model for this language is downloaded.
     *
     * @param context The context.
     * @return True if downloaded, false otherwise (e.g. if not supported).
     */
    public boolean isFormattingDownloaded(@NonNull Context context) {
        return modelManager.isModelDownloaded(context, formattingModel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanguageSupport that = (LanguageSupport) o;
        return Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language);
    }
}
