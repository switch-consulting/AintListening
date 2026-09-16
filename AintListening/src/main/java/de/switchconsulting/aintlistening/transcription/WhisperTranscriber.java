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

import com.redravencomputing.whispercore.Whisper;
import com.redravencomputing.whispercore.WhisperDelegate;
import com.redravencomputing.whispercore.WhisperOperationError;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelManager;
import kotlin.Unit;

/**
 * Handles the speech-to-text transcription process using the whisper.cpp library.
 */
public class WhisperTranscriber implements Transcriber {
    private static final String TAG = "WhisperTranscriber";

    private Whisper whisper;
    private int loadedModelIndex = -1;

    @Override
    public void ensureModelLoaded(Context context, int modelIndex) throws Exception {
        if (whisper != null && loadedModelIndex == modelIndex && whisper.isModelLoaded()) {
            return;
        }

        LanguageSupport language = ModelManager.SUPPORTED_LANGUAGES[modelIndex];
        if (!language.isWhisperDownloaded(context) || language.getWhisperModel() == null) {
            throw new IllegalStateException("Whisper model not found for language: " + language.getLocale().getDisplayName());
        }

        File modelFile = new File(context.getFilesDir(), language.getWhisperModel().name);
        Log.i(TAG, "Loading Whisper model from: " + modelFile.getAbsolutePath());

        if (whisper == null) {
            whisper = new Whisper(context);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        whisper.initializeModel(modelFile.getAbsolutePath(), true, (result) -> {
            future.complete(null);
            return Unit.INSTANCE;
        });

        future.get(60, TimeUnit.SECONDS);
        if (!whisper.isModelLoaded()) {
            throw new Exception("Failed to load Whisper model");
        }
        loadedModelIndex = modelIndex;
    }

    @Override
    public List<TranscriptionParagraph> transcribe(Context context, @NonNull File wavFile, TranscriptionListener listener) throws Exception {
        if (whisper == null || !whisper.isModelLoaded()) {
            throw new IllegalStateException("Whisper model not loaded. Call ensureModelLoaded first.");
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        whisper.setDelegate(new WhisperDelegate() {
            @Override
            public void didTranscribe(@NonNull String text) {
                future.complete(text);
            }

            @Override
            public void recordingFailed(@NonNull WhisperOperationError error) {
                // Not used for file transcription
            }

            @Override
            public void failedToTranscribe(@NonNull WhisperOperationError error) {
                future.completeExceptionally(new Exception(error.toString()));
            }

            @Override
            public void permissionRequestNeeded() {
                // Not used for file transcription
            }

            @Override
            public void didStartRecording() {
                // Not used for file transcription
            }

            @Override
            public void didStopRecording() {
                // Not used for file transcription
            }
        });

        Log.d(TAG, "Starting Whisper transcription for file: " + wavFile.getAbsolutePath());
        whisper.transcribeAudioFile(wavFile, true, false);

        String result = future.get(5, TimeUnit.MINUTES);
        List<TranscriptionParagraph> paragraphs = new ArrayList<>();

        if (result != null && !result.trim().isEmpty()) {
            String[] segments = result.split("\n\n");
            for (String segment : segments) {
                if (!segment.trim().isEmpty()) {
                    paragraphs.add(new TranscriptionParagraph(segment.trim(), null));
                }
            }
            if (listener != null) {
                listener.onResult(result);
            }
        }

        return paragraphs;
    }

    @Override
    public void close() {
        if (whisper != null) {
            whisper.cleanup();
            whisper = null;
        }
        loadedModelIndex = -1;
    }
}
