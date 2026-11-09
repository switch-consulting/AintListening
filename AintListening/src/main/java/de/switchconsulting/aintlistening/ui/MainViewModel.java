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

import android.content.Context;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.data.TranscriptionCallback;
import de.switchconsulting.aintlistening.data.TranscriptionProcessor;
import de.switchconsulting.aintlistening.data.TranscriptionStep;
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

    /** The application context. */
    private final Context context;
    /** The transcription processor used to handle audio transcription logic. */
    private final TranscriptionProcessor processor;
    /** Mutable LiveData representing the internal UI state. */
    private final MutableLiveData<MainUiState> uiStateMutable = new MutableLiveData<>();
    /** Public observable LiveData for the UI state. */
    public final LiveData<MainUiState> uiState = uiStateMutable;

    /**
     * Constructs a new MainViewModel.
     *
     * @param context   The application context.
     * @param processor The processor responsible for the transcription logic.
     */
    @Inject
    public MainViewModel(@ApplicationContext Context context, TranscriptionProcessor processor) {
        this.context = context;
        this.processor = processor;
        loadLastMessage();
    }

    /**
     * Starts the transcription process for the given audio URI.
     *
     * @param audioUri The URI of the audio file to transcribe.
     * @param locale   The locale of the language model to use.
     */
    public void startTranscription(Uri audioUri, Locale locale) {
        uiStateMutable.setValue(MainUiState.loading(getStatusMessage(TranscriptionStep.PREPARING, locale), new ArrayList<>()));
        processor.startTranscription(audioUri, locale, new TranscriptionCallback() {
            @Override
            public void onStepUpdate(TranscriptionStep step, Locale stepLocale) {
                Locale activeLocale = stepLocale != null ? stepLocale : locale;
                String message = getStatusMessage(step, activeLocale);
                MainUiState current = uiStateMutable.getValue();
                uiStateMutable.postValue(MainUiState.loading(message, current != null ? current.paragraphs() : new ArrayList<>()));
            }

            @Override
            public void onPartialResult(List<TranscriptionParagraph> paragraphs) {
                MainUiState current = uiStateMutable.getValue();
                String defaultTranscribing = getStatusMessage(TranscriptionStep.TRANSCRIBING, locale);
                uiStateMutable.postValue(MainUiState.loading(current != null ? current.statusMessage() : defaultTranscribing, paragraphs));
            }

            @Override
            public void onSmartFormattingProgress(int progress, int total, List<TranscriptionParagraph> paragraphs) {
                uiStateMutable.postValue(MainUiState.progress(getStatusMessage(TranscriptionStep.APPLYING_SMART_FORMATTING, locale), progress, total, paragraphs));
            }

            @Override
            public void onComplete(List<TranscriptionParagraph> paragraphs) {
                uiStateMutable.postValue(MainUiState.idle(paragraphs));
            }

            @Override
            public void onError(String message) {
                MainUiState current = uiStateMutable.getValue();
                uiStateMutable.postValue(MainUiState.error(message, current != null ? current.paragraphs() : new ArrayList<>()));
            }
        });
    }

    /**
     * Maps a TranscriptionStep and Locale to a localized status message string.
     *
     * @param step   The transcription step.
     * @param locale The locale being processed.
     * @return The localized status message.
     */
    private String getStatusMessage(TranscriptionStep step, Locale locale) {
        if (step == null) return "";
        return switch (step) {
            case PREPARING -> context.getString(R.string.status_preparing);
            case CONVERTING_AUDIO -> context.getString(R.string.status_converting);
            case LOADING_MODEL -> context.getString(R.string.status_loading_model);
            case TRANSCRIBING -> {
                String languageName = locale != null ? locale.getDisplayName() : "";
                yield context.getString(R.string.status_transcribing, languageName);
            }
            case APPLYING_SMART_FORMATTING -> context.getString(R.string.status_applying_smart_formatting);
        };
    }

    /**
     * Loads the last transcription result asynchronously and updates the UI state.
     */
    public void loadLastMessage() {
        processor.loadLastMessage(paragraphs -> uiStateMutable.setValue(MainUiState.idle(paragraphs)));
    }

    /**
     * Called when the ViewModel is cleared, ensuring resources in the processor are released and pending tasks canceled.
     */
    @Override
    protected void onCleared() {
        processor.cancelTranscription();
        processor.release();
    }
}
