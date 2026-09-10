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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the speech-to-text transcription process using the Vosk library.
 */
public class VoskTranscriber implements Transcriber {
    private static final String TAG = "VoskTranscriber";

    private Model model;
    private int loadedModelIndex = -1;

    /**
     * Ensures that the Vosk model for the specified language is loaded into memory.
     *
     * @param context    The application context.
     * @param modelIndex The index of the language in ModelManager.SUPPORTED_LANGUAGES.
     * @throws Exception If the model loading fails.
     */
    @Override
    public void ensureModelLoaded(Context context, int modelIndex) throws Exception {
        if (model != null && loadedModelIndex == modelIndex) {
            return;
        }

        if (model != null) {
            model.close();
        }

        LanguageSupport language = ModelManager.SUPPORTED_LANGUAGES[modelIndex];
        if (!language.isTranscriptionDownloaded(context)) {
            throw new IllegalStateException("Vosk model not found for language: " + language.getLocale().getDisplayName());
        }

        File modelDir = new File(context.getFilesDir(), language.getTranscriptionModel().name);
        Log.i(TAG, "Loading Vosk model from: " + modelDir.getAbsolutePath());
        model = new Model(modelDir.getAbsolutePath());
        loadedModelIndex = modelIndex;
    }

    /**
     * Transcribes a WAV file to text and splits audio into chunks.
     *
     * @param context  The application context.
     * @param wavFile  The WAV file to transcribe.
     * @param listener A listener to receive partial and final transcription results.
     * @return A list of transcription paragraphs with associated audio chunks.
     * @throws Exception If an error occurs during transcription.
     */
    @Override
    public List<TranscriptionParagraph> transcribe(Context context, @NonNull File wavFile, TranscriptionListener listener) throws Exception {
        if (model == null) {
            throw new IllegalStateException("Vosk model not loaded. Call ensureModelLoaded first.");
        }

        List<TranscriptionParagraph> paragraphs = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        
        File chunksDir = new File(context.getFilesDir(), "audio_chunks");
        if (!chunksDir.exists()) {
            if (!chunksDir.mkdirs()) {
                Log.w(TAG, "Failed to create chunks directory: " + chunksDir.getAbsolutePath());
            }
        }
        // Clean up old chunks
        File[] oldChunks = chunksDir.listFiles();
        if (oldChunks != null) {
            for (File f : oldChunks) {
                if (!f.delete()) {
                    Log.w(TAG, "Failed to delete old chunk: " + f.getAbsolutePath());
                }
            }
        }

        try (FileInputStream fis = new FileInputStream(wavFile);
             Recognizer recognizer = new Recognizer(model, 16000.0f);
             ByteArrayOutputStream currentPcm = new ByteArrayOutputStream()) {

            // Skip WAV header (44 bytes typical PCM header)
            long skipped = fis.skip(44);
            if (skipped < 44) {
                throw new IllegalStateException("Invalid WAV file header");
            }

            byte[] buffer = new byte[4096];
            int nread;
            int chunkIndex = 0;

            while ((nread = fis.read(buffer)) >= 0) {
                currentPcm.write(buffer, 0, nread);
                
                if (recognizer.acceptWaveForm(buffer, nread)) {
                    String resultJson = recognizer.getResult();
                    String text = extractTextFromResultJson(resultJson);
                    if (!TextUtils.isEmpty(text)) {
                        Log.d(TAG, "Vosk segment finalized: " + text);
                        
                        File chunkFile = new File(chunksDir, "chunk_" + (chunkIndex++) + ".wav");
                        WavUtils.savePcmAsWav(currentPcm.toByteArray(), chunkFile);
                        currentPcm.reset();

                        TranscriptionParagraph p = new TranscriptionParagraph(text, null, chunkFile.getAbsolutePath());
                        paragraphs.add(p);

                        if (!TextUtils.isEmpty(fullText)) {
                            fullText.append("\n\n");
                        }
                        fullText.append(text);
                        if (listener != null) listener.onResult(fullText.toString());
                    }
                } else {
                    String partialJson = recognizer.getPartialResult();
                    String partialText = extractPartialTextFromJson(partialJson);
                    if (!TextUtils.isEmpty(partialText) && listener != null) {
                        String currentDisplay = fullText.toString();
                        if (!TextUtils.isEmpty(currentDisplay)) {
                            currentDisplay += "\n\n";
                        }
                        listener.onPartialResult(currentDisplay + partialText);
                    }
                }
            }

            String finalJson = recognizer.getFinalResult();
            String finalText = extractTextFromResultJson(finalJson);
            if (!TextUtils.isEmpty(finalText)) {
                Log.d(TAG, "Vosk final segment: " + finalText);
                
                File chunkFile = new File(chunksDir, "chunk_" + (chunkIndex) + ".wav");
                WavUtils.savePcmAsWav(currentPcm.toByteArray(), chunkFile);
                
                TranscriptionParagraph p = new TranscriptionParagraph(finalText, null, chunkFile.getAbsolutePath());
                paragraphs.add(p);

                if (!TextUtils.isEmpty(fullText)) {
                    fullText.append("\n\n");
                }
                fullText.append(finalText);
                if (listener != null) listener.onResult(fullText.toString());
            }
        }

        return paragraphs;
    }

    /**
     * Extracts the partial transcription text from a Vosk JSON result string.
     *
     * @param json The JSON result from the recognizer.
     * @return The partial text string.
     */
    private String extractPartialTextFromJson(String json) {
        if (json == null || json.trim().isEmpty()) return "";
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("partial", "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Extracts the final transcription text from a Vosk JSON result string.
     *
     * @param json The JSON result from the recognizer.
     * @return The text string.
     */
    private String extractTextFromResultJson(String json) {
        if (json == null || json.trim().isEmpty()) return "";
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("text", "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Closes the Vosk model and releases resources.
     */
    @Override
    public void close() {
        if (model != null) {
            try {
                model.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing Vosk model", e);
            }
            model = null;
        }
        loadedModelIndex = -1;
    }
}
