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
    public void testSetEnableIncrementalUpdates() {
        transcriber.setEnableIncrementalUpdates(false);
        transcriber.setEnableIncrementalUpdates(true);
    }

    @Test
    public void testConfigurablePauseThresholds() {
        assertEquals(WhisperTranscriber.DEFAULT_SILENCE_GAP_MS, transcriber.getSilenceGapMs());
        assertEquals(WhisperTranscriber.DEFAULT_LONG_SILENCE_GAP_MS, transcriber.getLongSilenceGapMs());

        transcriber.setSilenceGapMs(250);
        assertEquals(250, transcriber.getSilenceGapMs());

        transcriber.setLongSilenceGapMs(500);
        assertEquals(500, transcriber.getLongSilenceGapMs());
    }

    @Test
    public void testPauseDetectionSplitsParagraphOnLongSilenceWithoutPunctuation() {
        File dummyWav = new File("dummy.wav");
        WhisperTranscriber.TranscriptionContext ctx = new WhisperTranscriber.TranscriptionContext(dummyWav, null);

        List<String> logLines = Arrays.asList(
                "[00:00:00.000 --> 00:00:02.000] Hallo ich spreche hier einen Text",
                "[00:00:02.800 --> 00:00:05.000] und dann spreche ich nach einer Pause weiter"
        );

        transcriber.processTranscriptionLines(logLines, ctx, true);

        assertEquals(2, ctx.paragraphs.size());
        assertEquals("Hallo ich spreche hier einen Text", ctx.paragraphs.get(0).getRawText());
        assertEquals("und dann spreche ich nach einer Pause weiter", ctx.paragraphs.get(1).getRawText());
    }

    @Test
    public void testPauseDetectionSplitsParagraphOnModerateSilenceWithPunctuation() {
        File dummyWav = new File("dummy.wav");
        WhisperTranscriber.TranscriptionContext ctx = new WhisperTranscriber.TranscriptionContext(dummyWav, null);

        // Gap is 400ms (02.400 - 02.000 = 400ms >= 300ms default)
        List<String> logLines = Arrays.asList(
                "[00:00:00.000 --> 00:00:02.000] Das ist ein erster Satz.",
                "[00:00:02.400 --> 00:00:05.000] Und hier folgt der zweite Satz."
        );

        transcriber.processTranscriptionLines(logLines, ctx, true);

        assertEquals(2, ctx.paragraphs.size());
        assertEquals("Das ist ein erster Satz.", ctx.paragraphs.get(0).getRawText());
        assertEquals("Und hier folgt der zweite Satz.", ctx.paragraphs.get(1).getRawText());
    }

    @Test
    public void testPauseDetectionDoesNotSplitOnBriefPauseMidSentence() {
        File dummyWav = new File("dummy.wav");
        WhisperTranscriber.TranscriptionContext ctx = new WhisperTranscriber.TranscriptionContext(dummyWav, null);

        // Gap is 150ms (< 300ms default) without punctuation
        List<String> logLines = Arrays.asList(
                "[00:00:00.000 --> 00:00:02.000] Ich spreche hier flüssig",
                "[00:00:02.150 --> 00:00:04.000] ohne lange Pause"
        );

        transcriber.processTranscriptionLines(logLines, ctx, true);

        assertEquals(1, ctx.paragraphs.size());
        assertEquals("Ich spreche hier flüssig ohne lange Pause", ctx.paragraphs.get(0).getRawText());
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

        // Actual English speech segment timestamps and text matching WhatsApp-ExampleMessage.ogg / .wav
        List<String> whatsappLogs = Arrays.asList(
                "[00:00:00.000 --> 00:00:02.800] Hey, quick thing.",
                "[00:00:02.800 --> 00:00:04.760] Don't get the green apples, get the red ones.",
                "[00:00:07.000 --> 00:00:11.260] Wait, actually check if they have honeycrisp.",
                "[00:00:11.260 --> 00:00:14.700] If they don't have honeycrisp, just forget the apples entirely",
                "[00:00:15.000 --> 00:00:17.960] and get strawberries.",
                "[00:00:18.660 --> 00:00:20.040] Oh, and milk.",
                "[00:00:20.040 --> 00:00:22.240] Oat milk, not almond.",
                "[00:00:22.240 --> 00:00:23.920] Bye."
        );

        transcriber.processTranscriptionLines(whatsappLogs, ctx, true);

        // Verify that 3 distinct paragraphs are identified strictly from the silence gaps (>600ms) in the audio segments
        assertEquals(3, ctx.paragraphs.size());

        assertEquals("Hey, quick thing. Don't get the green apples, get the red ones.", ctx.paragraphs.get(0).getRawText());
        assertEquals("chunk_0.wav", ctx.paragraphs.get(0).getAudioFilePath());

        assertEquals("Wait, actually check if they have honeycrisp. If they don't have honeycrisp, just forget the apples entirely and get strawberries.", ctx.paragraphs.get(1).getRawText());
        assertEquals("chunk_1.wav", ctx.paragraphs.get(1).getAudioFilePath());

        assertEquals("Oh, and milk. Oat milk, not almond. Bye.", ctx.paragraphs.get(2).getRawText());
        assertEquals("chunk_2.wav", ctx.paragraphs.get(2).getAudioFilePath());
    }

    @Test
    public void testShortMMSSTimestampFormatParsing() {
        File dummyWav = new File("dummy.wav");
        WhisperTranscriber.TranscriptionContext ctx = new WhisperTranscriber.TranscriptionContext(dummyWav, null);

        // Whisper.cpp output format for short audio (< 1 hour): [MM:SS.mmm --> MM:SS.mmm]
        List<String> shortLogLines = Arrays.asList(
                "[00:00.850 --> 00:02.000] Hey, quick thing.",
                "[00:02.800 --> 00:04.760] Don't get the green apples, get the red ones.",
                "[00:07.000 --> 00:11.260] Wait, actually check if they have honeycrisp."
        );

        transcriber.processTranscriptionLines(shortLogLines, ctx, true);

        // Segment 1 ends with '.' and has 800ms gap -> splits. Segment 2 has 2.24s gap -> splits. Total 3 paragraphs!
        assertEquals(3, ctx.paragraphs.size());
        assertEquals("Hey, quick thing.", ctx.paragraphs.get(0).getRawText());
        assertEquals("Don't get the green apples, get the red ones.", ctx.paragraphs.get(1).getRawText());
        assertEquals("Wait, actually check if they have honeycrisp.", ctx.paragraphs.get(2).getRawText());
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
