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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link UiDisplaySettings}.
 */
public class UiDisplaySettingsTest {

    @Test
    public void testCustomConstructor() {
        UiDisplaySettings settings = new UiDisplaySettings(true, false, true, false);

        assertTrue(settings.showPlaybackButton());
        assertFalse(settings.showCopyButton());
        assertTrue(settings.showRawText());
        assertFalse(settings.showSmartText());
    }

    @Test
    public void testDefaultSettings() {
        UiDisplaySettings settings = UiDisplaySettings.defaultSettings();

        assertTrue(settings.showPlaybackButton());
        assertTrue(settings.showCopyButton());
        assertTrue(settings.showRawText());
        assertTrue(settings.showSmartText());
    }
}
