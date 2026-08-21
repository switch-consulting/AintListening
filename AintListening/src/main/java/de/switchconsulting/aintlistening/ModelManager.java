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
    /** The list of speech models supported by the application. */
    static final ModelInfo[] SUPPORTED_MODELS = {
            new ModelInfo("vosk-model-small-de-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip", "Deutsch", "45MB"),
            new ModelInfo("vosk-model-small-en-us-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", "English", "40MB"),
            new ModelInfo("vosk-model-small-es-0.42", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", "Español", "39MB"),
            new ModelInfo("vosk-model-small-fr-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", "Français", "41MB"),
            new ModelInfo("vosk-model-small-it-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip", "Italiano", "48MB")
    };

    /**
     * Checks if a model at the given index is already downloaded and present on the device.
     *
     * @param context The context.
     * @param index   The index of the model in SUPPORTED_MODELS.
     * @return True if the model directory exists and is a directory, false otherwise.
     */
    static boolean isModelDownloaded(Context context, int index) {
        if (index < 0 || index >= SUPPORTED_MODELS.length) return false;
        File modelDir = new File(context.getFilesDir(), SUPPORTED_MODELS[index].name);
        return modelDir.exists() && modelDir.isDirectory();
    }

    /**
     * Returns a list of display names for all models that are currently downloaded.
     *
     * @param context The context.
     * @return A list of available language display names.
     */
    static List<String> getAvailableLanguageNames(Context context) {
        List<String> available = new ArrayList<>();
        for (int i = 0; i < SUPPORTED_MODELS.length; i++) {
            if (isModelDownloaded(context, i)) {
                available.add(SUPPORTED_MODELS[i].displayName);
            }
        }
        return available;
    }

    /**
     * Deletes the model files for the model at the specified index.
     *
     * @param context The context.
     * @param index   The index of the model to delete.
     * @return True if the model was successfully deleted, false otherwise.
     */
    static boolean deleteModel(Context context, int index) {
        if (index < 0 || index >= SUPPORTED_MODELS.length) return false;
        File modelDir = new File(context.getFilesDir(), SUPPORTED_MODELS[index].name);
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
