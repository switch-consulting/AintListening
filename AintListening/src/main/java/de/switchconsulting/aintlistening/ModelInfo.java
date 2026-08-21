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

/**
 * Data class containing metadata for a Vosk speech model.
 */
public class ModelInfo {
    /** The internal name/directory name of the model. */
    final String name;
    /** The URL where the model zip file can be downloaded. */
    final String url;
    /** The human-readable name of the language. */
    final String displayName;
    /** The approximate download size of the model (e.g., "45MB"). */
    final String size;

    /**
     * Constructs a new ModelInfo.
     *
     * @param name        The internal name of the model.
     * @param url         The download URL.
     * @param displayName The display name.
     * @param size        The download size.
     */
    public ModelInfo(String name, String url, String displayName, String size) {
        this.name = name;
        this.url = url;
        this.displayName = displayName;
        this.size = size;
    }
}
