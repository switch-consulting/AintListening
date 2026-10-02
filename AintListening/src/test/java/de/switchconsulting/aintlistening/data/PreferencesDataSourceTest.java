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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.util.Locale;

import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link PreferencesDataSource}.
 */
public class PreferencesDataSourceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PreferencesDataSource preferencesDataSource;
    private RxDataStore<Preferences> dataStore;

    @Before
    public void setUp() throws Exception {
        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getApplicationContext()).thenReturn(context);
        Mockito.when(context.getFilesDir()).thenReturn(temporaryFolder.getRoot());

        dataStore = new RxPreferenceDataStoreBuilder(context, "test_prefs").build();
        preferencesDataSource = new PreferencesDataSource(dataStore);
    }

    @After
    public void tearDown() {
        if (dataStore != null) {
            dataStore.dispose();
        }
    }

    @Test
    public void testShowPlaybackButton() {
        assertTrue(preferencesDataSource.isShowPlaybackButton());
        preferencesDataSource.setShowPlaybackButton(false);
        assertFalse(preferencesDataSource.isShowPlaybackButton());
        preferencesDataSource.setShowPlaybackButton(true);
        assertTrue(preferencesDataSource.isShowPlaybackButton());
    }

    @Test
    public void testShowCopyButton() {
        assertTrue(preferencesDataSource.isShowCopyButton());
        preferencesDataSource.setShowCopyButton(false);
        assertFalse(preferencesDataSource.isShowCopyButton());
        preferencesDataSource.setShowCopyButton(true);
        assertTrue(preferencesDataSource.isShowCopyButton());
    }

    @Test
    public void testShowRawText() {
        assertTrue(preferencesDataSource.isShowRawText());
        preferencesDataSource.setShowRawText(false);
        assertFalse(preferencesDataSource.isShowRawText());
        preferencesDataSource.setShowRawText(true);
        assertTrue(preferencesDataSource.isShowRawText());
    }

    @Test
    public void testShowSmartText() {
        assertTrue(preferencesDataSource.isShowSmartText());
        preferencesDataSource.setShowSmartText(false);
        assertFalse(preferencesDataSource.isShowSmartText());
        preferencesDataSource.setShowSmartText(true);
        assertTrue(preferencesDataSource.isShowSmartText());
    }

    @Test
    public void testSmartFormattingPerLocale() {
        assertTrue(preferencesDataSource.isSmartFormattingEnabled(Locale.GERMAN));

        preferencesDataSource.setSmartFormattingEnabled(Locale.GERMAN, false);
        assertFalse(preferencesDataSource.isSmartFormattingEnabled(Locale.GERMAN));
        assertTrue(preferencesDataSource.isSmartFormattingEnabled(Locale.ENGLISH));

        preferencesDataSource.setSmartFormattingEnabled(Locale.GERMAN, true);
        assertTrue(preferencesDataSource.isSmartFormattingEnabled(Locale.GERMAN));
    }

    @Test
    public void testTranscriberTypePerLocale() {
        assertEquals(TranscriberType.VOSK, preferencesDataSource.getDefaultTranscriberType());
        assertEquals(TranscriberType.VOSK, preferencesDataSource.getTranscriberType(Locale.GERMAN));

        preferencesDataSource.setTranscriberType(Locale.GERMAN, TranscriberType.WHISPER);
        assertEquals(TranscriberType.WHISPER, preferencesDataSource.getTranscriberType(Locale.GERMAN));
        assertEquals(TranscriberType.VOSK, preferencesDataSource.getTranscriberType(Locale.ENGLISH));
    }

    @Test
    public void testLanguageEnabledState() {
        assertTrue(preferencesDataSource.isLanguageEnabled(Locale.GERMAN));

        preferencesDataSource.setLanguageEnabled(Locale.GERMAN, false);
        assertFalse(preferencesDataSource.isLanguageEnabled(Locale.GERMAN));
        assertTrue(preferencesDataSource.isLanguageEnabled(Locale.ENGLISH));
    }
}
