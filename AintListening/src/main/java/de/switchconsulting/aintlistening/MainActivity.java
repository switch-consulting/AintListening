package de.switchconsulting.aintlistening;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AintListening";
    private static final String MODEL_NAME = "vosk-model-small-de-0.15";
    private static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip";

    private LinearProgressIndicator progressIndicator;
    private TextView transcriptTextView;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Model voskModel;
    private boolean isDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressIndicator = findViewById(R.id.progressIndicator);
        transcriptTextView = findViewById(R.id.transcriptTextView);
        MaterialButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());

        if (isModelMissing()) {
            showDownloadDialog();
        } else {
            handleIncomingIntent(getIntent());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        if (voskModel != null) {
            voskModel.close();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!isDownloading) {
            if (isModelMissing()) {
                showDownloadDialog();
            } else {
                handleIncomingIntent(intent);
            }
        }
    }

    private boolean isModelMissing() {
        File modelDir = new File(getFilesDir(), MODEL_NAME);
        return !modelDir.exists() || !modelDir.isDirectory();
    }

    private void showDownloadDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_download_title)
                .setMessage(R.string.dialog_download_message)
                .setPositiveButton(R.string.button_download, (dialog, which) -> startDownload())
                .setNegativeButton(R.string.button_cancel, (dialog, which) -> {
                    Toast.makeText(this, R.string.error_model_setup_cancelled, Toast.LENGTH_LONG).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void startDownload() {
        isDownloading = true;
        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setIndeterminate(false);
        progressIndicator.setProgress(0);
        transcriptTextView.setText(getString(R.string.status_downloading, 0));

        ModelDownloader.downloadAndExtract(MODEL_URL, getFilesDir(), new ModelDownloader.Callback() {
            @Override
            public void onProgress(int percentage) {
                runOnUiThread(() -> {
                    progressIndicator.setProgress(percentage);
                    transcriptTextView.setText(getString(R.string.status_downloading, percentage));
                });
            }

            @Override
            public void onExtracting() {
                runOnUiThread(() -> {
                    progressIndicator.setIndeterminate(true);
                    transcriptTextView.setText(R.string.status_extracting);
                });
            }

            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    isDownloading = false;
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Model ready", Toast.LENGTH_SHORT).show();
                    handleIncomingIntent(getIntent());
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    isDownloading = false;
                    progressIndicator.setVisibility(View.GONE);
                    showError(getString(R.string.error_download_failed));
                    showDownloadDialog();
                });
            }
        });
    }

    private void handleIncomingIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || type == null || !type.startsWith("audio/")) {
            progressIndicator.setVisibility(View.GONE);
            transcriptTextView.setText(R.string.intro_instruction);
            return;
        }

        Uri audioUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
        if (audioUri == null) {
            progressIndicator.setVisibility(View.GONE);
            transcriptTextView.setText(R.string.error_no_stream);
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setIndeterminate(true);
        transcriptTextView.setText(R.string.status_preparing);

        executorService.execute(() -> transcribeFromUri(audioUri));
    }

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

    private void runVoskRecognition(@NonNull File wavFile) {
        try {
            runOnUiThread(() -> transcriptTextView.setText(R.string.status_loading_model));
            if (voskModel == null) {
                voskModel = loadGermanModel();
            }

            runOnUiThread(() -> transcriptTextView.setText(R.string.status_transcribing));

            String transcript = recognizeWav(voskModel, wavFile);
            runOnUiThread(() -> {
                progressIndicator.setVisibility(View.GONE);
                transcriptTextView.setText(transcript.trim().isEmpty()
                        ? getString(R.string.status_no_speech)
                        : transcript.trim());
            });
        } catch (Exception e) {
            Log.e(TAG, "Vosk transcription failed", e);
            showError(getString(R.string.error_transcription_failed));
        }
    }

    private Model loadGermanModel() throws Exception {
        File modelDir = new File(getFilesDir(), MODEL_NAME);
        if (!modelDir.exists() || !modelDir.isDirectory()) {
            throw new IllegalStateException("Vosk model not found at: " + modelDir.getAbsolutePath());
        }
        return new Model(modelDir.getAbsolutePath());
    }

    private String recognizeWav(@NonNull Model model, @NonNull File wavFile) throws Exception {
        StringBuilder fullText = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(wavFile);
             Recognizer recognizer = new Recognizer(model, 16000.0f)) {

            // Skip WAV header (44 bytes typical PCM header)
            long skipped = fis.skip(44);
            if (skipped < 44) {
                throw new IllegalStateException("Invalid WAV file header");
            }

            byte[] buffer = new byte[4096];
            int nread;
            while ((nread = fis.read(buffer)) >= 0) {
                if (recognizer.acceptWaveForm(buffer, nread)) {
                    String resultJson = recognizer.getResult();
                    appendTextFromResultJson(fullText, resultJson);
                }
            }

            appendTextFromResultJson(fullText, recognizer.getFinalResult());
        }

        return fullText.toString().replaceAll("\\s+", " ").trim();
    }

    private void appendTextFromResultJson(@NonNull StringBuilder out, String json) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(json);
            String text = obj.optString("text", "").trim();
            if (!text.isEmpty()) {
                if (out.length() > 0) out.append(' ');
                out.append(text);
            }
        } catch (Exception ignored) {
            // ignore malformed partials
        }
    }

    private void showError(@NonNull String message) {
        runOnUiThread(() -> {
            progressIndicator.setVisibility(View.GONE);
            transcriptTextView.setText(message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }
}
