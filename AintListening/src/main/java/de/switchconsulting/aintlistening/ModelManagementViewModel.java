package de.switchconsulting.aintlistening;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.File;

public class ModelManagementViewModel extends AndroidViewModel {

    private final ModelDownloader modelDownloader = new ModelDownloader();
    private final MutableLiveData<DownloadState> _downloadState = new MutableLiveData<>(DownloadState.idle());
    public final LiveData<DownloadState> downloadState = _downloadState;

    public ModelManagementViewModel(@NonNull Application application) {
        super(application);
    }

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

    @Override
    protected void onCleared() {
        super.onCleared();
        modelDownloader.cancel();
    }

    public void resetState() {
        _downloadState.setValue(DownloadState.idle());
    }
}
