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

package de.switchconsulting.aintlistening.data;

/**
 * Callback interface for monitoring the download and extraction process.
 */
public interface ModelDownloadCallback {
    /**
     * Called when download progress is updated.
     *
     * @param percentage The current download percentage (0-100).
     */
    void onProgress(int percentage);

    /**
     * Called when the download is complete and extraction has started.
     */
    void onExtracting();

    /**
     * Called when the model has been successfully downloaded and extracted.
     */
    void onSuccess();

    /**
     * Called when an error occurs during download or extraction.
     *
     * @param e The exception that occurred.
     */
    void onError(Exception e);

    /**
     * Called if the operation was cancelled.
     */
    void onCancelled();
}
