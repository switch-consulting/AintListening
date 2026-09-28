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
import static org.mockito.Mockito.mock;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

/**
 * Unit tests for {@link VoskTranscriber}.
 */
public class VoskTranscriberTest {

    private VoskTranscriber transcriber;

    @Before
    public void setUp() {
        transcriber = new VoskTranscriber();
    }

    @Test
    public void testGetTypeAndProvidesPunctuation() {
        assertEquals(TranscriberType.VOSK, transcriber.getType());
        assertFalse(transcriber.providesPunctuation());
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
        assertEquals(TranscriberType.VOSK, transcriber.getType());
    }
}
