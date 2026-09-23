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

/**
 * Value object representing UI display preferences for transcription paragraphs.
 *
 * @param showPlaybackButton True if the audio playback button should be shown.
 * @param showCopyButton     True if the copy text button should be shown.
 * @param showRawText        True if raw transcribed text view toggle should be enabled.
 * @param showSmartText      True if smart formatted text view toggle should be enabled.
 */
public record UiDisplaySettings(boolean showPlaybackButton, boolean showCopyButton,
                                boolean showRawText, boolean showSmartText) {
    /**
     * Constructs a new UiDisplaySettings instance.
     *
     * @param showPlaybackButton True to show audio playback button.
     * @param showCopyButton     True to show copy text button.
     * @param showRawText        True to show raw text toggle.
     * @param showSmartText      True to show smart formatted text toggle.
     */
    public UiDisplaySettings {
    }

    /**
     * Creates a default display settings instance with all options enabled.
     *
     * @return The default display settings.
     */
    public static UiDisplaySettings defaultSettings() {
        return new UiDisplaySettings(true, true, true, true);
    }
}
