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

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence of transcription data.
 */
public class Persistency {

    private static final String TAG = "Persistency";
    private static final String PREFS_NAME = "AintListeningPrefs";
    private static final String KEY_LAST_PARAGRAPHS_JSON = "last_paragraphs_json";
    private static final String KEY_LAST_MODEL_INDEX = "last_model_index";
    private static final String KEY_LAST_MESSAGE = "last_message";
    private static final String KEY_SHOW_PLAYBACK_BUTTON = "show_playback_button";
    private static final String KEY_SHOW_COPY_BUTTON = "show_copy_button";
    private static final String KEY_SHOW_RAW_TEXT = "show_raw_text";
    private static final String KEY_SHOW_SMART_TEXT = "show_smart_text";

    private final Context context;

    /**
     * Constructs a new Persistency instance.
     *
     * @param context The application context.
     */
    public Persistency(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Saves the last transcription paragraphs and the model index used.
     *
     * @param paragraphs The list of transcription paragraphs to save.
     * @param modelIndex The index of the model used for transcription.
     */
    public void saveLastMessage(List<TranscriptionParagraph> paragraphs, int modelIndex) {
        try {
            JSONArray array = new JSONArray();
            for (TranscriptionParagraph p : paragraphs) {
                JSONObject obj = new JSONObject();
                obj.put("raw", p.getRawText());
                obj.put("formatted", p.getFormattedText());
                obj.put("showFormatted", p.isShowFormatted());
                obj.put("audioPath", p.getAudioFilePath());
                array.put(obj);
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_PARAGRAPHS_JSON, array.toString())
                    .putInt(KEY_LAST_MODEL_INDEX, modelIndex)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save last message", e);
        }
    }

    /**
     * Loads the last transcription paragraphs.
     *
     * @return The list of last saved transcription paragraphs, or null if none exist.
     */
    public List<TranscriptionParagraph> loadLastMessage() {
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_PARAGRAPHS_JSON, null);

        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                List<TranscriptionParagraph> paragraphs = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    TranscriptionParagraph p = new TranscriptionParagraph(
                            obj.getString("raw"),
                            obj.has("formatted") && !obj.isNull("formatted") ? obj.getString("formatted") : null,
                            obj.has("audioPath") && !obj.isNull("audioPath") ? obj.getString("audioPath") : null
                    );
                    p.setShowFormatted(obj.optBoolean("showFormatted", p.isShowFormatted()));
                    paragraphs.add(p);
                }
                return paragraphs;
            } catch (Exception e) {
                Log.e(TAG, "Failed to load last message", e);
            }
        }
        return null;
    }

    /**
     * Loads the last raw message in case the new JSON format is not available.
     *
     * @return The last saved raw message, or null if none exists.
     */
    public String loadLegacyLastMessage() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_MESSAGE, null);
    }

    /**
     * Loads the last model index used.
     *
     * @return The last saved model index.
     */
    public int loadLastModelIndex() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_LAST_MODEL_INDEX, 0);
    }

    /**
     * @return True if the playback button should be shown.
     */
    public boolean isShowPlaybackButton() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_PLAYBACK_BUTTON, true);
    }

    /**
     * @param show True to show the playback button.
     */
    public void setShowPlaybackButton(boolean show) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_PLAYBACK_BUTTON, show)
                .apply();
    }

    /**
     * @return True if the copy button should be shown.
     */
    public boolean isShowCopyButton() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_COPY_BUTTON, true);
    }

    /**
     * @param show True to show the copy button.
     */
    public void setShowCopyButton(boolean show) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_COPY_BUTTON, show)
                .apply();
    }

    /**
     * @return True if the raw text should be shown by default.
     */
    public boolean isShowRawText() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_RAW_TEXT, true);
    }

    /**
     * @param show True to show raw text by default.
     */
    public void setShowRawText(boolean show) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_RAW_TEXT, show)
                .apply();
    }

    /**
     * @return True if the smart formatted text should be shown by default.
     */
    public boolean isShowSmartText() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_SMART_TEXT, true);
    }

    /**
     * @param show True to show smart formatted text by default.
     */
    public void setShowSmartText(boolean show) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_SMART_TEXT, show)
                .apply();
    }

    /**
     * @return The file object for the main temporary incoming audio WAV.
     */
    public File getIncomingWavFile() {
        return new File(context.getCacheDir(), "incoming_audio_16k_mono.wav");
    }

    /**
     * @return The directory for storing audio chunks.
     */
    public File getAudioChunksDir() {
        File chunksDir = new File(context.getFilesDir(), "audio_chunks");
        if (!chunksDir.exists()) {
            if (!chunksDir.mkdirs()) {
                Log.w(TAG, "Failed to create chunks directory: " + chunksDir.getAbsolutePath());
            }
        }
        return chunksDir;
    }

    /**
     * Saves raw PCM data as a WAV chunk.
     *
     * @param pcmData The raw PCM data.
     * @param index   The chunk index.
     * @return The absolute path to the saved chunk file.
     * @throws IOException If saving fails.
     */
    public String saveAudioChunk(byte[] pcmData, int index) throws IOException {
        File chunksDir = getAudioChunksDir();
        File chunkFile = new File(chunksDir, "chunk_" + index + ".wav");
        WavUtils.savePcmAsWav(pcmData, chunkFile);
        return chunkFile.getAbsolutePath();
    }

    /**
     * Deletes all temporary audio files and chunks.
     */
    public void clearTemporaryFiles() {
        // Delete incoming wav
        File incomingWav = getIncomingWavFile();
        if (incomingWav.exists()) {
            if (incomingWav.delete()) {
                Log.d(TAG, "Deleted incoming WAV: " + incomingWav.getAbsolutePath());
            }
        }

        // Delete chunks
        File chunksDir = new File(context.getFilesDir(), "audio_chunks");
        if (chunksDir.exists() && chunksDir.isDirectory()) {
            File[] files = chunksDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.delete()) {
                        Log.d(TAG, "Deleted chunk: " + f.getAbsolutePath());
                    }
                }
            }
        }
    }
}
