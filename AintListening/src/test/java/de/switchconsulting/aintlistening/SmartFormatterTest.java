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
     * Tests reconstruction of text from tokens and logits, specifically handling word splitting.
     */
    @Test
    public void testReconstructText_WordSplitting() {
        // Example: "besorgen" split into " be", "sorge", "n"
        // Model predicts label 1 (.) for the first sub-token " be"
        String[] tokens = {" be", "sorge", "n", " morgen"};
        float[][] logits = new float[4][6];
        logits[0][1] = 1.0f; // label 1 (.) for " be"
        logits[1][0] = 1.0f; // label 0 for "sorge"
        logits[2][0] = 1.0f; // label 0 for "n"
        logits[3][0] = 1.0f; // label 0 for " morgen"

        String result = SmartFormatter.reconstructText(tokens, logits);
        // Expect "Besorgen. Morgen" (Capitalized start, period after full word "besorgen", capitalized next word)
        assertEquals("Besorgen. Morgen", result);
    }

    /**
     * Tests reconstruction of text focusing on capitalization and question mark placement.
     */
    @Test
    public void testReconstructText_Capitalization() {
        String[] tokens = {" hallo", " wie", " geht", " es", " dir"};
        float[][] logits = new float[5][6];
        logits[0][0] = 1.0f;
        logits[1][0] = 1.0f;
        logits[2][0] = 1.0f;
        logits[3][0] = 1.0f;
        logits[4][3] = 1.0f; // label 3 (?) for " dir"

        String result = SmartFormatter.reconstructText(tokens, logits);
        assertEquals("Hallo wie geht es dir?", result);
    }

    /**
     * Tests reconstruction of text with multiple sentences.
     */
    @Test
    public void testReconstructText_MultipleSentences() {
        String[] tokens = {" das", " ist", " gut", " super"};
        float[][] logits = new float[4][6];
        logits[0][0] = 1.0f;
        logits[1][0] = 1.0f;
        logits[2][1] = 1.0f; // label 1 (.) for " gut"
        logits[3][0] = 1.0f;

        String result = SmartFormatter.reconstructText(tokens, logits);
        assertEquals("Das ist gut. Super", result);
    }

    /**
     * Tests German capitalization heuristic.
     */
    @Test
    public void testReconstructText_GermanCapitalization() {
        // "ja ist es büro mutter"
        // Start "Ja" (always capitalized)
        // "ist", "es" (function words, stay lowercase)
        // "büro", "mutter" (nouns, should be capitalized)
        String[] tokens = {" ja", " ist", " es", " büro", " mutter"};
        float[][] logits = new float[5][6];

        String result = SmartFormatter.reconstructText(tokens, logits);
        assertEquals("Ja ist es Büro Mutter", result);
    }

    /**
     * Tests robust punctuation (taking it from any sub-token).
     */
    @Test
    public void testReconstructText_SplitWordPunctuation() {
        // "besorgen" split into " be", "sorge", "n"
        // Period predicted on the LAST sub-token "n"
        String[] tokens = {" be", "sorge", "n", " morgen"};
        float[][] logits = new float[4][6];
        logits[0][0] = 1.0f;
        logits[1][0] = 1.0f;
        logits[2][1] = 1.0f; // label 1 (.) for "n"
        logits[3][0] = 1.0f;

        String result = SmartFormatter.reconstructText(tokens, logits);
        // "Besorgen" is capitalized (heuristic or start), followed by period, then "Morgen" (heuristic)
        assertEquals("Besorgen. Morgen", result);
    }

    /**
     * Verifies the user's specific problematic case.
     */
    @Test
    public void testReconstructText_UserExample() {
        String[] tokens = {" ja", " ist", " es", " so", " dass", " ich", " gestern", " war", " es", " sogar",
                           " dienstag", " bis", " donnerstag", " im", " büro", " mutter", " pflege"};
        float[][] logits = new float[tokens.length][6];

        String result = SmartFormatter.reconstructText(tokens, logits);
        assertEquals("Ja ist es so dass ich gestern war es sogar Dienstag bis Donnerstag im Büro Mutter Pflege", result);
    }
}
