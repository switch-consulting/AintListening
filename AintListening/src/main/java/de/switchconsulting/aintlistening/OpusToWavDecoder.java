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

package de.switchconsulting.aintlistening;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class for decoding audio files (primarily Opus) to a WAV format
 * compatible with the Vosk speech recognition engine (16kHz, Mono, PCM 16-bit).
 */
public class OpusToWavDecoder {

    private static final String TAG = "OpusToWavDecoder";
    private static final long TIMEOUT_US = 5000;
    private static final int TARGET_SAMPLE_RATE = 16000;
    private static final int TARGET_CHANNELS = 1;

    /**
     * Decodes an audio file from a URI and saves it as a 16kHz mono WAV file.
     *
     * @param context    The context.
     * @param inputUri   The URI of the input audio file.
     * @param outputFile The file where the decoded WAV should be saved.
     * @return True if the decoding and conversion were successful, false otherwise.
     */
    public static boolean decodeOpusToWav(Context context, Uri inputUri, File outputFile) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        FileOutputStream fos = null;

        try {
            extractor.setDataSource(context, inputUri, null);
            int trackIndex = -1;
            MediaFormat format = null;
            String mime = null;

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat mimeFormat = extractor.getTrackFormat(i);
                String trackMime = mimeFormat.getString(MediaFormat.KEY_MIME);
                if (trackMime != null && trackMime.startsWith("audio/")) {
                    trackIndex = i;
                    format = mimeFormat;
                    mime = trackMime;
                    break;
                }
            }

            if (trackIndex < 0) return false;

            extractor.selectTrack(trackIndex);

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, null, null, 0);
            decoder.start();

            fos = new FileOutputStream(outputFile);
            writeWavHeader(fos);

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isInputEOS = false;
            boolean isOutputEOS = false;
            int totalPcmBytes = 0;

            int nativeSampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 48000;
            int channelCount = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;

            while (!isOutputEOS) {
                if (!isInputEOS) {
                    int inIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = decoder.getInputBuffer(inIndex);
                        if (buffer == null) continue;
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isInputEOS = true;
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, TIMEOUT_US);
                if (outIndex >= 0) {
                    ByteBuffer outBuffer = decoder.getOutputBuffer(outIndex);
                    if (outBuffer != null && info.size > 0) {
                        outBuffer.position(info.offset);
                        outBuffer.limit(info.offset + info.size);

                        byte[] pcmData = new byte[info.size];
                        outBuffer.get(pcmData);

                        byte[] processedPcm = processPcmTo16kMono(pcmData, nativeSampleRate, channelCount);
                        fos.write(processedPcm);
                        totalPcmBytes += processedPcm.length;
                    }

                    decoder.releaseOutputBuffer(outIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true;
                    }
                }
            }

            updateWavHeader(outputFile, totalPcmBytes);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error decoding opus to wav", e);
            return false;
        } finally {
            try {
                if (fos != null) fos.close();
                if (decoder != null) {
                    decoder.stop();
                    decoder.release();
                }
                extractor.release();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Processes PCM data to convert it to 16kHz mono.
     *
     * @param inputPcm   The input PCM bytes.
     * @param sampleRate The native sample rate of the input.
     * @param channels   The number of channels in the input.
     * @return The processed PCM bytes (16kHz, mono).
     */
    private static byte[] processPcmTo16kMono(byte[] inputPcm, int sampleRate, int channels) {
        short[] shorts = new short[inputPcm.length / 2];
        ByteBuffer.wrap(inputPcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

        int step = sampleRate / TARGET_SAMPLE_RATE;
        if (step < 1) step = 1;

        int outputSize = shorts.length / (channels * step);
        short[] outputShorts = new short[outputSize];

        int outIndex = 0;
        for (int i = 0; i < shorts.length && outIndex < outputSize; i += channels * step) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += shorts[i + c];
            }
            outputShorts[outIndex++] = (short) (sum / channels);
        }

        byte[] outputBytes = new byte[outputShorts.length * 2];
        ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outputShorts);
        return outputBytes;
    }

    /**
     * Writes a placeholder WAV header to the output stream.
     *
     * @param out The output stream.
     * @throws Exception If an error occurs during writing.
     */
    private static void writeWavHeader(FileOutputStream out) throws Exception {
        out.write(createWavHeader(0), 0, 44);
    }

    /**
     * Updates the WAV header in the file with the correct PCM data length.
     *
     * @param wavFile    The WAV file to update.
     * @param pcmLen     The actual length of the PCM data written.
     * @throws Exception If an error occurs during updating.
     */
    private static void updateWavHeader(File wavFile, int pcmLen) throws Exception {
        byte[] header = createWavHeader(pcmLen);
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            raf.seek(0);
            raf.write(header);
        }
    }

    /**
     * Creates a 44-byte WAV (RIFF) header.
     *
     * @param pcmLen The length of the PCM data.
     * @return The header byte array.
     */
    private static byte[] createWavHeader(int pcmLen) {
        ByteBuffer buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);

        // RIFF header
        buffer.put(new byte[]{'R', 'I', 'F', 'F'});
        buffer.putInt(pcmLen + 36);
        buffer.put(new byte[]{'W', 'A', 'V', 'E'});

        // fmt subchunk
        buffer.put(new byte[]{'f', 'm', 't', ' '});
        buffer.putInt(16); // Subchunk1Size
        buffer.putShort((short) 1); // AudioFormat (PCM)
        buffer.putShort((short) TARGET_CHANNELS);
        buffer.putInt(TARGET_SAMPLE_RATE);
        buffer.putInt(TARGET_SAMPLE_RATE * TARGET_CHANNELS * 2); // ByteRate
        buffer.putShort((short) (TARGET_CHANNELS * 2)); // BlockAlign
        buffer.putShort((short) 16); // BitsPerSample

        // data subchunk
        buffer.put(new byte[]{'d', 'a', 't', 'a'});
        buffer.putInt(pcmLen);

        return buffer.array();
    }
}
