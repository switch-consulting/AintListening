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

package de.switchconsulting.aintlistening.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a transcription session.
 */
@Entity(tableName = "transcriptions")
public class TranscriptionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;

    public String localeTag;

    /**
     * Constructs a new TranscriptionEntity.
     *
     * @param timestamp The creation timestamp.
     * @param localeTag The language locale tag.
     */
    public TranscriptionEntity(long timestamp, String localeTag) {
        this.timestamp = timestamp;
        this.localeTag = localeTag;
    }
}
