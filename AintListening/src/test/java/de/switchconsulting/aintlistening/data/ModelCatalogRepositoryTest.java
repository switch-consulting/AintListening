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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link ModelCatalogRepository}.
 */
public class ModelCatalogRepositoryTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Context context;
    private PreferencesDataSource preferencesDataSource;
    private ModelCatalogRepository repository;

    @Before
    public void setUp() throws IOException {
        context = mock(Context.class);
        preferencesDataSource = mock(PreferencesDataSource.class);
        File filesDir = temporaryFolder.newFolder("filesDir");
        when(context.getFilesDir()).thenReturn(filesDir);
        when(preferencesDataSource.getDefaultTranscriberType()).thenReturn(TranscriberType.VOSK);

        repository = new ModelCatalogRepository(context, preferencesDataSource);
    }

    @Test
    public void testGetSupportedLanguages() {
        assertNotNull(repository.getSupportedLanguages());
        assertFalse(repository.getSupportedLanguages().isEmpty());
    }

    @Test
    public void testGetLanguageSupport() {
        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);
        assertEquals(Locale.GERMAN, german.getLocale());

        assertNull(repository.getLanguageSupport(Locale.JAPANESE));
        assertNull(repository.getLanguageSupport(null));
    }

    @Test
    public void testIsModelDownloadedZipModel() {
        ModelInfo zipModel = new ModelInfo("vosk-model-de", "http://example.com", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);

        assertFalse(repository.isModelDownloaded(zipModel));

        File modelDir = new File(context.getFilesDir(), "vosk-model-de");
        assertTrue(modelDir.mkdir());

        assertTrue(repository.isModelDownloaded(zipModel));
    }

    @Test
    public void testIsModelDownloadedNonZipModel() throws IOException {
        ModelInfo binModel = new ModelInfo("model.bin", "http://example.com", Locale.GERMAN, "75MB", TranscriberType.VOSK, false);

        assertFalse(repository.isModelDownloaded(binModel));

        File modelFile = new File(context.getFilesDir(), "model.bin");
        assertTrue(modelFile.createNewFile());

        assertTrue(repository.isModelDownloaded(binModel));
    }

    @Test
    public void testIsModelDownloadedWhisperModel() throws IOException {
        ModelInfo binModel = new ModelInfo("ggml-tiny.bin", "http://example.com", Locale.GERMAN, "75MB", TranscriberType.WHISPER, false);

        assertFalse(repository.isModelDownloaded(binModel));

        File whisperDir = new File(context.getFilesDir(), "whisper_models");
        assertTrue(whisperDir.mkdirs() || whisperDir.isDirectory());
        File modelFile = new File(whisperDir, "ggml-tiny.bin");
        assertTrue(modelFile.createNewFile());
        try (FileOutputStream fos = new FileOutputStream(modelFile)) {
            fos.write(1);
        }

        assertTrue(repository.isModelDownloaded(binModel));
    }

    @Test
    public void testIsModelDownloadedNull() {
        assertFalse(repository.isModelDownloaded(null));
    }

    @Test
    public void testIsDownloadedWithNulls() {
        assertFalse(repository.isDownloaded(null, TranscriberType.VOSK));
        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertFalse(repository.isDownloaded(german, null));
    }

    @Test
    public void testIsDownloadedForLanguageAndType() {
        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);

        assertFalse(repository.isDownloaded(german, TranscriberType.VOSK));

        File modelDir = new File(context.getFilesDir(), "vosk-model-small-de-0.15");
        assertTrue(modelDir.mkdir());

        assertTrue(repository.isDownloaded(german, TranscriberType.VOSK));
    }

    @Test
    public void testIsFormattingDownloaded() {
        assertFalse(repository.isFormattingDownloaded(null));

        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);

        assertFalse(repository.isFormattingDownloaded(german));

        File puncDir = new File(context.getFilesDir(), "ONNXModel_multilingual");
        assertTrue(puncDir.mkdir());

        assertTrue(repository.isFormattingDownloaded(german));
    }

    @Test
    public void testHasTranscriptionModelDownloaded() {
        assertFalse(repository.hasTranscriptionModelDownloaded(null));

        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);

        assertFalse(repository.hasTranscriptionModelDownloaded(german));

        File modelDir = new File(context.getFilesDir(), "vosk-model-small-de-0.15");
        assertTrue(modelDir.mkdir());

        assertTrue(repository.hasTranscriptionModelDownloaded(german));
    }

    @Test
    public void testGetActiveTranscriberTypePreferredDownloaded() {
        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);

        when(preferencesDataSource.getTranscriberType(Locale.GERMAN)).thenReturn(TranscriberType.VOSK);

        File modelDir = new File(context.getFilesDir(), "vosk-model-small-de-0.15");
        assertTrue(modelDir.mkdir());

        assertEquals(TranscriberType.VOSK, repository.getActiveTranscriberType(german));
    }

    @Test
    public void testGetActiveTranscriberTypePreferredNotDownloadedFallbackToAvailable() throws IOException {
        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);

        when(preferencesDataSource.getTranscriberType(Locale.GERMAN)).thenReturn(TranscriberType.VOSK);

        // Vosk NOT downloaded, but Whisper IS downloaded
        File whisperDir = new File(context.getFilesDir(), "whisper_models");
        assertTrue(whisperDir.mkdirs() || whisperDir.isDirectory());
        File modelFile = new File(whisperDir, "ggml-tiny.bin");
        assertTrue(modelFile.createNewFile());
        try (FileOutputStream fos = new FileOutputStream(modelFile)) {
            fos.write(1);
        }

        assertEquals(TranscriberType.WHISPER, repository.getActiveTranscriberType(german));
    }

    @Test
    public void testGetActiveTranscriberTypeNoModelsDownloadedFallbackToDefault() {
        LanguageSupport german = repository.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);

        when(preferencesDataSource.getTranscriberType(Locale.GERMAN)).thenReturn(TranscriberType.VOSK);
        when(preferencesDataSource.getDefaultTranscriberType()).thenReturn(TranscriberType.VOSK);

        assertEquals(TranscriberType.VOSK, repository.getActiveTranscriberType(german));
    }

    @Test
    public void testGetAvailableLanguageNames() {
        when(preferencesDataSource.isLanguageEnabled(any())).thenReturn(true);

        // Initially no models downloaded -> empty list
        assertTrue(repository.getAvailableLanguageNames().isEmpty());

        // Download German Vosk model
        File germanModelDir = new File(context.getFilesDir(), "vosk-model-small-de-0.15");
        assertTrue(germanModelDir.mkdir());

        List<String> names = repository.getAvailableLanguageNames();
        assertEquals(1, names.size());

        // Now disable German in preferences
        when(preferencesDataSource.isLanguageEnabled(Locale.GERMAN)).thenReturn(false);
        assertTrue(repository.getAvailableLanguageNames().isEmpty());
    }

    @Test
    public void testDeleteModelNull() {
        assertFalse(repository.deleteModel(null));
    }

    @Test
    public void testDeleteModelFileAndDir() throws IOException {
        ModelInfo model = new ModelInfo("model-to-delete", "http://example.com", Locale.GERMAN, "10MB", TranscriberType.VOSK, true);
        File dir = new File(context.getFilesDir(), "model-to-delete");
        assertTrue(dir.mkdir());
        File subDir = new File(dir, "subdir");
        assertTrue(subDir.mkdir());
        File subFile = new File(subDir, "data.txt");
        assertTrue(subFile.createNewFile());

        assertTrue(repository.deleteModel(model));
        assertFalse(dir.exists());
    }

    @Test
    public void testDeleteModelWhisper() throws IOException {
        ModelInfo whisperModel = new ModelInfo("ggml-tiny.bin", "http://example.com", Locale.GERMAN, "75MB", TranscriberType.WHISPER, false);

        File whisperDir = new File(context.getFilesDir(), "whisper_models");
        assertTrue(whisperDir.mkdirs() || whisperDir.isDirectory());
        File modelFile = new File(whisperDir, "ggml-tiny.bin");
        assertTrue(modelFile.createNewFile());

        assertTrue(repository.deleteModel(whisperModel));
        assertFalse(modelFile.exists());
    }

    @Test
    public void testGetEngineNameResId() {
        assertEquals(R.string.engine_vosk, repository.getEngineNameResId(TranscriberType.VOSK));
        assertEquals(R.string.engine_whisper, repository.getEngineNameResId(TranscriberType.WHISPER));
    }
}
