package de.switchconsulting.aintlistening;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
    private static final String PREFS_NAME = "AintListeningPrefs";
    private static final String KEY_LAST_MESSAGE = "last_message";

    private LinearProgressIndicator progressIndicator;
    private TextView transcriptTextView;
    private ImageView modelStatusIcon;
    private TextView modelStatusText;
    private MaterialButton inlineDownloadButton;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Model voskModel;
    private boolean isDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressIndicator = findViewById(R.id.progressIndicator);
        transcriptTextView = findViewById(R.id.transcriptTextView);
        modelStatusIcon = findViewById(R.id.modelStatusIcon);
        modelStatusText = findViewById(R.id.modelStatusText);
        inlineDownloadButton = findViewById(R.id.inlineDownloadButton);
        MaterialButton closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());
        inlineDownloadButton.setOnClickListener(v -> startDownload());

        updateModelStatusUI();
        handleIncomingIntent(getIntent());
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
            handleIncomingIntent(intent);
        }
    }

    private boolean isModelMissing() {
        File modelDir = new File(getFilesDir(), MODEL_NAME);
        return !modelDir.exists() || !modelDir.isDirectory();
    }

    private void startDownload() {
        isDownloading = true;
        inlineDownloadButton.setEnabled(false);
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
                    inlineDownloadButton.setEnabled(true);
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Model ready", Toast.LENGTH_SHORT).show();
                    updateModelStatusUI();
                    handleIncomingIntent(getIntent());
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    isDownloading = false;
                    inlineDownloadButton.setEnabled(true);
                    progressIndicator.setVisibility(View.GONE);
                    showError(getString(R.string.error_download_failed));
                    updateModelStatusUI();
                });
            }
        });
    }

    private void updateModelStatusUI() {
        if (isModelMissing()) {
            modelStatusIcon.setImageResource(R.drawable.ic_error);
            modelStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            modelStatusText.setText(R.string.status_unavailable);
            inlineDownloadButton.setVisibility(View.VISIBLE);
        } else {
            modelStatusIcon.setImageResource(R.drawable.ic_check_circle);
            modelStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            modelStatusText.setText(R.string.status_available);
            inlineDownloadButton.setVisibility(View.GONE);
        }
    }

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
                String result = transcript.trim().isEmpty()
                        ? getString(R.string.status_no_speech)
                        : transcript.trim();
                transcriptTextView.setText(result);
                if (!transcript.trim().isEmpty()) {
                    saveLastMessage(result);
                }
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
                    updateTranscriptUI(fullText.toString());
                } else {
                    String partialJson = recognizer.getPartialResult();
                    String partialText = getPartialTextFromJson(partialJson);
                    if (!partialText.isEmpty()) {
                        String currentDisplay = fullText.toString();
                        if (fullText.length() > 0) currentDisplay += "\n\n";
                        updateTranscriptUI(currentDisplay + partialText);
                    }
                }
            }

            appendTextFromResultJson(fullText, recognizer.getFinalResult());
        }

        return fullText.toString();
    }

    private void updateTranscriptUI(String text) {
        runOnUiThread(() -> transcriptTextView.setText(text.trim()));
    }

    private String getPartialTextFromJson(String json) {
        if (json == null || json.trim().isEmpty()) return "";
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("partial", "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void appendTextFromResultJson(@NonNull StringBuilder out, String json) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(json);
            String text = obj.optString("text", "").trim();
            if (!text.isEmpty()) {
                if (out.length() > 0) out.append("\n\n");
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

    private void saveLastMessage(String transcript) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_MESSAGE, transcript)
                .apply();
    }

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
