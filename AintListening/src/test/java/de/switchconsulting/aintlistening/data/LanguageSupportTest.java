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
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link LanguageSupport}.
 */
public class LanguageSupportTest {

    private ModelInfo voskModel;
    private ModelInfo whisperModel;
    private ModelInfo formattingModel;
    private LanguageSupport languageSupport;

    @Before
    public void setUp() {
        voskModel = new ModelInfo("vosk-de", "http://example.com/vosk", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);
        whisperModel = new ModelInfo("whisper-de", "http://example.com/whisper", Locale.GERMAN, "75MB", TranscriberType.WHISPER, false);
        formattingModel = new ModelInfo("formatting-de", "http://example.com/fmt", Locale.GERMAN, "280MB", TranscriberType.VOSK, true);

        Map<TranscriberType, ModelInfo> map = new EnumMap<>(TranscriberType.class);
        map.put(TranscriberType.VOSK, voskModel);
        map.put(TranscriberType.WHISPER, whisperModel);

        languageSupport = new LanguageSupport(Locale.GERMAN, map, formattingModel);
    }

    @Test
    public void testGetters() {
        assertEquals(Locale.GERMAN, languageSupport.getLocale());
        assertEquals(voskModel, languageSupport.getModel(TranscriberType.VOSK));
        assertEquals(whisperModel, languageSupport.getModel(TranscriberType.WHISPER));
        assertEquals(formattingModel, languageSupport.getFormattingModel());
        assertEquals(2, languageSupport.getTranscriptionModels().size());
    }

    @Test
    public void testEqualsAndHashCode() {
        Map<TranscriberType, ModelInfo> map = new EnumMap<>(TranscriberType.class);

        LanguageSupport lang1 = new LanguageSupport(Locale.GERMAN, map, null);
        LanguageSupport lang2 = new LanguageSupport(Locale.GERMAN, map, null);
        LanguageSupport lang3 = new LanguageSupport(Locale.ENGLISH, map, null);

        assertEquals(lang1, lang2);
        assertEquals(lang1.hashCode(), lang2.hashCode());

        assertNotEquals(lang1, lang3);
    }
}
