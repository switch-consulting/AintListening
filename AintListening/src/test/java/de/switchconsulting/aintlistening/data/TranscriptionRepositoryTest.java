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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Unit tests for {@link TranscriptionRepository}.
 */
public class TranscriptionRepositoryTest {

    private TranscriptionLocalDataSource localDataSource;
    private AudioStorageManager audioStorageManager;
    private PreferencesDataSource preferencesDataSource;
    private TranscriptionRepository repository;

    @Before
    public void setUp() {
        localDataSource = mock(TranscriptionLocalDataSource.class);
        audioStorageManager = mock(AudioStorageManager.class);
        preferencesDataSource = mock(PreferencesDataSource.class);
        repository = new TranscriptionRepository(localDataSource, audioStorageManager, preferencesDataSource);
    }

    @Test
    public void testLoadLastMessageDelegatesToLocalDataSource() {
        List<TranscriptionParagraph> expected = Collections.singletonList(new TranscriptionParagraph("raw", "formatted"));
        when(localDataSource.loadLastMessage()).thenReturn(expected);

        List<TranscriptionParagraph> actual = repository.loadLastMessage();
        assertEquals(expected, actual);
        verify(localDataSource).loadLastMessage();
    }

    @Test
    public void testSaveLastMessageDelegatesToLocalDataSource() {
        List<TranscriptionParagraph> paragraphs = Collections.singletonList(new TranscriptionParagraph("raw", "formatted"));
        repository.saveLastMessage(paragraphs, Locale.GERMAN);

        verify(localDataSource).saveLastMessage(paragraphs, Locale.GERMAN);
    }

    @Test
    public void testGetIncomingWavFileDelegatesToAudioStorageManager() {
        File expectedFile = new File("/tmp/test.wav");
        when(audioStorageManager.getIncomingWavFile()).thenReturn(expectedFile);

        File actualFile = repository.getIncomingWavFile();
        assertEquals(expectedFile, actualFile);
        verify(audioStorageManager).getIncomingWavFile();
    }

    @Test
    public void testClearTemporaryFilesDelegatesToAudioStorageManager() {
        repository.clearTemporaryFiles();
        verify(audioStorageManager).clearTemporaryFiles();
    }

    @Test
    public void testSaveAudioChunkDelegatesToAudioStorageManager() throws IOException {
        byte[] pcm = new byte[]{1, 2, 3};
        when(audioStorageManager.saveAudioChunk(pcm, 1)).thenReturn("/path/chunk_1.wav");

        String path = repository.saveAudioChunk(pcm, 1);
        assertEquals("/path/chunk_1.wav", path);
        verify(audioStorageManager).saveAudioChunk(pcm, 1);
    }

    @Test
    public void testGetPreferencesDataSourceReturnsInstance() {
        assertEquals(preferencesDataSource, repository.getPreferencesDataSource());
    }
}
