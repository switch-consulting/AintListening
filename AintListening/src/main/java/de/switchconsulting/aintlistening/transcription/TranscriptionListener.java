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
 * Interface for receiving transcription results.
 */
public interface TranscriptionListener {
    /**
     * Called when a partial recognition result is available.
     *
     * @param result The partial recognition result containing text and timestamps.
     */
    void onPartialRecognition(@NonNull PartialRecognitionResult result);

    /**
     * Called when a final transcription result is available.
     *
     * @param result The final recognition result containing partial results, audio file, and total length.
     */
    void onResult(@NonNull FinalRecognitionResult result);

    /**
     * Called when a new audio chunk is ready to be saved.
     *
     * @param pcmData    The raw PCM data for the chunk.
     * @param chunkIndex The index of the chunk.
     * @return The absolute path where the chunk was saved, or null if it couldn't be saved.
     */
    String onAudioChunkAvailable(byte[] pcmData, int chunkIndex);
}
