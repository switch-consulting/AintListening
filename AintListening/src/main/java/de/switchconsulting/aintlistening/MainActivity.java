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

    private LinearProgressIndicator progressIndicator;
    private TextView transcriptTextView;
    private MaterialButton closeButton;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Model voskModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressIndicator = findViewById(R.id.progressIndicator);
        transcriptTextView = findViewById(R.id.transcriptTextView);
        closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> finish());

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
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (!Intent.ACTION_SEND.equals(action) || type == null || !type.startsWith("audio/")) {
            progressIndicator.setVisibility(View.GONE);
            transcriptTextView.setText(R.string.intro_instruction);
            return;
        }

        Uri audioUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (audioUri == null) {
            progressIndicator.setVisibility(View.GONE);
            transcriptTextView.setText(R.string.error_no_stream);
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
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
                transcriptTextView.setText(transcript == null || transcript.trim().isEmpty()
                        ? getString(R.string.status_no_speech)
                        : transcript.trim());
            });
        } catch (Exception e) {
            Log.e(TAG, "Vosk transcription failed", e);
            showError(getString(R.string.error_transcription_failed));
        }
    }

    private Model loadGermanModel() throws Exception {
        File modelDir = new File(getFilesDir(), "vosk-model-small-de-0.15");
        if (!modelDir.exists() || !modelDir.isDirectory()) {
            throw new IllegalStateException("Vosk model not found at: " + modelDir.getAbsolutePath() +
                    "\nPlease copy the unpacked model folder there.");
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
