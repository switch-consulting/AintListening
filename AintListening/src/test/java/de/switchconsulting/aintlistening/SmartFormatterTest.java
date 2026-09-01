package de.switchconsulting.aintlistening;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SmartFormatterTest {

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
}
