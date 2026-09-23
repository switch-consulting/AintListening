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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.switchconsulting.aintlistening.data.DownloadState;
import de.switchconsulting.aintlistening.data.DownloadStatus;

/**
 * Unit tests for {@link ModelManagementViewModel}.
 */
public class ModelManagementViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ModelManagementViewModel viewModel;

    @Before
    public void setUp() {
        Application application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(application);
        viewModel = new ModelManagementViewModel(application);
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
}
