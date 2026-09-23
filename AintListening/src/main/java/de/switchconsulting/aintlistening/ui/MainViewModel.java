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

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import de.switchconsulting.aintlistening.data.TranscriptionCallback;
import de.switchconsulting.aintlistening.data.TranscriptionProcessor;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;

/**
 * ViewModel for the main transcription screen, handling UI state and interacting
 * with the TranscriptionProcessor.
 */
@HiltViewModel
public class MainViewModel extends ViewModel {

    /** The transcription processor used to handle audio transcription logic. */
    private final TranscriptionProcessor processor;
    /** Mutable LiveData representing the internal UI state. */
    private final MutableLiveData<MainUiState> uiStateMutable = new MutableLiveData<>();
    /** Public observable LiveData for the UI state. */
    public final LiveData<MainUiState> uiState = uiStateMutable;

    /**
     * Constructs a new MainViewModel.
     *
     * @param processor The processor responsible for the transcription logic.
     */
    @Inject
    public MainViewModel(TranscriptionProcessor processor) {
        this.processor = processor;
        uiStateMutable.setValue(MainUiState.idle(processor.loadLastMessage()));
    }

    /**
     * Starts the transcription process for the given audio URI.
     *
     * @param audioUri The URI of the audio file to transcribe.
     * @param locale   The locale of the language model to use.
     */
    public void startTranscription(Uri audioUri, Locale locale) {
        uiStateMutable.setValue(MainUiState.loading("Preparing...", new ArrayList<>()));
        processor.startTranscription(audioUri, locale, new TranscriptionCallback() {
            @Override
            public void onStatusUpdate(String message) {
                MainUiState current = uiStateMutable.getValue();
                uiStateMutable.postValue(MainUiState.loading(message, current != null ? current.paragraphs : new ArrayList<>()));
            }

            @Override
            public void onPartialResult(List<TranscriptionParagraph> paragraphs) {
                MainUiState current = uiStateMutable.getValue();
                uiStateMutable.postValue(MainUiState.loading(current != null ? current.statusMessage : "Transcribing...", paragraphs));
            }

            @Override
            public void onSmartFormattingProgress(int progress, int total, List<TranscriptionParagraph> paragraphs) {
                uiStateMutable.postValue(MainUiState.progress("Applying smart formatting...", progress, total, paragraphs));
            }

            @Override
            public void onComplete(List<TranscriptionParagraph> paragraphs) {
                uiStateMutable.postValue(MainUiState.idle(paragraphs));
            }

            @Override
            public void onError(String message) {
                MainUiState current = uiStateMutable.getValue();
                uiStateMutable.postValue(MainUiState.error(message, current != null ? current.paragraphs : new ArrayList<>()));
            }
        });
    }

    /**
     * Loads the last transcription result and updates the UI state.
     */
    public void loadLastMessage() {
        uiStateMutable.setValue(MainUiState.idle(processor.loadLastMessage()));
    }

    /**
     * Called when the ViewModel is cleared, ensuring resources in the processor are released.
     */
    @Override
    protected void onCleared() {
        processor.release();
    }
}
