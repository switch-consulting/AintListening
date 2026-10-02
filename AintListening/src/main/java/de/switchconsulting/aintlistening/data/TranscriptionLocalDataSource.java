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

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.switchconsulting.aintlistening.data.db.ParagraphEntity;
import de.switchconsulting.aintlistening.data.db.TranscriptionDao;
import de.switchconsulting.aintlistening.data.db.TranscriptionEntity;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Handles database persistence of transcription sessions and paragraphs using Room.
 */
@Singleton
public class TranscriptionLocalDataSource {

    private static final String TAG = "TranscriptionLocalDataSource";

    private final TranscriptionDao transcriptionDao;
    private final Object dbLock = new Object();

    /**
     * Constructs a new TranscriptionLocalDataSource instance.
     *
     * @param transcriptionDao The Room DAO for transcription operations.
     */
    @Inject
    public TranscriptionLocalDataSource(TranscriptionDao transcriptionDao) {
        this.transcriptionDao = transcriptionDao;
    }

    /**
     * Saves the last transcription paragraphs and the language locale used.
     *
     * @param paragraphs The list of transcription paragraphs to save.
     * @param locale     The locale of the model used for transcription.
     */
    public void saveLastMessage(List<TranscriptionParagraph> paragraphs, Locale locale) {
        synchronized (dbLock) {
            try {
                // Clear previous sessions or keep history; keeping history is supported by Room,
                // but for matching "last message" behavior, we insert a new session record.
                long sessionId = transcriptionDao.insertTranscription(
                        new TranscriptionEntity(
                                System.currentTimeMillis(),
                                locale != null ? locale.toLanguageTag() : null
                        )
                );

                if (paragraphs != null && !paragraphs.isEmpty()) {
                    List<ParagraphEntity> paragraphEntities = new ArrayList<>();
                    for (TranscriptionParagraph p : paragraphs) {
                        paragraphEntities.add(new ParagraphEntity(
                                sessionId,
                                p.getRawText(),
                                p.getFormattedText(),
                                p.isShowFormatted(),
                                p.getAudioFilePath()
                        ));
                    }
                    transcriptionDao.insertParagraphs(paragraphEntities);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save last message to Room database", e);
            }
        }
    }

    /**
     * Loads the last transcription paragraphs.
     *
     * @return The list of last saved transcription paragraphs, or null if none exist.
     */
    public List<TranscriptionParagraph> loadLastMessage() {
        synchronized (dbLock) {
            try {
                TranscriptionEntity latestSession = transcriptionDao.getLatestTranscriptionSession();
                if (latestSession == null) {
                    return null;
                }

                List<ParagraphEntity> paragraphEntities = transcriptionDao.getParagraphsForSession(latestSession.id);
                if (paragraphEntities == null || paragraphEntities.isEmpty()) {
                    return null;
                }

                List<TranscriptionParagraph> result = new ArrayList<>();
                for (ParagraphEntity entity : paragraphEntities) {
                    TranscriptionParagraph p = new TranscriptionParagraph(
                            entity.rawText,
                            entity.formattedText,
                            entity.audioFilePath
                    );
                    p.setShowFormatted(entity.showFormatted);
                    result.add(p);
                }
                return result;
            } catch (Exception e) {
                Log.e(TAG, "Failed to load last message from Room database", e);
                return null;
            }
        }
    }
}
