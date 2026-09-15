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

package de.switchconsulting.aintlistening.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import de.switchconsulting.aintlistening.data.Persistency;
import de.switchconsulting.aintlistening.transcription.Transcriber;
import de.switchconsulting.aintlistening.transcription.VoskTranscriber;
import javax.inject.Singleton;

/**
 * Hilt module for providing dependencies that have a singleton scope.
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    /**
     * Provides the singleton instance of Persistency.
     *
     * @param context The application context.
     * @return The Persistency instance.
     */
    @Provides
    @Singleton
    public static Persistency providePersistency(@ApplicationContext Context context) {
        return new Persistency(context);
    }

    /**
     * Provides the singleton instance of the Transcriber engine.
     *
     * @return The Transcriber instance.
     */
    @Provides
    @Singleton
    public static Transcriber provideTranscriber() {
        return new VoskTranscriber();
    }
}
