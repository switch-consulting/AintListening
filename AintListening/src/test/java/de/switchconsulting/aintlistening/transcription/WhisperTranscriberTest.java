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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.switchconsulting.aintlistening.data.ModelCatalogRepository;

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
    }

    @Test
    public void testConfigurablePauseThresholds() {
        assertEquals(WhisperTranscriber.DEFAULT_SILENCE_GAP_MS, transcriber.getSilenceGapMs());

        transcriber.setSilenceGapMs(250);
        assertEquals(250, transcriber.getSilenceGapMs());
    }

    @Test
    public void testTokenTimestampPauseDetection() {
        File dummyWav = new File("dummy.wav");
        WhisperTranscriber.TranscriptionContext ctx = new WhisperTranscriber.TranscriptionContext(dummyWav, null);

        // Tokens with start and end timestamps matching the user's structure
        List<WhisperTranscriber.WhisperToken> tokens = Arrays.asList(
                new WhisperTranscriber.WhisperToken("<|startoftext|>", 0, 0),
                new WhisperTranscriber.WhisperToken("Hey,", 0, 1300),
                new WhisperTranscriber.WhisperToken("quick", 1300, 1560),
                new WhisperTranscriber.WhisperToken("thing.", 1560, 2800),
                // Pause duration = 3340 - 2800 = 540ms >= 500ms -> SPLIT!
                new WhisperTranscriber.WhisperToken("Don't", 3340, 3800),
                new WhisperTranscriber.WhisperToken("get", 3800, 4000),
                new WhisperTranscriber.WhisperToken("the", 4000, 4100),
                new WhisperTranscriber.WhisperToken("green", 4100, 4300),
                new WhisperTranscriber.WhisperToken("apples,", 4300, 4500),
                new WhisperTranscriber.WhisperToken("get", 4500, 4600),
                new WhisperTranscriber.WhisperToken("the", 4600, 4650),
                new WhisperTranscriber.WhisperToken("red", 4650, 4700),
                new WhisperTranscriber.WhisperToken("ones.", 4700, 4760),
                // Pause duration = 7000 - 4760 = 2240ms >= 500ms -> SPLIT!
                new WhisperTranscriber.WhisperToken("Wait,", 7000, 8120),
                new WhisperTranscriber.WhisperToken("<|endoftext|>", 8120, 8120)
        );

        transcriber.setSilenceGapMs(500);
        transcriber.processTokens(tokens, ctx, true);

        // Verify token-based pause detection identified 3 distinct paragraphs based strictly on token timestamps
        assertEquals(3, ctx.paragraphs.size());
        assertEquals("Hey, quick thing.", ctx.paragraphs.get(0).getRawText());
        assertEquals("Don't get the green apples, get the red ones.", ctx.paragraphs.get(1).getRawText());
        assertEquals("Wait,", ctx.paragraphs.get(2).getRawText());
    }

    @Test
    public void testParseNativeTokensWithAppResultFormat() {
        String appOutput = "Transcription done (1438 ms): [00:00:00.000 --> 00:00:09.600]:  Hey, quick thing. Don't get the green apples together at once. Wait, actually check if\n" +
                "                                [00:00:09.600 --> 00:00:14.960]:  they have honey crisp, if they don't have honey crisp, just forget the apples entirely\n" +
                "                                [00:00:14.960 --> 00:00:22.760]:  and get strawberries. Oh and milk. Out milk, not almond. Bye.";

        List<WhisperTranscriber.WhisperToken> tokens = transcriber.parseNativeTokens(appOutput);

        assertEquals(3, tokens.size());
        assertEquals("Hey, quick thing. Don't get the green apples together at once. Wait, actually check if", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getStartMs());
        assertEquals(9600, tokens.get(0).getEndMs());
    }

    @Test
    public void testDirectWhisperSegmentProcessing() {
        File dummyWav = new File("dummy.wav");
        WhisperTranscriber.TranscriptionContext ctx = new WhisperTranscriber.TranscriptionContext(dummyWav, null);

        List<WhisperTranscriber.WhisperSegment> segments = Arrays.asList(
                new WhisperTranscriber.WhisperSegment("First segment text.", 0, 2000),
                new WhisperTranscriber.WhisperSegment("Second segment text after pause.", 3000, 5000)
        );

        transcriber.processSegments(segments, ctx, true);

        assertEquals(2, ctx.paragraphs.size());
        assertEquals("First segment text.", ctx.paragraphs.get(0).getRawText());
        assertEquals("Second segment text after pause.", ctx.paragraphs.get(1).getRawText());
    }

    @Test
    public void testParagraphIdentificationWithWhatsAppExampleAudioFile() {
        URL resource = getClass().getClassLoader().getResource("whatsapp_example_message.wav");
        assertNotNull("Test resource whatsapp_example_message.wav should exist in test resources", resource);

        File wavFile = new File(java.util.Objects.requireNonNull(resource).getFile());
        assertTrue("WAV file should exist on disk", wavFile.exists());

        // TranscriptionContext configured with actual WhatsApp example audio file
        WhisperTranscriber.TranscriptionContext ctx = new WhisperTranscriber.TranscriptionContext(wavFile, new TranscriptionListener() {
            @Override
            public void onPartialResult(String text) {}

            @Override
            public void onResult(String text) {}

            @Override
            public String onAudioChunkAvailable(byte[] pcmData, int chunkIndex) {
                assertTrue("Audio chunk extracted from whatsapp_example_message.wav should not be empty", pcmData.length > 0);
                return "chunk_" + chunkIndex + ".wav";
            }
        });

        // Exact coarse segment output string delivered by WhisperCore on device
        String appOutput = "Transcription done (1438 ms): [00:00:00.000 --> 00:00:09.600]:  Hey, quick thing. Don't get the green apples together at once. Wait, actually check if\n" +
                "                                [00:00:09.600 --> 00:00:14.960]:  they have honey crisp, if they don't have honey crisp, just forget the apples entirely\n" +
                "                                [00:00:14.960 --> 00:00:22.760]:  and get strawberries. Oh and milk. Out milk, not almond. Bye.";

        List<WhisperTranscriber.WhisperToken> tokens = transcriber.parseNativeTokens(appOutput);
        transcriber.processTokens(tokens, ctx, true);

        // Since WhisperCore's coarse segments have 0ms pause between them (0-9.6s, 9.6-14.96s, 14.96-22.76s),
        // pure token timestamp pause math (gap >= 500ms) produces 1 paragraph.
        assertEquals(1, ctx.paragraphs.size());
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
}
