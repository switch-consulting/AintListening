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
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelCatalogRepository;
import de.switchconsulting.androidwhisperer.AndroidWhisperer;
import de.switchconsulting.androidwhisperer.AudioUtils;
import de.switchconsulting.androidwhisperer.WhisperModel;
import de.switchconsulting.androidwhisperer.model.SegmentResult;
import de.switchconsulting.androidwhisperer.model.TranscriptionResult;

/**
 * Handles the speech-to-text transcription process using the AndroidWhisperer library.
 */
@Singleton
public class WhisperTranscriber implements Transcriber {
    private static final String TAG = "WhisperTranscriber";

    private final ModelCatalogRepository modelRepository;
    private AndroidWhisperer whisperer;
    private Locale loadedLocale;

    /**
     * Constructs a new WhisperTranscriber instance.
     *
     * @param modelRepository The repository for model metadata and disk checks.
     */
    @Inject
    public WhisperTranscriber(ModelCatalogRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    /**
     * Ensures that the Whisper model for the specified language is loaded into memory.
     *
     * @param context The application context.
     * @param locale  The locale of the language to load.
     * @throws Exception If the model loading fails.
     */
    @Override
    public void ensureModelLoaded(Context context, Locale locale) throws Exception {
        if (whisperer != null && Objects.equals(loadedLocale, locale)) {
            return;
        }

        if (whisperer != null) {
            whisperer.close();
            whisperer = null;
        }

        LanguageSupport language = modelRepository.getLanguageSupport(locale);
        if (language == null || !modelRepository.isDownloaded(language, getType())) {
            throw new IllegalStateException("Whisper model not found for language: " + (locale != null ? locale.getDisplayName() : "null"));
        }

        Log.i(TAG, "Initializing AndroidWhisperer for locale: " + locale);
        whisperer = new AndroidWhisperer();
        whisperer.init(context.getApplicationContext(), WhisperModel.TINY);
        loadedLocale = locale;
    }

    /**
     * Transcribes a WAV file to text using AndroidWhisperer.
     *
     * @param context  The application context.
     * @param wavFile  The WAV file to transcribe.
     * @param listener A listener to receive transcription results.
     * @return A list of transcription paragraphs.
     * @throws Exception If an error occurs during transcription.
     */
    @Override
    public List<TranscriptionParagraph> transcribe(Context context, @NonNull File wavFile, TranscriptionListener listener) throws Exception {
        if (whisperer == null) {
            throw new IllegalStateException("Whisper model not loaded. Call ensureModelLoaded first.");
        }

        Log.i(TAG, "Starting Whisper transcription for file: " + wavFile.getAbsolutePath());
        float[] samples = AudioUtils.readWav(wavFile);

        List<PartialRecognitionResult> partialResults = new ArrayList<>();
        de.switchconsulting.androidwhisperer.TranscriptionListener whisperListener = createWhisperListener(listener, partialResults);

        whisperer.addListener(whisperListener);
        TranscriptionResult result;
        try {
            result = whisperer.transcribe(samples);
        } finally {
            whisperer.removeListener(whisperListener);
        }

        List<TranscriptionParagraph> paragraphs = new ArrayList<>();
        List<SegmentResult> segments = result.getSegments();

        if (!segments.isEmpty()) {
            for (SegmentResult segment : segments) {
                String text = segment.getText();
                if (!text.trim().isEmpty()) {
                    paragraphs.add(new TranscriptionParagraph(text.trim(), null, null));
                }
            }
        } else {
            String fullText = result.getText();
            if (!fullText.trim().isEmpty()) {
                paragraphs.add(new TranscriptionParagraph(fullText.trim(), null, null));
            }
        }

        if (listener != null) {
            long totalLengthMs = (wavFile.length() - 44) / 32;
            if (partialResults.isEmpty() && !paragraphs.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (TranscriptionParagraph p : paragraphs) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(p.getRawText());
                }
                partialResults.add(new PartialRecognitionResult(sb.toString(), 0, totalLengthMs));
            }
            FinalRecognitionResult finalResult = new FinalRecognitionResult(partialResults, wavFile, totalLengthMs);
            listener.onResult(finalResult);
        }

        return paragraphs;
    }

    /**
     * Creates a Whisper transcription listener for receiving partial segments.
     *
     * @param listener       The application transcription listener.
     * @param partialResults The list accumulating valid partial recognition results.
     * @return The AndroidWhisperer transcription listener instance.
     */
    private de.switchconsulting.androidwhisperer.TranscriptionListener createWhisperListener(
            TranscriptionListener listener, List<PartialRecognitionResult> partialResults) {
        return new de.switchconsulting.androidwhisperer.TranscriptionListener() {
            @Override
            public void onNewSegment(@NonNull SegmentResult segment, @NonNull String accumulatedTranscript) {
                String text = segment.getText();
                if (!text.trim().isEmpty()) {
                    PartialRecognitionResult pr = new PartialRecognitionResult(
                            text.trim(), segment.getStartMs(), segment.getEndMs());
                    partialResults.add(pr);
                    if (listener != null) {
                        listener.onPartialRecognition(pr);
                    }
                }
            }

            @Override
            public void onProgress(int progressPercent) {
            }

            @Override
            public void onError(@NonNull de.switchconsulting.androidwhisperer.TranscriptionException exception) {
                Log.e(TAG, "AndroidWhisperer encountered an error during transcription", exception);
            }
        };
    }

    /**
     * Returns the transcriber engine type for Whisper.
     *
     * @return TranscriberType.WHISPER.
     */
    @Override
    public TranscriberType getType() {
        return TranscriberType.WHISPER;
    }

    /**
     * Returns whether Whisper provides punctuation in its output.
     *
     * @return True because Whisper provides punctuation and casing natively.
     */
    @Override
    public boolean providesPunctuation() {
        return true;
    }

    /**
     * Releases resources held by the Whisper engine.
     */
    @Override
    public void close() {
        if (whisperer != null) {
            whisperer.close();
            whisperer = null;
        }
        loadedLocale = null;
    }
}
