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

package de.switchconsulting.aintlistening.ui;

import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelInfo;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Interface for handling interactions with model items in the list.
 */
public interface ModelInteractionListener {
    /**
     * Called when the download button is clicked for a model.
     *
     * @param info The model information.
     */
    void onDownloadClicked(ModelInfo info);

    /**
     * Called when the delete button is clicked for a model.
     *
     * @param info The model information.
     */
    void onDeleteClicked(ModelInfo info);

    /**
     * Called when the enabled state of a language is toggled.
     *
     * @param language The language support information.
     * @param enabled  True if enabled, false otherwise.
     */
    void onLanguageEnabledChanged(LanguageSupport language, boolean enabled);

    /**
     * Called when a specific transcriber is selected for a language.
     *
     * @param language The language support information.
     * @param type     The selected transcriber type.
     */
    void onTranscriberSelected(LanguageSupport language, TranscriberType type);

    /**
     * Called when smart formatting is toggled for a language.
     *
     * @param language The language support information.
     * @param enabled  True if enabled, false otherwise.
     */
    void onSmartFormattingToggled(LanguageSupport language, boolean enabled);
}
