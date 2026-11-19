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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.switchconsulting.aintlistening.data.ModelCatalogRepository;
import de.switchconsulting.androidwhisperer.TranscriptionException;
import de.switchconsulting.androidwhisperer.model.SegmentResult;

/**
 * Unit tests for {@link WhisperTranscriber}.
 */
public class WhisperTranscriberTest {

    private WhisperTranscriber transcriber;

    @Before
    public void setUp() {
        ModelCatalogRepository modelRepository = mock(ModelCatalogRepository.class);
        transcriber = new WhisperTranscriber(modelRepository);
    }

    @Test
    public void testGetTypeAndProvidesPunctuation() {
        assertEquals(TranscriberType.WHISPER, transcriber.getType());
        assertTrue(transcriber.providesPunctuation());
        assertTrue(transcriber.supportsPauseLengthConfiguration());
    }

    @Test
    public void testSetPauseLengthMs() {
        transcriber.setPauseLengthMs(1000L);
    }

    @Test(expected = IllegalStateException.class)
    public void testTranscribeWithoutModelLoadedThrowsException() throws Exception {
        Context context = mock(Context.class);
        File dummyFile = new File("dummy.wav");
        transcriber.transcribe(context, dummyFile, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testEnsureModelLoadedUnsupportedLanguageThrowsException() throws Exception {
        Context context = mock(Context.class);
        transcriber.ensureModelLoaded(context, Locale.JAPANESE);
    }

    @Test(expected = IllegalStateException.class)
    public void testEnsureModelLoadedNotDownloadedThrowsException() throws Exception {
        Context context = mock(Context.class);
        transcriber.ensureModelLoaded(context, Locale.GERMAN);
    }

    @Test
    public void testCloseResetsState() {
        transcriber.close();
        assertEquals(TranscriberType.WHISPER, transcriber.getType());
    }

    @Test
    public void testCreateWhisperListenerAccumulatesSegmentsAndHandlesEvents() {
        List<PartialRecognitionResult> partialResults = new ArrayList<>();
        List<PartialRecognitionResult> emitted = new ArrayList<>();

        TranscriptionListener listener = new TranscriptionListener() {
            @Override
            public void onPartialRecognition(@NonNull PartialRecognitionResult result) {
                emitted.add(result);
            }

            @Override
            public void onResult(@NonNull FinalRecognitionResult result) {
            }
        };

        de.switchconsulting.androidwhisperer.TranscriptionListener whisperListener =
                transcriber.createWhisperListener(listener, partialResults);

        whisperListener.onNewSegment(
                new SegmentResult("Hello world", 0, 1000, null),
                "Hello world"
        );

        assertEquals(1, emitted.size());
        assertEquals("Hello world", emitted.get(0).text());
        assertEquals(0, emitted.get(0).startMs());
        assertEquals(1000, emitted.get(0).endMs());

        whisperListener.onNewSegment(
                new SegmentResult("How are you?", 1500, 2500, null),
                "Hello world How are you?"
        );

        assertEquals(2, emitted.size());
        assertEquals("Hello world\n\nHow are you?", emitted.get(1).text());
        assertEquals(0, emitted.get(1).startMs());
        assertEquals(2500, emitted.get(1).endMs());

        assertEquals(2, partialResults.size());
        assertEquals("Hello world", partialResults.get(0).text());
        assertEquals("How are you?", partialResults.get(1).text());

        // Test coverage for empty whitespace segment
        whisperListener.onNewSegment(
                new SegmentResult("   ", 2600, 3000, null),
                "   "
        );
        assertEquals(2, partialResults.size());

        // Test progress and error callbacks
        whisperListener.onProgress(50);
        whisperListener.onError(new TranscriptionException("Test exception"));
    }
}
