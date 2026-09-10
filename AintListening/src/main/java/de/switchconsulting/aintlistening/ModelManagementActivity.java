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

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Arrays;
import java.util.List;

/**
 * Activity for managing speech models. Allows users to view available models,
 * download new ones, and delete installed ones.
 */
public class ModelManagementActivity extends AppCompatActivity {

    private static final String TAG = "ModelManagement";
    private LinearProgressIndicator progressIndicator;
    private RecyclerView recyclerView;
    private ModelAdapter adapter;
    private ModelManagementViewModel viewModel;
    private Persistency persistency;
    private SwitchMaterial switchRaw;
    private SwitchMaterial switchSmart;

    /**
     * Initializes the activity, sets up the ViewModel, and UI components.
     *
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_management);

        viewModel = new ViewModelProvider(this).get(ModelManagementViewModel.class);
        viewModel.downloadState.observe(this, this::handleDownloadState);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressIndicator = findViewById(R.id.progressIndicator);
        recyclerView = findViewById(R.id.modelRecyclerView);

        persistency = new Persistency(this);
        setupUISettings();

        setupRecyclerView();

        updateModelStatusUI();
    }

    private void setupUISettings() {
        SwitchMaterial switchPlayback = findViewById(R.id.switchPlayback);
        SwitchMaterial switchCopy = findViewById(R.id.switchCopy);
        switchRaw = findViewById(R.id.switchRaw);
        switchSmart = findViewById(R.id.switchSmart);

        switchPlayback.setChecked(persistency.isShowPlaybackButton());
        switchCopy.setChecked(persistency.isShowCopyButton());
        switchRaw.setChecked(persistency.isShowRawText());
        switchSmart.setChecked(persistency.isShowSmartText());

        switchPlayback.setOnCheckedChangeListener((buttonView, isChecked) -> persistency.setShowPlaybackButton(isChecked));
        switchCopy.setOnCheckedChangeListener((buttonView, isChecked) -> persistency.setShowCopyButton(isChecked));

        switchRaw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && !switchSmart.isChecked()) {
                switchRaw.setChecked(true);
                Toast.makeText(this, R.string.message_at_least_one_mode, Toast.LENGTH_SHORT).show();
            } else {
                persistency.setShowRawText(isChecked);
            }
        });

        switchSmart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked && !switchRaw.isChecked()) {
                switchSmart.setChecked(true);
                Toast.makeText(this, R.string.message_at_least_one_mode, Toast.LENGTH_SHORT).show();
            } else {
                persistency.setShowSmartText(isChecked);
            }
        });

        updateSmartFormattingSwitchState();
    }

    private void updateSmartFormattingSwitchState() {
        boolean isFormattingAvailable = false;
        for (LanguageSupport lang : ModelManager.SUPPORTED_LANGUAGES) {
            if (lang.isFormattingDownloaded(this)) {
                isFormattingAvailable = true;
                break;
            }
        }

        if (!isFormattingAvailable) {
            switchSmart.setChecked(false);
            switchSmart.setEnabled(false);
            persistency.setShowSmartText(false);
            
            // If we forced smart off, ensure raw is on
            if (!switchRaw.isChecked()) {
                switchRaw.setChecked(true);
                persistency.setShowRawText(true);
            }
        } else {
            switchSmart.setEnabled(true);
        }
    }

    /**
     * Configures the RecyclerView and its adapter.
     */
    private void setupRecyclerView() {
        List<LanguageSupport> languages = Arrays.asList(ModelManager.SUPPORTED_LANGUAGES);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ModelAdapter(languages, new ModelAdapter.InteractionListener() {
            @Override
            public void onDownloadClicked(ModelInfo info) {
                Toast.makeText(ModelManagementActivity.this, getString(R.string.message_starting_download, info.locale.getDisplayName()), Toast.LENGTH_SHORT).show();
                startDownload(info);
            }

            @Override
            public void onDeleteClicked(ModelInfo info) {
                confirmDelete(info);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    /**
     * Handles changes in the download state reported by the ViewModel.
     *
     * @param state The new DownloadState.
     */
    private void handleDownloadState(DownloadState state) {
        if (state == null) return;
        switch (state.status) {
            case IDLE:
                progressIndicator.setVisibility(View.GONE);
                break;
            case DOWNLOADING:
                progressIndicator.setVisibility(View.VISIBLE);
                progressIndicator.setIndeterminate(false);
                progressIndicator.setProgress(state.progress);
                break;
            case EXTRACTING:
                progressIndicator.setVisibility(View.VISIBLE);
                progressIndicator.setIndeterminate(true);
                break;
            case SUCCESS:
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(this, R.string.message_model_ready, Toast.LENGTH_SHORT).show();
                viewModel.resetState();
                break;
            case ERROR:
                progressIndicator.setVisibility(View.GONE);
                Toast.makeText(this, R.string.error_download_failed, Toast.LENGTH_LONG).show();
                viewModel.resetState();
                break;
        }
        updateModelStatusUI();
    }

    /**
     * Initiates the download of the specified model.
     *
     * @param info The model information to download.
     */
    private void startDownload(ModelInfo info) {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_LONG).show();
            return;
        }
        Log.d(TAG, "startDownload called for model: " + info.name);
        viewModel.startDownload(info);
    }

    /**
     * Updates the UI to reflect the current installation status of all models.
     */
    private void updateModelStatusUI() {
        DownloadState currentState = viewModel.downloadState.getValue();
        boolean isBusy = currentState != null && currentState.status != DownloadState.Status.IDLE;

        if (adapter != null) {
            adapter.setBusy(isBusy);
            adapter.refresh();
        }
        updateSmartFormattingSwitchState();
    }

    /**
     * Shows a confirmation dialog before deleting a model.
     *
     * @param info The model information to delete.
     */
    private void confirmDelete(ModelInfo info) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_confirm_delete_title)
                .setMessage(getString(R.string.dialog_confirm_delete_message, info.locale.getDisplayName()))
                .setPositiveButton(R.string.button_remove, (dialog, which) -> {
                    if (ModelManager.deleteModel(this, info)) {
                        updateModelStatusUI();
                        Toast.makeText(this, R.string.message_model_removed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }
}
