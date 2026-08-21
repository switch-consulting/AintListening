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
