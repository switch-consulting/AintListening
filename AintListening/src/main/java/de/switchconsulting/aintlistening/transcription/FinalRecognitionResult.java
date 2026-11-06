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

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

/**
 * Represents the final recognition result containing all partial recognition results,
 * the input audio file used for transcription, and the total audio length in milliseconds.
 *
 * @param partialResults The list of all partial recognition results.
 * @param audioFile      The converted audio file used as input for transcription.
 * @param totalLengthMs  The total length in milliseconds of the audio file.
 */
public record FinalRecognitionResult(
        @NonNull List<PartialRecognitionResult> partialResults,
        @NonNull File audioFile,
        long totalLengthMs
) {
    /**
     * Calculates and returns the full transcription text out of the texts from the partial recognition results.
     *
     * @return The concatenated full transcription text.
     */
    @NonNull
    public String getFullTranscription() {
        StringBuilder sb = new StringBuilder();
        for (PartialRecognitionResult result : partialResults) {
            String text = result.text().trim();
            if (!text.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }
}
