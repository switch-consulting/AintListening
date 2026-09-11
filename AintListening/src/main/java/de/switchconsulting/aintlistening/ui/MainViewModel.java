package de.switchconsulting.aintlistening.ui;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import de.switchconsulting.aintlistening.data.TranscriptionRepository;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class MainViewModel extends ViewModel {

    public static class UiState {
        public final boolean isLoading;
        public final boolean isIndeterminate;
        public final int progress;
        public final int maxProgress;
        public final String statusMessage;
        public final List<TranscriptionParagraph> paragraphs;
        public final String errorMessage;

        public UiState(boolean isLoading, boolean isIndeterminate, int progress, int maxProgress,
                       String statusMessage, List<TranscriptionParagraph> paragraphs, String errorMessage) {
            this.isLoading = isLoading;
            this.isIndeterminate = isIndeterminate;
            this.progress = progress;
            this.maxProgress = maxProgress;
            this.statusMessage = statusMessage;
            this.paragraphs = paragraphs;
            this.errorMessage = errorMessage;
        }

        public static UiState idle(List<TranscriptionParagraph> paragraphs) {
            return new UiState(false, false, 0, 0, null, paragraphs, null);
        }

        public static UiState loading(String message, List<TranscriptionParagraph> paragraphs) {
            return new UiState(true, true, 0, 0, message, paragraphs, null);
        }

        public static UiState progress(String message, int progress, int max, List<TranscriptionParagraph> paragraphs) {
            return new UiState(true, false, progress, max, message, paragraphs, null);
        }

        public static UiState error(String message, List<TranscriptionParagraph> paragraphs) {
            return new UiState(false, false, 0, 0, message, paragraphs, message);
        }
    }

    private final TranscriptionRepository repository;
    private final MutableLiveData<UiState> _uiState = new MutableLiveData<>();
    public final LiveData<UiState> uiState = _uiState;

    @Inject
    public MainViewModel(TranscriptionRepository repository) {
        this.repository = repository;
        _uiState.setValue(UiState.idle(repository.loadLastMessage()));
    }

    public void startTranscription(Uri audioUri, int modelIndex) {
        _uiState.setValue(UiState.loading("Preparing...", new ArrayList<>()));
        repository.startTranscription(audioUri, modelIndex, new TranscriptionRepository.TranscriptionCallback() {
            @Override
            public void onStatusUpdate(String message) {
                UiState current = _uiState.getValue();
                _uiState.postValue(UiState.loading(message, current != null ? current.paragraphs : new ArrayList<>()));
            }

            @Override
            public void onPartialResult(List<TranscriptionParagraph> paragraphs) {
                UiState current = _uiState.getValue();
                _uiState.postValue(UiState.loading(current != null ? current.statusMessage : "Transcribing...", paragraphs));
            }

            @Override
            public void onSmartFormattingProgress(int progress, int total, List<TranscriptionParagraph> paragraphs) {
                _uiState.postValue(UiState.progress("Applying smart formatting...", progress, total, paragraphs));
            }

            @Override
            public void onComplete(List<TranscriptionParagraph> paragraphs) {
                _uiState.postValue(UiState.idle(paragraphs));
            }

            @Override
            public void onError(String message) {
                UiState current = _uiState.getValue();
                _uiState.postValue(UiState.error(message, current != null ? current.paragraphs : new ArrayList<>()));
            }
        });
    }

    public void loadLastMessage() {
        _uiState.setValue(UiState.idle(repository.loadLastMessage()));
    }

    @Override
    protected void onCleared() {
        repository.release();
    }
}
