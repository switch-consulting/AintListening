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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class for working with WAV files.
 */
public class WavUtils {

    /**
     * Saves raw PCM data to a WAV file with a proper header.
     * Assumes 16kHz, 16-bit, Mono PCM.
     *
     * @param pcmData  The raw PCM data.
     * @param outFile The output WAV file.
     * @throws IOException If an I/O error occurs.
     */
    public static void savePcmAsWav(byte[] pcmData, File outFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(createWavHeader(pcmData.length));
            fos.write(pcmData);
        }
    }

    private static byte[] createWavHeader(int pcmDataLength) {
        int totalDataLen = pcmDataLength + 36;
        int sampleRate = 16000;
        int channels = 1;
        int byteRate = sampleRate * channels * 2; // 16-bit = 2 bytes

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1 (PCM)
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = 0;
        header[27] = 0;
        header[28] = 0;
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = 0;
        header[31] = 0;
        header[32] = (byte) (channels * 2); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (pcmDataLength & 0xff);
        header[41] = (byte) ((pcmDataLength >> 8) & 0xff);
        header[42] = (byte) ((pcmDataLength >> 16) & 0xff);
        header[43] = (byte) ((pcmDataLength >> 24) & 0xff);

        return header;
    }
}
