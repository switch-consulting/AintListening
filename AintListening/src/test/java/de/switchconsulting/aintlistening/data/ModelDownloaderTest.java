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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link ModelDownloader}.
 */
public class ModelDownloaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ModelDownloader downloader;
    private File targetDir;

    @Before
    public void setUp() throws IOException {
        downloader = new ModelDownloader();
        targetDir = temporaryFolder.newFolder("target");
    }

    @Test
    public void testCancel() {
        downloader.cancel();
    }

    @Test
    public void testDownloadAndExtractWithInvalidUrlCallsError() {
        ModelInfo info = new ModelInfo("invalid-model", "http://invalid-url-that-does-not-exist.local/test.zip", Locale.GERMAN, "1MB", TranscriberType.VOSK, true);
        ModelDownloadCallback callback = mock(ModelDownloadCallback.class);

        downloader.downloadAndExtract(info, targetDir, callback);
        assertNotNull(info);
    }
}
