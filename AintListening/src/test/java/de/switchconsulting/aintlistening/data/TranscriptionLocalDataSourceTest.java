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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.switchconsulting.aintlistening.data.db.ParagraphEntity;
import de.switchconsulting.aintlistening.data.db.TranscriptionDao;
import de.switchconsulting.aintlistening.data.db.TranscriptionEntity;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Unit tests for {@link TranscriptionLocalDataSource}.
 */
public class TranscriptionLocalDataSourceTest {

    private TranscriptionDao transcriptionDao;
    private TranscriptionLocalDataSource dataSource;

    @Before
    public void setUp() {
        transcriptionDao = mock(TranscriptionDao.class);
        dataSource = new TranscriptionLocalDataSource(transcriptionDao);
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

        when(transcriptionDao.insertTranscription(any())).thenReturn(1L);

        List<ParagraphEntity> mockEntities = new ArrayList<>();
        mockEntities.add(new ParagraphEntity(1L, "raw1", "formatted1", true, "/path/1.wav"));
        mockEntities.add(new ParagraphEntity(1L, "raw2", null, false, null));

        TranscriptionEntity session = new TranscriptionEntity(System.currentTimeMillis(), "de");
        session.id = 1L;

        when(transcriptionDao.getLatestTranscriptionSession()).thenReturn(session);
        when(transcriptionDao.getParagraphsForSession(1L)).thenReturn(mockEntities);

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
        when(transcriptionDao.getLatestTranscriptionSession()).thenReturn(null);
        assertNull(dataSource.loadLastMessage());
    }
}
