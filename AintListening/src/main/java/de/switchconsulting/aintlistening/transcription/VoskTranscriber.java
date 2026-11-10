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
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelCatalogRepository;
import de.switchconsulting.aintlistening.data.ModelInfo;

/**
 * Handles the speech-to-text transcription process using the Vosk library.
 */
@Singleton
public class VoskTranscriber implements Transcriber {
    private static final String TAG = "VoskTranscriber";

    private final ModelCatalogRepository modelRepository;
    private Model model;
    private Locale loadedLocale;

    /**
     * Constructs a new VoskTranscriber instance.
     *
     * @param modelRepository The repository for model metadata and disk checks.
     */
    @Inject
    public VoskTranscriber(ModelCatalogRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    /**
     * Ensures that the Vosk model for the specified language is loaded into memory.
     *
     * @param context The application context.
     * @param locale  The locale of the language to load.
     * @throws Exception If the model loading fails.
     */
    @Override
    public void ensureModelLoaded(Context context, Locale locale) throws Exception {
        if (model != null && Objects.equals(loadedLocale, locale)) {
            return;
        }

        if (model != null) {
            model.close();
        }

        LanguageSupport language = modelRepository.getLanguageSupport(locale);
        if (language == null || !modelRepository.isDownloaded(language, getType())) {
            throw new IllegalStateException("Vosk model not found for language: " + (locale != null ? locale.getDisplayName() : "null"));
        }

        ModelInfo modelInfo = language.getModel(getType());
        if (modelInfo == null) {
            throw new IllegalStateException("Vosk model info not found for language: " + language.getLocale().getDisplayName());
        }

        File modelDir = new File(context.getFilesDir(), modelInfo.name());
        Log.i(TAG, "Loading Vosk model from: " + modelDir.getAbsolutePath());
        model = new Model(modelDir.getAbsolutePath());
        loadedLocale = locale;
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
        List<PartialRecognitionResult> partialResults = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

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
            long totalBytesProcessed = 0;

            while ((nread = fis.read(buffer)) >= 0) {
                currentPcm.write(buffer, 0, nread);
                totalBytesProcessed += nread;

                if (recognizer.acceptWaveForm(buffer, nread)) {
                    String resultJson = recognizer.getResult();
                    String text = extractTextFromResultJson(resultJson);
                    if (!TextUtils.isEmpty(text)) {
                        Log.d(TAG, "Vosk segment finalized: " + text);
                        processFinalizedSegment(text, currentPcm, totalBytesProcessed, wavFile,
                                paragraphs, partialResults, fullText, listener);
                    }
                } else {
                    String partialJson = recognizer.getPartialResult();
                    String partialText = extractPartialTextFromJson(partialJson);
                    processPartialResult(partialText, currentPcm, totalBytesProcessed, fullText, listener);
                }
            }

            String finalJson = recognizer.getFinalResult();
            String finalText = extractTextFromResultJson(finalJson);
            if (!TextUtils.isEmpty(finalText)) {
                Log.d(TAG, "Vosk final segment: " + finalText);
                processFinalizedSegment(finalText, currentPcm, totalBytesProcessed, wavFile,
                        paragraphs, partialResults, fullText, listener);
            }
        }

        return paragraphs;
    }

    /**
     * Extracts delta text by subtracting any already accumulated full text.
     */
    private String computeDeltaText(String accumulatedFullText, String rawText) {
        if (TextUtils.isEmpty(rawText)) return "";
        if (TextUtils.isEmpty(accumulatedFullText)) return rawText.trim();
        if (rawText.equals(accumulatedFullText)) return "";
        if (rawText.startsWith(accumulatedFullText)) {
            return rawText.substring(accumulatedFullText.length()).trim();
        }
        return rawText.trim();
    }

    /**
     * Processes a finalized recognition segment.
     */
    private void processFinalizedSegment(
            String rawText,
            ByteArrayOutputStream currentPcm,
            long totalBytesProcessed,
            File wavFile,
            List<TranscriptionParagraph> paragraphs,
            List<PartialRecognitionResult> partialResults,
            StringBuilder fullText,
            TranscriptionListener listener
    ) {
        String textToAdd = computeDeltaText(fullText.toString(), rawText);
        if (TextUtils.isEmpty(textToAdd)) return;

        long currentPcmSize = currentPcm.size();
        long startMs = (totalBytesProcessed - currentPcmSize) / 32;
        long endMs = totalBytesProcessed / 32;
        PartialRecognitionResult partialResult = new PartialRecognitionResult(textToAdd, startMs, endMs);
        partialResults.add(partialResult);

        currentPcm.reset();

        paragraphs.add(new TranscriptionParagraph(textToAdd));

        if (fullText.length() != 0) {
            fullText.append("\n\n");
        }
        fullText.append(textToAdd);

        if (listener != null && !partialResults.isEmpty()) {
            long totalLengthMs = (wavFile.length() - 44) / 32;
            listener.onResult(new FinalRecognitionResult(new ArrayList<>(partialResults), wavFile, totalLengthMs));
        }
    }

    /**
     * Processes a partial recognition result.
     */
    private void processPartialResult(
            String partialText,
            ByteArrayOutputStream currentPcm,
            long totalBytesProcessed,
            StringBuilder fullText,
            TranscriptionListener listener
    ) {
        if (TextUtils.isEmpty(partialText) || listener == null) return;

        StringBuilder currentPartialState = new StringBuilder();
        String finalizedSoFar = fullText.toString();
        if (!TextUtils.isEmpty(finalizedSoFar)) {
            currentPartialState.append(finalizedSoFar);
        }
        if (!TextUtils.isEmpty(partialText)) {
            if (currentPartialState.length() != 0) {
                currentPartialState.append(" ");
            }
            currentPartialState.append(partialText);
        }

        long currentPcmSize = currentPcm.size();
        long startMs = (totalBytesProcessed - currentPcmSize) / 32;
        long endMs = totalBytesProcessed / 32;
        PartialRecognitionResult result = new PartialRecognitionResult(currentPartialState.toString(), startMs, endMs);
        listener.onPartialRecognition(result);
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
     * Returns the transcriber engine type for Vosk.
     *
     * @return TranscriberType.VOSK.
     */
    @Override
    public TranscriberType getType() {
        return TranscriberType.VOSK;
    }

    /**
     * Returns whether Vosk provides punctuation in its output.
     *
     * @return False because Vosk does not provide punctuation.
     */
    @Override
    public boolean providesPunctuation() {
        return false;
    }

    /**
     * Returns whether Vosk supports pause length configuration for paragraph detection.
     *
     * @return False because Vosk relies on internal endpointing.
     */
    @Override
    public boolean supportsPauseLengthConfiguration() {
        return false;
    }

    /**
     * Sets the pause length in milliseconds for paragraph detection.
     *
     * @param pauseLengthMs The pause length in milliseconds.
     */
    @Override
    public void setPauseLengthMs(long pauseLengthMs) {
        // No-op: Vosk does not support configurable pause length.
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
        loadedLocale = null;
    }
}
