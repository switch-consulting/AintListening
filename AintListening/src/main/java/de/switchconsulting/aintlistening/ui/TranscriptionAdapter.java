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

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * Adapter for displaying transcription paragraphs in a RecyclerView using ListAdapter with DiffUtil.
 */
public class TranscriptionAdapter extends ListAdapter<TranscriptionParagraph, TranscriptionViewHolder> {

    private static final DiffUtil.ItemCallback<TranscriptionParagraph> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull TranscriptionParagraph oldItem, @NonNull TranscriptionParagraph newItem) {
                    return Objects.equals(oldItem.getRawText(), newItem.getRawText());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TranscriptionParagraph oldItem, @NonNull TranscriptionParagraph newItem) {
                    return Objects.equals(oldItem.getRawText(), newItem.getRawText()) &&
                            Objects.equals(oldItem.getFormattedText(), newItem.getFormattedText()) &&
                            Objects.equals(oldItem.getAudioFilePath(), newItem.getAudioFilePath()) &&
                            oldItem.isShowFormatted() == newItem.isShowFormatted();
                }
            };

    /** The MediaPlayer instance used for paragraph audio playback. */
    MediaPlayer mediaPlayer;
    /** The list position of the paragraph currently playing audio, or -1 if none. */
    int currentlyPlayingPosition = -1;
    /** The UI display settings for configuring visibility options. */
    UiDisplaySettings displaySettings;

    /**
     * Constructs a new TranscriptionAdapter with specified display settings.
     *
     * @param displaySettings The display settings.
     */
    public TranscriptionAdapter(UiDisplaySettings displaySettings) {
        super(DIFF_CALLBACK);
        this.displaySettings = displaySettings != null ? displaySettings : UiDisplaySettings.defaultSettings();
    }

    /**
     * Updates the UI display settings and refreshes visible items.
     *
     * @param displaySettings The new display settings.
     */
    public void setDisplaySettings(UiDisplaySettings displaySettings) {
        this.displaySettings = displaySettings != null ? displaySettings : UiDisplaySettings.defaultSettings();
        notifyItemRangeChanged(0, getItemCount());
    }

    /**
     * Updates the list of paragraphs displayed by the adapter.
     *
     * @param newParagraphs The new list of transcription paragraphs.
     */
    public void setParagraphs(List<TranscriptionParagraph> newParagraphs) {
        submitList(newParagraphs);
    }

    /**
     * Releases resources, such as the MediaPlayer.
     */
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @NonNull
    @Override
    public TranscriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transcription_paragraph, parent, false);
        return new TranscriptionViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull TranscriptionViewHolder holder, int position) {
        TranscriptionParagraph paragraph = getItem(position);
        holder.bind(paragraph, position);
    }

    /**
     * Toggles audio playback for the paragraph at the specified position.
     *
     * @param position   The position of the paragraph in the list.
     * @param playButton The button that triggered the playback toggle.
     */
    void togglePlayback(int position, MaterialButton playButton) {
        if (currentlyPlayingPosition == position) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playButton.setIconResource(R.drawable.ic_play_arrow);
            } else if (mediaPlayer != null) {
                mediaPlayer.start();
                playButton.setIconResource(R.drawable.ic_pause);
            }
        } else {
            startPlayback(position, playButton);
        }
    }

    /**
     * Starts audio playback for the paragraph at the specified position.
     *
     * @param position   The position of the paragraph in the list.
     * @param playButton The button that triggered the playback.
     */
    void startPlayback(int position, MaterialButton playButton) {
        stopPlayback();

        TranscriptionParagraph paragraph = getItem(position);
        if (paragraph.getAudioFilePath() == null) return;

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(paragraph.getAudioFilePath());
            mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentlyPlayingPosition = position;
            notifyItemChanged(position);
        } catch (IOException e) {
            Toast.makeText(playButton.getContext(), "Playback failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stops current audio playback and resets the playback state.
     */
    void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        int oldPos = currentlyPlayingPosition;
        currentlyPlayingPosition = -1;
        if (oldPos != -1) {
            notifyItemChanged(oldPos);
        }
    }
}
