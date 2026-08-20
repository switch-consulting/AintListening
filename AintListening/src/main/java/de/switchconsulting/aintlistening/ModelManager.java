package de.switchconsulting.aintlistening;

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ModelManager {
    static final ModelInfo[] SUPPORTED_MODELS = {
            new ModelInfo("vosk-model-small-de-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip", "Deutsch", "45MB"),
            new ModelInfo("vosk-model-small-en-us-0.15", "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", "English", "40MB"),
            new ModelInfo("vosk-model-small-fr-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", "Français", "41MB"),
            new ModelInfo("vosk-model-small-it-0.22", "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip", "Italiano", "48MB"),
            new ModelInfo("vosk-model-small-es-0.42", "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip", "Español", "39MB")
    };

    static boolean isModelDownloaded(Context context, int index) {
        if (index < 0 || index >= SUPPORTED_MODELS.length) return false;
        File modelDir = new File(context.getFilesDir(), SUPPORTED_MODELS[index].name);
        return modelDir.exists() && modelDir.isDirectory();
    }

    static List<String> getAvailableLanguageNames(Context context) {
        List<String> available = new ArrayList<>();
        for (int i = 0; i < SUPPORTED_MODELS.length; i++) {
            if (isModelDownloaded(context, i)) {
                available.add(SUPPORTED_MODELS[i].displayName);
            }
        }
        return available;
    }
}
