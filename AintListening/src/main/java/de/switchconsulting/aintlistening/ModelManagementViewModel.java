package de.switchconsulting.aintlistening;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.File;

/**
 * ViewModel for managing the download and extraction of speech models.
 * Maintains the current download state and handles background operations.
 */
public class ModelManagementViewModel extends AndroidViewModel {

    private final ModelDownloader modelDownloader = new ModelDownloader();
    private final MutableLiveData<DownloadState> _downloadState = new MutableLiveData<>(DownloadState.idle());
    /** Observable LiveData for the current download state. */
    public final LiveData<DownloadState> downloadState = _downloadState;

    /**
     * Constructs a new ModelManagementViewModel.
     *
     * @param application The application context.
     */
    public ModelManagementViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Starts the download and extraction of the model at the specified index.
     * If a download is already in progress, this method does nothing.
     *
     * @param index The index of the model in ModelManager.SUPPORTED_MODELS.
     */
    public void startDownload(int index) {
        if (_downloadState.getValue() != null && 
            (_downloadState.getValue().status == DownloadState.Status.DOWNLOADING || 
             _downloadState.getValue().status == DownloadState.Status.EXTRACTING)) {
            return;
        }

        ModelInfo info = ModelManager.SUPPORTED_MODELS[index];
        _downloadState.setValue(DownloadState.downloading(0));

        File filesDir = getApplication().getFilesDir();
        modelDownloader.downloadAndExtract(info.url, filesDir, new ModelDownloader.Callback() {
            @Override
            public void onProgress(int percentage) {
                _downloadState.postValue(DownloadState.downloading(percentage));
            }

            @Override
            public void onExtracting() {
                _downloadState.postValue(DownloadState.extracting());
            }

            @Override
            public void onSuccess() {
                _downloadState.postValue(DownloadState.success());
            }

            @Override
            public void onError(Exception e) {
                _downloadState.postValue(DownloadState.error(e));
            }

            @Override
            public void onCancelled() {
                _downloadState.postValue(DownloadState.idle());
            }
        });
    }

    /**
     * Cancels any ongoing download when the ViewModel is cleared.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        modelDownloader.cancel();
    }

    /**
     * Resets the download state to IDLE.
     */
    public void resetState() {
        _downloadState.setValue(DownloadState.idle());
    }
}
