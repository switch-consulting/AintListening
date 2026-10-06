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
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.di.BackgroundExecutor;
import de.switchconsulting.aintlistening.di.MainExecutor;
import de.switchconsulting.aintlistening.formatting.SmartFormatter;
import de.switchconsulting.aintlistening.formatting.SmartFormatterFactory;
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
    private final ModelCatalogRepository modelRepository;
    private final TranscriberRegistry transcriberRegistry;
    private final SmartFormatterFactory smartFormatterFactory;
    private final ExecutorService backgroundExecutor;
    private final Executor mainExecutor;

    private Future<?> activeTask;
    private SmartFormatter smartFormatter;
    private Transcriber activeTranscriber;

    /**
     * Constructs a new TranscriptionProcessor.
     *
     * @param context               The application context.
     * @param repository            The repository for state saving, loading, and temporary file cache.
     * @param modelRepository       The repository for model metadata and disk availability.
     * @param transcriberRegistry   The registry for transcription engines.
     * @param smartFormatterFactory The factory for creating smart formatters.
     * @param backgroundExecutor    The background executor service.
     * @param mainExecutor          The main thread executor.
     */
    @Inject
    public TranscriptionProcessor(@ApplicationContext Context context,
                                  TranscriptionRepository repository,
                                  ModelCatalogRepository modelRepository,
                                  TranscriberRegistry transcriberRegistry,
                                  SmartFormatterFactory smartFormatterFactory,
                                  @BackgroundExecutor ExecutorService backgroundExecutor,
                                  @MainExecutor Executor mainExecutor) {
        this.context = context;
        this.repository = repository;
        this.modelRepository = modelRepository;
        this.transcriberRegistry = transcriberRegistry;
        this.smartFormatterFactory = smartFormatterFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.mainExecutor = mainExecutor;
    }

    /**
     * Cancels any ongoing transcription task.
     */
    public synchronized void cancelTranscription() {
        if (activeTask != null && !activeTask.isDone()) {
            activeTask.cancel(true);
        }
    }

    /**
     * Starts an asynchronous transcription of the audio at the given URI.
     *
     * @param audioUri The URI of the audio file to transcribe.
     * @param locale   The locale of the language model to use.
     * @param callback The callback to receive status updates and results.
     */
    public synchronized void startTranscription(Uri audioUri, Locale locale, TranscriptionCallback callback) {
        cancelTranscription();

        activeTask = backgroundExecutor.submit(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) return;
                notifyStatusUpdate(callback, "Converting audio...");
                File wavFile = repository.getIncomingWavFile();
                repository.clearTemporaryFiles();

                boolean success = OpusToWavDecoder.decodeOpusToWav(context, audioUri, wavFile);
                if (!success || Thread.currentThread().isInterrupted()) {
                    if (!Thread.currentThread().isInterrupted()) {
                        notifyError(callback, "Audio conversion failed");
                    }
                    return;
                }

                if (Thread.currentThread().isInterrupted()) return;
                notifyStatusUpdate(callback, "Loading model...");
                LanguageSupport language = modelRepository.getLanguageSupport(locale);
                if (language == null) {
                    throw new IllegalStateException("Unsupported language locale: " + (locale != null ? locale.getDisplayName() : "null"));
                }
                TranscriberType activeType = modelRepository.getActiveTranscriberType(language);
                activeTranscriber = transcriberRegistry.getTranscriber(activeType);
                
                if (activeTranscriber == null) {
                    throw new IllegalStateException("Transcriber not found for type: " + activeType);
                }

                activeTranscriber.ensureModelLoaded(context, locale);

                if (Thread.currentThread().isInterrupted()) return;
                notifyStatusUpdate(callback, "Transcribing...");
                boolean providesPunctuation = activeTranscriber.providesPunctuation();
                List<TranscriptionParagraph> rawParagraphs = activeTranscriber.transcribe(context, wavFile, new TranscriptionListener() {
                    @Override
                    public void onPartialResult(String text) {
                        if (!Thread.currentThread().isInterrupted()) {
                            notifyPartialResult(callback, parseParagraphs(text, providesPunctuation));
                        }
                    }

                    @Override
                    public void onResult(String text) {
                        if (!Thread.currentThread().isInterrupted()) {
                            notifyPartialResult(callback, parseParagraphs(text, providesPunctuation));
                        }
                    }

                    @Override
                    public String onAudioChunkAvailable(byte[] pcmData, int chunkIndex) {
                        if (Thread.currentThread().isInterrupted()) return null;
                        try {
                            return repository.saveAudioChunk(pcmData, chunkIndex);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save audio chunk", e);
                            return null;
                        }
                    }
                });

                if (Thread.currentThread().isInterrupted()) return;
                applySmartFormatting(rawParagraphs, locale, callback);

            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "Transcription job interrupted/cancelled");
                } else {
                    Log.e(TAG, "Transcription failed", e);
                    notifyError(callback, "Transcription failed: " + e.getMessage());
                }
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
        LanguageSupport selectedLanguage = modelRepository.getLanguageSupport(locale);
        List<TranscriptionParagraph> formattedParagraphs = new ArrayList<>();

        boolean engineProvidesPunctuation = activeTranscriber != null && activeTranscriber.providesPunctuation();
        boolean userWantsSmart = selectedLanguage != null && repository.getPreferencesDataSource().isSmartFormattingEnabled(selectedLanguage.getLocale());
        boolean modelAvailable = selectedLanguage != null && modelRepository.isFormattingDownloaded(selectedLanguage);

        if (!engineProvidesPunctuation && userWantsSmart && modelAvailable && !paragraphs.isEmpty()) {
            try {
                notifyStatusUpdate(callback, "Applying smart formatting...");
                ModelInfo targetModel = selectedLanguage.getFormattingModel();
                if (targetModel != null) {
                    if (smartFormatter != null && !Objects.equals(smartFormatter.getModelInfo(), targetModel)) {
                        smartFormatter.close();
                        smartFormatter = null;
                    }
                    if (smartFormatter == null) {
                        smartFormatter = smartFormatterFactory.create(targetModel);
                    }
                }

                for (int i = 0; i < paragraphs.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) return;
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
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
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

        if (Thread.currentThread().isInterrupted()) return;
        repository.saveLastMessage(formattedParagraphs, locale);
        notifyComplete(callback, formattedParagraphs);
    }

    /**
     * Posts a status update notification to the callback on the main thread.
     *
     * @param callback The callback listener.
     * @param message  The status update message.
     */
    private void notifyStatusUpdate(TranscriptionCallback callback, String message) {
        mainExecutor.execute(() -> callback.onStatusUpdate(message));
    }

    /**
     * Posts partial transcription results to the callback on the main thread.
     *
     * @param callback   The callback listener.
     * @param paragraphs The partial transcription paragraphs.
     */
    private void notifyPartialResult(TranscriptionCallback callback, List<TranscriptionParagraph> paragraphs) {
        mainExecutor.execute(() -> callback.onPartialResult(paragraphs));
    }

    /**
     * Posts smart formatting progress updates to the callback on the main thread.
     *
     * @param callback   The callback listener.
     * @param step       The current step count.
     * @param total      The total steps.
     * @param paragraphs The current paragraphs list.
     */
    private void notifySmartFormattingProgress(TranscriptionCallback callback, int step, int total, List<TranscriptionParagraph> paragraphs) {
        mainExecutor.execute(() -> callback.onSmartFormattingProgress(step, total, paragraphs));
    }

    /**
     * Posts completion results to the callback on the main thread.
     *
     * @param callback   The callback listener.
     * @param paragraphs The final completed transcription paragraphs.
     */
    private void notifyComplete(TranscriptionCallback callback, List<TranscriptionParagraph> paragraphs) {
        mainExecutor.execute(() -> callback.onComplete(paragraphs));
    }

    /**
     * Posts error messages to the callback on the main thread.
     *
     * @param callback The callback listener.
     * @param message  The error description string.
     */
    private void notifyError(TranscriptionCallback callback, String message) {
        mainExecutor.execute(() -> callback.onError(message));
    }

    /**
     * Parses the raw transcription text into a list of TranscriptionParagraphs.
     *
     * @param text                The raw text to parse.
     * @param providesPunctuation True if the engine already provides punctuation.
     * @return A list of paragraphs.
     */
    List<TranscriptionParagraph> parseParagraphs(String text, boolean providesPunctuation) {
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
     * Loads the last transcription result from the repository synchronously.
     *
     * @return The last list of transcription paragraphs.
     */
    public List<TranscriptionParagraph> loadLastMessage() {
        return repository.loadLastMessage();
    }

    /**
     * Asynchronously loads the last transcription result from the repository and delivers it to the callback on the main thread.
     *
     * @param callback The callback receiving the loaded transcription paragraphs.
     */
    public void loadLastMessage(Consumer<List<TranscriptionParagraph>> callback) {
        backgroundExecutor.submit(() -> {
            List<TranscriptionParagraph> paragraphs = loadLastMessage();
            if (callback != null) {
                mainExecutor.execute(() -> callback.accept(paragraphs));
            }
        });
    }

    /**
     * Releases resources used by the transcriber and formatter and cancels pending tasks.
     */
    public void release() {
        cancelTranscription();
        transcriberRegistry.closeAll();
        if (smartFormatter != null) {
            smartFormatter.close();
        }
    }
}
