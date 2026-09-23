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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link ModelManager}.
 */
public class ModelManagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetEngineNameResId() {
        assertEquals(R.string.engine_vosk, ModelManager.getEngineNameResId(TranscriberType.VOSK));
        assertEquals(R.string.engine_whisper, ModelManager.getEngineNameResId(TranscriberType.WHISPER));
    }

    @Test
    public void testGetLanguageSupport() {
        LanguageSupport german = ModelManager.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);
        assertEquals(Locale.GERMAN, german.getLocale());

        LanguageSupport english = ModelManager.getLanguageSupport(Locale.ENGLISH);
        assertNotNull(english);
        assertEquals(Locale.ENGLISH, english.getLocale());

        LanguageSupport unsupported = ModelManager.getLanguageSupport(Locale.JAPANESE);
        assertNull(unsupported);

        assertNull(ModelManager.getLanguageSupport(null));
    }

    @Test
    public void testIsModelDownloadedZipModel() throws IOException {
        Context context = mock(Context.class);
        File filesDir = temporaryFolder.newFolder("filesDir");
        when(context.getFilesDir()).thenReturn(filesDir);

        ModelInfo zipModel = new ModelInfo("vosk-model-de", "http://example.com", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);

        // Not downloaded yet
        assertFalse(ModelManager.INSTANCE.isModelDownloaded(context, zipModel));

        // Create folder for model
        File modelDir = new File(filesDir, "vosk-model-de");
        assertTrue(modelDir.mkdir());

        // Now downloaded
        assertTrue(ModelManager.INSTANCE.isModelDownloaded(context, zipModel));
    }

    @Test
    public void testIsModelDownloadedNonZipModel() throws IOException {
        Context context = mock(Context.class);
        File filesDir = temporaryFolder.newFolder("filesDir");
        when(context.getFilesDir()).thenReturn(filesDir);

        ModelInfo binModel = new ModelInfo("ggml-tiny.bin", "http://example.com", Locale.GERMAN, "75MB", TranscriberType.WHISPER, false);

        // Not downloaded yet
        assertFalse(ModelManager.INSTANCE.isModelDownloaded(context, binModel));

        // Create single file for model
        File modelFile = new File(filesDir, "ggml-tiny.bin");
        assertTrue(modelFile.createNewFile());

        // Now downloaded
        assertTrue(ModelManager.INSTANCE.isModelDownloaded(context, binModel));
    }
}
