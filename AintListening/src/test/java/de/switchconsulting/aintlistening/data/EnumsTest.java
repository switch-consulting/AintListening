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

import org.junit.Test;

import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for enums.
 */
public class EnumsTest {

    @Test
    public void testDownloadStatusValuesAndValueOf() {
        DownloadStatus[] values = DownloadStatus.values();
        assertEquals(5, values.length);
        assertEquals(DownloadStatus.IDLE, DownloadStatus.valueOf("IDLE"));
        assertEquals(DownloadStatus.DOWNLOADING, DownloadStatus.valueOf("DOWNLOADING"));
        assertEquals(DownloadStatus.EXTRACTING, DownloadStatus.valueOf("EXTRACTING"));
        assertEquals(DownloadStatus.SUCCESS, DownloadStatus.valueOf("SUCCESS"));
        assertEquals(DownloadStatus.ERROR, DownloadStatus.valueOf("ERROR"));
    }

    @Test
    public void testTranscriberTypeValuesAndValueOf() {
        TranscriberType[] values = TranscriberType.values();
        assertEquals(2, values.length);
        assertEquals(TranscriberType.VOSK, TranscriberType.valueOf("VOSK"));
        assertEquals(TranscriberType.WHISPER, TranscriberType.valueOf("WHISPER"));
    }
}
