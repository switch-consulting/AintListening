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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main activity of the application that handles audio transcription using Vosk.
 * It processes incoming audio shares (Intents) and displays the resulting transcript.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AintListening";

    private LinearProgressIndicator progressIndicator;
    private TextView statusTextView;
    private TranscriptionAdapter transcriptionAdapter;
    private Persistency persistency;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Transcriber transcriber = new VoskTranscriber();
    private SmartFormatter smartFormatter;
    private int selectedModelIndex = 0;

    /**
     * Initializes the activity, sets up the UI components, and handles incoming intents.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        persistency = new Persistency(this);
        selectedModelIndex = 0;

        progressIndicator = findViewById(R.id.progressIndicator);
        statusTextView = findViewById(R.id.statusTextView);
        RecyclerView transcriptRecyclerView = findViewById(R.id.transcriptRecyclerView);
        transcriptionAdapter = new TranscriptionAdapter();
        transcriptRecyclerView.setAdapter(transcriptionAdapter);

        MaterialButton configureButton = findViewById(R.id.configureButton);
        MaterialButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());
        configureButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ModelManagementActivity.class);
            startActivity(intent);
        });

        updateAvailableLanguagesUI();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAvailableLanguagesUI();
    }

    /**
     * Updates the UI to show which transcription models are currently installed.
     */
    private void updateAvailableLanguagesUI() {
        TextView supportedLanguagesText = findViewById(R.id.supportedLanguagesText);
        List<String> available = ModelManager.getAvailableLanguageNames(this);
        
        if (available.isEmpty()) {
            supportedLanguagesText.setText(R.string.status_no_models_installed);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < available.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(available.get(i));
            }
            supportedLanguagesText.setText(sb.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        if (transcriber != null) {
            transcriber.close();
        }
        if (smartFormatter != null) {
            smartFormatter.close();
        }
        if (transcriptionAdapter != null) {
            transcriptionAdapter.release();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    /**
     * Handles an incoming ACTION_SEND intent containing audio data.
     * If the intent is not a valid audio share, it loads the last saved transcription.
     *
     * @param intent The intent to handle.
     */
    private void handleIncomingIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || type == null || !type.startsWith("audio/")) {
            progressIndicator.setVisibility(View.GONE);
            loadLastMessage();
            return;
        }

        Uri audioUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
        if (audioUri == null) {
            progressIndicator.setVisibility(View.GONE);
            showError(getString(R.string.error_no_stream));
            return;
        }

        checkModelsAndProceed(audioUri);
    }

    /**
     * Checks which models are downloaded and proceeds with transcription or prompts for language selection.
     *
     * @param audioUri The URI of the audio to transcribe.
     */
    private void checkModelsAndProceed(Uri audioUri) {
        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < ModelManager.SUPPORTED_LANGUAGES.length; i++) {
            if (ModelManager.SUPPORTED_LANGUAGES[i].isTranscriptionDownloaded(this)) {
                availableIndices.add(i);
            }
        }

        if (availableIndices.isEmpty()) {
            showError(getString(R.string.status_no_models_installed));
            return;
        }

        if (availableIndices.size() == 1) {
            selectedModelIndex = availableIndices.get(0);
            startTranscription(audioUri);
        } else {
            showLanguageSelectionDialog(availableIndices, audioUri);
        }
    }

    /**
     * Shows a dialog for the user to select the transcription language when multiple models are available.
     *
     * @param availableIndices The indices of the available models in {@link ModelManager#SUPPORTED_LANGUAGES}.
     * @param audioUri         The URI of the audio to transcribe.
     */
    private void showLanguageSelectionDialog(List<Integer> availableIndices, Uri audioUri) {
        String[] languages = new String[availableIndices.size()];
        for (int i = 0; i < availableIndices.size(); i++) {
            languages[i] = ModelManager.SUPPORTED_LANGUAGES[availableIndices.get(i)].getLocale().getDisplayName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_select_transcription_language)
                .setItems(languages, (dialog, which) -> {
                    selectedModelIndex = availableIndices.get(which);
                    startTranscription(audioUri);
                })
                .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                    progressIndicator.setVisibility(View.GONE);
                    showInfo(getString(R.string.intro_instruction));
                })
                .show();
    }

    /**
     * Prepares the UI and starts the transcription process in the background.
     *
     * @param audioUri The URI of the audio to transcribe.
     */
    private void startTranscription(Uri audioUri) {
        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setIndeterminate(true);
        transcriptionAdapter.setParagraphs(new ArrayList<>());
        showStatus(getString(R.string.status_preparing));

        executorService.execute(() -> transcribeFromUri(audioUri));
    }

    /**
     * Decodes the audio from the URI into a WAV file suitable for Vosk.
     *
     * @param audioUri The source audio URI.
     */
    private void transcribeFromUri(@NonNull Uri audioUri) {
        File wavFile = new File(getCacheDir(), "incoming_audio_16k_mono.wav");

        try {
            showStatus(getString(R.string.status_converting));

            boolean success = OpusToWavDecoder.decodeOpusToWav(this, audioUri, wavFile);

            if (success) {
                executorService.execute(() -> runVoskRecognition(wavFile));
            } else {
                Log.e(TAG, "Native conversion failed");
                showError(getString(R.string.error_conversion_failed));
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed preparing audio", e);
            showError(getString(R.string.error_read_failed));
        }
    }

    /**
     * Runs the Vosk recognition engine on the provided WAV file.
     *
     * @param wavFile The audio file to transcribe.
     */
    private void runVoskRecognition(@NonNull File wavFile) {
        try {
            showStatus(getString(R.string.status_loading_model));
            transcriber.ensureModelLoaded(this, selectedModelIndex);
            showStatus(getString(R.string.status_transcribing));

            List<TranscriptionParagraph> paragraphList = transcriber.transcribe(this, wavFile, new TranscriptionListener() {
                @Override
                public void onPartialResult(String text) {
                    updateTranscriptUI(text);
                }

                @Override
                public void onResult(String text) {
                    updateTranscriptUI(text);
                }
            });

            applySmartFormattingAndDisplay(paragraphList, selectedModelIndex, true);
        } catch (Exception e) {
            Log.e(TAG, "Vosk transcription failed", e);
            showError(getString(R.string.error_transcription_failed));
        }
    }

    /**
     * Applies smart formatting (punctuation, casing) to the raw transcription paragraphs using an ONNX model.
     * Displays the progress and final result in the UI.
     *
     * @param paragraphs The raw transcription paragraphs.
     * @param modelIndex The index of the language model used.
     * @param shouldSave Whether the result should be saved to persistent storage.
     */
    private void applySmartFormattingAndDisplay(List<TranscriptionParagraph> paragraphs, int modelIndex, boolean shouldSave) {
        executorService.execute(() -> {
            LanguageSupport selectedLanguage = ModelManager.SUPPORTED_LANGUAGES[modelIndex];
            List<TranscriptionParagraph> paragraphList = new ArrayList<>();

            if (selectedLanguage.isFormattingDownloaded(this) && !paragraphs.isEmpty()) {
                try {
                    showStatus(getString(R.string.status_applying_smart_formatting));
                    ModelInfo targetModel = selectedLanguage.getFormattingModel();
                    if (targetModel != null) {
                        if (smartFormatter != null && !smartFormatter.getModelInfo().equals(targetModel)) {
                            smartFormatter.close();
                            smartFormatter = null;
                        }
                        if (smartFormatter == null) {
                            smartFormatter = new OnnxSmartFormatter(this, targetModel);
                        }
                    }

                    runOnUiThread(() -> {
                        progressIndicator.setVisibility(View.VISIBLE);
                        progressIndicator.setIndeterminate(false);
                        progressIndicator.setMax(paragraphs.size());
                        progressIndicator.setProgress(0);
                    });

                    for (int i = 0; i < paragraphs.size(); i++) {
                        TranscriptionParagraph p = paragraphs.get(i);
                        String rawPara = p.getRawText();
                        if (rawPara.trim().isEmpty()) continue;

                        String formattedPara = smartFormatter.format(rawPara);
                        Log.d(TAG, "Formatting paragraph " + (i + 1) + "/" + paragraphs.size());

                        TranscriptionParagraph formattedP = new TranscriptionParagraph(rawPara, formattedPara, p.getAudioFilePath());
                        paragraphList.add(formattedP);
                        
                        // Show already formatted paragraphs + remaining raw ones
                        final List<TranscriptionParagraph> currentDisplayList = new ArrayList<>(paragraphList);
                        for (int j = i + 1; j < paragraphs.size(); j++) {
                            currentDisplayList.add(paragraphs.get(j));
                        }
                        
                        final int progress = i + 1;
                        runOnUiThread(() -> {
                            transcriptionAdapter.setParagraphs(currentDisplayList);
                            progressIndicator.setProgress(progress);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Smart formatting failed", e);
                    paragraphList.clear();
                    paragraphList.addAll(paragraphs);
                }
            } else {
                Log.d(TAG, "Smart formatting skipped: model not downloaded or transcript empty.");
                paragraphList.addAll(paragraphs);
            }

            if (paragraphList.isEmpty()) {
                showStatus(getString(R.string.status_no_speech));
                runOnUiThread(() -> progressIndicator.setVisibility(View.GONE));
            } else {
                runOnUiThread(() -> {
                    statusTextView.setVisibility(View.GONE);
                    progressIndicator.setVisibility(View.GONE);
                    transcriptionAdapter.setParagraphs(paragraphList);
                    if (shouldSave) {
                        persistency.saveLastMessage(paragraphList, modelIndex);
                    }
                });
            }
        });
    }

    /**
     * Updates the transcription adapter with new text (partial or full results).
     *
     * @param text The transcription text.
     */
    private void updateTranscriptUI(String text) {
        runOnUiThread(() -> {
            if (text.trim().isEmpty()) return;
            String[] paras = text.split("\n\n");
            List<TranscriptionParagraph> pList = new ArrayList<>();
            for (String p : paras) {
                if (!p.trim().isEmpty()) {
                    pList.add(new TranscriptionParagraph(p.trim(), null));
                }
            }
            transcriptionAdapter.setParagraphs(pList);
        });
    }

    /**
     * Shows a status message to the user.
     *
     * @param message The status message.
     */
    private void showStatus(String message) {
        runOnUiThread(() -> {
            statusTextView.setText(message);
            statusTextView.setVisibility(View.VISIBLE);
        });
    }

    private void showInfo(String message) {
        runOnUiThread(() -> {
            statusTextView.setText(message);
            statusTextView.setVisibility(View.VISIBLE);
            transcriptionAdapter.setParagraphs(new ArrayList<>());
        });
    }

    /**
     * Shows an error message to the user via UI and Toast.
     *
     * @param message The error message.
     */
    private void showError(@NonNull String message) {
        runOnUiThread(() -> {
            progressIndicator.setVisibility(View.GONE);
            statusTextView.setText(message);
            statusTextView.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Loads the last saved transcription from persistent storage.
     */
    private void loadLastMessage() {
        List<TranscriptionParagraph> paragraphs = persistency.loadLastMessage();

        if (paragraphs != null) {
            transcriptionAdapter.setParagraphs(paragraphs);
        } else {
            // Fallback to old format if present
            String lastMessage = persistency.loadLegacyLastMessage();
            if (lastMessage != null) {
                int lastModelIndex = persistency.loadLastModelIndex();
                String[] rawParas = lastMessage.split("\n\n");
                List<TranscriptionParagraph> pList = new ArrayList<>();
                for (String rp : rawParas) {
                    if (!rp.trim().isEmpty()) {
                        pList.add(new TranscriptionParagraph(rp, null));
                    }
                }
                applySmartFormattingAndDisplay(pList, lastModelIndex, false);
            } else {
                showInfo(getString(R.string.intro_instruction));
            }
        }
    }
}
