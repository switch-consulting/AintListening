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

import android.content.Context;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.switchconsulting.aintlistening.data.Persistency;

/**
 * A transcriber implementation that delegates to either Vosk or Whisper
 * depending on the user's preference stored in Persistency.
 */
@Singleton
public class DelegatingTranscriber implements Transcriber {

    private final Persistency persistency;
    private final VoskTranscriber voskTranscriber;
    private final WhisperTranscriber whisperTranscriber;

    @Inject
    public DelegatingTranscriber(Persistency persistency) {
        this.persistency = persistency;
        this.voskTranscriber = new VoskTranscriber();
        this.whisperTranscriber = new WhisperTranscriber();
    }

    private Transcriber getActiveTranscriber() {
        if (persistency.getTranscriberType() == TranscriberType.WHISPER) {
            return whisperTranscriber;
        } else {
            return voskTranscriber;
        }
    }

    @Override
    public void ensureModelLoaded(Context context, int modelIndex) throws Exception {
        getActiveTranscriber().ensureModelLoaded(context, modelIndex);
    }

    @Override
    public List<TranscriptionParagraph> transcribe(Context context, File wavFile, TranscriptionListener listener) throws Exception {
        return getActiveTranscriber().transcribe(context, wavFile, listener);
    }

    @Override
    public void close() {
        voskTranscriber.close();
        whisperTranscriber.close();
    }
}
