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
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.formatting.OnnxSmartFormatter;
import de.switchconsulting.aintlistening.formatting.SmartFormatter;
import de.switchconsulting.aintlistening.transcription.Transcriber;
import de.switchconsulting.aintlistening.transcription.TranscriberRegistry;
import de.switchconsulting.aintlistening.transcription.TranscriberType;
import de.switchconsulting.aintlistening.transcription.TranscriptionListener;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;
import de.switchconsulting.aintlistening.util.OpusToWavDecoder;

/**
 * Handles the end-to-end transcription process, including audio conversion,
 * speech-to-text transcription using a Vosk or Whisper model, and optional smart formatting.
 */
@Singleton
public class TranscriptionProcessor {
    private static final String TAG = "TranscriptionProcessor";

    private final Context context;
    private final TranscriptionRepository repository;
    private final TranscriberRegistry transcriberRegistry;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SmartFormatter smartFormatter;
    private Transcriber activeTranscriber;

    /**
     * Constructs a new TranscriptionProcessor.
     *
     * @param context             The application context.
     * @param repository          The repository for state saving, loading, and temporary file cache.
     * @param transcriberRegistry The registry for transcription engines.
     */
    @Inject
    public TranscriptionProcessor(@ApplicationContext Context context, TranscriptionRepository repository, TranscriberRegistry transcriberRegistry) {
        this.context = context;
        this.repository = repository;
        this.transcriberRegistry = transcriberRegistry;
    }

    /**
     * Starts an asynchronous transcription of the audio at the given URI.
     *
     * @param audioUri The URI of the audio file to transcribe.
     * @param locale   The locale of the language model to use.
     * @param callback The callback to receive status updates and results.
     */
    public void startTranscription(Uri audioUri, Locale locale, TranscriptionCallback callback) {
        executorService.execute(() -> {
            try {
                notifyStatusUpdate(callback, "Converting audio...");
                File wavFile = repository.getIncomingWavFile();
                repository.clearTemporaryFiles();

                boolean success = OpusToWavDecoder.decodeOpusToWav(context, audioUri, wavFile);
                if (!success) {
                    notifyError(callback, "Audio conversion failed");
                    return;
                }

                notifyStatusUpdate(callback, "Loading model...");
                LanguageSupport language = ModelManager.getLanguageSupport(locale);
                if (language == null) {
                    throw new IllegalStateException("Unsupported language locale: " + (locale != null ? locale.getDisplayName() : "null"));
                }
                TranscriberType activeType = language.getActiveTranscriberType(context, repository.getPersistency());
                activeTranscriber = transcriberRegistry.getTranscriber(activeType);
                
                if (activeTranscriber == null) {
                    throw new IllegalStateException("Transcriber not found for type: " + activeType);
                }

                activeTranscriber.ensureModelLoaded(context, locale);

                notifyStatusUpdate(callback, "Transcribing...");
                boolean providesPunctuation = activeTranscriber.providesPunctuation();
                List<TranscriptionParagraph> rawParagraphs = activeTranscriber.transcribe(context, wavFile, new TranscriptionListener() {
                    @Override
                    public void onPartialResult(String text) {
                        notifyPartialResult(callback, parseParagraphs(text, providesPunctuation));
                    }

                    @Override
                    public void onResult(String text) {
                        notifyPartialResult(callback, parseParagraphs(text, providesPunctuation));
                    }

                    @Override
                    public String onAudioChunkAvailable(byte[] pcmData, int chunkIndex) {
                        try {
                            return repository.saveAudioChunk(pcmData, chunkIndex);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save audio chunk", e);
                            return null;
                        }
                    }
                });

                applySmartFormatting(rawParagraphs, locale, callback);

            } catch (Exception e) {
                Log.e(TAG, "Transcription failed", e);
                notifyError(callback, "Transcription failed: " + e.getMessage());
            }
        });
    }

    /**
     * Applies smart formatting (punctuation and casing) to the transcribed paragraphs.
     *
     * @param paragraphs The raw transcription paragraphs.
     * @param locale     The locale of the language model to use for formatting.
     * @param callback   The callback to receive progress updates and the final result.
     */
    private void applySmartFormatting(List<TranscriptionParagraph> paragraphs, Locale locale, TranscriptionCallback callback) {
        LanguageSupport selectedLanguage = ModelManager.getLanguageSupport(locale);
        List<TranscriptionParagraph> formattedParagraphs = new ArrayList<>();

        boolean engineProvidesPunctuation = activeTranscriber != null && activeTranscriber.providesPunctuation();
        boolean userWantsSmart = selectedLanguage != null && repository.getPersistency().isSmartFormattingEnabled(selectedLanguage.getLocale());
        boolean modelAvailable = selectedLanguage != null && selectedLanguage.isFormattingDownloaded(context);

        if (!engineProvidesPunctuation && userWantsSmart && modelAvailable && !paragraphs.isEmpty()) {
            try {
                notifyStatusUpdate(callback, "Applying smart formatting...");
                ModelInfo targetModel = selectedLanguage.getFormattingModel();
                if (targetModel != null) {
                    if (smartFormatter != null && !smartFormatter.getModelInfo().equals(targetModel)) {
                        smartFormatter.close();
                        smartFormatter = null;
                    }
                    if (smartFormatter == null) {
                        smartFormatter = new OnnxSmartFormatter(context, targetModel);
                    }
                }

                for (int i = 0; i < paragraphs.size(); i++) {
                    TranscriptionParagraph p = paragraphs.get(i);
                    String rawPara = p.getRawText();
                    if (rawPara.trim().isEmpty()) continue;

                    String formattedPara = smartFormatter.format(rawPara);
                    TranscriptionParagraph formattedP = new TranscriptionParagraph(rawPara, formattedPara, p.getAudioFilePath());
                    formattedParagraphs.add(formattedP);

                    List<TranscriptionParagraph> currentDisplayList = new ArrayList<>(formattedParagraphs);
                    for (int j = i + 1; j < paragraphs.size(); j++) {
                        currentDisplayList.add(paragraphs.get(j));
                    }
                    notifySmartFormattingProgress(callback, i + 1, paragraphs.size(), currentDisplayList);
                }
            } catch (Exception e) {
                Log.e(TAG, "Smart formatting failed", e);
                formattedParagraphs.clear();
                formattedParagraphs.addAll(paragraphs);
            }
        } else {
            if (engineProvidesPunctuation) {
                for (TranscriptionParagraph p : paragraphs) {
                    if (p.getFormattedText() == null) {
                        formattedParagraphs.add(new TranscriptionParagraph(p.getRawText(), p.getRawText(), p.getAudioFilePath()));
                    } else {
                        formattedParagraphs.add(p);
                    }
                }
            } else {
                formattedParagraphs.addAll(paragraphs);
            }
        }

        repository.saveLastMessage(formattedParagraphs, locale);
        notifyComplete(callback, formattedParagraphs);
    }

    private void notifyStatusUpdate(TranscriptionCallback callback, String message) {
        mainHandler.post(() -> callback.onStatusUpdate(message));
    }

    private void notifyPartialResult(TranscriptionCallback callback, List<TranscriptionParagraph> paragraphs) {
        mainHandler.post(() -> callback.onPartialResult(paragraphs));
    }

    private void notifySmartFormattingProgress(TranscriptionCallback callback, int step, int total, List<TranscriptionParagraph> paragraphs) {
        mainHandler.post(() -> callback.onSmartFormattingProgress(step, total, paragraphs));
    }

    private void notifyComplete(TranscriptionCallback callback, List<TranscriptionParagraph> paragraphs) {
        mainHandler.post(() -> callback.onComplete(paragraphs));
    }

    private void notifyError(TranscriptionCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    /**
     * Parses the raw transcription text into a list of TranscriptionParagraphs.
     *
     * @param text                The raw text to parse.
     * @param providesPunctuation True if the engine already provides punctuation.
     * @return A list of paragraphs.
     */
    private List<TranscriptionParagraph> parseParagraphs(String text, boolean providesPunctuation) {
        if (text.trim().isEmpty()) return new ArrayList<>();
        String[] paras = text.split("\n\n");
        List<TranscriptionParagraph> pList = new ArrayList<>();
        for (String p : paras) {
            if (!p.trim().isEmpty()) {
                String raw = p.trim();
                String formatted = providesPunctuation ? raw : null;
                pList.add(new TranscriptionParagraph(raw, formatted));
            }
        }
        return pList;
    }

    /**
     * Loads the last transcription result from the repository.
     *
     * @return The last list of transcription paragraphs.
     */
    public List<TranscriptionParagraph> loadLastMessage() {
        return repository.loadLastMessage();
    }

    /**
     * Releases resources used by the transcriber and formatter.
     */
    public void release() {
        transcriberRegistry.closeAll();
        if (smartFormatter != null) {
            smartFormatter.close();
        }
    }
}
