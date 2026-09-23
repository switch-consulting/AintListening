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

import java.util.EnumMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Registry that manages and provides access to different transcription engine implementations.
 */
@Singleton
public class TranscriberRegistry {

    private final Map<TranscriberType, Transcriber> transcribers = new EnumMap<>(TranscriberType.class);

    @Inject
    public TranscriberRegistry(VoskTranscriber voskTranscriber, WhisperTranscriber whisperTranscriber) {
        transcribers.put(TranscriberType.VOSK, voskTranscriber);
        transcribers.put(TranscriberType.WHISPER, whisperTranscriber);
    }

    /**
     * Returns the transcriber implementation for the specified type.
     *
     * @param type The transcriber type.
     * @return The transcriber instance, or null if not registered.
     */
    public Transcriber getTranscriber(TranscriberType type) {
        return transcribers.get(type);
    }

    /**
     * Releases resources for all registered transcribers.
     */
    public void closeAll() {
        for (Transcriber transcriber : transcribers.values()) {
            transcriber.close();
        }
    }
}
