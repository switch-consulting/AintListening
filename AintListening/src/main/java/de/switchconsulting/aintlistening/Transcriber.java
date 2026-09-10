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

package de.switchconsulting.aintlistening;

import android.content.Context;
import java.io.File;
import java.util.List;

/**
 * Interface for speech-to-text transcription.
 */
public interface Transcriber {
    void ensureModelLoaded(Context context, int modelIndex) throws Exception;
    List<TranscriptionParagraph> transcribe(Context context, File wavFile, TranscriptionListener listener) throws Exception;
    void close();
}
