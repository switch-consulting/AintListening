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

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility class to slice PCM data or audio chunks from a source WAV file based on millisecond timestamps.
 */
public class AudioSlicer {

    private static final int BYTES_PER_MS = 32; // 16kHz, 16-bit, mono = 16000 * 2 bytes/sec = 32 bytes/ms
    private static final int WAV_HEADER_SIZE = 44;

    /**
     * Extracts raw PCM bytes corresponding to the given time range [startMs, endMs] from a WAV file.
     *
     * @param wavFile The source WAV file.
     * @param startMs The start time in milliseconds.
     * @param endMs   The end time in milliseconds.
     * @return The sliced raw PCM data.
     * @throws IOException If an I/O error occurs.
     */
    @NonNull
    public static byte[] extractPcmSlice(@NonNull File wavFile, long startMs, long endMs) throws IOException {
        if (startMs >= endMs || !wavFile.exists()) {
            return new byte[0];
        }

        long fileLength = wavFile.length();
        long maxPcmLength = Math.max(0, fileLength - WAV_HEADER_SIZE);

        long startByteOffset = WAV_HEADER_SIZE + (startMs * BYTES_PER_MS);
        long endByteOffset = WAV_HEADER_SIZE + (endMs * BYTES_PER_MS);

        // Clamp offsets within valid audio bounds
        startByteOffset = Math.min(Math.max(WAV_HEADER_SIZE, startByteOffset), WAV_HEADER_SIZE + maxPcmLength);
        endByteOffset = Math.min(Math.max(startByteOffset, endByteOffset), WAV_HEADER_SIZE + maxPcmLength);

        int sliceLength = (int) (endByteOffset - startByteOffset);
        if (sliceLength <= 0) {
            return new byte[0];
        }

        byte[] pcmSlice = new byte[sliceLength];
        try (FileInputStream fis = new FileInputStream(wavFile)) {
            long bytesToSkip = startByteOffset;
            while (bytesToSkip > 0) {
                long skipped = fis.skip(bytesToSkip);
                if (skipped <= 0) {
                    break;
                }
                bytesToSkip -= skipped;
            }
            int totalRead = 0;
            while (totalRead < sliceLength) {
                int read = fis.read(pcmSlice, totalRead, sliceLength - totalRead);
                if (read < 0) {
                    break;
                }
                totalRead += read;
            }
            if (totalRead < sliceLength) {
                byte[] truncated = new byte[totalRead];
                System.arraycopy(pcmSlice, 0, truncated, 0, totalRead);
                return truncated;
            }
        }
        return pcmSlice;
    }
}
