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

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoSet;

/**
 * Hilt module for binding transcriber implementations into a set for {@link TranscriberRegistry}.
 */
@Module
@InstallIn(SingletonComponent.class)
@SuppressWarnings("unused")
public abstract class TranscriptionModule {

    /**
     * Binds {@link VoskTranscriber} into the set of available transcribers.
     *
     * @param impl The VoskTranscriber instance.
     * @return The Transcriber interface.
     */
    @Binds
    @IntoSet
    @Singleton
    public abstract Transcriber bindVoskTranscriber(VoskTranscriber impl);

    /**
     * Binds {@link WhisperTranscriber} into the set of available transcribers.
     *
     * @param impl The WhisperTranscriber instance.
     * @return The Transcriber interface.
     */
    @Binds
    @IntoSet
    @Singleton
    public abstract Transcriber bindWhisperTranscriber(WhisperTranscriber impl);
}
