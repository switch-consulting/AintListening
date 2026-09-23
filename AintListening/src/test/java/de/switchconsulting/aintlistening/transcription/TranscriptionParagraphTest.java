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

package de.switchconsulting.aintlistening.transcription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link TranscriptionParagraph}.
 */
public class TranscriptionParagraphTest {

    @Test
    public void testParagraphWithFormattedText() {
        TranscriptionParagraph paragraph = new TranscriptionParagraph("hallo welt", "Hallo Welt.", "/path/to/chunk.wav");

        assertEquals("hallo welt", paragraph.getRawText());
        assertEquals("Hallo Welt.", paragraph.getFormattedText());
        assertEquals("/path/to/chunk.wav", paragraph.getAudioFilePath());
        assertTrue(paragraph.isShowFormatted());
        assertEquals("Hallo Welt.", paragraph.getDisplayText());
    }

    @Test
    public void testParagraphWithoutFormattedText() {
        TranscriptionParagraph paragraph = new TranscriptionParagraph("hallo welt", null);

        assertEquals("hallo welt", paragraph.getRawText());
        assertNull(paragraph.getFormattedText());
        assertNull(paragraph.getAudioFilePath());
        assertFalse(paragraph.isShowFormatted());
        assertEquals("hallo welt", paragraph.getDisplayText());
    }

    @Test
    public void testParagraphWithEmptyFormattedText() {
        TranscriptionParagraph paragraph = new TranscriptionParagraph("hallo welt", "");

        assertEquals("hallo welt", paragraph.getRawText());
        assertEquals("", paragraph.getFormattedText());
        assertFalse(paragraph.isShowFormatted());
        assertEquals("hallo welt", paragraph.getDisplayText());
    }

    @Test
    public void testToggleShowFormatted() {
        TranscriptionParagraph paragraph = new TranscriptionParagraph("raw text", "Formatted Text.");

        assertTrue(paragraph.isShowFormatted());
        assertEquals("Formatted Text.", paragraph.getDisplayText());

        paragraph.setShowFormatted(false);
        assertFalse(paragraph.isShowFormatted());
        assertEquals("raw text", paragraph.getDisplayText());

        paragraph.setShowFormatted(true);
        assertTrue(paragraph.isShowFormatted());
        assertEquals("Formatted Text.", paragraph.getDisplayText());
    }
}
