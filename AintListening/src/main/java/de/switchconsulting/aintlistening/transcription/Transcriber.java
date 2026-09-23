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

import android.content.Context;
import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Interface for speech-to-text transcription services.
 */
public interface Transcriber {
    /**
     * Ensures that the transcription model for the specified locale is loaded and ready.
     *
     * @param context The application context.
     * @param locale  The locale of the target language model.
     * @throws Exception if model loading fails.
     */
    void ensureModelLoaded(Context context, Locale locale) throws Exception;

    /**
     * Transcribes the provided audio file.
     *
     * @param context  The application context.
     * @param wavFile  The WAV audio file to transcribe.
     * @param listener A listener to receive transcription updates.
     * @return A list of transcribed paragraphs.
     * @throws Exception if transcription fails.
     */
    List<TranscriptionParagraph> transcribe(Context context, File wavFile, TranscriptionListener listener) throws Exception;

    /**
     * Returns the type of the transcriber.
     *
     * @return The transcriber type.
     */
    TranscriberType getType();

    /**
     * Returns whether this transcriber provides punctuation and casing in its output.
     *
     * @return True if the engine provides punctuation, false otherwise.
     */
    boolean providesPunctuation();

    /**
     * Releases any resources held by the transcriber.
     */
    void close();
}
