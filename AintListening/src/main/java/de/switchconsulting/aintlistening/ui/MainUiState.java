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

import java.util.List;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Represents the UI state for the main transcription screen.
 *
 * @param isLoading       True if a transcription or loading process is ongoing.
 * @param isIndeterminate True if the ongoing loading process has indeterminate progress.
 * @param progress        The current progress value.
 * @param maxProgress     The maximum progress value.
 * @param statusMessage   An optional status message describing the current operation.
 * @param paragraphs      The list of transcription paragraphs to display.
 * @param errorMessage    An optional error message if an error occurred.
 */
public record MainUiState(boolean isLoading, boolean isIndeterminate, int progress, int maxProgress,
                          String statusMessage, List<TranscriptionParagraph> paragraphs,
                          String errorMessage) {
    /**
     * Constructs a new MainUiState.
     *
     * @param isLoading       Whether the state is loading.
     * @param isIndeterminate Whether the progress is indeterminate.
     * @param progress        The current progress.
     * @param maxProgress     The max progress.
     * @param statusMessage   The status message.
     * @param paragraphs      The list of paragraphs.
     * @param errorMessage    The error message.
     */
    public MainUiState {
    }

    public static MainUiState idle(List<TranscriptionParagraph> paragraphs) {
        return new MainUiState(false, false, 0, 0, null, paragraphs, null);
    }

    public static MainUiState loading(String message, List<TranscriptionParagraph> paragraphs) {
        return new MainUiState(true, true, 0, 0, message, paragraphs, null);
    }

    public static MainUiState progress(String message, int progress, int max, List<TranscriptionParagraph> paragraphs) {
        return new MainUiState(true, false, progress, max, message, paragraphs, null);
    }

    public static MainUiState error(String message, List<TranscriptionParagraph> paragraphs) {
        return new MainUiState(false, false, 0, 0, message, paragraphs, message);
    }
}
