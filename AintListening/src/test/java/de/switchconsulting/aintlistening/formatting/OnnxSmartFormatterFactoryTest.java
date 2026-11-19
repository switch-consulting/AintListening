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

package de.switchconsulting.aintlistening.formatting;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Locale;

import de.switchconsulting.aintlistening.data.ModelInfo;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * Unit tests for {@link OnnxSmartFormatterFactory}.
 */
public class OnnxSmartFormatterFactoryTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testFactoryInstantiation() {
        Context context = mock(Context.class);
        OnnxSmartFormatterFactory factory = new OnnxSmartFormatterFactory(context);
        assertNotNull(factory);
    }

    @Test(expected = Throwable.class)
    public void testCreateThrowsExceptionWhenModelMissing() throws Exception {
        Context context = mock(Context.class);
        File filesDir = temporaryFolder.newFolder("files");
        when(context.getFilesDir()).thenReturn(filesDir);

        OnnxSmartFormatterFactory factory = new OnnxSmartFormatterFactory(context);
        ModelInfo modelInfo = new ModelInfo("nonexistent", "http://example.com", Locale.GERMAN, "1MB", TranscriberType.VOSK, true);
        factory.create(modelInfo);
    }
}
