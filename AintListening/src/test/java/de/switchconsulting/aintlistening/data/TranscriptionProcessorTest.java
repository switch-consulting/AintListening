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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import de.switchconsulting.aintlistening.transcription.TranscriberRegistry;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Unit tests for {@link TranscriptionProcessor}.
 */
public class TranscriptionProcessorTest {

    private TranscriptionProcessor processor;

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        TranscriptionRepository repository = mock(TranscriptionRepository.class);
        TranscriberRegistry registry = mock(TranscriberRegistry.class);
        processor = new TranscriptionProcessor(context, repository, registry);
    }

    @Test
    public void testParseParagraphsEmptyText() {
        List<TranscriptionParagraph> result = processor.parseParagraphs("   ", false);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseParagraphsWithoutPunctuationEngine() {
        String text = "Paragraph one\n\nParagraph two";
        List<TranscriptionParagraph> result = processor.parseParagraphs(text, false);

        assertEquals(2, result.size());
        assertEquals("Paragraph one", result.get(0).getRawText());
        assertNull(result.get(0).getFormattedText());

        assertEquals("Paragraph two", result.get(1).getRawText());
        assertNull(result.get(1).getFormattedText());
    }

    @Test
    public void testParseParagraphsWithPunctuationEngine() {
        String text = "Paragraph one.\n\nParagraph two.";
        List<TranscriptionParagraph> result = processor.parseParagraphs(text, true);

        assertEquals(2, result.size());
        assertEquals("Paragraph one.", result.get(0).getRawText());
        assertEquals("Paragraph one.", result.get(0).getFormattedText());

        assertEquals("Paragraph two.", result.get(1).getRawText());
        assertEquals("Paragraph two.", result.get(1).getFormattedText());
    }
}
