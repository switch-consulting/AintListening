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

package de.switchconsulting.aintlistening.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import de.switchconsulting.aintlistening.data.DownloadState;
import de.switchconsulting.aintlistening.data.DownloadStatus;
import de.switchconsulting.aintlistening.data.ModelDownloadCallback;
import de.switchconsulting.aintlistening.data.ModelDownloader;
import de.switchconsulting.aintlistening.data.ModelInfo;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link ModelManagementViewModel}.
 */
public class ModelManagementViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ModelDownloader modelDownloader;
    private ModelManagementViewModel viewModel;

    @Before
    public void setUp() {
        Application application = mock(Application.class);
        File dummyFilesDir = new File("/tmp");
        when(application.getFilesDir()).thenReturn(dummyFilesDir);
        when(application.getApplicationContext()).thenReturn(application);

        modelDownloader = mock(ModelDownloader.class);
        viewModel = new ModelManagementViewModel(application, modelDownloader);
    }

    @Test
    public void testInitialStateIsIdle() {
        DownloadState state = viewModel.downloadState.getValue();
        assertNotNull(state);
        assertEquals(DownloadStatus.IDLE, state.status);
    }

    @Test
    public void testResetStateSetsStatusToIdle() {
        viewModel.resetState();
        DownloadState state = viewModel.downloadState.getValue();
        assertNotNull(state);
        assertEquals(DownloadStatus.IDLE, state.status);
    }

    @Test
    public void testStartDownloadWithNullInfoDoesNothing() {
        viewModel.startDownload(null);
        DownloadState state = viewModel.downloadState.getValue();
        assertNotNull(state);
        assertEquals(DownloadStatus.IDLE, state.status);
    }

    @Test
    public void testStartDownloadSuccessFlow() {
        ModelInfo info = new ModelInfo("vosk-de", "http://example.com/vosk.zip", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);

        doAnswer(invocation -> {
            ModelDownloadCallback callback = invocation.getArgument(2);
            callback.onProgress(50);
            callback.onExtracting();
            callback.onSuccess();
            return null;
        }).when(modelDownloader).downloadAndExtract(any(), any(), any());

        viewModel.startDownload(info);

        DownloadState state = viewModel.downloadState.getValue();
        assertNotNull(state);
        assertEquals(DownloadStatus.SUCCESS, state.status);
    }

    @Test
    public void testStartDownloadErrorFlow() {
        ModelInfo info = new ModelInfo("vosk-de", "http://example.com/vosk.zip", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);
        Exception error = new Exception("Download failed");

        doAnswer(invocation -> {
            ModelDownloadCallback callback = invocation.getArgument(2);
            callback.onError(error);
            return null;
        }).when(modelDownloader).downloadAndExtract(any(), any(), any());

        viewModel.startDownload(info);

        DownloadState state = viewModel.downloadState.getValue();
        assertNotNull(state);
        assertEquals(DownloadStatus.ERROR, state.status);
        assertEquals(error, state.error);
    }

    @Test
    public void testStartDownloadCancelledFlow() {
        ModelInfo info = new ModelInfo("vosk-de", "http://example.com/vosk.zip", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);

        doAnswer(invocation -> {
            ModelDownloadCallback callback = invocation.getArgument(2);
            callback.onCancelled();
            return null;
        }).when(modelDownloader).downloadAndExtract(any(), any(), any());

        viewModel.startDownload(info);

        DownloadState state = viewModel.downloadState.getValue();
        assertNotNull(state);
        assertEquals(DownloadStatus.IDLE, state.status);
    }

    @Test
    public void testStartDownloadIgnoredWhenAlreadyDownloading() {
        ModelInfo info = new ModelInfo("vosk-de", "http://example.com/vosk.zip", Locale.GERMAN, "45MB", TranscriberType.VOSK, true);

        doAnswer(invocation -> {
            ModelDownloadCallback callback = invocation.getArgument(2);
            callback.onProgress(10);
            return null;
        }).when(modelDownloader).downloadAndExtract(any(), any(), any());

        viewModel.startDownload(info);
        DownloadState currentState = viewModel.downloadState.getValue();
        assertNotNull(currentState);
        assertEquals(DownloadStatus.DOWNLOADING, currentState.status);

        // Call again while DOWNLOADING
        viewModel.startDownload(info);
        // Verify only 1 call to downloadAndExtract
        verify(modelDownloader).downloadAndExtract(any(), any(), any());
    }
}
