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

package de.switchconsulting.aintlistening.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelManager;
import de.switchconsulting.aintlistening.data.Persistency;

/**
 * The main activity of the application that handles audio transcription using Vosk.
 * It processes incoming audio shares (Intents) and displays the resulting transcript.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private LinearProgressIndicator progressIndicator;
    private TextView statusTextView;
    private TranscriptionAdapter transcriptionAdapter;
    private MainViewModel viewModel;

    @Inject
    Persistency persistency;

    /**
     * Called when the activity is first created. Initializes the UI and ViewModel.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        progressIndicator = findViewById(R.id.progressIndicator);
        statusTextView = findViewById(R.id.statusTextView);
        RecyclerView transcriptRecyclerView = findViewById(R.id.transcriptRecyclerView);
        transcriptionAdapter = new TranscriptionAdapter(getUiDisplaySettings());
        transcriptRecyclerView.setAdapter(transcriptionAdapter);

        MaterialButton configureButton = findViewById(R.id.configureButton);
        MaterialButton closeButton = findViewById(R.id.closeButton);
        MaterialButton aboutButton = findViewById(R.id.aboutButton);

        closeButton.setOnClickListener(v -> finish());
        configureButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ModelManagementActivity.class);
            startActivity(intent);
        });
        aboutButton.setOnClickListener(v -> showAboutDialog());

        viewModel.uiState.observe(this, this::handleUiState);

        updateAvailableLanguagesUI();
        handleIncomingIntent(getIntent());
    }

    /**
     * Called when the activity is being destroyed. Releases adapter resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transcriptionAdapter != null) {
            transcriptionAdapter.release();
        }
    }

    /**
     * Processes changes in the MainUiState and updates the UI accordingly.
     *
     * @param state The new state to display.
     */
    private void handleUiState(MainUiState state) {
        if (state == null) return;

        progressIndicator.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        progressIndicator.setIndeterminate(state.isIndeterminate());
        if (!state.isIndeterminate()) {
            progressIndicator.setMax(state.maxProgress());
            progressIndicator.setProgress(state.progress());
        }

        if (state.statusMessage() != null) {
            statusTextView.setText(state.statusMessage());
            statusTextView.setVisibility(View.VISIBLE);
        } else if (state.paragraphs() != null && !state.paragraphs().isEmpty()) {
            statusTextView.setVisibility(View.GONE);
        }

        if (state.paragraphs() != null) {
            transcriptionAdapter.setParagraphs(state.paragraphs());
        }

        if (state.errorMessage() != null) {
            Toast.makeText(this, state.errorMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the activity is resumed. Refreshes the available languages and UI toggle states.
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateAvailableLanguagesUI();
        if (transcriptionAdapter != null) {
            transcriptionAdapter.setDisplaySettings(getUiDisplaySettings());
        }
    }

    /**
     * Creates a UiDisplaySettings instance reflecting current persistency preferences.
     *
     * @return Current UiDisplaySettings.
     */
    private UiDisplaySettings getUiDisplaySettings() {
        return new UiDisplaySettings(
                persistency.isShowPlaybackButton(),
                persistency.isShowCopyButton(),
                persistency.isShowRawText(),
                persistency.isShowSmartText()
        );
    }

    /**
     * Updates the text view displaying the currently installed language models.
     */
    private void updateAvailableLanguagesUI() {
        TextView supportedLanguagesText = findViewById(R.id.supportedLanguagesText);
        List<String> available = ModelManager.getAvailableLanguageNames(this);

        if (available.isEmpty()) {
            supportedLanguagesText.setText(R.string.status_no_models_installed);
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String lang : available) {
                if (!first) sb.append(", ");
                sb.append(lang);
                first = false;
            }
            supportedLanguagesText.setText(sb.toString());
        }
    }

    /**
     * Handle incoming intents, for instance when the application is already running.
     *
     * @param intent The incoming intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    /**
     * Parses the incoming intent and triggers transcription if an audio stream is found.
     *
     * @param intent The intent to handle.
     */
    private void handleIncomingIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || type == null || !type.startsWith("audio/")) {
            viewModel.loadLastMessage();
            return;
        }

        Uri audioUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
        if (audioUri == null) {
            Toast.makeText(this, R.string.error_no_stream, Toast.LENGTH_SHORT).show();
            return;
        }

        checkModelsAndProceed(audioUri);
    }

    /**
     * Checks which models are installed and either starts transcription or prompts the user for language selection.
     *
     * @param audioUri The URI of the audio to transcribe.
     */
    private void checkModelsAndProceed(Uri audioUri) {
        List<LanguageSupport> availableLanguages = new ArrayList<>();
        for (LanguageSupport lang : ModelManager.SUPPORTED_LANGUAGES) {
            boolean isEnabled = persistency.isLanguageEnabled(lang.getLocale());
            boolean isDownloaded = lang.hasTranscriptionModelDownloaded(this);
            if (isEnabled && isDownloaded) {
                availableLanguages.add(lang);
            }
        }

        if (availableLanguages.isEmpty()) {
            Toast.makeText(this, R.string.status_no_models_installed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (availableLanguages.size() == 1) {
            viewModel.startTranscription(audioUri, availableLanguages.get(0).getLocale());
        } else {
            showLanguageSelectionDialog(availableLanguages, audioUri);
        }
    }

    /**
     * Displays a dialog for the user to select the language for transcription.
     *
     * @param availableLanguages The available language supports.
     * @param audioUri           The URI of the audio to transcribe.
     */
    private void showLanguageSelectionDialog(List<LanguageSupport> availableLanguages, Uri audioUri) {
        String[] languages = new String[availableLanguages.size()];
        for (int i = 0; i < availableLanguages.size(); i++) {
            languages[i] = availableLanguages.get(i).getLocale().getDisplayName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_select_transcription_language)
                .setItems(languages, (dialog, which) -> viewModel.startTranscription(audioUri, availableLanguages.get(which).getLocale()))
                .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                    statusTextView.setText(R.string.intro_instruction);
                    statusTextView.setVisibility(View.VISIBLE);
                })
                .show();
    }

    /**
     * Displays the About and Licenses dialog with author info, links, and open-source attributions.
     */
    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_about_title)
                .setMessage(R.string.dialog_about_message)
                .setPositiveButton(R.string.button_close, (dialog, which) -> dialog.dismiss())
                .setNeutralButton(R.string.button_visit_website, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://switch-consulting.de/"));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.button_visit_github, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/switch-consulting/AintListening"));
                    startActivity(intent);
                })
                .show();
    }
}
