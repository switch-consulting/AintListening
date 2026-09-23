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

import de.switchconsulting.aintlistening.transcription.TranscriberType;
import java.util.Locale;

/**
 * Data class containing metadata for a speech or formatting model.
 *
 * @param name   The internal name/directory name of the model.
 * @param url    The URL where the model file can be downloaded.
 * @param locale The locale of the language.
 * @param size   The approximate download size of the model (e.g., "45MB").
 * @param type   The type of transcription engine this model is for.
 * @param isZip  Whether the downloaded file is a zip that needs extraction.
 */
public record ModelInfo(String name, String url, Locale locale, String size, TranscriberType type,
                        boolean isZip) {
    /**
     * Constructs a new ModelInfo.
     *
     * @param name   The internal name of the model.
     * @param url    The download URL.
     * @param locale The locale of the language.
     * @param size   The download size.
     * @param type   The transcriber type.
     * @param isZip  Whether the file is a zip.
     */
    public ModelInfo {
    }

    /**
     * Backward compatibility constructor for Vosk models and formatting models (typically zips).
     */
    public ModelInfo(String name, String url, Locale locale, String size) {
        this(name, url, locale, size, TranscriberType.VOSK, true);
    }
}
