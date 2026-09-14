package de.switchconsulting.aintlistening.ui;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import de.switchconsulting.aintlistening.data.TranscriptionCallback;
import de.switchconsulting.aintlistening.data.TranscriptionRepository;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final TranscriptionRepository repository;
    private final MutableLiveData<MainUiState> _uiState = new MutableLiveData<>();
    public final LiveData<MainUiState> uiState = _uiState;

    @Inject
    public MainViewModel(TranscriptionRepository repository) {
        this.repository = repository;
        _uiState.setValue(MainUiState.idle(repository.loadLastMessage()));
    }

    public void startTranscription(Uri audioUri, int modelIndex) {
        _uiState.setValue(MainUiState.loading("Preparing...", new ArrayList<>()));
        repository.startTranscription(audioUri, modelIndex, new TranscriptionCallback() {
            @Override
            public void onStatusUpdate(String message) {
                MainUiState current = _uiState.getValue();
                _uiState.postValue(MainUiState.loading(message, current != null ? current.paragraphs : new ArrayList<>()));
            }

            @Override
            public void onPartialResult(List<TranscriptionParagraph> paragraphs) {
                MainUiState current = _uiState.getValue();
                _uiState.postValue(MainUiState.loading(current != null ? current.statusMessage : "Transcribing...", paragraphs));
            }

            @Override
            public void onSmartFormattingProgress(int progress, int total, List<TranscriptionParagraph> paragraphs) {
                _uiState.postValue(MainUiState.progress("Applying smart formatting...", progress, total, paragraphs));
            }

            @Override
            public void onComplete(List<TranscriptionParagraph> paragraphs) {
                _uiState.postValue(MainUiState.idle(paragraphs));
            }

            @Override
            public void onError(String message) {
                MainUiState current = _uiState.getValue();
                _uiState.postValue(MainUiState.error(message, current != null ? current.paragraphs : new ArrayList<>()));
            }
        });
    }

    public void loadLastMessage() {
        _uiState.setValue(MainUiState.idle(repository.loadLastMessage()));
    }

    @Override
    protected void onCleared() {
        repository.release();
    }
}
