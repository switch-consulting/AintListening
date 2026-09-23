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
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Unit tests for {@link WavUtils}.
 */
public class WavUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testSavePcmAsWavHeaderAndContent() throws IOException {
        byte[] pcmData = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        File wavFile = temporaryFolder.newFile("test_audio.wav");

        WavUtils.savePcmAsWav(pcmData, wavFile);

        assertTrue(wavFile.exists());
        assertEquals(44 + pcmData.length, wavFile.length());

        byte[] fileBytes = new byte[(int) wavFile.length()];
        try (FileInputStream fis = new FileInputStream(wavFile)) {
            int read = fis.read(fileBytes);
            assertEquals(fileBytes.length, read);
        }

        // Validate RIFF header
        assertEquals('R', (char) fileBytes[0]);
        assertEquals('I', (char) fileBytes[1]);
        assertEquals('F', (char) fileBytes[2]);
        assertEquals('F', (char) fileBytes[3]);

        // Validate total size = PCM length + 36
        ByteBuffer buffer = ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN);
        int totalDataLen = buffer.getInt(4);
        assertEquals(pcmData.length + 36, totalDataLen);

        // Validate WAVE and fmt
        assertEquals('W', (char) fileBytes[8]);
        assertEquals('A', (char) fileBytes[9]);
        assertEquals('V', (char) fileBytes[10]);
        assertEquals('E', (char) fileBytes[11]);

        assertEquals('f', (char) fileBytes[12]);
        assertEquals('m', (char) fileBytes[13]);
        assertEquals('t', (char) fileBytes[14]);
        assertEquals(' ', (char) fileBytes[15]);

        int fmtChunkSize = buffer.getInt(16);
        assertEquals(16, fmtChunkSize);

        short audioFormat = buffer.getShort(20);
        assertEquals(1, audioFormat); // PCM

        short numChannels = buffer.getShort(22);
        assertEquals(1, numChannels); // Mono

        int sampleRate = buffer.getInt(24);
        assertEquals(16000, sampleRate);

        int byteRate = buffer.getInt(28);
        assertEquals(32000, byteRate); // 16000 * 1 * 2

        short blockAlign = buffer.getShort(32);
        assertEquals(2, blockAlign);

        short bitsPerSample = buffer.getShort(34);
        assertEquals(16, bitsPerSample);

        // Validate data chunk
        assertEquals('d', (char) fileBytes[36]);
        assertEquals('a', (char) fileBytes[37]);
        assertEquals('t', (char) fileBytes[38]);
        assertEquals('a', (char) fileBytes[39]);

        int pcmDataLen = buffer.getInt(40);
        assertEquals(pcmData.length, pcmDataLen);

        // Validate PCM payload
        byte[] actualPcm = new byte[pcmData.length];
        System.arraycopy(fileBytes, 44, actualPcm, 0, pcmData.length);
        assertArrayEquals(pcmData, actualPcm);
    }
}
