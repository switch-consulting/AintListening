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

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for managing speech models. Provides metadata for supported models
 * and helper methods to check their installation status and perform file operations.
 */
public class ModelManager {
    /** The singleton instance of the manager. */
    public static final ModelManager INSTANCE = new ModelManager();

    /** The list of languages and their associated models supported by the application. */
    public static final LanguageSupport[] SUPPORTED_LANGUAGES;

    private static final String SHARED_PUNC_MODEL_NAME = "ONNXModel_multilingual";
    private static final String SHARED_PUNC_MODEL_URL = "https://github.com/switch-consulting/AintListening/raw/main/models/ONNXModel_multilingual.zip";
    private static final String SHARED_PUNC_MODEL_SIZE = "280MB";

    static {
        SUPPORTED_LANGUAGES = new LanguageSupport[]{
                new LanguageSupport(Locale.GERMAN,
                        new ModelInfo("vosk-model-small-de-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip", Locale.GERMAN, "45MB"),
                        new ModelInfo(SHARED_PUNC_MODEL_NAME, SHARED_PUNC_MODEL_URL, Locale.GERMAN, SHARED_PUNC_MODEL_SIZE),
                        INSTANCE),
                new LanguageSupport(Locale.ENGLISH,
                        new ModelInfo("vosk-model-small-en-us-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", Locale.ENGLISH, "40MB"),
                        new ModelInfo(SHARED_PUNC_MODEL_NAME, SHARED_PUNC_MODEL_URL, Locale.ENGLISH, SHARED_PUNC_MODEL_SIZE),
                        INSTANCE),
                new LanguageSupport(Locale.forLanguageTag("es"),
                        new ModelInfo("vosk-model-small-es-0.42", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", Locale.forLanguageTag("es"), "39MB"),
                        new ModelInfo(SHARED_PUNC_MODEL_NAME, SHARED_PUNC_MODEL_URL, Locale.forLanguageTag("es"), SHARED_PUNC_MODEL_SIZE),
                        INSTANCE),
                new LanguageSupport(Locale.FRENCH,
                        new ModelInfo("vosk-model-small-fr-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", Locale.FRENCH, "41MB"),
                        new ModelInfo(SHARED_PUNC_MODEL_NAME, SHARED_PUNC_MODEL_URL, Locale.FRENCH, SHARED_PUNC_MODEL_SIZE),
                        INSTANCE),
                new LanguageSupport(Locale.ITALIAN,
                        new ModelInfo("vosk-model-small-it-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip", Locale.ITALIAN, "48MB"),
                        new ModelInfo(SHARED_PUNC_MODEL_NAME, SHARED_PUNC_MODEL_URL, Locale.ITALIAN, SHARED_PUNC_MODEL_SIZE),
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
    public boolean isModelDownloaded(Context context, ModelInfo info) {
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
    public static List<String> getAvailableLanguageNames(Context context) {
        List<String> available = new ArrayList<>();
        for (LanguageSupport language : SUPPORTED_LANGUAGES) {
            if (language.isTranscriptionDownloaded(context)) {
                available.add(language.getLocale().getDisplayName());
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
    public static boolean deleteModel(Context context, ModelInfo info) {
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
