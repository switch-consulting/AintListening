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
import static org.junit.Assert.assertTrue;
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

/**
 * Unit tests for {@link AudioStorageManager}.
 */
public class AudioStorageManagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AudioStorageManager audioStorageManager;
    private File cacheDir;

    @Before
    public void setUp() throws IOException {
        Context context = mock(Context.class);
        cacheDir = temporaryFolder.newFolder("cache");
        File filesDir = temporaryFolder.newFolder("files");

        when(context.getApplicationContext()).thenReturn(context);
        when(context.getCacheDir()).thenReturn(cacheDir);
        when(context.getFilesDir()).thenReturn(filesDir);

        audioStorageManager = new AudioStorageManager(context);
    }

    @Test
    public void testGetIncomingWavFile() {
        File file = audioStorageManager.getIncomingWavFile();
        assertNotNull(file);
        assertEquals("incoming_audio_16k_mono.wav", file.getName());
        assertEquals(cacheDir.getAbsolutePath(), file.getParent());
    }

    @Test
    public void testGetAudioChunksDir() {
        File chunksDir = audioStorageManager.getAudioChunksDir();
        assertNotNull(chunksDir);
        assertTrue(chunksDir.exists());
        assertTrue(chunksDir.isDirectory());
    }

    @Test
    public void testSaveAudioChunk() throws IOException {
        byte[] pcmData = new byte[]{1, 2, 3, 4, 5};
        String chunkPath = audioStorageManager.saveAudioChunk(pcmData, 1);
        assertNotNull(chunkPath);

        File chunkFile = new File(chunkPath);
        assertTrue(chunkFile.exists());
        assertTrue(chunkFile.length() > 0);
    }

    @Test
    public void testClearTemporaryFiles() throws IOException {
        File incoming = audioStorageManager.getIncomingWavFile();
        try (FileOutputStream fos = new FileOutputStream(incoming)) {
            fos.write(new byte[]{1, 2, 3});
        }
        assertTrue(incoming.exists());

        byte[] pcmData = new byte[]{1, 2, 3};
        audioStorageManager.saveAudioChunk(pcmData, 1);
        audioStorageManager.saveAudioChunk(pcmData, 2);

        audioStorageManager.clearTemporaryFiles();

        assertFalse(incoming.exists());
        File chunksDir = audioStorageManager.getAudioChunksDir();
        if (chunksDir.exists() && chunksDir.isDirectory()) {
            File[] files = chunksDir.listFiles();
            assertTrue(files == null || files.length == 0);
        }
    }
}
