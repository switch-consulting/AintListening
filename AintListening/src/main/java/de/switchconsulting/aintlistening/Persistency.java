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

    public boolean isShowPlaybackButton() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_PLAYBACK_BUTTON, true);
    }

    public void setShowPlaybackButton(boolean show) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_PLAYBACK_BUTTON, show)
                .apply();
    }

    public boolean isShowCopyButton() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_COPY_BUTTON, true);
    }

    public void setShowCopyButton(boolean show) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_COPY_BUTTON, show)
                .apply();
    }
}
