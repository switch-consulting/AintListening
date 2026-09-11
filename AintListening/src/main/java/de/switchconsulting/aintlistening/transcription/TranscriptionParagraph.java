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

package de.switchconsulting.aintlistening.transcription;

/**
 * Represents a single paragraph of transcription, which can be toggled between
 * raw transcribed text and smart formatted text.
 */
public class TranscriptionParagraph {
    private final String rawText;
    private final String formattedText;
    private final String audioFilePath;
    private boolean showFormatted;

    /**
     * Constructs a new TranscriptionParagraph with raw and optionally formatted text.
     *
     * @param rawText       The raw transcription text.
     * @param formattedText The smart-formatted text, or null if not available.
     */
    public TranscriptionParagraph(String rawText, String formattedText) {
        this(rawText, formattedText, null);
    }

    /**
     * Constructs a new TranscriptionParagraph with raw text, formatted text, and an audio file path.
     *
     * @param rawText       The raw transcription text.
     * @param formattedText The smart-formatted text, or null if not available.
     * @param audioFilePath The absolute path to the associated audio chunk file.
     */
    public TranscriptionParagraph(String rawText, String formattedText, String audioFilePath) {
        this.rawText = rawText;
        this.formattedText = formattedText;
        this.audioFilePath = audioFilePath;
        this.showFormatted = formattedText != null && !formattedText.isEmpty();
    }

    /**
     * @return The raw transcription text.
     */
    public String getRawText() {
        return rawText;
    }

    /**
     * @return The smart-formatted text, or null if not available.
     */
    public String getFormattedText() {
        return formattedText;
    }

    /**
     * @return The absolute path to the audio file chunk.
     */
    public String getAudioFilePath() {
        return audioFilePath;
    }

    /**
     * @return True if the formatted text should be displayed, false for raw text.
     */
    public boolean isShowFormatted() {
        return showFormatted;
    }

    /**
     * Sets whether to show the formatted or raw text.
     *
     * @param showFormatted True to show formatted text.
     */
    public void setShowFormatted(boolean showFormatted) {
        this.showFormatted = showFormatted;
    }

    /**
     * Returns the text to be displayed based on the current toggle state.
     *
     * @return The text to display.
     */
    public String getDisplayText() {
        if (showFormatted && formattedText != null && !formattedText.isEmpty()) {
            return formattedText;
        }
        return rawText;
    }
}
