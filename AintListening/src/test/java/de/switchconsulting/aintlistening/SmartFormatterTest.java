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

package de.switchconsulting.aintlistening;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Unit tests for {@link SmartFormatter}.
 */
public class SmartFormatterTest {

    /**
     * Tests reconstruction of text from tokens and predictions using multi-head logic.
     */
    @Test
    public void testReconstructTextBadCode_WordSplitting() {
        // Example: "besorgen" split into " be", "sorge", "n"
        String[] tokens = {" be", "sorge", "n", " morgen"};
        long[] prePreds = {0, 0, 0, 0};
        long[] postPreds = {0, 0, 2, 0}; // Period on "n"
        long[][] capPreds = {
            {1, 0}, // " be" -> " Be"
            {0, 0, 0, 0, 0}, // "sorge"
            {0}, // "n"
            {0, 0, 0, 0, 0, 0} // " morgen"
        };
        long[] sbdPreds = {0, 0, 1, 0}; // Boundary on "n"

        String result = SmartFormatter.reconstructTextBadCode(tokens, prePreds, postPreds, capPreds, sbdPreds);
        // Expect "Besorgen. Morgen"
        assertEquals("Besorgen. Morgen", result);
    }

    /**
     * Tests German noun capitalization in the middle of a sentence.
     */
    @Test
    public void testReconstructTextBadCode_GermanNouns() {
        String[] tokens = {" ich", " gehe", " ins", " büro"};
        long[] prePreds = new long[4];
        long[] postPreds = new long[4];
        long[][] capPreds = {
            {0, 0, 0}, // " ich" (will be force-capped)
            {0, 0, 0, 0}, // " gehe"
            {0, 0, 0}, // " ins"
            {1, 0, 0, 0} // " büro" -> " Büro"
        };
        long[] sbdPreds = new long[4];

        String result = SmartFormatter.reconstructTextBadCode(tokens, prePreds, postPreds, capPreds, sbdPreds);
        assertEquals("Ich gehe ins Büro", result);
    }

    /**
     * Tests acronym handling.
     */
    @Test
    public void testReconstructTextBadCode_Acronyms() {
        String[] tokens = {" die", " usa"};
        long[] prePreds = new long[2];
        long[] postPreds = {0, 1}; // Acronym marker on "usa"
        long[][] capPreds = {
            {0, 0, 0},
            {1, 1, 1} // All caps
        };

        String result = SmartFormatter.reconstructTextBadCode(tokens, prePreds, postPreds, capPreds, null);
        assertEquals("Die U.S.A.", result);
    }
}
