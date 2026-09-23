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

package de.switchconsulting.aintlistening.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Unit tests for {@link MainUiState}.
 */
public class MainUiStateTest {

    @Test
    public void testIdleState() {
        List<TranscriptionParagraph> paragraphs = Collections.singletonList(new TranscriptionParagraph("raw", "formatted"));
        MainUiState state = MainUiState.idle(paragraphs);

        assertFalse(state.isLoading);
        assertFalse(state.isIndeterminate);
        assertEquals(0, state.progress);
        assertEquals(0, state.maxProgress);
        assertNull(state.statusMessage);
        assertEquals(paragraphs, state.paragraphs);
        assertNull(state.errorMessage);
    }

    @Test
    public void testLoadingState() {
        List<TranscriptionParagraph> paragraphs = Collections.emptyList();
        MainUiState state = MainUiState.loading("Transcribing...", paragraphs);

        assertTrue(state.isLoading);
        assertTrue(state.isIndeterminate);
        assertEquals(0, state.progress);
        assertEquals(0, state.maxProgress);
        assertEquals("Transcribing...", state.statusMessage);
        assertEquals(paragraphs, state.paragraphs);
        assertNull(state.errorMessage);
    }

    @Test
    public void testProgressState() {
        List<TranscriptionParagraph> paragraphs = Collections.emptyList();
        MainUiState state = MainUiState.progress("Formatting...", 3, 10, paragraphs);

        assertTrue(state.isLoading);
        assertFalse(state.isIndeterminate);
        assertEquals(3, state.progress);
        assertEquals(10, state.maxProgress);
        assertEquals("Formatting...", state.statusMessage);
        assertEquals(paragraphs, state.paragraphs);
        assertNull(state.errorMessage);
    }

    @Test
    public void testErrorState() {
        List<TranscriptionParagraph> paragraphs = Collections.emptyList();
        MainUiState state = MainUiState.error("Error occurred", paragraphs);

        assertFalse(state.isLoading);
        assertFalse(state.isIndeterminate);
        assertEquals(0, state.progress);
        assertEquals(0, state.maxProgress);
        assertEquals("Error occurred", state.statusMessage);
        assertEquals(paragraphs, state.paragraphs);
        assertEquals("Error occurred", state.errorMessage);
    }
}
