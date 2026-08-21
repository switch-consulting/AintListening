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

/**
 * Represents the current state of a model download and extraction process.
 * This class is immutable and contains information about the status, progress,
 * and any errors encountered during the operation.
 */
public class DownloadState {
    /**
     * Enumeration of possible statuses for a download operation.
     */
    public enum Status { IDLE, DOWNLOADING, EXTRACTING, SUCCESS, ERROR }

    /** The current status of the download operation. */
    public final Status status;
    /** The progress percentage (0-100) of the current operation. */
    public final int progress;
    /** The exception encountered if the status is ERROR. */
    public final Exception error;

    /**
     * Private constructor to create a new DownloadState.
     *
     * @param status   The status of the download.
     * @param progress The progress percentage.
     * @param error    The exception if an error occurred.
     */
    private DownloadState(Status status, int progress, Exception error) {
        this.status = status;
        this.progress = progress;
        this.error = error;
    }

    /**
     * Creates a DownloadState in the IDLE status.
     *
     * @return A new DownloadState with status IDLE.
     */
    public static DownloadState idle() { return new DownloadState(Status.IDLE, 0, null); }

    /**
     * Creates a DownloadState in the DOWNLOADING status with the given progress.
     *
     * @param progress The download progress percentage.
     * @return A new DownloadState with status DOWNLOADING.
     */
    public static DownloadState downloading(int progress) { return new DownloadState(Status.DOWNLOADING, progress, null); }

    /**
     * Creates a DownloadState in the EXTRACTING status.
     *
     * @return A new DownloadState with status EXTRACTING.
     */
    public static DownloadState extracting() { return new DownloadState(Status.EXTRACTING, 0, null); }

    /**
     * Creates a DownloadState in the SUCCESS status.
     *
     * @return A new DownloadState with status SUCCESS.
     */
    public static DownloadState success() { return new DownloadState(Status.SUCCESS, 100, null); }

    /**
     * Creates a DownloadState in the ERROR status with the given exception.
     *
     * @param e The exception that occurred.
     * @return A new DownloadState with status ERROR.
     */
    public static DownloadState error(Exception e) { return new DownloadState(Status.ERROR, 0, e); }
}
