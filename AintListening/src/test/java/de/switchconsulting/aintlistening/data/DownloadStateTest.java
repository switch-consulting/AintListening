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
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link DownloadState}.
 */
public class DownloadStateTest {

    @Test
    public void testIdleState() {
        DownloadState state = DownloadState.idle();
        assertEquals(DownloadStatus.IDLE, state.status);
        assertEquals(0, state.progress);
        assertNull(state.error);
    }

    @Test
    public void testDownloadingState() {
        DownloadState state = DownloadState.downloading(75);
        assertEquals(DownloadStatus.DOWNLOADING, state.status);
        assertEquals(75, state.progress);
        assertNull(state.error);
    }

    @Test
    public void testExtractingState() {
        DownloadState state = DownloadState.extracting();
        assertEquals(DownloadStatus.EXTRACTING, state.status);
        assertEquals(0, state.progress);
        assertNull(state.error);
    }

    @Test
    public void testSuccessState() {
        DownloadState state = DownloadState.success();
        assertEquals(DownloadStatus.SUCCESS, state.status);
        assertEquals(100, state.progress);
        assertNull(state.error);
    }

    @Test
    public void testErrorState() {
        Exception exception = new Exception("Download failed");
        DownloadState state = DownloadState.error(exception);
        assertEquals(DownloadStatus.ERROR, state.status);
        assertEquals(0, state.progress);
        assertEquals(exception, state.error);
    }
}
