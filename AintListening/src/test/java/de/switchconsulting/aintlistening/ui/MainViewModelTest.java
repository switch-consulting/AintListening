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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.Uri;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.switchconsulting.aintlistening.data.TranscriptionCallback;
import de.switchconsulting.aintlistening.data.TranscriptionProcessor;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Unit tests for {@link MainViewModel}.
 */
public class MainViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private TranscriptionProcessor processor;
    private MainViewModel viewModel;

    @Before
    public void setUp() {
        processor = mock(TranscriptionProcessor.class);
        List<TranscriptionParagraph> paragraphs = Collections.singletonList(new TranscriptionParagraph("raw", "formatted"));
        when(processor.loadLastMessage()).thenReturn(paragraphs);

        viewModel = new MainViewModel(processor);
    }

    @Test
    public void testInitialStateLoadsLastMessage() {
        MainUiState state = viewModel.uiState.getValue();
        assertNotNull(state);
        assertEquals(1, state.paragraphs.size());
        assertEquals("raw", state.paragraphs.get(0).getRawText());
    }

    @Test
    public void testLoadLastMessageUpdatesUiState() {
        List<TranscriptionParagraph> newParagraphs = Collections.singletonList(new TranscriptionParagraph("new raw", "new formatted"));
        when(processor.loadLastMessage()).thenReturn(newParagraphs);

        viewModel.loadLastMessage();

        MainUiState state = viewModel.uiState.getValue();
        assertNotNull(state);
        assertEquals(newParagraphs, state.paragraphs);
    }

    @Test
    public void testStartTranscriptionCallbackFlow() {
        Uri audioUri = mock(Uri.class);
        List<TranscriptionParagraph> resultParagraphs = Collections.singletonList(new TranscriptionParagraph("done", "Done."));

        doAnswer(invocation -> {
            TranscriptionCallback callback = invocation.getArgument(2);
            callback.onStatusUpdate("Converting...");
            callback.onPartialResult(Collections.singletonList(new TranscriptionParagraph("partial", null)));
            callback.onSmartFormattingProgress(1, 1, resultParagraphs);
            callback.onComplete(resultParagraphs);
            return null;
        }).when(processor).startTranscription(eq(audioUri), eq(Locale.GERMAN), any());

        viewModel.startTranscription(audioUri, Locale.GERMAN);

        MainUiState finalState = viewModel.uiState.getValue();
        assertNotNull(finalState);
        assertEquals(resultParagraphs, finalState.paragraphs);
        assertFalse(finalState.isLoading);
    }

    @Test
    public void testStartTranscriptionErrorFlow() {
        Uri audioUri = mock(Uri.class);

        doAnswer(invocation -> {
            TranscriptionCallback callback = invocation.getArgument(2);
            callback.onError("Failed to decode audio");
            return null;
        }).when(processor).startTranscription(eq(audioUri), eq(Locale.GERMAN), any());

        viewModel.startTranscription(audioUri, Locale.GERMAN);

        MainUiState finalState = viewModel.uiState.getValue();
        assertNotNull(finalState);
        assertEquals("Failed to decode audio", finalState.errorMessage);
        assertFalse(finalState.isLoading);
    }
}
