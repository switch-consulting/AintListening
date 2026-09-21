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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelManager;
import de.switchconsulting.aintlistening.data.Persistency;

/**
 * A transcriber implementation that delegates to either Vosk or Whisper
 * depending on the user's preference stored in Persistency.
 */
@Singleton
public class DelegatingTranscriber implements Transcriber {

    private final Persistency persistency;
    private final Map<TranscriberType, Transcriber> transcribers = new EnumMap<>(TranscriberType.class);
    private TranscriberType activeType = null;

    @Inject
    public DelegatingTranscriber(Persistency persistency) {
        this.persistency = persistency;
        this.transcribers.put(TranscriberType.VOSK, new VoskTranscriber());
        this.transcribers.put(TranscriberType.WHISPER, new WhisperTranscriber());
    }

    private Transcriber getActiveTranscriber() {
        TranscriberType type = activeType != null ? activeType : persistency.getDefaultTranscriberType();
        Transcriber transcriber = transcribers.get(type);
        if (transcriber == null) {
            // Fallback to VOSK if something goes wrong
            return transcribers.get(TranscriberType.VOSK);
        }
        return transcriber;
    }

    @Override
    public void ensureModelLoaded(Context context, int modelIndex) throws Exception {
        if (modelIndex >= 0 && modelIndex < ModelManager.SUPPORTED_LANGUAGES.length) {
            LanguageSupport language = ModelManager.SUPPORTED_LANGUAGES[modelIndex];
            activeType = language.getActiveTranscriberType(context, persistency);
        }
        getActiveTranscriber().ensureModelLoaded(context, modelIndex);
    }

    @Override
    public List<TranscriptionParagraph> transcribe(Context context, File wavFile, TranscriptionListener listener) throws Exception {
        return getActiveTranscriber().transcribe(context, wavFile, listener);
    }

    @Override
    public TranscriberType getType() {
        return getActiveTranscriber().getType();
    }

    @Override
    public int getNameResId() {
        return getActiveTranscriber().getNameResId();
    }

    @Override
    public boolean providesPunctuation() {
        return getActiveTranscriber().providesPunctuation();
    }

    @Override
    public void close() {
        for (Transcriber transcriber : transcribers.values()) {
            transcriber.close();
        }
    }
}
