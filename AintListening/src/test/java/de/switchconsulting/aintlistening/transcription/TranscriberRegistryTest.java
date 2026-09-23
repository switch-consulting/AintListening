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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link TranscriberRegistry}.
 */
public class TranscriberRegistryTest {

    private VoskTranscriber voskTranscriber;
    private WhisperTranscriber whisperTranscriber;
    private TranscriberRegistry registry;

    @Before
    public void setUp() {
        voskTranscriber = mock(VoskTranscriber.class);
        whisperTranscriber = mock(WhisperTranscriber.class);
        registry = new TranscriberRegistry(voskTranscriber, whisperTranscriber);
    }

    @Test
    public void testGetTranscriberReturnsRegisteredInstances() {
        assertEquals(voskTranscriber, registry.getTranscriber(TranscriberType.VOSK));
        assertEquals(whisperTranscriber, registry.getTranscriber(TranscriberType.WHISPER));
    }

    @Test
    public void testCloseAllDelegatesToTranscribers() {
        registry.closeAll();

        verify(voskTranscriber).close();
        verify(whisperTranscriber).close();
    }
}
