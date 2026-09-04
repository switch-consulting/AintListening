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

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * Handles post-processing of transcribed text to add punctuation and capitalization
 * using an ONNX model.
 */
public class SmartFormatter {
    private static final String TAG = "SmartFormatter";
    
    private final OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;
    private final ModelInfo modelInfo;

    // Label mapping for oliverguhr/fullstop-punctuation-multilingual-sonar-base
    // 0: 0 (None), 1: . , 2: , , 3: ? , 4: - , 5: :
    static final Map<Integer, String> LABEL_MAP = new HashMap<>();
    static {
        LABEL_MAP.put(1, ".");
        LABEL_MAP.put(2, ",");
        LABEL_MAP.put(3, "?");
        LABEL_MAP.put(4, "-");
        LABEL_MAP.put(5, ":");
    }

    /**
     * Heuristic for German noun capitalization (Negative POS Tagging approach).
     * In German, nouns and proper nouns are capitalized, while function words, verbs,
     * and adjectives are lowercase (unless at the start of a sentence).
     * This set contains common German words that should remain lowercase.
     */
    private static final Set<String> GERMAN_LOWERCASE_WORDS = new HashSet<>(Arrays.asList(
            "der", "die", "das", "ein", "eine", "einer", "einem", "einen", "eines",
            "und", "oder", "aber", "denn", "doch", "noch", "als", "wie", "so", "ja", "nein",
            "ich", "du", "er", "sie", "es", "wir", "ihr", "mein", "dein", "sein", "unser", "euer",
            "mich", "dich", "sich", "uns", "euch", "mir", "dir", "ihm", "ihr", "den", "dem",
            "in", "an", "zu", "auf", "mit", "von", "aus", "bei", "nach", "für", "um", "über", "vor", "durch", "seit", "gegen",
            "ist", "sind", "war", "waren", "bin", "bist", "habe", "hat", "hatte", "wird", "werden", "kann", "können", "muss", "müssen", "soll", "wollen",
            "nicht", "auch", "nur", "schon", "jetzt", "immer", "wenn", "dass", "weil", "da", "dort", "hier",
            "man", "jemand", "etwas", "nichts", "alles", "alle", "jeder", "kein", "keine",
            "diese", "dieser", "dieses", "diesem", "diesen", "welche", "welcher", "welches",
            "am", "im", "ans", "ins", "zur", "zum", "vom", "beim", "bis",
            "gut", "geht", "sehr", "viel", "ganz", "mehr", "immer", "nie", "oft", "vielleicht",
            "dann", "danach", "heute", "morgen", "gestern", "sogar", "gibt", "drauf", "auch", "oder",
            "teilweise", "um", "dient", "wegen", "meine", "deine", "seine", "ihre", "unser", "unserer", "euer", "eurer",
            "doch", "diese", "dieser", "dieses", "diesem", "diesen", "einigen", "einiger", "einiges",
            "dringend", "weiter", "kümmern", "machen", "tun", "geht", "gut", "schon", "noch", "nur", "viel", "mehr", "sehr",
            "für", "mit", "von", "aus", "bei", "nach", "seit", "zu", "um", "über", "unter", "zwischen", "vor", "nach", "ohne", "gegen",
            "ergibt", "was", "gegessen", "länger", "also", "verschiedne", "wo", "raus", "genommen", "nutzt", "wieder",
            "warum", "wie", "wann", "wer", "wen", "wem", "weshalb", "wieso", "viele", "alle", "alles", "etwas", "nichts"
    ));

    /**
     * Constructs a new SmartFormatter and initializes the ONNX environment and model.
     *
     * @param context The application context.
     * @param info    The metadata for the smart formatting model to load.
     * @throws Exception If model or tokenizer initialization fails.
     */
    public SmartFormatter(@NonNull Context context, @NonNull ModelInfo info) throws Exception {
        this.env = OrtEnvironment.getEnvironment();
        this.modelInfo = info;
        loadModel(context, info);
    }

    /**
     * @return The metadata of the currently loaded model.
     */
    @NonNull
    public ModelInfo getModelInfo() {
        return modelInfo;
    }

    /**
     * Loads the ONNX model and tokenizer from the application's internal files directory.
     *
     * @param context The application context.
     * @param info    The model information.
     * @throws Exception If the model or tokenizer files are not found or fail to load.
     */
    private void loadModel(Context context, ModelInfo info) throws Exception {
        File initialDir = new File(context.getFilesDir(), info.name);
        File nestedDir = new File(initialDir, info.name);
        
        final File actualDir;
        if (new File(initialDir, "model.onnx").exists()) {
            actualDir = initialDir;
        } else if (new File(nestedDir, "model.onnx").exists()) {
            actualDir = nestedDir;
        } else {
            actualDir = initialDir; // Default to initial for the error message
        }

        File modelFile = new File(actualDir, "model.onnx");
        File tokenizerFile = new File(actualDir, "tokenizer.json");
        
        if (!modelFile.exists()) {
            throw new Exception("Smart formatting model not found at " + modelFile.getAbsolutePath());
        }
        if (!tokenizerFile.exists()) {
            throw new Exception("Tokenizer file not found at " + tokenizerFile.getAbsolutePath());
        }

        Log.i(TAG, "Loading ONNX model from: " + modelFile.getAbsolutePath());
        session = env.createSession(modelFile.getAbsolutePath(), new OrtSession.SessionOptions());
        
        for (String inputName : session.getInputNames()) {
            Log.i(TAG, "Model input: " + inputName);
        }
        for (String outputName : session.getOutputNames()) {
            Log.i(TAG, "Model output: " + outputName);
        }
        
        Log.i(TAG, "Loading tokenizer from: " + tokenizerFile.getAbsolutePath());
        tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile.toPath());
        
        Log.i(TAG, "Smart formatting model and tokenizer loaded successfully");
    }

    /**
     * Applies smart formatting (punctuation and capitalization) to the input text.
     *
     * @param text The raw transcribed text.
     * @return The formatted text.
     */
    public String format(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        Log.i(TAG, "Starting smart formatting for: " + text);
        try {
            String result = processWithModel(text);
            if (result.equals(text.trim())) {
                Log.w(TAG, "Formatting produced no changes to the text.");
            } else {
                Log.i(TAG, "Formatting successful. New text: " + result);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Failed to apply smart formatting", e);
            return text;
        }
    }

    private String processWithModel(String text) throws Exception {
        Log.d(TAG, "Input text: " + text);
        Encoding encoding = tokenizer.encode(text);
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        String[] tokens = encoding.getTokens();
        
        Log.d(TAG, "Tokens: " + Arrays.toString(tokens));
        
        long[] shape = {1, inputIds.length};
        OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape);
        OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape);
        
        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", inputIdsTensor);
        inputs.put("attention_mask", attentionMaskTensor);
        
        // Add token_type_ids if required by the model (common in BERT/RoBERTa)
        final OnnxTensor tokenTypeIdsTensor;
        if (session.getInputNames().contains("token_type_ids")) {
            long[] tokenTypeIds = new long[inputIds.length]; // All zeros
            tokenTypeIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape);
            inputs.put("token_type_ids", tokenTypeIdsTensor);
        } else {
            tokenTypeIdsTensor = null;
        }
        
        try (OrtSession.Result results = session.run(inputs)) {
            // Logits shape: [batch, sequence, num_labels]
            float[][][] logits = (float[][][]) results.get(0).getValue();
            Log.d(TAG, "Logits shape: " + logits.length + "x" + logits[0].length + "x" + logits[0][0].length);
            
            return reconstructText(tokens, logits[0]);
        } finally {
            inputIdsTensor.close();
            attentionMaskTensor.close();
            if (tokenTypeIdsTensor != null) {
                tokenTypeIdsTensor.close();
            }
        }
    }

    static String reconstructText(String[] tokens, float[][] logits) {
        StringBuilder result = new StringBuilder();
        String pendingPunct = null;
        boolean shouldCapitalize = true;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (isSpecialToken(token)) continue;

            if (isNewWord(token)) {
                // 1. Apply punctuation from the PREVIOUS word
                if (pendingPunct != null) {
                    result.append(pendingPunct);
                    if (isSentenceEnding(pendingPunct)) {
                        shouldCapitalize = true;
                    }
                }

                // 2. Peek ahead to collect all sub-tokens of the CURRENT word
                StringBuilder wordBuilder = new StringBuilder();
                int label = 0;
                int j = i;

                // Process first sub-token
                wordBuilder.append(getCleanToken(tokens[j]));
                int firstLabel = argmax(logits[j]);
                if (firstLabel > 0) label = firstLabel;

                // Collect remaining sub-tokens of the same word
                j++;
                while (j < tokens.length) {
                    if (isSpecialToken(tokens[j])) {
                        j++;
                        continue;
                    }
                    if (isNewWord(tokens[j])) break;

                    wordBuilder.append(getCleanToken(tokens[j]));
                    int subLabel = argmax(logits[j]);
                    if (subLabel > 0) label = subLabel; // Take the last predicted punctuation
                    j++;
                }

                String wordStr = wordBuilder.toString();
                if (!wordStr.isEmpty()) {
                    // Add space if not the first word
                    if (!TextUtils.isEmpty(result) && result.charAt(result.length() - 1) != ' ') {
                        result.append(" ");
                    }

                    // Capitalize if start of sentence OR German noun/adjective heuristic
                    if (shouldCapitalize || shouldCapitalizeGermanWord(wordStr)) {
                        result.append(Character.toUpperCase(wordStr.charAt(0)));
                        if (wordStr.length() > 1) {
                            result.append(wordStr.substring(1));
                        }
                    } else {
                        result.append(wordStr);
                    }

                    shouldCapitalize = false;
                    pendingPunct = LABEL_MAP.get(label);
                }

                // Advance main loop to the last token of this word
                i = j - 1;
            }
        }

        // Apply final punctuation
        if (pendingPunct != null) {
            result.append(pendingPunct);
        }

        return result.toString().trim();
    }

    private static boolean shouldCapitalizeGermanWord(String word) {
        if (word == null || word.isEmpty()) return false;
        String lower = word.toLowerCase(Locale.GERMAN);
        return !GERMAN_LOWERCASE_WORDS.contains(lower);
    }

    static boolean isSpecialToken(String token) {
        return token.equals("<s>") || token.equals("</s>") || token.equals("<pad>") ||
                token.equals("[CLS]") || token.equals("[SEP]") || token.equals("<unk>");
    }

    static boolean isNewWord(String token) {
        // SentencePiece uses   (U+2581) or a regular space to denote the start of a word
        return token.startsWith(" ") || token.startsWith("\u2581");
    }

    static String getCleanToken(String token) {
        if (token.startsWith(" ") || token.startsWith("\u2581")) {
            return token.substring(1);
        }
        return token;
    }

    static boolean isSentenceEnding(String punct) {
        return ".".equals(punct) || "?".equals(punct) || ":".equals(punct);
    }

    static int argmax(float[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public void close() {
        if (tokenizer != null) {
            tokenizer.close();
        }
        if (session != null) {
            try {
                session.close();
            } catch (Exception ignored) {}
        }
        if (env != null) {
            env.close();
        }
    }
}
