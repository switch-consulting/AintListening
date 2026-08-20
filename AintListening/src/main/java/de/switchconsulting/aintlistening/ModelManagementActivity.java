package de.switchconsulting.aintlistening;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class ModelManagementActivity extends AppCompatActivity {

    private static final String TAG = "ModelManagement";
    private LinearProgressIndicator progressIndicator;
    private LinearLayout modelListContainer;
    private boolean isDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_management);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressIndicator = findViewById(R.id.progressIndicator);
        modelListContainer = findViewById(R.id.modelListContainer);

        updateModelStatusUI();
    }

    private String getModelUrl(int index) {
        return ModelManager.SUPPORTED_MODELS[index].url;
    }

    private boolean isModelMissing(int index) {
        return !ModelManager.isModelDownloaded(this, index);
    }

    private void startDownload(int index) {
        Log.d(TAG, "startDownload called for index: " + index);
        if (isDownloading) {
            Log.d(TAG, "Download already in progress, ignoring.");
            return;
        }

        isDownloading = true;
        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setIndeterminate(false);
        progressIndicator.setProgress(0);

        updateModelStatusUI();

        Log.d(TAG, "Starting ModelDownloader for index " + index);
        ModelDownloader.downloadAndExtract(getModelUrl(index), getFilesDir(), new ModelDownloader.Callback() {
            @Override
            public void onProgress(int percentage) {
                runOnUiThread(() -> progressIndicator.setProgress(percentage));
            }

            @Override
            public void onExtracting() {
                Log.d(TAG, "Extracting model...");
                runOnUiThread(() -> progressIndicator.setIndeterminate(true));
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "Download and extraction successful.");
                runOnUiThread(() -> {
                    isDownloading = false;
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(ModelManagementActivity.this, R.string.message_model_ready, Toast.LENGTH_SHORT).show();
                    updateModelStatusUI();
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Download failed", e);
                runOnUiThread(() -> {
                    isDownloading = false;
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(ModelManagementActivity.this, R.string.error_download_failed, Toast.LENGTH_LONG).show();
                    updateModelStatusUI();
                });
            }
        });
    }

    private void updateModelStatusUI() {
        if (modelListContainer.getChildCount() == 0) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < ModelManager.SUPPORTED_MODELS.length; i++) {
                View itemView = inflater.inflate(R.layout.item_model_status, modelListContainer, false);
                modelListContainer.addView(itemView);
            }
        }

        for (int i = 0; i < ModelManager.SUPPORTED_MODELS.length; i++) {
            View itemView = modelListContainer.getChildAt(i);
            ImageView icon = itemView.findViewById(R.id.modelStatusIcon);
            TextView nameText = itemView.findViewById(R.id.modelNameText);
            TextView statusText = itemView.findViewById(R.id.modelStatusText);
            MaterialButton downloadButton = itemView.findViewById(R.id.inlineDownloadButton);
            MaterialButton deleteButton = itemView.findViewById(R.id.inlineDeleteButton);

            final int index = i;
            ModelInfo info = ModelManager.SUPPORTED_MODELS[i];
            nameText.setText(info.displayName);

            if (isModelMissing(i)) {
                icon.setImageResource(R.drawable.ic_error);
                icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                statusText.setText(getString(R.string.status_unavailable_with_size, info.size));
                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setEnabled(!isDownloading);
                downloadButton.setOnClickListener(v -> {
                    Toast.makeText(this, getString(R.string.message_starting_download, info.displayName), Toast.LENGTH_SHORT).show();
                    startDownload(index);
                });
                deleteButton.setVisibility(View.GONE);
            } else {
                icon.setImageResource(R.drawable.ic_check_circle);
                icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                statusText.setText(R.string.status_available);
                downloadButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setEnabled(!isDownloading);
                deleteButton.setOnClickListener(v -> confirmDelete(index, info.displayName));
            }
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
