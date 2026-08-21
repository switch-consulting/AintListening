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
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class ModelManagementActivity extends AppCompatActivity {

    private static final String TAG = "ModelManagement";
    private LinearProgressIndicator progressIndicator;
    private RecyclerView recyclerView;
    private ModelAdapter adapter;
    private ModelManagementViewModel viewModel;

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

        setupRecyclerView();

        updateModelStatusUI();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ModelAdapter(java.util.Arrays.asList(ModelManager.SUPPORTED_MODELS), new ModelAdapter.InteractionListener() {
            @Override
            public void onDownloadClicked(int index) {
                ModelInfo info = ModelManager.SUPPORTED_MODELS[index];
                Toast.makeText(ModelManagementActivity.this, getString(R.string.message_starting_download, info.displayName), Toast.LENGTH_SHORT).show();
                startDownload(index);
            }

            @Override
            public void onDeleteClicked(int index, String displayName) {
                confirmDelete(index, displayName);
            }
        });
        recyclerView.setAdapter(adapter);
    }

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

    private void startDownload(int index) {
        Log.d(TAG, "startDownload called for index: " + index);
        viewModel.startDownload(index);
    }

    private void updateModelStatusUI() {
        DownloadState currentState = viewModel.downloadState.getValue();
        boolean isBusy = currentState != null && currentState.status != DownloadState.Status.IDLE;

        if (adapter != null) {
            adapter.setBusy(isBusy);
            adapter.refresh();
        }
    }

    private void confirmDelete(int index, String displayName) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_confirm_delete_title)
                .setMessage(getString(R.string.dialog_confirm_delete_message, displayName))
                .setPositiveButton(R.string.button_remove, (dialog, which) -> {
                    if (ModelManager.deleteModel(this, index)) {
                        updateModelStatusUI();
                        Toast.makeText(this, R.string.message_model_removed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }
}
