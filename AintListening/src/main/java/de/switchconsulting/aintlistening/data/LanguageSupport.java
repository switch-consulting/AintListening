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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Container class that groups all model information for a specific language.
 * It includes the transcription model and an optional formatting model.
 */
public class LanguageSupport {
    /** The locale of the language, serving as the primary key. */
    private final Locale locale;
    /** The metadata for the transcription models, keyed by type. */
    private final Map<TranscriberType, ModelInfo> transcriptionModels = new EnumMap<>(TranscriberType.class);
    /** The metadata for the smart formatting (punctuation) model. */
    private final ModelInfo formattingModel;

    /**
     * Constructs a new LanguageSupport instance.
     *
     * @param locale              The locale of the language (primary key).
     * @param transcriptionModels The map of transcription models.
     * @param formattingModel     The formatting model information, or null if not supported.
     */
    public LanguageSupport(@NonNull Locale locale,
                           @NonNull Map<TranscriberType, ModelInfo> transcriptionModels,
                           @Nullable ModelInfo formattingModel) {
        this.locale = locale;
        this.transcriptionModels.putAll(transcriptionModels);
        this.formattingModel = formattingModel;
    }

    /**
     * Returns the locale of the language.
     *
     * @return The locale of the language.
     */
    @NonNull
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the model for the specified transcriber type.
     *
     * @param type The transcriber type.
     * @return The model information, or null if not supported.
     */
    @Nullable
    public ModelInfo getModel(TranscriberType type) {
        return transcriptionModels.get(type);
    }

    /**
     * Returns all available transcription models for this language.
     *
     * @return All available transcription models for this language.
     */
    public Collection<ModelInfo> getTranscriptionModels() {
        return transcriptionModels.values();
    }

    /**
     * Returns the formatting model information for this language.
     *
     * @return The formatting model information, or null if not supported for this language.
     */
    @Nullable
    public ModelInfo getFormattingModel() {
        return formattingModel;
    }

    /**
     * Compares this LanguageSupport instance with another object based on locale identity.
     *
     * @param o The object to compare with.
     * @return True if the specified object is a LanguageSupport instance with an equal locale.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanguageSupport that = (LanguageSupport) o;
        return Objects.equals(locale, that.locale);
    }

    /**
     * Returns a hash code value for this LanguageSupport instance based on locale identity.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(locale);
    }
}
