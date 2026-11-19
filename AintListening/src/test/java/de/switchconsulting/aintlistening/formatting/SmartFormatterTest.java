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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import android.text.TextUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link SmartFormatter}.
 */
public class SmartFormatterTest {

    private MockedStatic<TextUtils> textUtilsMock;

    @Before
    public void setUp() {
        textUtilsMock = mockStatic(TextUtils.class);
        textUtilsMock.when(() -> TextUtils.isEmpty(any())).thenAnswer(invocation -> {
            CharSequence s = invocation.getArgument(0);
            return s == null || s.isEmpty();
        });
    }

    @After
    public void tearDown() {
        if (textUtilsMock != null) {
            textUtilsMock.close();
        }
    }

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

        String result = OnnxSmartFormatter.reconstructTextBadCode(tokens, prePreds, postPreds, capPreds, sbdPreds);
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

        String result = OnnxSmartFormatter.reconstructTextBadCode(tokens, prePreds, postPreds, capPreds, sbdPreds);
        assertEquals("Ich gehe ins Büro", result);
    }

    /**
     * Tests acronym handling where postPreds == POST_PUNC_ACRONYM_INDEX (1).
     */
    @Test
    public void testReconstructTextBadCode_Acronym() {
        String[] tokens = {" usa"};
        long[] prePreds = {0};
        long[] postPreds = {OnnxSmartFormatter.POST_PUNC_ACRONYM_INDEX}; // 1 = acronym
        long[][] capPreds = {
            {1, 1, 1, 1} // " usa" -> "USA"
        };
        long[] sbdPreds = {0};

        String result = OnnxSmartFormatter.reconstructTextBadCode(tokens, prePreds, postPreds, capPreds, sbdPreds);
        assertEquals("U.S.A.", result);
    }

    @Test
    public void testIsSpecialToken() {
        assertTrue(OnnxSmartFormatter.isSpecialToken("<s>"));
        assertTrue(OnnxSmartFormatter.isSpecialToken("</s>"));
        assertTrue(OnnxSmartFormatter.isSpecialToken("<pad>"));
        assertTrue(OnnxSmartFormatter.isSpecialToken("[CLS]"));
        assertTrue(OnnxSmartFormatter.isSpecialToken("[SEP]"));
        assertTrue(OnnxSmartFormatter.isSpecialToken("<unk>"));
        assertFalse(OnnxSmartFormatter.isSpecialToken(" hello"));
    }

    @Test
    public void testIsNewWord() {
        assertTrue(OnnxSmartFormatter.isNewWord(" hello"));
        assertTrue(OnnxSmartFormatter.isNewWord("▁hello"));
        assertFalse(OnnxSmartFormatter.isNewWord("hello"));
    }

    @Test
    public void testIsSentenceEnding() {
        assertTrue(OnnxSmartFormatter.isSentenceEnding("."));
        assertTrue(OnnxSmartFormatter.isSentenceEnding("?"));
        assertTrue(OnnxSmartFormatter.isSentenceEnding(":"));
        assertFalse(OnnxSmartFormatter.isSentenceEnding(","));
        assertFalse(OnnxSmartFormatter.isSentenceEnding(";"));
    }

    @Test
    public void testReconstructTextBadCode_PrePunctuation() {
        String[] tokens = {"<s>", " wie", " geht", " es", " dir"};
        long[] prePreds = {0, 1, 0, 0, 0}; // 1 = "¿"
        long[] postPreds = {0, 0, 0, 0, 4}; // 4 = "?"
        long[][] capPreds = {
            {0},
            {1, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0},
            {0, 0, 0, 0}
        };

        String result = OnnxSmartFormatter.reconstructTextBadCode(tokens, prePreds, postPreds, capPreds, null);
        assertEquals("¿Wie geht es dir?", result);
    }
}
