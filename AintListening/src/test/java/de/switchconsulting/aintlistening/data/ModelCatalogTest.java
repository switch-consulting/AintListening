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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Locale;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link ModelCatalog}.
 */
public class ModelCatalogTest {

    @Test
    public void testGetEngineNameResId() {
        assertEquals(R.string.engine_vosk, ModelCatalog.getEngineNameResId(TranscriberType.VOSK));
        assertEquals(R.string.engine_whisper, ModelCatalog.getEngineNameResId(TranscriberType.WHISPER));
    }

    @Test
    public void testGetLanguageSupport() {
        LanguageSupport german = ModelCatalog.getLanguageSupport(Locale.GERMAN);
        assertNotNull(german);
        assertEquals(Locale.GERMAN, german.getLocale());

        LanguageSupport english = ModelCatalog.getLanguageSupport(Locale.ENGLISH);
        assertNotNull(english);
        assertEquals(Locale.ENGLISH, english.getLocale());

        LanguageSupport unsupported = ModelCatalog.getLanguageSupport(Locale.JAPANESE);
        assertNull(unsupported);

        assertNull(ModelCatalog.getLanguageSupport(null));
    }
}
