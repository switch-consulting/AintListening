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
import java.util.Locale;
import java.util.Map;

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

    // 1-800-BAD-CODE XLM-RoBERTa multi-head mappings
    static final String[] PRE_PUNC_LABELS = {"", "¿", "¡"};
    static final String[] POST_PUNC_LABELS = {
            "", "", ".", ",", "?", "？", "，", "。", "、", "・", "।", "؟", "،", ";", "።", "፣", "፧"
    };
    static final int POST_PUNC_ACRONYM_INDEX = 1;

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
        // Bad-code model expects lowercased input
        Encoding encoding = tokenizer.encode(text.toLowerCase(Locale.GERMAN));
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

        try (OrtSession.Result results = session.run(inputs)) {
            // Expected outputs: pre_preds, post_preds, cap_preds, sbd_preds
            // These are typically indices (Long) already argmaxed by the model export
            long[][] prePreds = (long[][]) results.get("pre_preds").map(v -> {
                try { return v.getValue(); } catch (Exception e) { return null; }
            }).orElse(null);
            long[][] postPreds = (long[][]) results.get("post_preds").map(v -> {
                try { return v.getValue(); } catch (Exception e) { return null; }
            }).orElse(null);
            long[][][] capPreds = (long[][][]) results.get("cap_preds").map(v -> {
                try { return v.getValue(); } catch (Exception e) { return null; }
            }).orElse(null);
            long[][] sbdPreds = (long[][]) results.get("sbd_preds").map(v -> {
                try { return v.getValue(); } catch (Exception e) { return null; }
            }).orElse(null);

            if (prePreds == null || postPreds == null || capPreds == null) {
                Log.e(TAG, "Failed to extract predictions from multi-head model");
                return text.trim();
            }

            return reconstructTextBadCode(tokens, prePreds[0], postPreds[0], capPreds[0], sbdPreds != null ? sbdPreds[0] : null);
        } finally {
            inputIdsTensor.close();
            attentionMaskTensor.close();
        }
    }

    static String reconstructTextBadCode(String[] tokens, long[] prePreds, long[] postPreds, long[][] capPreds, long[] sbdPreds) {
        StringBuilder result = new StringBuilder();
        boolean forceCapitalizeNext = true;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (isSpecialToken(token)) continue;

            if (isNewWord(token)) {
                // 1. Peek ahead to collect all sub-tokens of the CURRENT word
                int j = i;
                StringBuilder wordContent = new StringBuilder();
                int prePuncIdx = (int) prePreds[i];
                int postPuncIdx = 0;
                boolean isAcronym = false;
                
                // Track capitalization for the first character of the word
                boolean shouldCapFirst = forceCapitalizeNext;

                while (j < tokens.length) {
                    if (isSpecialToken(tokens[j])) { j++; continue; }
                    if (j > i && isNewWord(tokens[j])) break;

                    String subToken = getCleanToken(tokens[j]);
                    
                    // Apply character-level capitalization from capPreds
                    for (int k = 0; k < subToken.length(); k++) {
                        char c = subToken.charAt(k);
                        // capPreds[j] contains 0 or 1 for each character
                        // We check the k-th prediction if available
                        boolean cap = false;
                        if (k < capPreds[j].length) {
                            cap = capPreds[j][k] == 1;
                        }
                        
                        if (j == i && k == 0 && shouldCapFirst) {
                            cap = true;
                        }

                        if (cap) {
                            wordContent.append(Character.toUpperCase(c));
                        } else {
                            wordContent.append(c);
                        }
                    }

                    // Update post-punctuation prediction (usually predicted on the last sub-token)
                    postPuncIdx = (int) postPreds[j];
                    if (postPuncIdx == POST_PUNC_ACRONYM_INDEX) isAcronym = true;
                    
                    // If any sub-token marks a sentence boundary, the NEXT word should be capitalized
                    if (sbdPreds != null && sbdPreds[j] == 1) {
                        forceCapitalizeNext = true;
                    }

                    j++;
                }

                String finishedWord = wordContent.toString();
                if (!finishedWord.isEmpty()) {
                    if (!TextUtils.isEmpty(result) && result.charAt(result.length() - 1) != ' ') {
                        result.append(" ");
                    }

                    // Apply pre-punctuation
                    if (prePuncIdx > 0 && prePuncIdx < PRE_PUNC_LABELS.length) {
                        result.append(PRE_PUNC_LABELS[prePuncIdx]);
                    }

                    // Handle acronym special case (periods after every letter)
                    if (isAcronym) {
                        StringBuilder acro = new StringBuilder();
                        for (char c : finishedWord.toCharArray()) {
                            acro.append(c).append(".");
                        }
                        result.append(acro);
                        forceCapitalizeNext = false; 
                    } else {
                        result.append(finishedWord);
                        forceCapitalizeNext = false;
                        // Apply post-punctuation
                        if (postPuncIdx > 1 && postPuncIdx < POST_PUNC_LABELS.length) {
                            String punct = POST_PUNC_LABELS[postPuncIdx];
                            result.append(punct);
                            if (isSentenceEnding(punct)) {
                                forceCapitalizeNext = true;
                            }
                        }
                    }
                }
                
                i = j - 1;
            }
        }

        return result.toString().trim();
    }

    // Old methods removed to clean up multi-head refactor

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
