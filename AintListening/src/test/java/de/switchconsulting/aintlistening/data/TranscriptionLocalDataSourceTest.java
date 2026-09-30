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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Unit tests for {@link TranscriptionLocalDataSource}.
 */
public class TranscriptionLocalDataSourceTest {

    private TranscriptionLocalDataSource dataSource;

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);

        MockSharedPreferences mockPreferences = new MockSharedPreferences();
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences);

        dataSource = new TranscriptionLocalDataSource(context);
    }

    @Test
    public void testSaveAndLoadLastMessage() {
        List<TranscriptionParagraph> paragraphs = new ArrayList<>();
        TranscriptionParagraph p1 = new TranscriptionParagraph("raw1", "formatted1", "/path/1.wav");
        p1.setShowFormatted(true);
        TranscriptionParagraph p2 = new TranscriptionParagraph("raw2", null, null);
        p2.setShowFormatted(false);

        paragraphs.add(p1);
        paragraphs.add(p2);

        dataSource.saveLastMessage(paragraphs, Locale.GERMAN);

        List<TranscriptionParagraph> loaded = dataSource.loadLastMessage();
        assertNotNull(loaded);
        assertEquals(2, loaded.size());

        assertEquals("raw1", loaded.get(0).getRawText());
        assertEquals("formatted1", loaded.get(0).getFormattedText());
        assertEquals("/path/1.wav", loaded.get(0).getAudioFilePath());
        assertTrue(loaded.get(0).isShowFormatted());

        assertEquals("raw2", loaded.get(1).getRawText());
        assertNull(loaded.get(1).getFormattedText());
        assertNull(loaded.get(1).getAudioFilePath());
    }

    @Test
    public void testLoadLastMessageWhenEmpty() {
        assertNull(dataSource.loadLastMessage());
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
