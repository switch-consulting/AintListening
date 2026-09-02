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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main activity of the application that handles audio transcription using Vosk.
 * It processes incoming audio shares (Intents) and displays the resulting transcript.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AintListening";
    private static final String PREFS_NAME = "AintListeningPrefs";
    private static final String KEY_LAST_MESSAGE = "last_message";

    private LinearProgressIndicator progressIndicator;
    private TextView transcriptTextView;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Transcriber transcriber = new Transcriber();
    private SmartFormatter smartFormatter;
    private int selectedModelIndex = 0;

    /**
     * Initializes the activity, sets up UI components, and handles any incoming intent.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep it for German now as requested
        selectedModelIndex = 0;

        progressIndicator = findViewById(R.id.progressIndicator);
        transcriptTextView = findViewById(R.id.transcriptTextView);
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

    /**
     * Refreshes the UI when the activity resumes, specifically the list of available languages.
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateAvailableLanguagesUI();
    }

    /**
     * Updates the UI to show which transcription languages (models) are currently installed.
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

    /**
     * Shuts down the executor service and closes the Vosk model when the activity is destroyed.
     */
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
    }

    /**
     * Handles new intents received while the activity is running.
     *
     * @param intent The new intent that was started for the activity.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    /**
     * Processes an incoming intent, checking if it contains an audio stream to transcribe.
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
            transcriptTextView.setText(R.string.error_no_stream);
            return;
        }

        checkModelsAndProceed(audioUri);
    }

    /**
     * Checks which models are downloaded and decides whether to start transcription
     * or show a language selection dialog.
     *
     * @param audioUri The URI of the audio to transcribe.
     */
    private void checkModelsAndProceed(Uri audioUri) {
        List<Integer> availableIndices = new java.util.ArrayList<>();
        for (int i = 0; i < ModelManager.SUPPORTED_MODELS.length; i++) {
            if (ModelManager.isModelDownloaded(this, ModelManager.SUPPORTED_MODELS[i])) {
                availableIndices.add(i);
            }
        }

        if (availableIndices.isEmpty()) {
            showError(getString(R.string.status_no_models_installed));
            return;
        }

        if (availableIndices.size() == 1) {
            // Only one model, use it automatically
            selectedModelIndex = availableIndices.get(0);
            startTranscription(audioUri);
        } else {
            // Multiple models, ask the user
            showLanguageSelectionDialog(availableIndices, audioUri);
        }
    }

    /**
     * Shows a dialog allowing the user to select the language for transcription.
     *
     * @param availableIndices The indices of available models in ModelManager.SUPPORTED_MODELS.
     * @param audioUri         The URI of the audio to transcribe.
     */
    private void showLanguageSelectionDialog(List<Integer> availableIndices, Uri audioUri) {
        String[] languages = new String[availableIndices.size()];
        for (int i = 0; i < availableIndices.size(); i++) {
            languages[i] = ModelManager.SUPPORTED_MODELS[availableIndices.get(i)].displayName;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_select_transcription_language)
                .setItems(languages, (dialog, which) -> {
                    selectedModelIndex = availableIndices.get(which);
                    startTranscription(audioUri);
                })
                .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                    progressIndicator.setVisibility(View.GONE);
                    transcriptTextView.setText(R.string.intro_instruction);
                })
                .show();
    }

    /**
     * Starts the transcription process by updating the UI and executing the transcription task.
     *
     * @param audioUri The URI of the audio to transcribe.
     */
    private void startTranscription(Uri audioUri) {
        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setIndeterminate(true);
        transcriptTextView.setText(R.string.status_preparing);

        executorService.execute(() -> transcribeFromUri(audioUri));
    }

    /**
     * Decodes the audio from a URI to a WAV file suitable for Vosk and then runs recognition.
     *
     * @param audioUri The URI of the audio to transcribe.
     */
    private void transcribeFromUri(@NonNull Uri audioUri) {
        File wavFile = new File(getCacheDir(), "incoming_audio_16k_mono.wav");

        try {
            runOnUiThread(() -> transcriptTextView.setText(R.string.status_converting));

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
     * Loads the speech model (if needed) and runs the Vosk recognition on the given WAV file.
     *
     * @param wavFile The WAV file to recognize.
     */
    private void runVoskRecognition(@NonNull File wavFile) {
        try {
            runOnUiThread(() -> transcriptTextView.setText(R.string.status_loading_model));

            transcriber.ensureModelLoaded(this, selectedModelIndex);

            runOnUiThread(() -> transcriptTextView.setText(R.string.status_transcribing));

            String transcript = transcriber.transcribe(wavFile, new TranscriptionListener() {
                @Override
                public void onPartialResult(String text) {
                    updateTranscriptUI(text);
                }

                @Override
                public void onResult(String text) {
                    updateTranscriptUI(text);
                }
            });

            String finalTranscript = transcript;
            // selectedModelIndex 0 is Deutsch
            if (selectedModelIndex == 0 && ModelManager.isModelDownloaded(this, ModelManager.SUPPORTED_SMART_FORMATTING_MODELS[0])) {
                Log.i(TAG, "Smart formatting model is available for German. Applying paragraph-wise...");
                try {
                    if (smartFormatter == null) {
                        smartFormatter = new SmartFormatter(this);
                    }
                    
                    String[] paragraphs = transcript.split("\n\n");
                    runOnUiThread(() -> {
                        progressIndicator.setIndeterminate(false);
                        progressIndicator.setMax(paragraphs.length);
                        progressIndicator.setProgress(0);
                    });

                    StringBuilder currentFormatted = new StringBuilder();
                    for (int i = 0; i < paragraphs.length; i++) {
                        String para = paragraphs[i];
                        if (para.trim().isEmpty()) {
                            if (i > 0) currentFormatted.append("\n\n");
                            currentFormatted.append(para);
                        } else {
                            String formattedPara = smartFormatter.format(para);
                            if (i > 0) currentFormatted.append("\n\n");
                            currentFormatted.append(formattedPara);
                        }
                        
                        // Construct the full text for stepwise UI update
                        StringBuilder fullDisplay = new StringBuilder(currentFormatted);
                        for (int j = i + 1; j < paragraphs.length; j++) {
                            fullDisplay.append("\n\n").append(paragraphs[j]);
                        }
                        
                        final String displayUpdate = fullDisplay.toString().trim();
                        final int progress = i + 1;
                        runOnUiThread(() -> {
                            transcriptTextView.setText(displayUpdate);
                            progressIndicator.setProgress(progress);
                        });
                    }
                    finalTranscript = currentFormatted.toString();
                } catch (Exception e) {
                    Log.e(TAG, "Smart formatting failed", e);
                }
            } else {
                Log.d(TAG, "Smart formatting skipped (not German or model not downloaded).");
            }

            String result = finalTranscript.trim().isEmpty()
                    ? getString(R.string.status_no_speech)
                    : finalTranscript.trim();

            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                transcriptTextView.setText(result);
                if (!result.equals(getString(R.string.status_no_speech))) {
                    saveLastMessage(result);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Vosk transcription failed", e);
            showError(getString(R.string.error_transcription_failed));
        }
    }

    /**
     * Updates the transcript TextView with the provided text on the UI thread.
     *
     * @param text The text to display.
     */
    private void updateTranscriptUI(String text) {
        runOnUiThread(() -> transcriptTextView.setText(text.trim()));
    }

    /**
     * Shows an error message in the UI and as a Toast.
     *
     * @param message The error message to display.
     */
    private void showError(@NonNull String message) {
        runOnUiThread(() -> {
            progressIndicator.setVisibility(View.GONE);
            transcriptTextView.setText(message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Saves the last transcribed message to SharedPreferences.
     *
     * @param transcript The transcription text to save.
     */
    private void saveLastMessage(String transcript) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_MESSAGE, transcript)
                .apply();
    }

    /**
     * Loads and displays the last transcribed message from SharedPreferences.
     */
    private void loadLastMessage() {
        String lastMessage = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_LAST_MESSAGE, null);

        if (lastMessage != null) {
            String displayedText = getString(R.string.last_message_header) + "\n\n" + lastMessage;
            transcriptTextView.setText(displayedText);
        } else {
            transcriptTextView.setText(R.string.intro_instruction);
        }
    }
}
