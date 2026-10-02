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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import android.content.Context;

import org.junit.Test;

import de.switchconsulting.aintlistening.data.AudioStorageManager;
import de.switchconsulting.aintlistening.data.PreferencesDataSource;
import de.switchconsulting.aintlistening.data.TranscriptionLocalDataSource;
import de.switchconsulting.aintlistening.data.db.TranscriptionDao;

/**
 * Unit tests for {@link AppModule}.
 */
public class AppModuleTest {

    @Test
    public void testProvidePreferencesDataSource() {
        Context context = mock(Context.class);
        PreferencesDataSource source = AppModule.providePreferencesDataSource(context);
        assertNotNull(source);
    }

    @Test
    public void testProvideAudioStorageManager() {
        Context context = mock(Context.class);
        AudioStorageManager manager = AppModule.provideAudioStorageManager(context);
        assertNotNull(manager);
    }

    @Test
    public void testProvideTranscriptionLocalDataSource() {
        TranscriptionDao dao = mock(TranscriptionDao.class);
        TranscriptionLocalDataSource source = AppModule.provideTranscriptionLocalDataSource(dao);
        assertNotNull(source);
    }
}
