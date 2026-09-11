package de.switchconsulting.aintlistening.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.LanguageSupport;
import de.switchconsulting.aintlistening.ModelInfo;
import de.switchconsulting.aintlistening.ModelManager;
import de.switchconsulting.aintlistening.OnnxSmartFormatter;
import de.switchconsulting.aintlistening.OpusToWavDecoder;
import de.switchconsulting.aintlistening.Persistency;
import de.switchconsulting.aintlistening.SmartFormatter;
import de.switchconsulting.aintlistening.Transcriber;
import de.switchconsulting.aintlistening.TranscriptionListener;
import de.switchconsulting.aintlistening.TranscriptionParagraph;

@Singleton
public class TranscriptionRepository {
    private static final String TAG = "TranscriptionRepository";

    private final Context context;
    private final Persistency persistency;
    private final Transcriber transcriber;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private SmartFormatter smartFormatter;

    public interface TranscriptionCallback {
        void onStatusUpdate(String message);
        void onPartialResult(List<TranscriptionParagraph> paragraphs);
        void onSmartFormattingProgress(int progress, int total, List<TranscriptionParagraph> paragraphs);
        void onComplete(List<TranscriptionParagraph> paragraphs);
        void onError(String message);
    }

    @Inject
    public TranscriptionRepository(@ApplicationContext Context context, Persistency persistency, Transcriber transcriber) {
        this.context = context;
        this.persistency = persistency;
        this.transcriber = transcriber;
    }

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
                transcriber.ensureModelLoaded(context, modelIndex);

                callback.onStatusUpdate("Transcribing...");
                List<TranscriptionParagraph> rawParagraphs = transcriber.transcribe(context, wavFile, new TranscriptionListener() {
                    @Override
                    public void onPartialResult(String text) {
                        callback.onPartialResult(parseParagraphs(text));
                    }

                    @Override
                    public void onResult(String text) {
                        callback.onPartialResult(parseParagraphs(text));
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

    private void applySmartFormatting(List<TranscriptionParagraph> paragraphs, int modelIndex, TranscriptionCallback callback) {
        LanguageSupport selectedLanguage = ModelManager.SUPPORTED_LANGUAGES[modelIndex];
        List<TranscriptionParagraph> formattedParagraphs = new ArrayList<>();

        if (persistency.isShowSmartText() && selectedLanguage.isFormattingDownloaded(context) && !paragraphs.isEmpty()) {
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
            formattedParagraphs.addAll(paragraphs);
        }

        persistency.saveLastMessage(formattedParagraphs, modelIndex);
        callback.onComplete(formattedParagraphs);
    }

    private List<TranscriptionParagraph> parseParagraphs(String text) {
        if (text.trim().isEmpty()) return new ArrayList<>();
        String[] paras = text.split("\n\n");
        List<TranscriptionParagraph> pList = new ArrayList<>();
        for (String p : paras) {
            if (!p.trim().isEmpty()) {
                pList.add(new TranscriptionParagraph(p.trim(), null));
            }
        }
        return pList;
    }

    public List<TranscriptionParagraph> loadLastMessage() {
        return persistency.loadLastMessage();
    }

    public void release() {
        if (transcriber != null) {
            transcriber.close();
        }
        if (smartFormatter != null) {
            smartFormatter.close();
        }
        executorService.shutdownNow();
    }
}
