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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

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
    private ModelManager modelManager;
    private LanguageSupport languageSupport;
    private Context context;

    @Before
    public void setUp() {
        context = mock(Context.class);
        modelManager = mock(ModelManager.class);

        voskModel = new ModelInfo("vosk-de", "http://example.com/vosk", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);
        whisperModel = new ModelInfo("whisper-de", "http://example.com/whisper", Locale.GERMAN, "75MB", TranscriberType.WHISPER, false);
        formattingModel = new ModelInfo("formatting-de", "http://example.com/fmt", Locale.GERMAN, "280MB", TranscriberType.VOSK, true);

        Map<TranscriberType, ModelInfo> map = new EnumMap<>(TranscriberType.class);
        map.put(TranscriberType.VOSK, voskModel);
        map.put(TranscriberType.WHISPER, whisperModel);

        languageSupport = new LanguageSupport(Locale.GERMAN, map, formattingModel, modelManager);
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
    public void testIsDownloadedDelegatesToModelManager() {
        when(modelManager.isModelDownloaded(any(), eq(voskModel))).thenReturn(true);
        when(modelManager.isModelDownloaded(any(), eq(whisperModel))).thenReturn(false);

        assertTrue(languageSupport.isDownloaded(context, TranscriberType.VOSK));
        assertFalse(languageSupport.isDownloaded(context, TranscriberType.WHISPER));
    }

    @Test
    public void testIsFormattingDownloaded() {
        when(modelManager.isModelDownloaded(any(), eq(formattingModel))).thenReturn(true);
        assertTrue(languageSupport.isFormattingDownloaded(context));
    }

    @Test
    public void testHasTranscriptionModelDownloaded() {
        when(modelManager.isModelDownloaded(any(), eq(voskModel))).thenReturn(false);
        when(modelManager.isModelDownloaded(any(), eq(whisperModel))).thenReturn(true);

        assertTrue(languageSupport.hasTranscriptionModelDownloaded(context));
    }

    @Test
    public void testGetActiveTranscriberTypeUsesPreferredWhenDownloaded() {
        Persistency persistency = mock(Persistency.class);
        when(persistency.getTranscriberType(Locale.GERMAN)).thenReturn(TranscriberType.WHISPER);

        when(modelManager.isModelDownloaded(any(), eq(whisperModel))).thenReturn(true);

        TranscriberType active = languageSupport.getActiveTranscriberType(context, persistency);
        assertEquals(TranscriberType.WHISPER, active);
    }

    @Test
    public void testGetActiveTranscriberTypeFallsBackToFirstDownloadedWhenPreferredNotDownloaded() {
        Persistency persistency = mock(Persistency.class);
        when(persistency.getTranscriberType(Locale.GERMAN)).thenReturn(TranscriberType.WHISPER);

        when(modelManager.isModelDownloaded(any(), eq(whisperModel))).thenReturn(false);
        when(modelManager.isModelDownloaded(any(), eq(voskModel))).thenReturn(true);

        TranscriberType active = languageSupport.getActiveTranscriberType(context, persistency);
        assertEquals(TranscriberType.VOSK, active);
    }

    @Test
    public void testEqualsAndHashCode() {
        Map<TranscriberType, ModelInfo> map = new EnumMap<>(TranscriberType.class);

        LanguageSupport lang1 = new LanguageSupport(Locale.GERMAN, map, null, modelManager);
        LanguageSupport lang2 = new LanguageSupport(Locale.GERMAN, map, null, modelManager);
        LanguageSupport lang3 = new LanguageSupport(Locale.ENGLISH, map, null, modelManager);

        assertEquals(lang1, lang2);
        assertEquals(lang1.hashCode(), lang2.hashCode());

        assertNotEquals(lang1, lang3);
    }
}
