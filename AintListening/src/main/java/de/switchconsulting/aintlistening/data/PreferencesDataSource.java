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
import android.content.SharedPreferences;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Handles key-value preference storage using {@link SharedPreferences}.
 */
@Singleton
public class PreferencesDataSource {

    private static final String PREFS_NAME = "AintListeningPrefs";
    private static final String KEY_SHOW_PLAYBACK_BUTTON = "show_playback_button";
    private static final String KEY_SHOW_COPY_BUTTON = "show_copy_button";
    private static final String KEY_SHOW_RAW_TEXT = "show_raw_text";
    private static final String KEY_SHOW_SMART_TEXT = "show_smart_text";
    private static final String KEY_TRANSCRIBER_TYPE = "transcriber_type";
    private static final String KEY_LANGUAGE_ENABLED_PREFIX = "lang_enabled_";

    private final Context context;

    /**
     * Constructs a new PreferencesDataSource instance.
     *
     * @param context The application context.
     */
    @Inject
    public PreferencesDataSource(@ApplicationContext Context context) {
        this.context = context.getApplicationContext();
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
     * @return True if raw text should be shown by default.
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
     * @return True if smart formatted text should be shown by default.
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
     * @param locale The locale to check smart formatting for.
     * @return True if smart formatting is enabled for this locale, or global default.
     */
    public boolean isSmartFormattingEnabled(Locale locale) {
        String key = KEY_SHOW_SMART_TEXT + "_" + locale.toLanguageTag();
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(key, isShowSmartText());
    }

    /**
     * @param locale  The locale to set smart formatting for.
     * @param enabled True to enable, false to disable.
     */
    public void setSmartFormattingEnabled(Locale locale, boolean enabled) {
        String key = KEY_SHOW_SMART_TEXT + "_" + locale.toLanguageTag();
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, enabled)
                .apply();
    }

    /**
     * @return The default transcription engine type.
     */
    public TranscriberType getDefaultTranscriberType() {
        return TranscriberType.VOSK;
    }

    /**
     * @param locale The locale to get the transcriber for.
     * @return The selected transcription engine type for the locale, or the default.
     */
    public TranscriberType getTranscriberType(Locale locale) {
        String key = KEY_TRANSCRIBER_TYPE + "_" + locale.toLanguageTag();
        String typeName = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, null);
        if (typeName == null) {
            return getDefaultTranscriberType();
        }
        try {
            return TranscriberType.valueOf(typeName);
        } catch (Exception e) {
            return getDefaultTranscriberType();
        }
    }

    /**
     * @param locale The locale to set the transcriber for.
     * @param type   The transcription engine type to use.
     */
    public void setTranscriberType(Locale locale, TranscriberType type) {
        String key = KEY_TRANSCRIBER_TYPE + "_" + locale.toLanguageTag();
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key, type.name())
                .apply();
    }

    /**
     * Checks if a language is enabled by the user.
     *
     * @param locale The locale of the language.
     * @return True if enabled, false otherwise. Defaults to true.
     */
    public boolean isLanguageEnabled(Locale locale) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LANGUAGE_ENABLED_PREFIX + locale.toLanguageTag(), true);
    }

    /**
     * Sets whether a language is enabled.
     *
     * @param locale  The locale of the language.
     * @param enabled True to enable, false to disable.
     */
    public void setLanguageEnabled(Locale locale, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LANGUAGE_ENABLED_PREFIX + locale.toLanguageTag(), enabled)
                .apply();
    }
}
