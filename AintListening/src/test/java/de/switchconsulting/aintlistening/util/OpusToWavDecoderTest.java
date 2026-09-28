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

package de.switchconsulting.aintlistening.util;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.net.Uri;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for {@link OpusToWavDecoder}.
 */
public class OpusToWavDecoderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testDecodeOpusToWavWithInvalidUriReturnsFalse() throws IOException {
        Context context = mock(Context.class);
        Uri invalidUri = mock(Uri.class);
        File outputFile = temporaryFolder.newFile("output.wav");

        boolean result = OpusToWavDecoder.decodeOpusToWav(context, invalidUri, outputFile);
        assertFalse(result);
    }
}
