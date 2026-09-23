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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link ModelInfo}.
 */
public class ModelInfoTest {

    @Test
    public void testFullConstructor() {
        ModelInfo info = new ModelInfo(
                "ggml-tiny.bin",
                "https://example.com/whisper.bin",
                Locale.GERMAN,
                "75MB",
                TranscriberType.WHISPER,
                false
        );

        assertEquals("ggml-tiny.bin", info.name);
        assertEquals("https://example.com/whisper.bin", info.url);
        assertEquals(Locale.GERMAN, info.locale);
        assertEquals("75MB", info.size);
        assertEquals(TranscriberType.WHISPER, info.type);
        assertFalse(info.isZip);
    }

    @Test
    public void testConvenienceConstructorDefaultsToVoskAndZip() {
        ModelInfo info = new ModelInfo(
                "vosk-model-de",
                "https://example.com/vosk.zip",
                Locale.GERMAN,
                "45MB"
        );

        assertEquals("vosk-model-de", info.name);
        assertEquals("https://example.com/vosk.zip", info.url);
        assertEquals(Locale.GERMAN, info.locale);
        assertEquals("45MB", info.size);
        assertEquals(TranscriberType.VOSK, info.type);
        assertTrue(info.isZip);
    }
}
