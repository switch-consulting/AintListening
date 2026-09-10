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

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter for displaying transcription paragraphs in a RecyclerView.
 * Allows toggling each paragraph between raw and smart formatted text.
 */
public class TranscriptionAdapter extends RecyclerView.Adapter<TranscriptionAdapter.ViewHolder> {

    private final List<TranscriptionParagraph> paragraphs = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private int currentlyPlayingPosition = -1;

    public void setParagraphs(List<TranscriptionParagraph> newParagraphs) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return paragraphs.size();
            }

            @Override
            public int getNewListSize() {
                return newParagraphs.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(paragraphs.get(oldItemPosition).getRawText(),
                        newParagraphs.get(newItemPosition).getRawText());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                TranscriptionParagraph oldItem = paragraphs.get(oldItemPosition);
                TranscriptionParagraph newItem = newParagraphs.get(newItemPosition);
                return Objects.equals(oldItem.getRawText(), newItem.getRawText()) &&
                        Objects.equals(oldItem.getFormattedText(), newItem.getFormattedText()) &&
                        Objects.equals(oldItem.getAudioFilePath(), newItem.getAudioFilePath()) &&
                        oldItem.isShowFormatted() == newItem.isShowFormatted();
            }
        });

        paragraphs.clear();
        paragraphs.addAll(newParagraphs);
        diffResult.dispatchUpdatesTo(this);
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transcription_paragraph, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranscriptionParagraph paragraph = paragraphs.get(position);
        holder.bind(paragraph, position);
    }

    @Override
    public int getItemCount() {
        return paragraphs.size();
    }

    private void togglePlayback(int position, MaterialButton playButton) {
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

    private void startPlayback(int position, MaterialButton playButton) {
        stopPlayback();

        TranscriptionParagraph paragraph = paragraphs.get(position);
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

    private void stopPlayback() {
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final MaterialButton btnPlay;
        private final MaterialButton btnRaw;
        private final MaterialButton btnSmart;
        private final MaterialButtonToggleGroup toggleGroup;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.paragraphText);
            btnPlay = view.findViewById(R.id.btnPlay);
            btnRaw = view.findViewById(R.id.btnRaw);
            btnSmart = view.findViewById(R.id.btnSmart);
            toggleGroup = view.findViewById(R.id.toggleGroup);
        }

        void bind(TranscriptionParagraph paragraph, int position) {
            textView.setText(paragraph.getDisplayText());

            if (paragraph.getAudioFilePath() == null) {
                btnPlay.setVisibility(View.GONE);
            } else {
                btnPlay.setVisibility(View.VISIBLE);
                boolean isCurrent = (currentlyPlayingPosition == position);
                boolean isPlaying = isCurrent && mediaPlayer != null && mediaPlayer.isPlaying();
                btnPlay.setIconResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
                btnPlay.setOnClickListener(v -> togglePlayback(position, btnPlay));
            }

            // If no formatted text is available, hide the toggle buttons
            if (paragraph.getFormattedText() == null || paragraph.getFormattedText().isEmpty()) {
                btnRaw.setVisibility(View.GONE);
                btnSmart.setVisibility(View.GONE);
            } else {
                btnRaw.setVisibility(View.VISIBLE);
                btnSmart.setVisibility(View.VISIBLE);
                
                toggleGroup.clearOnButtonCheckedListeners();
                toggleGroup.check(paragraph.isShowFormatted() ? R.id.btnSmart : R.id.btnRaw);
                
                toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                    if (isChecked) {
                        if (checkedId == R.id.btnPlay) {
                            group.uncheck(R.id.btnPlay);
                            return;
                        }
                        
                        boolean showFormatted = (checkedId == R.id.btnSmart);
                        if (showFormatted) {
                            group.uncheck(R.id.btnRaw);
                        } else {
                            group.uncheck(R.id.btnSmart);
                        }

                        if (paragraph.isShowFormatted() != showFormatted) {
                            paragraph.setShowFormatted(showFormatted);
                            textView.setText(paragraph.getDisplayText());
                        }
                    } else {
                        // Prevent unchecking the currently selected text mode
                        if (checkedId == R.id.btnRaw && !paragraph.isShowFormatted()) {
                            group.check(R.id.btnRaw);
                        } else if (checkedId == R.id.btnSmart && paragraph.isShowFormatted()) {
                            group.check(R.id.btnSmart);
                        }
                    }
                });
            }
        }
    }
}
