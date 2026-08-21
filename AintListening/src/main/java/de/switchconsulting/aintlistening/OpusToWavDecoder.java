package de.switchconsulting.aintlistening;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

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

    private static final long TIMEOUT_US = 5000;

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

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat mimeFormat = extractor.getTrackFormat(i);
                String mime = mimeFormat.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    trackIndex = i;
                    format = mimeFormat;
                    break;
                }
            }

            if (trackIndex < 0 || format == null) return false;

            extractor.selectTrack(trackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, null, null, 0);
            decoder.start();

            fos = new FileOutputStream(outputFile);
            writeWavHeader(fos, 0, 16000, 1);

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

            updateWavHeader(outputFile, totalPcmBytes, 16000, 1);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
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

        int step = sampleRate / 16000;
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
     * @param out        The output stream.
     * @param pcmLen     The length of the PCM data (can be 0 initially).
     * @param sampleRate The sample rate.
     * @param channels   The number of channels.
     * @throws Exception If an error occurs during writing.
     */
    private static void writeWavHeader(FileOutputStream out, int pcmLen, int sampleRate, int channels) throws Exception {
        out.write(createWavHeader(pcmLen, sampleRate, channels), 0, 44);
    }

    /**
     * Updates the WAV header in the file with the correct PCM data length.
     *
     * @param wavFile    The WAV file to update.
     * @param pcmLen     The actual length of the PCM data written.
     * @param sampleRate The sample rate.
     * @param channels   The number of channels.
     * @throws Exception If an error occurs during updating.
     */
    private static void updateWavHeader(File wavFile, int pcmLen, int sampleRate, int channels) throws Exception {
        byte[] header = createWavHeader(pcmLen, sampleRate, channels);
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "rw")) {
            raf.seek(0);
            raf.write(header);
        }
    }

    /**
     * Creates a 44-byte WAV (RIFF) header.
     *
     * @param pcmLen     The length of the PCM data.
     * @param sampleRate The sample rate.
     * @param channels   The number of channels.
     * @return The header byte array.
     */
    private static byte[] createWavHeader(int pcmLen, int sampleRate, int channels) {
        long totalDataLen = pcmLen + 36;
        long byteRate = (long) sampleRate * channels * 2;
        byte[] header = new byte[44];

        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); header[33] = 0;
        header[34] = 16; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (pcmLen & 0xff);
        header[41] = (byte) ((pcmLen >> 8) & 0xff);
        header[42] = (byte) ((pcmLen >> 16) & 0xff);
        header[43] = (byte) ((pcmLen >> 24) & 0xff);

        return header;
    }
}
