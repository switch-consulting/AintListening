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

/**
 * Represents a partial or final recognition result containing transcribed text
 * and segment start/end timestamps both in milliseconds since the start of the audio file.
 *
 * @param text    The transcribed text of the segment.
 * @param startMs The start time of the segment in milliseconds since the start of the audio file.
 * @param endMs   The end time of the segment in milliseconds since the start of the audio file.
 */
public record PartialRecognitionResult(
        @NonNull String text,
        long startMs,
        long endMs
) {
}
