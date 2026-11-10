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

package de.switchconsulting.aintlistening.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for {@link AudioSlicer}.
 */
public class AudioSlicerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testExtractPcmSlice() throws IOException {
        // 100 ms of PCM data = 100 * 32 = 3200 bytes
        byte[] pcmData = new byte[3200];
        for (int i = 0; i < pcmData.length; i++) {
            pcmData[i] = (byte) (i % 256);
        }

        File wavFile = temporaryFolder.newFile("test_source.wav");
        WavUtils.savePcmAsWav(pcmData, wavFile);

        // Extract slice from 10ms to 30ms (duration 20ms = 640 bytes)
        byte[] slice = AudioSlicer.extractPcmSlice(wavFile, 10, 30);
        assertEquals(640, slice.length);

        // Expected bytes from index 10 * 32 = 320 to (30 * 32) = 960 in pcmData
        byte[] expected = new byte[640];
        System.arraycopy(pcmData, 320, expected, 0, 640);
        assertArrayEquals(expected, slice);
    }

    @Test
    public void testExtractPcmSliceInvalidRange() throws IOException {
        byte[] pcmData = new byte[3200];
        File wavFile = temporaryFolder.newFile("test_source.wav");
        WavUtils.savePcmAsWav(pcmData, wavFile);

        byte[] slice = AudioSlicer.extractPcmSlice(wavFile, 50, 20);
        assertEquals(0, slice.length);
    }
}
