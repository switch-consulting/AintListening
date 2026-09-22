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
 * speech-to-text transcription using a Vosk model, and optional smart formatting.
 */
@Singleton
public class TranscriptionProcessor {
    private static final String TAG = "TranscriptionProcessor";

    private final Context context;
    private final Persistency persistency;
    private final TranscriberRegistry transcriberRegistry;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private SmartFormatter smartFormatter;
    private Transcriber activeTranscriber;

    /**
     * Constructs a new TranscriptionProcessor.
     *
     * @param context             The application context.
     * @param persistency         The persistency manager for saving results and temporary files.
     * @param transcriberRegistry The registry for transcription engines.
     */
    @Inject
    public TranscriptionProcessor(@ApplicationContext Context context, Persistency persistency, TranscriberRegistry transcriberRegistry) {
        this.context = context;
        this.persistency = persistency;
        this.transcriberRegistry = transcriberRegistry;
    }

    /**
     * Starts an asynchronous transcription of the audio at the given URI.
     *
     * @param audioUri   The URI of the audio file to transcribe.
     * @param modelIndex The index of the language model to use.
     * @param callback   The callback to receive status updates and results.
     */
    public void startTranscription(Uri audioUri, int modelIndex, TranscriptionCallback callback) {
        executorService.execute(() -> {
            try {
                callback.onStatusUpdate("Converting audio...");
                File wavFile = persistency.getIncomingWavFile();
                persistency.clearTemporaryFiles();

                boolean success = OpusToWavDecoder.decodeOpusToWav(context, audioUri, wavFile);
                if (!success) {
                    callback.onError("Audio conversion failed");
                    return;
                }

                callback.onStatusUpdate("Loading model...");
                LanguageSupport language = ModelManager.SUPPORTED_LANGUAGES[modelIndex];
                TranscriberType activeType = language.getActiveTranscriberType(context, persistency);
                activeTranscriber = transcriberRegistry.getTranscriber(activeType);
                
                if (activeTranscriber == null) {
                    throw new IllegalStateException("Transcriber not found for type: " + activeType);
                }

                activeTranscriber.ensureModelLoaded(context, modelIndex);

                callback.onStatusUpdate("Transcribing...");
                boolean providesPunctuation = activeTranscriber.providesPunctuation();
                List<TranscriptionParagraph> rawParagraphs = activeTranscriber.transcribe(context, wavFile, new TranscriptionListener() {
                    @Override
                    public void onPartialResult(String text) {
                        callback.onPartialResult(parseParagraphs(text, providesPunctuation));
                    }

                    @Override
                    public void onResult(String text) {
                        callback.onPartialResult(parseParagraphs(text, providesPunctuation));
                    }

                    @Override
                    public String onAudioChunkAvailable(byte[] pcmData, int chunkIndex) {
                        try {
                            return persistency.saveAudioChunk(pcmData, chunkIndex);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save audio chunk", e);
                            return null;
                        }
                    }
                });

                applySmartFormatting(rawParagraphs, modelIndex, callback);

            } catch (Exception e) {
                Log.e(TAG, "Transcription failed", e);
                callback.onError("Transcription failed: " + e.getMessage());
            }
        });
    }

    /**
     * Applies smart formatting (punctuation and casing) to the transcribed paragraphs.
     *
     * @param paragraphs     The raw transcription paragraphs.
     * @param modelIndex     The index of the language model to use for formatting.
     * @param callback       The callback to receive progress updates and the final result.
     */
    private void applySmartFormatting(List<TranscriptionParagraph> paragraphs, int modelIndex, TranscriptionCallback callback) {
        LanguageSupport selectedLanguage = ModelManager.SUPPORTED_LANGUAGES[modelIndex];
        List<TranscriptionParagraph> formattedParagraphs = new ArrayList<>();

        boolean engineProvidesPunctuation = activeTranscriber != null && activeTranscriber.providesPunctuation();
        boolean userWantsSmart = persistency.isSmartFormattingEnabled(selectedLanguage.getLocale());
        boolean modelAvailable = selectedLanguage.isFormattingDownloaded(context);

        if (!engineProvidesPunctuation && userWantsSmart && modelAvailable && !paragraphs.isEmpty()) {
            try {
                callback.onStatusUpdate("Applying smart formatting...");
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
                    callback.onSmartFormattingProgress(i + 1, paragraphs.size(), currentDisplayList);
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

        persistency.saveLastMessage(formattedParagraphs, modelIndex);
        callback.onComplete(formattedParagraphs);
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
     * Loads the last transcription result from persistency.
     *
     * @return The last list of transcription paragraphs.
     */
    public List<TranscriptionParagraph> loadLastMessage() {
        return persistency.loadLastMessage();
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
