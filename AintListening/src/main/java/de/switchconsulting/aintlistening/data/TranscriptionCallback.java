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

import java.util.List;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Callback interface for monitoring the transcription process.
 */
public interface TranscriptionCallback {
    /**
     * Called when there is a status update message.
     *
     * @param message The status message string.
     */
    void onStatusUpdate(String message);

    /**
     * Called when a partial transcription result becomes available.
     *
     * @param paragraphs The list of partial transcription paragraphs.
     */
    void onPartialResult(List<TranscriptionParagraph> paragraphs);

    /**
     * Called to report progress on smart text formatting.
     *
     * @param progress The current progress value.
     * @param total    The total expected value for complete progress.
     * @param paragraphs The current list of transcription paragraphs.
     */
    void onSmartFormattingProgress(int progress, int total, List<TranscriptionParagraph> paragraphs);

    /**
     * Called when the transcription and formatting are completely finished.
     *
     * @param paragraphs The final list of complete transcription paragraphs.
     */
    void onComplete(List<TranscriptionParagraph> paragraphs);

    /**
     * Called when an error occurs during any phase of transcription.
     *
     * @param message The error message string.
     */
    void onError(String message);
}
