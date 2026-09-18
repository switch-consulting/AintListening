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

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import de.switchconsulting.aintlistening.data.DownloadState;
import de.switchconsulting.aintlistening.data.DownloadStatus;
import de.switchconsulting.aintlistening.data.ModelDownloadCallback;
import de.switchconsulting.aintlistening.data.ModelDownloader;
import de.switchconsulting.aintlistening.data.ModelInfo;

/**
 * ViewModel for managing the download and extraction of speech models.
 * Maintains the current download state and handles background operations.
 */
@HiltViewModel
public class ModelManagementViewModel extends AndroidViewModel {

    /** The model downloader responsible for fetching and extracting speech models. */
    private final ModelDownloader modelDownloader = new ModelDownloader();
    /** Mutable LiveData representing the internal download state. */
    private final MutableLiveData<DownloadState> downloadStateMutable = new MutableLiveData<>(DownloadState.idle());
    /** Observable LiveData for the current download state. */
    public final LiveData<DownloadState> downloadState = downloadStateMutable;

    /**
     * Constructs a new ModelManagementViewModel.
     *
     * @param application The application context.
     */
    @Inject
    public ModelManagementViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Starts the download process for a given model.
     *
     * @param info The model information.
     */
    public void startDownload(ModelInfo info) {
        if (downloadStateMutable.getValue() != null && 
            (downloadStateMutable.getValue().status == DownloadStatus.DOWNLOADING || 
             downloadStateMutable.getValue().status == DownloadStatus.EXTRACTING)) {
            return;
        }

        if (info == null) return;
        
        downloadStateMutable.setValue(DownloadState.downloading(0));

        File filesDir = getApplication().getFilesDir();
        modelDownloader.downloadAndExtract(info, filesDir, new ModelDownloadCallback() {
            @Override
            public void onProgress(int percentage) {
                downloadStateMutable.postValue(DownloadState.downloading(percentage));
            }

            @Override
            public void onExtracting() {
                downloadStateMutable.postValue(DownloadState.extracting());
            }

            @Override
            public void onSuccess() {
                downloadStateMutable.postValue(DownloadState.success());
            }

            @Override
            public void onError(Exception e) {
                downloadStateMutable.postValue(DownloadState.error(e));
            }

            @Override
            public void onCancelled() {
                downloadStateMutable.postValue(DownloadState.idle());
            }
        });
    }

    /**
     * Cancels any ongoing download when the ViewModel is cleared.
     */
    @Override
    protected void onCleared() {
        modelDownloader.cancel();
    }

    /**
     * Resets the download state to idle.
     */
    public void resetState() {
        downloadStateMutable.setValue(DownloadState.idle());
    }
}
