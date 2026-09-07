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
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing speech models. Provides metadata for supported models
 * and helper methods to check their installation status and perform file operations.
 */
class ModelManager {
    /** The singleton instance of the manager. */
    static final ModelManager INSTANCE = new ModelManager();

    /** The list of languages and their associated models supported by the application. */
    static final LanguageSupport[] SUPPORTED_LANGUAGES;

    static {
        SUPPORTED_LANGUAGES = new LanguageSupport[]{
                new LanguageSupport("Deutsch",
                        new ModelInfo("vosk-model-small-de-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip", "Deutsch", "45MB"),
                        new ModelInfo("ONNXModel_de", "https://github.com/switch-consulting/AintListening/raw/main/models/ONNXModel_de.zip", "Smart Formatting (DE)", "280MB"),
                        INSTANCE),
                new LanguageSupport("English",
                        new ModelInfo("vosk-model-small-en-us-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", "English", "40MB"),
                        null,
                        INSTANCE),
                new LanguageSupport("Español",
                        new ModelInfo("vosk-model-small-es-0.42", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", "Español", "39MB"),
                        null,
                        INSTANCE),
                new LanguageSupport("Français",
                        new ModelInfo("vosk-model-small-fr-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", "Français", "41MB"),
                        null,
                        INSTANCE),
                new LanguageSupport("Italiano",
                        new ModelInfo("vosk-model-small-it-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip", "Italiano", "48MB"),
                        null,
                        INSTANCE)
        };
    }

    /**
     * Checks if a model is already downloaded and present on the device.
     *
     * @param context The context.
     * @param info    The model information.
     * @return True if the model directory exists and is a directory, false otherwise.
     */
    boolean isModelDownloaded(Context context, ModelInfo info) {
        if (info == null) return false;
        File modelDir = new File(context.getFilesDir(), info.name);
        return modelDir.exists() && modelDir.isDirectory();
    }

    /**
     * Returns a list of language display names for all languages where at least the transcription model is downloaded.
     *
     * @param context The context.
     * @return A list of available language display names.
     */
    static List<String> getAvailableLanguageNames(Context context) {
        List<String> available = new ArrayList<>();
        for (LanguageSupport language : SUPPORTED_LANGUAGES) {
            if (language.isTranscriptionDownloaded(context)) {
                available.add(language.getLanguage());
            }
        }
        return available;
    }

    /**
     * Deletes the model files for the specified model.
     *
     * @param context The context.
     * @param info    The model information to delete.
     * @return True if the model was successfully deleted, false otherwise.
     */
    static boolean deleteModel(Context context, ModelInfo info) {
        if (info == null) return false;
        File modelDir = new File(context.getFilesDir(), info.name);
        return deleteRecursive(modelDir);
    }

    /**
     * Recursively deletes a file or directory and all its contents.
     *
     * @param fileOrDirectory The file or directory to delete.
     * @return True if the deletion was successful.
     */
    private static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }
}
