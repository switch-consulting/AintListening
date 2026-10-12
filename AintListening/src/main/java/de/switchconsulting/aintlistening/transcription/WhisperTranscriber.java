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
import de.switchconsulting.aintlistening.data.ModelCatalogRepository;
import de.switchconsulting.aintlistening.data.ModelInfo;
import kotlin.Unit;

/**
 * Handles the speech-to-text transcription process using the whisper.cpp library.
 */
@Singleton
public class WhisperTranscriber implements Transcriber {
    private static final String TAG = "WhisperTranscriber";
    /** Polling interval in milliseconds for incremental transcription updates. */
    public static final int INCREMENTAL_UPDATE_POLLING_INTERVAL_MS = 100;
    /** Default moderate silence gap threshold in milliseconds used to split paragraphs when punctuation is present. */
    public static final long DEFAULT_SILENCE_GAP_MS = 300;
    /** Default long silence gap threshold in milliseconds used to split paragraphs without requiring punctuation. */
    public static final long DEFAULT_LONG_SILENCE_GAP_MS = 600;

    private final ModelCatalogRepository modelRepository;
    private Whisper whisper;
    private Locale loadedLocale;
    private boolean enableIncrementalUpdates = true;
    private long silenceGapMs = DEFAULT_SILENCE_GAP_MS;
    private long longSilenceGapMs = DEFAULT_LONG_SILENCE_GAP_MS;

    /** Pattern for parsing timestamped whisper logs (e.g., [00:00.000 --> 00:05.000] or [00:00:00.000 --> 00:00:05.000] text). */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[((?:\\d{2}:)?\\d{2}:\\d{2}[.,]\\d{1,3})\\s*-->\\s*((?:\\d{2}:)?\\d{2}:\\d{2}[.,]\\d{1,3})]\\s*(.*)");

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
     * Sets whether to enable incremental transcription updates via log polling.
     *
     * @param enable True to enable incremental updates, false to only show the final result.
     */
    public void setEnableIncrementalUpdates(boolean enable) {
        this.enableIncrementalUpdates = enable;
    }

    /**
     * Sets the moderate silence gap threshold in milliseconds.
     *
     * @param silenceGapMs Gap threshold in milliseconds.
     */
    public void setSilenceGapMs(long silenceGapMs) {
        this.silenceGapMs = silenceGapMs;
    }

    /**
     * Returns the current moderate silence gap threshold in milliseconds.
     *
     * @return Gap threshold in milliseconds.
     */
    public long getSilenceGapMs() {
        return silenceGapMs;
    }

    /**
     * Sets the long silence gap threshold in milliseconds.
     *
     * @param longSilenceGapMs Long gap threshold in milliseconds.
     */
    public void setLongSilenceGapMs(long longSilenceGapMs) {
        this.longSilenceGapMs = longSilenceGapMs;
    }

    /**
     * Returns the current long silence gap threshold in milliseconds.
     *
     * @return Long gap threshold in milliseconds.
     */
    public long getLongSilenceGapMs() {
        return longSilenceGapMs;
    }

    @Override
    public void ensureModelLoaded(Context context, Locale locale) throws Exception {
        if (whisper != null && Objects.equals(loadedLocale, locale) && whisper.isModelLoaded()) {
            return;
        }

        LanguageSupport language = modelRepository.getLanguageSupport(locale);
        if (language == null) {
            throw new IllegalStateException("Unsupported language locale: " + (locale != null ? locale.getDisplayName() : "null"));
        }
        ModelInfo modelInfo = language.getModel(getType());
        if (!modelRepository.isDownloaded(language, getType()) || modelInfo == null) {
            throw new IllegalStateException("Whisper model not found for language: " + language.getLocale().getDisplayName());
        }

        File modelFile = new File(context.getFilesDir(), modelInfo.name());
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
    @SuppressWarnings("BusyWait")
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
                    //noinspection BusyWait
                    Thread.sleep(INCREMENTAL_UPDATE_POLLING_INTERVAL_MS);
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
            if (!enableIncrementalUpdates || transcriptionContext.paragraphs.isEmpty()) {
                processTranscriptionLines(Arrays.asList(result.split("\n")), transcriptionContext, true);
            } else {
                finalizeRemaining(transcriptionContext);
            }
        }

        return transcriptionContext.paragraphs;
    }

    /**
     * Parses timestamped output lines from Whisper log updates and aggregates segments into paragraphs.
     *
     * @param lines   The log lines containing timestamps and transcribed text.
     * @param ctx     The ongoing transcription state context.
     * @param isFinal True if this is the final processing pass.
     */
    void processTranscriptionLines(List<String> lines, TranscriptionContext ctx, boolean isFinal) {
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

                // Skip if this segment is completely contained in an already processed time window
                if (startMs < ctx.lastSegmentEndMs && endMs <= ctx.lastSegmentEndMs) continue;

                // Detect pauses between segments
                long currentGap = (ctx.lastSegmentEndMs != -1) ? (startMs - ctx.lastSegmentEndMs) : 0;
                if (currentGap < 0) currentGap = 0;

                // Decide if we should start a new paragraph
                boolean isEndOfSentence = false;
                boolean isClauseBoundary = false;
                if (!TextUtils.isEmpty(ctx.currentParaText)) {
                    String currentTextStr = ctx.currentParaText.toString().trim();
                    isEndOfSentence = currentTextStr.endsWith(".") || currentTextStr.endsWith("?") || currentTextStr.endsWith("!");
                    isClauseBoundary = isEndOfSentence || currentTextStr.endsWith(",") || currentTextStr.endsWith(":") ||
                            currentTextStr.endsWith(";") || currentTextStr.endsWith("-");
                }

                boolean isLongPause = (ctx.lastSegmentEndMs != -1) && (currentGap >= longSilenceGapMs);
                boolean isModeratePause = (ctx.lastSegmentEndMs != -1) && (currentGap >= silenceGapMs);

                boolean shouldSplit = isLongPause || (isModeratePause && isClauseBoundary) || (ctx.currentParaText.length() > 400 && isEndOfSentence);

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

    /**
     * Finalizes any remaining paragraph text in the transcription context upon completion.
     *
     * @param ctx The transcription context.
     */
    private void finalizeRemaining(TranscriptionContext ctx) {
        if (!TextUtils.isEmpty(ctx.currentParaText)) {
            processParagraph(ctx.currentParaText.toString(), ctx.currentParaStartMs, ctx.currentParaEndMs, ctx.wavFile, ctx.chunkIndex, ctx.paragraphs, ctx.fullText, ctx.listener);
            ctx.currentParaText.setLength(0);
            ctx.currentParaStartMs = -1;
        }
    }

    /**
     * Extracts PCM audio for a paragraph time window, saves the audio chunk, and appends the paragraph.
     *
     * @param text       The paragraph text.
     * @param startMs    The start timestamp in milliseconds.
     * @param endMs      The end timestamp in milliseconds.
     * @param wavFile    The source audio file.
     * @param chunkIndex The chunk sequence index.
     * @param paragraphs The target list of paragraphs.
     * @param fullText   The aggregated full transcript text builder.
     * @param listener   The transcription listener for callback events.
     */
    private void processParagraph(String text, long startMs, long endMs, File wavFile, int chunkIndex,
                                  List<TranscriptionParagraph> paragraphs, StringBuilder fullText,
                                  TranscriptionListener listener) {
        String cleanText = text != null ? text.trim() : "";
        if (cleanText.isEmpty()) return;

        byte[] pcmData = extractPcm(wavFile, startMs, endMs);
        String audioPath = null;
        if (pcmData.length > 0 && listener != null) {
            audioPath = listener.onAudioChunkAvailable(pcmData, chunkIndex);
        }

        TranscriptionParagraph p = new TranscriptionParagraph(cleanText, null, audioPath);
        paragraphs.add(p);

        if (!TextUtils.isEmpty(fullText)) {
            fullText.append("\n\n");
        }
        fullText.append(cleanText);

        if (listener != null) {
            listener.onResult(fullText.toString());
        }
    }

    /**
     * Parses a string formatted as HH:MM:SS.mmm or MM:SS.mmm into milliseconds.
     *
     * @param ts The timestamp string.
     * @return The time duration in milliseconds.
     */
    private long parseTimestampToMs(String ts) {
        try {
            String[] parts = ts.split(":");
            long h = 0, m = 0, s = 0, ms = 0;
            if (parts.length == 3) {
                h = Long.parseLong(parts[0]);
                m = Long.parseLong(parts[1]);
                String[] secParts = parts[2].split("[.,]");
                s = Long.parseLong(secParts[0]);
                if (secParts.length > 1) {
                    ms = Long.parseLong(padRightToThreeDigits(secParts[1]));
                }
            } else if (parts.length == 2) {
                m = Long.parseLong(parts[0]);
                String[] secParts = parts[1].split("[.,]");
                s = Long.parseLong(secParts[0]);
                if (secParts.length > 1) {
                    ms = Long.parseLong(padRightToThreeDigits(secParts[1]));
                }
            }
            return h * 3600000 + m * 60000 + s * 1000 + ms;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse timestamp: " + ts, e);
            return 0;
        }
    }

    private static String padRightToThreeDigits(String s) {
        if (s.length() >= 3) return s.substring(0, 3);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < 3) {
            sb.append('0');
        }
        return sb.toString();
    }

    /**
     * Extracts PCM audio bytes for the given time interval from a WAV file.
     *
     * @param wavFile The WAV file.
     * @param startMs The start time in milliseconds.
     * @param endMs   The end time in milliseconds.
     * @return The extracted raw PCM bytes.
     */
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
        if (whisper != null) {
            whisper.cleanup();
            whisper = null;
        }
        loadedLocale = null;
    }

    /**
     * Helper context structure to maintain state during asynchronous Whisper log polling and line parsing.
     */
    static class TranscriptionContext {
        /** The source WAV audio file. */
        final File wavFile;
        /** The transcription listener receiving callbacks. */
        final TranscriptionListener listener;
        /** List accumulating finalized transcription paragraphs. */
        final List<TranscriptionParagraph> paragraphs = new ArrayList<>();
        /** StringBuilder accumulating the full aggregated transcript text. */
        final StringBuilder fullText = new StringBuilder();
        /** StringBuilder accumulating text for the current pending paragraph. */
        final StringBuilder currentParaText = new StringBuilder();
        /** Start timestamp in milliseconds for the current paragraph, or -1 if uninitialized. */
        long currentParaStartMs = -1;
        /** End timestamp in milliseconds for the current paragraph. */
        long currentParaEndMs = -1;
        /** End timestamp in milliseconds of the last processed speech segment. */
        long lastSegmentEndMs = -1;
        /** Counter index for audio chunk creation. */
        int chunkIndex = 0;

        /**
         * Constructs a new TranscriptionContext.
         *
         * @param wavFile  The WAV audio file being transcribed.
         * @param listener The listener for transcription updates.
         */
        TranscriptionContext(File wavFile, TranscriptionListener listener) {
            this.wavFile = wavFile;
            this.listener = listener;
        }
    }
}
