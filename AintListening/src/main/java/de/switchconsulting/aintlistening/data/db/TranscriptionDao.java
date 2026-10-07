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

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for Room transcription database operations.
 */
@Dao
public interface TranscriptionDao {

    /**
     * Returns the most recent transcription session entity that contains paragraphs.
     *
     * @return The most recent transcription session entity that contains paragraphs, or null if none exist.
     */
    @Query("SELECT * FROM transcriptions WHERE id IN (SELECT DISTINCT transcriptionId FROM paragraphs) ORDER BY timestamp DESC, id DESC LIMIT 1")
    TranscriptionEntity getLatestTranscriptionSession();

    /**
     * Returns the list of paragraphs associated with the given session ID.
     *
     * @param transcriptionId The parent transcription session ID.
     * @return List of paragraphs associated with the session.
     */
    @Query("SELECT * FROM paragraphs WHERE transcriptionId = :transcriptionId")
    List<ParagraphEntity> getParagraphsForSession(long transcriptionId);

    /**
     * Inserts a new transcription session.
     *
     * @param transcription The transcription entity to insert.
     * @return The generated row ID.
     */
    @Insert
    long insertTranscription(TranscriptionEntity transcription);

    /**
     * Inserts a list of paragraph entities.
     *
     * @param paragraphs The paragraphs to insert.
     */
    @Insert
    void insertParagraphs(List<ParagraphEntity> paragraphs);
}
