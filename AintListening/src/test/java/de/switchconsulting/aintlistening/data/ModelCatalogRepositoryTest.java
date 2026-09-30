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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
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
    private ModelCatalogRepository repository;

    @Before
    public void setUp() throws IOException {
        context = mock(Context.class);
        PreferencesDataSource preferencesDataSource = mock(PreferencesDataSource.class);
        File filesDir = temporaryFolder.newFolder("filesDir");
        when(context.getFilesDir()).thenReturn(filesDir);

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
        ModelInfo binModel = new ModelInfo("ggml-tiny.bin", "http://example.com", Locale.GERMAN, "75MB", TranscriberType.WHISPER, false);

        assertFalse(repository.isModelDownloaded(binModel));

        File modelFile = new File(context.getFilesDir(), "ggml-tiny.bin");
        assertTrue(modelFile.createNewFile());

        assertTrue(repository.isModelDownloaded(binModel));
    }

    @Test
    public void testIsModelDownloadedNull() {
        assertFalse(repository.isModelDownloaded(null));
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
        File subFile = new File(dir, "data.txt");
        assertTrue(subFile.createNewFile());

        assertTrue(repository.deleteModel(model));
        assertFalse(dir.exists());
    }

    @Test
    public void testGetEngineNameResId() {
        assertEquals(R.string.engine_vosk, repository.getEngineNameResId(TranscriberType.VOSK));
        assertEquals(R.string.engine_whisper, repository.getEngineNameResId(TranscriberType.WHISPER));
    }
}
