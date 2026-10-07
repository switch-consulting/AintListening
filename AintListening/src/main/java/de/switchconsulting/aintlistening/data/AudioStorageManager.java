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

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.util.WavUtils;

/**
 * Handles temporary audio file creation, chunk storage, and file cache cleanup with thread synchronization.
 */
@Singleton
public class AudioStorageManager {

    private static final String TAG = "AudioStorageManager";
    private final Context context;
    private final Object fileLock = new Object();

    /**
     * Constructs a new AudioStorageManager instance.
     *
     * @param context The application context.
     */
    @Inject
    public AudioStorageManager(@ApplicationContext Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns the file object for the main temporary incoming audio WAV file.
     *
     * @return The file object for the main temporary incoming audio WAV file.
     */
    public File getIncomingWavFile() {
        return new File(context.getCacheDir(), "incoming_audio_16k_mono.wav");
    }

    /**
     * Returns the directory for storing audio chunks.
     *
     * @return The directory for storing audio chunks.
     */
    public File getAudioChunksDir() {
        File chunksDir = new File(context.getFilesDir(), "audio_chunks");
        if (!chunksDir.exists()) {
            if (!chunksDir.mkdirs()) {
                Log.w(TAG, "Failed to create chunks directory: " + chunksDir.getAbsolutePath());
            }
        }
        return chunksDir;
    }

    /**
     * Saves raw PCM data as a WAV chunk file in a synchronized manner.
     *
     * @param pcmData The raw PCM data.
     * @param index   The chunk index.
     * @return The absolute path to the saved chunk file.
     * @throws IOException If saving fails.
     */
    public String saveAudioChunk(byte[] pcmData, int index) throws IOException {
        synchronized (fileLock) {
            File chunksDir = getAudioChunksDir();
            File chunkFile = new File(chunksDir, "chunk_" + index + ".wav");
            WavUtils.savePcmAsWav(pcmData, chunkFile);
            return chunkFile.getAbsolutePath();
        }
    }

    /**
     * Deletes all temporary incoming audio files and chunk directories in a synchronized manner.
     */
    public void clearTemporaryFiles() {
        synchronized (fileLock) {
            // Delete incoming wav
            File incomingWav = getIncomingWavFile();
            if (incomingWav.exists()) {
                if (incomingWav.delete()) {
                    Log.d(TAG, "Deleted incoming WAV: " + incomingWav.getAbsolutePath());
                }
            }

            // Delete chunks
            File chunksDir = new File(context.getFilesDir(), "audio_chunks");
            if (chunksDir.exists() && chunksDir.isDirectory()) {
                File[] files = chunksDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.delete()) {
                            Log.d(TAG, "Deleted chunk: " + f.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }
}
