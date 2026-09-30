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

package de.switchconsulting.aintlistening.data;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Handles JSON serialization and local persistence of last transcription paragraphs.
 */
@Singleton
public class TranscriptionLocalDataSource {

    private static final String TAG = "TranscriptionLocalDataSource";
    private static final String PREFS_NAME = "AintListeningPrefs";
    private static final String KEY_LAST_PARAGRAPHS_JSON = "last_paragraphs_json";
    private static final String KEY_LAST_LOCALE_TAG = "last_locale_tag";

    private final Context context;
    private final Object jsonLock = new Object();

    /**
     * Constructs a new TranscriptionLocalDataSource instance.
     *
     * @param context The application context.
     */
    @Inject
    public TranscriptionLocalDataSource(@ApplicationContext Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Saves the last transcription paragraphs and the language locale used.
     *
     * @param paragraphs The list of transcription paragraphs to save.
     * @param locale     The locale of the model used for transcription.
     */
    public void saveLastMessage(List<TranscriptionParagraph> paragraphs, Locale locale) {
        synchronized (jsonLock) {
            try {
                JSONArray array = new JSONArray();
                if (paragraphs != null) {
                    for (TranscriptionParagraph p : paragraphs) {
                        JSONObject obj = new JSONObject();
                        obj.put("raw", p.getRawText());
                        obj.put("formatted", p.getFormattedText());
                        obj.put("showFormatted", p.isShowFormatted());
                        obj.put("audioPath", p.getAudioFilePath());
                        array.put(obj);
                    }
                }
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_LAST_PARAGRAPHS_JSON, array.toString())
                        .putString(KEY_LAST_LOCALE_TAG, locale != null ? locale.toLanguageTag() : null)
                        .apply();
            } catch (Exception e) {
                Log.e(TAG, "Failed to save last message", e);
            }
        }
    }

    /**
     * Loads the last transcription paragraphs.
     *
     * @return The list of last saved transcription paragraphs, or null if none exist.
     */
    public List<TranscriptionParagraph> loadLastMessage() {
        synchronized (jsonLock) {
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
    }
}
