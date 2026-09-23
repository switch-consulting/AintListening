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

import com.redravencomputing.whispercore.Whisper;
import com.redravencomputing.whispercore.WhisperDelegate;
import com.redravencomputing.whispercore.WhisperOperationError;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelInfo;
import de.switchconsulting.aintlistening.data.ModelManager;
import kotlin.Unit;

/**
 * Handles the speech-to-text transcription process using the whisper.cpp library.
 */
@Singleton
public class WhisperTranscriber implements Transcriber {
    private static final String TAG = "WhisperTranscriber";
    public static final int INCREMENTAL_UPDATE_POLLING_INTERVALL_MS = 100;
    public static final int SILENCE_GAP_MS = 100;

    @Inject
    public WhisperTranscriber() {
    }

    private Whisper whisper;
    private Locale loadedLocale;
    private boolean enableIncrementalUpdates = true;

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*-->\\s*(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})]\\s*(.*)");

    /**
     * Sets whether to enable incremental transcription updates via log polling.
     *
     * @param enable True to enable incremental updates, false to only show the final result.
     */
    public void setEnableIncrementalUpdates(boolean enable) {
        this.enableIncrementalUpdates = enable;
    }

    @Override
    public void ensureModelLoaded(Context context, Locale locale) throws Exception {
        if (whisper != null && Objects.equals(loadedLocale, locale) && whisper.isModelLoaded()) {
            return;
        }

        LanguageSupport language = ModelManager.getLanguageSupport(locale);
        if (language == null) {
            throw new IllegalStateException("Unsupported language locale: " + (locale != null ? locale.getDisplayName() : "null"));
        }
        ModelInfo modelInfo = language.getModel(getType());
        if (!language.isDownloaded(context, getType()) || modelInfo == null) {
            throw new IllegalStateException("Whisper model not found for language: " + language.getLocale().getDisplayName());
        }

        File modelFile = new File(context.getFilesDir(), modelInfo.name);
        Log.i(TAG, "Loading Whisper model from: " + modelFile.getAbsolutePath());

        if (whisper == null) {
            whisper = new Whisper(context.getApplicationContext());
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
        loadedLocale = locale;
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
        // Enable timestamps to allow splitting into paragraphs with audio chunks
        whisper.transcribeAudioFile(wavFile, true, true);

        TranscriptionContext transcriptionContext = new TranscriptionContext(wavFile, listener);
        int lastLogLineCount = 0;

        if (enableIncrementalUpdates) {
            while (!future.isDone()) {
                try {
                    Thread.sleep(INCREMENTAL_UPDATE_POLLING_INTERVALL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                String logs = whisper.getMessageLogs();
                if (!TextUtils.isEmpty(logs)) {
                    String[] logLines = logs.split("\n");
                    if (logLines.length > lastLogLineCount) {
                        List<String> newLines = new ArrayList<>(Arrays.asList(logLines).subList(lastLogLineCount, logLines.length));
                        processTranscriptionLines(newLines, transcriptionContext, false);
                        lastLogLineCount = logLines.length;
                    }
                }
            }
        }

        String result = future.get(10, TimeUnit.MINUTES);
        if (result != null && !result.trim().isEmpty()) {
            if (!enableIncrementalUpdates) {
                processTranscriptionLines(Arrays.asList(result.split("\n")), transcriptionContext, true);
            } else {
                finalizeRemaining(transcriptionContext);
            }
        }

        return transcriptionContext.paragraphs;
    }

    private void processTranscriptionLines(List<String> lines, TranscriptionContext ctx, boolean isFinal) {
        for (String line : lines) {
            Matcher matcher = TIMESTAMP_PATTERN.matcher(line);
            if (matcher.find()) {
                String startTs = matcher.group(1);
                String endTs = matcher.group(2);
                String segmentRaw = matcher.group(3);
                String segmentText = segmentRaw != null ? segmentRaw.trim() : "";

                // Cleaning of leading artifacts
                while (segmentText.startsWith(":") || segmentText.startsWith(".") ||
                        segmentText.startsWith("-") || segmentText.startsWith(" ")) {
                    segmentText = segmentText.substring(1).trim();
                }

                if (segmentText.isEmpty()) continue;

                long startMs = parseTimestampToMs(startTs);
                long endMs = parseTimestampToMs(endTs);

                // Skip if this segment has already been processed (by checking end timestamp)
                if (startMs < ctx.lastSegmentEndMs) continue;

                // Detect pauses between segments
                long silenceGapMs = (ctx.lastSegmentEndMs != -1) ? (startMs - ctx.lastSegmentEndMs) : 0;
                boolean isSignificantPause = silenceGapMs > SILENCE_GAP_MS;

                // Decide if we should start a new paragraph
                boolean isEndOfSentence = false;
                if (!TextUtils.isEmpty(ctx.currentParaText)) {
                    String currentTextStr = ctx.currentParaText.toString();
                    isEndOfSentence = currentTextStr.endsWith(".") || currentTextStr.endsWith("?") || currentTextStr.endsWith("!");
                }

                boolean shouldSplit = (isSignificantPause && isEndOfSentence) || (ctx.currentParaText.length() > 400 && isEndOfSentence);

                if (shouldSplit && ctx.currentParaStartMs != -1) {
                    processParagraph(ctx.currentParaText.toString(), ctx.currentParaStartMs, ctx.currentParaEndMs, ctx.wavFile, ctx.chunkIndex++, ctx.paragraphs, ctx.fullText, ctx.listener);
                    ctx.currentParaText.setLength(0);
                    ctx.currentParaStartMs = -1;
                }

                if (ctx.currentParaStartMs == -1) {
                    ctx.currentParaStartMs = startMs;
                }

                if (!TextUtils.isEmpty(ctx.currentParaText)) {
                    ctx.currentParaText.append(" ");
                }
                ctx.currentParaText.append(segmentText);
                ctx.currentParaEndMs = endMs;
                ctx.lastSegmentEndMs = endMs;

                // For incremental updates, show partial results
                if (!isFinal && ctx.listener != null) {
                    String currentDisplay = ctx.fullText.toString();
                    if (!TextUtils.isEmpty(currentDisplay)) {
                        currentDisplay += "\n\n";
                    }
                    ctx.listener.onPartialResult(currentDisplay + ctx.currentParaText);
                }
            }
        }

        if (isFinal) {
            finalizeRemaining(ctx);
        }
    }

    private void finalizeRemaining(TranscriptionContext ctx) {
        if (!TextUtils.isEmpty(ctx.currentParaText)) {
            processParagraph(ctx.currentParaText.toString(), ctx.currentParaStartMs, ctx.currentParaEndMs, ctx.wavFile, ctx.chunkIndex, ctx.paragraphs, ctx.fullText, ctx.listener);
            ctx.currentParaText.setLength(0);
            ctx.currentParaStartMs = -1;
        }
    }

    private void processParagraph(String text, long startMs, long endMs, File wavFile, int chunkIndex,
                                  List<TranscriptionParagraph> paragraphs, StringBuilder fullText,
                                  TranscriptionListener listener) {
        
        byte[] pcmData = extractPcm(wavFile, startMs, endMs);
        String audioPath = null;
        if (pcmData.length > 0 && listener != null) {
            audioPath = listener.onAudioChunkAvailable(pcmData, chunkIndex);
        }

        TranscriptionParagraph p = new TranscriptionParagraph(text, null, audioPath);
        paragraphs.add(p);

        if (!TextUtils.isEmpty(fullText)) {
            fullText.append("\n\n");
        }
        fullText.append(text);
        
        if (listener != null) {
            listener.onResult(fullText.toString());
        }
    }

    private long parseTimestampToMs(String ts) {
        try {
            String[] parts = ts.split(":");
            long h = Long.parseLong(parts[0]);
            long m = Long.parseLong(parts[1]);
            String[] secParts = parts[2].split("\\.");
            long s = Long.parseLong(secParts[0]);
            long ms = Long.parseLong(secParts[1]);
            return h * 3600000 + m * 60000 + s * 1000 + ms;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse timestamp: " + ts, e);
            return 0;
        }
    }

    private byte[] extractPcm(File wavFile, long startMs, long endMs) {
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "r")) {
            // 16kHz, 16-bit Mono PCM = 16 samples/ms * 2 bytes/sample = 32 bytes/ms
            long startByte = 44 + (startMs * 32);
            long endByte = 44 + (endMs * 32);

            if (startByte >= raf.length()) return new byte[0];
            if (endByte > raf.length()) endByte = raf.length();

            int length = (int) (endByte - startByte);
            if (length <= 0) return new byte[0];

            byte[] pcm = new byte[length];
            raf.seek(startByte);
            raf.readFully(pcm);
            return pcm;
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract PCM from " + wavFile.getAbsolutePath(), e);
            return new byte[0];
        }
    }

    @Override
    public TranscriberType getType() {
        return TranscriberType.WHISPER;
    }

    @Override
    public boolean providesPunctuation() {
        return true;
    }

    @Override
    public void close() {
        if (whisper != null) {
            whisper.cleanup();
            whisper = null;
        }
        loadedLocale = null;
    }

    /**
     * Helper to maintain state during transcription.
     */
    private static class TranscriptionContext {
        final File wavFile;
        final TranscriptionListener listener;
        final List<TranscriptionParagraph> paragraphs = new ArrayList<>();
        final StringBuilder fullText = new StringBuilder();
        final StringBuilder currentParaText = new StringBuilder();
        long currentParaStartMs = -1;
        long currentParaEndMs = -1;
        long lastSegmentEndMs = -1;
        int chunkIndex = 0;

        TranscriptionContext(File wavFile, TranscriptionListener listener) {
            this.wavFile = wavFile;
            this.listener = listener;
        }
    }
}
