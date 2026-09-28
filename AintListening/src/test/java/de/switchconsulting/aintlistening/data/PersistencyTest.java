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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.switchconsulting.aintlistening.transcription.TranscriberType;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Unit tests for {@link Persistency}.
 */
public class PersistencyTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private MockSharedPreferences mockPreferences;
    private Persistency persistency;
    private File cacheDir;
    private File filesDir;

    @Before
    public void setUp() throws IOException {
        Context context = mock(Context.class);
        cacheDir = temporaryFolder.newFolder("cache");
        filesDir = temporaryFolder.newFolder("files");

        when(context.getApplicationContext()).thenReturn(context);
        when(context.getCacheDir()).thenReturn(cacheDir);
        when(context.getFilesDir()).thenReturn(filesDir);

        mockPreferences = new MockSharedPreferences();
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences);

        persistency = new Persistency(context);
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

        persistency.saveLastMessage(paragraphs, Locale.GERMAN);

        List<TranscriptionParagraph> loaded = persistency.loadLastMessage();
        assertNotNull(loaded);
        assertEquals(2, loaded.size());

        assertEquals("raw1", loaded.get(0).getRawText());
        assertEquals("formatted1", loaded.get(0).getFormattedText());
        assertEquals("/path/1.wav", loaded.get(0).getAudioFilePath());
        assertTrue(loaded.get(0).isShowFormatted());

        assertEquals("raw2", loaded.get(1).getRawText());
        assertNull(loaded.get(1).getFormattedText());
        assertNull(loaded.get(1).getAudioFilePath());
        assertFalse(loaded.get(1).isShowFormatted());
    }

    @Test
    public void testLoadLastMessageWhenEmpty() {
        assertNull(persistency.loadLastMessage());
    }

    @Test
    public void testLoadLastMessageWithCorruptedJson() {
        mockPreferences.edit().putString("last_paragraphs_json", "invalid json").apply();
        assertNull(persistency.loadLastMessage());
    }

    @Test
    public void testSaveLastMessageWithNullLocale() {
        List<TranscriptionParagraph> paragraphs = Collections.singletonList(new TranscriptionParagraph("raw", "fmt"));
        persistency.saveLastMessage(paragraphs, null);

        List<TranscriptionParagraph> loaded = persistency.loadLastMessage();
        assertNotNull(loaded);
        assertEquals(1, loaded.size());
    }

    @Test
    public void testShowPlaybackButton() {
        assertTrue(persistency.isShowPlaybackButton());
        persistency.setShowPlaybackButton(false);
        assertFalse(persistency.isShowPlaybackButton());
        persistency.setShowPlaybackButton(true);
        assertTrue(persistency.isShowPlaybackButton());
    }

    @Test
    public void testShowCopyButton() {
        assertTrue(persistency.isShowCopyButton());
        persistency.setShowCopyButton(false);
        assertFalse(persistency.isShowCopyButton());
        persistency.setShowCopyButton(true);
        assertTrue(persistency.isShowCopyButton());
    }

    @Test
    public void testShowRawText() {
        assertTrue(persistency.isShowRawText());
        persistency.setShowRawText(false);
        assertFalse(persistency.isShowRawText());
        persistency.setShowRawText(true);
        assertTrue(persistency.isShowRawText());
    }

    @Test
    public void testShowSmartText() {
        assertTrue(persistency.isShowSmartText());
        persistency.setShowSmartText(false);
        assertFalse(persistency.isShowSmartText());
        persistency.setShowSmartText(true);
        assertTrue(persistency.isShowSmartText());
    }

    @Test
    public void testSmartFormattingEnabledPerLocale() {
        assertTrue(persistency.isSmartFormattingEnabled(Locale.GERMAN));

        persistency.setSmartFormattingEnabled(Locale.GERMAN, false);
        assertFalse(persistency.isSmartFormattingEnabled(Locale.GERMAN));
        assertTrue(persistency.isSmartFormattingEnabled(Locale.ENGLISH));

        persistency.setSmartFormattingEnabled(Locale.GERMAN, true);
        assertTrue(persistency.isSmartFormattingEnabled(Locale.GERMAN));
    }

    @Test
    public void testTranscriberTypePerLocale() {
        assertEquals(TranscriberType.VOSK, persistency.getDefaultTranscriberType());
        assertEquals(TranscriberType.VOSK, persistency.getTranscriberType(Locale.GERMAN));

        persistency.setTranscriberType(Locale.GERMAN, TranscriberType.WHISPER);
        assertEquals(TranscriberType.WHISPER, persistency.getTranscriberType(Locale.GERMAN));
        assertEquals(TranscriberType.VOSK, persistency.getTranscriberType(Locale.ENGLISH));
    }

    @Test
    public void testGetTranscriberTypeInvalidEnumFallback() {
        mockPreferences.edit().putString("transcriber_type_de", "INVALID_TYPE").apply();
        assertEquals(TranscriberType.VOSK, persistency.getTranscriberType(Locale.GERMAN));
    }

    @Test
    public void testLanguageEnabled() {
        assertTrue(persistency.isLanguageEnabled(Locale.GERMAN));

        persistency.setLanguageEnabled(Locale.GERMAN, false);
        assertFalse(persistency.isLanguageEnabled(Locale.GERMAN));
        assertTrue(persistency.isLanguageEnabled(Locale.ENGLISH));

        persistency.setLanguageEnabled(Locale.GERMAN, true);
        assertTrue(persistency.isLanguageEnabled(Locale.GERMAN));
    }

    @Test
    public void testGetIncomingWavFile() {
        File file = persistency.getIncomingWavFile();
        assertNotNull(file);
        assertEquals(cacheDir, file.getParentFile());
        assertEquals("incoming_audio_16k_mono.wav", file.getName());
    }

    @Test
    public void testGetAudioChunksDirAndSaveChunk() throws IOException {
        File chunksDir = persistency.getAudioChunksDir();
        assertNotNull(chunksDir);
        assertTrue(chunksDir.exists());
        assertTrue(chunksDir.isDirectory());

        byte[] pcmData = new byte[]{0x01, 0x02, 0x03, 0x04};
        String chunkPath = persistency.saveAudioChunk(pcmData, 1);
        assertNotNull(chunkPath);
        File chunkFile = new File(chunkPath);
        assertTrue(chunkFile.exists());
        assertEquals("chunk_1.wav", chunkFile.getName());
    }

    @Test
    public void testClearTemporaryFiles() throws IOException {
        File incoming = persistency.getIncomingWavFile();
        assertTrue(incoming.createNewFile());

        byte[] pcmData = new byte[]{0x01, 0x02, 0x03, 0x04};
        persistency.saveAudioChunk(pcmData, 1);
        persistency.saveAudioChunk(pcmData, 2);

        persistency.clearTemporaryFiles();

        assertFalse(incoming.exists());
        File chunksDir = new File(filesDir, "audio_chunks");
        if (chunksDir.exists() && chunksDir.isDirectory()) {
            File[] files = chunksDir.listFiles();
            assertTrue(files == null || files.length == 0);
        }
    }

    /**
     * In-memory implementation of SharedPreferences for testing.
     */
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
