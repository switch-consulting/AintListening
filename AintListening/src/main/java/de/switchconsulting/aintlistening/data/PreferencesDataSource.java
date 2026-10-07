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

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.transcription.TranscriberType;
import io.reactivex.rxjava3.core.Single;

/**
 * Handles key-value preference storage using Jetpack Preferences DataStore with RxJava3.
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

    private final RxDataStore<Preferences> dataStore;

    /**
     * Constructs a new PreferencesDataSource instance.
     *
     * @param context The application context.
     */
    @Inject
    public PreferencesDataSource(@ApplicationContext Context context) {
        this.dataStore = new RxPreferenceDataStoreBuilder(context, PREFS_NAME).build();
    }

    /**
     * Constructs a new PreferencesDataSource instance with a custom DataStore (for testing).
     *
     * @param dataStore The RxDataStore instance.
     */
    public PreferencesDataSource(RxDataStore<Preferences> dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Retrieves a boolean preference value synchronously.
     *
     * @param key          The preference key.
     * @param defaultValue The default value if not set or on error.
     * @return The boolean value.
     */
    private boolean getBoolean(String key, boolean defaultValue) {
        try {
            Preferences.Key<Boolean> prefKey = PreferencesKeys.booleanKey(key);
            Preferences prefs = dataStore.data().firstOrError().blockingGet();
            Boolean val = prefs.get(prefKey);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Sets a boolean preference value asynchronously.
     *
     * @param key   The preference key.
     * @param value The boolean value to set.
     */
    private void setBoolean(String key, boolean value) {
        Preferences.Key<Boolean> prefKey = PreferencesKeys.booleanKey(key);
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.set(prefKey, value);
            return Single.just(mutable);
        }).ignoreElement().blockingAwait();
    }

    /**
     * Retrieves a string preference value synchronously.
     *
     * @param key The preference key.
     * @return The string value, or null if not found or on error.
     */
    private String getString(String key) {
        try {
            Preferences.Key<String> prefKey = PreferencesKeys.stringKey(key);
            Preferences prefs = dataStore.data().firstOrError().blockingGet();
            return prefs.get(prefKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sets a string preference value asynchronously.
     *
     * @param key   The preference key.
     * @param value The string value to set.
     */
    private void setString(String key, String value) {
        Preferences.Key<String> prefKey = PreferencesKeys.stringKey(key);
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.set(prefKey, value);
            return Single.just(mutable);
        }).ignoreElement().blockingAwait();
    }

    /**
     * Returns whether the audio playback button should be shown.
     *
     * @return True if the playback button should be shown.
     */
    public boolean isShowPlaybackButton() {
        return getBoolean(KEY_SHOW_PLAYBACK_BUTTON, true);
    }

    /**
     * Sets whether the audio playback button should be shown.
     *
     * @param show True to show the playback button.
     */
    public void setShowPlaybackButton(boolean show) {
        setBoolean(KEY_SHOW_PLAYBACK_BUTTON, show);
    }

    /**
     * Returns whether the copy text button should be shown.
     *
     * @return True if the copy button should be shown.
     */
    public boolean isShowCopyButton() {
        return getBoolean(KEY_SHOW_COPY_BUTTON, true);
    }

    /**
     * Sets whether the copy text button should be shown.
     *
     * @param show True to show the copy button.
     */
    public void setShowCopyButton(boolean show) {
        setBoolean(KEY_SHOW_COPY_BUTTON, show);
    }

    /**
     * Returns whether raw text should be shown by default.
     *
     * @return True if raw text should be shown by default.
     */
    public boolean isShowRawText() {
        return getBoolean(KEY_SHOW_RAW_TEXT, true);
    }

    /**
     * Sets whether raw text should be shown by default.
     *
     * @param show True to show raw text by default.
     */
    public void setShowRawText(boolean show) {
        setBoolean(KEY_SHOW_RAW_TEXT, show);
    }

    /**
     * Returns whether smart formatted text should be shown by default.
     *
     * @return True if smart formatted text should be shown by default.
     */
    public boolean isShowSmartText() {
        return getBoolean(KEY_SHOW_SMART_TEXT, true);
    }

    /**
     * Sets whether smart formatted text should be shown by default.
     *
     * @param show True to show smart formatted text by default.
     */
    public void setShowSmartText(boolean show) {
        setBoolean(KEY_SHOW_SMART_TEXT, show);
    }

    /**
     * Returns whether smart formatting is enabled for the specified locale.
     *
     * @param locale The locale to check smart formatting for.
     * @return True if smart formatting is enabled for this locale, or global default.
     */
    public boolean isSmartFormattingEnabled(Locale locale) {
        String key = KEY_SHOW_SMART_TEXT + "_" + locale.toLanguageTag();
        return getBoolean(key, isShowSmartText());
    }

    /**
     * Sets whether smart formatting is enabled for the specified locale.
     *
     * @param locale  The locale to set smart formatting for.
     * @param enabled True to enable, false to disable.
     */
    public void setSmartFormattingEnabled(Locale locale, boolean enabled) {
        String key = KEY_SHOW_SMART_TEXT + "_" + locale.toLanguageTag();
        setBoolean(key, enabled);
    }

    /**
     * Returns the default transcription engine type.
     *
     * @return The default transcription engine type.
     */
    public TranscriberType getDefaultTranscriberType() {
        return TranscriberType.VOSK;
    }

    /**
     * Returns the transcription engine type selected for the specified locale.
     *
     * @param locale The locale to get the transcriber for.
     * @return The selected transcription engine type for the locale, or the default.
     */
    public TranscriberType getTranscriberType(Locale locale) {
        String key = KEY_TRANSCRIBER_TYPE + "_" + locale.toLanguageTag();
        String typeName = getString(key);
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
     * Sets the transcription engine type to use for the specified locale.
     *
     * @param locale The locale to set the transcriber for.
     * @param type   The transcription engine type to use.
     */
    public void setTranscriberType(Locale locale, TranscriberType type) {
        String key = KEY_TRANSCRIBER_TYPE + "_" + locale.toLanguageTag();
        setString(key, type.name());
    }

    /**
     * Checks if a language is enabled by the user.
     *
     * @param locale The locale of the language.
     * @return True if enabled, false otherwise. Defaults to true.
     */
    public boolean isLanguageEnabled(Locale locale) {
        return getBoolean(KEY_LANGUAGE_ENABLED_PREFIX + locale.toLanguageTag(), true);
    }

    /**
     * Sets whether a language is enabled.
     *
     * @param locale  The locale of the language.
     * @param enabled True to enable, false to disable.
     */
    public void setLanguageEnabled(Locale locale, boolean enabled) {
        setBoolean(KEY_LANGUAGE_ENABLED_PREFIX + locale.toLanguageTag(), enabled);
    }
}
