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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link PreferencesDataSource}.
 */
public class PreferencesDataSourceTest {

    private PreferencesDataSource preferencesDataSource;

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);

        MockSharedPreferences mockPreferences = new MockSharedPreferences();
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences);

        preferencesDataSource = new PreferencesDataSource(context);
    }

    @Test
    public void testShowPlaybackButton() {
        assertTrue(preferencesDataSource.isShowPlaybackButton());
        preferencesDataSource.setShowPlaybackButton(false);
        assertFalse(preferencesDataSource.isShowPlaybackButton());
        preferencesDataSource.setShowPlaybackButton(true);
        assertTrue(preferencesDataSource.isShowPlaybackButton());
    }

    @Test
    public void testShowCopyButton() {
        assertTrue(preferencesDataSource.isShowCopyButton());
        preferencesDataSource.setShowCopyButton(false);
        assertFalse(preferencesDataSource.isShowCopyButton());
        preferencesDataSource.setShowCopyButton(true);
        assertTrue(preferencesDataSource.isShowCopyButton());
    }

    @Test
    public void testShowRawText() {
        assertTrue(preferencesDataSource.isShowRawText());
        preferencesDataSource.setShowRawText(false);
        assertFalse(preferencesDataSource.isShowRawText());
        preferencesDataSource.setShowRawText(true);
        assertTrue(preferencesDataSource.isShowRawText());
    }

    @Test
    public void testShowSmartText() {
        assertTrue(preferencesDataSource.isShowSmartText());
        preferencesDataSource.setShowSmartText(false);
        assertFalse(preferencesDataSource.isShowSmartText());
        preferencesDataSource.setShowSmartText(true);
        assertTrue(preferencesDataSource.isShowSmartText());
    }

    @Test
    public void testSmartFormattingPerLocale() {
        assertTrue(preferencesDataSource.isSmartFormattingEnabled(Locale.GERMAN));

        preferencesDataSource.setSmartFormattingEnabled(Locale.GERMAN, false);
        assertFalse(preferencesDataSource.isSmartFormattingEnabled(Locale.GERMAN));
        assertTrue(preferencesDataSource.isSmartFormattingEnabled(Locale.ENGLISH));

        preferencesDataSource.setSmartFormattingEnabled(Locale.GERMAN, true);
        assertTrue(preferencesDataSource.isSmartFormattingEnabled(Locale.GERMAN));
    }

    @Test
    public void testTranscriberTypePerLocale() {
        assertEquals(TranscriberType.VOSK, preferencesDataSource.getDefaultTranscriberType());
        assertEquals(TranscriberType.VOSK, preferencesDataSource.getTranscriberType(Locale.GERMAN));

        preferencesDataSource.setTranscriberType(Locale.GERMAN, TranscriberType.WHISPER);
        assertEquals(TranscriberType.WHISPER, preferencesDataSource.getTranscriberType(Locale.GERMAN));
        assertEquals(TranscriberType.VOSK, preferencesDataSource.getTranscriberType(Locale.ENGLISH));
    }

    @Test
    public void testLanguageEnabledState() {
        assertTrue(preferencesDataSource.isLanguageEnabled(Locale.GERMAN));

        preferencesDataSource.setLanguageEnabled(Locale.GERMAN, false);
        assertFalse(preferencesDataSource.isLanguageEnabled(Locale.GERMAN));
        assertTrue(preferencesDataSource.isLanguageEnabled(Locale.ENGLISH));

        preferencesDataSource.setLanguageEnabled(Locale.GERMAN, true);
        assertTrue(preferencesDataSource.isLanguageEnabled(Locale.GERMAN));
    }

    private static class MockSharedPreferences implements SharedPreferences {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(map);
        }

        @Override
        public String getString(String key, String defValue) {
            Object val = map.get(key);
            return val instanceof String ? (String) val : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object val = map.get(key);
            return val instanceof Integer ? (Integer) val : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object val = map.get(key);
            return val instanceof Long ? (Long) val : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object val = map.get(key);
            return val instanceof Float ? (Float) val : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object val = map.get(key);
            return val instanceof Boolean ? (Boolean) val : defValue;
        }

        @Override
        public boolean contains(String key) {
            return map.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new Editor() {
                @Override
                public Editor putString(String key, String value) {
                    if (value == null) map.remove(key); else map.put(key, value);
                    return this;
                }

                @Override
                public Editor putStringSet(String key, Set<String> values) {
                    return this;
                }

                @Override
                public Editor putInt(String key, int value) {
                    map.put(key, value);
                    return this;
                }

                @Override
                public Editor putLong(String key, long value) {
                    map.put(key, value);
                    return this;
                }

                @Override
                public Editor putFloat(String key, float value) {
                    map.put(key, value);
                    return this;
                }

                @Override
                public Editor putBoolean(String key, boolean value) {
                    map.put(key, value);
                    return this;
                }

                @Override
                public Editor remove(String key) {
                    map.remove(key);
                    return this;
                }

                @Override
                public Editor clear() {
                    map.clear();
                    return this;
                }

                @Override
                public boolean commit() {
                    return true;
                }

                @Override
                public void apply() {
                }
            };
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }
    }
}
