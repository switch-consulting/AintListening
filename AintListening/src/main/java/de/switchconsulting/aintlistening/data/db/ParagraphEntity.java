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
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a transcribed paragraph within a session.
 */
@Entity(
        tableName = "paragraphs",
        foreignKeys = @ForeignKey(
                entity = TranscriptionEntity.class,
                parentColumns = "id",
                childColumns = "transcriptionId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("transcriptionId")}
)
public class ParagraphEntity {

    /** Primary key for the paragraph entity. */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Foreign key referencing the parent transcription session ID. */
    public long transcriptionId;

    /** The raw transcribed text. */
    public String rawText;

    /** The smart-formatted text, or null if not formatted. */
    public String formattedText;

    /** Whether the formatted text should be displayed by default. */
    public boolean showFormatted;

    /** The absolute path to the audio file chunk for this paragraph. */
    public String audioFilePath;

    /**
     * Constructs a new ParagraphEntity.
     *
     * @param transcriptionId The parent transcription session ID.
     * @param rawText         The raw transcribed text.
     * @param formattedText   The formatted text, if any.
     * @param showFormatted   Whether formatted text should be shown.
     * @param audioFilePath   The path to the associated audio file chunk.
     */
    public ParagraphEntity(long transcriptionId, String rawText, String formattedText, boolean showFormatted, String audioFilePath) {
        this.transcriptionId = transcriptionId;
        this.rawText = rawText;
        this.formattedText = formattedText;
        this.showFormatted = showFormatted;
        this.audioFilePath = audioFilePath;
    }
}
