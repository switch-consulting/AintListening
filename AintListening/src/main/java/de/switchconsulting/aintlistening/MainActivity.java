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
import de.switchconsulting.aintlistening.ui.MainViewModel;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        progressIndicator = findViewById(R.id.progressIndicator);
        statusTextView = findViewById(R.id.statusTextView);
        RecyclerView transcriptRecyclerView = findViewById(R.id.transcriptRecyclerView);
        transcriptionAdapter = new TranscriptionAdapter(persistency);
        transcriptRecyclerView.setAdapter(transcriptionAdapter);

        MaterialButton configureButton = findViewById(R.id.configureButton);
        MaterialButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());
        configureButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ModelManagementActivity.class);
            startActivity(intent);
        });

        viewModel.uiState.observe(this, this::handleUiState);

        updateAvailableLanguagesUI();
        handleIncomingIntent(getIntent());
    }

    private void handleUiState(MainViewModel.UiState state) {
        if (state == null) return;

        progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);
        progressIndicator.setIndeterminate(state.isIndeterminate);
        if (!state.isIndeterminate) {
            progressIndicator.setMax(state.maxProgress);
            progressIndicator.setProgress(state.progress);
        }

        if (state.statusMessage != null) {
            statusTextView.setText(state.statusMessage);
            statusTextView.setVisibility(View.VISIBLE);
        } else if (state.paragraphs != null && !state.paragraphs.isEmpty()) {
            statusTextView.setVisibility(View.GONE);
        }

        if (state.paragraphs != null) {
            transcriptionAdapter.setParagraphs(state.paragraphs);
        }

        if (state.errorMessage != null) {
            Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAvailableLanguagesUI();
        if (transcriptionAdapter != null) {
            transcriptionAdapter.notifyItemRangeChanged(0, transcriptionAdapter.getItemCount());
        }
    }

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

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

    private void checkModelsAndProceed(Uri audioUri) {
        List<Integer> availableIndices = new ArrayList<>();
        int index = 0;
        for (LanguageSupport lang : ModelManager.SUPPORTED_LANGUAGES) {
            if (lang.isTranscriptionDownloaded(this)) {
                availableIndices.add(index);
            }
            index++;
        }

        if (availableIndices.isEmpty()) {
            Toast.makeText(this, R.string.status_no_models_installed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (availableIndices.size() == 1) {
            viewModel.startTranscription(audioUri, availableIndices.get(0));
        } else {
            showLanguageSelectionDialog(availableIndices, audioUri);
        }
    }

    private void showLanguageSelectionDialog(List<Integer> availableIndices, Uri audioUri) {
        String[] languages = new String[availableIndices.size()];
        int idx = 0;
        for (Integer availableIndex : availableIndices) {
            languages[idx++] = ModelManager.SUPPORTED_LANGUAGES[availableIndex].getLocale().getDisplayName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_select_transcription_language)
                .setItems(languages, (dialog, which) -> {
                    viewModel.startTranscription(audioUri, availableIndices.get(which));
                })
                .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                    statusTextView.setText(R.string.intro_instruction);
                    statusTextView.setVisibility(View.VISIBLE);
                })
                .show();
    }
}
