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

    private Persistency persistency;
    private TranscriptionRepository repository;

    @Before
    public void setUp() {
        persistency = mock(Persistency.class);
        repository = new TranscriptionRepository(persistency);
    }

    @Test
    public void testLoadLastMessageDelegatesToPersistency() {
        List<TranscriptionParagraph> expected = Collections.singletonList(new TranscriptionParagraph("raw", "formatted"));
        when(persistency.loadLastMessage()).thenReturn(expected);

        List<TranscriptionParagraph> actual = repository.loadLastMessage();
        assertEquals(expected, actual);
        verify(persistency).loadLastMessage();
    }

    @Test
    public void testSaveLastMessageDelegatesToPersistency() {
        List<TranscriptionParagraph> paragraphs = Collections.singletonList(new TranscriptionParagraph("raw", "formatted"));
        repository.saveLastMessage(paragraphs, Locale.GERMAN);

        verify(persistency).saveLastMessage(paragraphs, Locale.GERMAN);
    }

    @Test
    public void testGetIncomingWavFileDelegatesToPersistency() {
        File expectedFile = new File("/tmp/test.wav");
        when(persistency.getIncomingWavFile()).thenReturn(expectedFile);

        File actualFile = repository.getIncomingWavFile();
        assertEquals(expectedFile, actualFile);
        verify(persistency).getIncomingWavFile();
    }

    @Test
    public void testClearTemporaryFilesDelegatesToPersistency() {
        repository.clearTemporaryFiles();
        verify(persistency).clearTemporaryFiles();
    }

    @Test
    public void testSaveAudioChunkDelegatesToPersistency() throws IOException {
        byte[] pcm = new byte[]{1, 2, 3};
        when(persistency.saveAudioChunk(pcm, 1)).thenReturn("/path/chunk_1.wav");

        String path = repository.saveAudioChunk(pcm, 1);
        assertEquals("/path/chunk_1.wav", path);
        verify(persistency).saveAudioChunk(pcm, 1);
    }

    @Test
    public void testGetPersistencyReturnsInstance() {
        assertEquals(persistency, repository.getPersistency());
    }
}
