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
 * Enumeration of possible statuses for a download operation.
 */
public enum DownloadStatus {
    /** No download operation is currently active. */
    IDLE,
    /** The model archive or file is actively downloading. */
    DOWNLOADING,
    /** The downloaded archive is being extracted. */
    EXTRACTING,
    /** The download and extraction process completed successfully. */
    SUCCESS,
    /** An error occurred during the download or extraction operation. */
    ERROR
}
