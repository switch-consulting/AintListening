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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
    private final Persistency persistency;

    public TranscriptionAdapter(Persistency persistency) {
        this.persistency = persistency;
    }

    /**
     * Updates the list of paragraphs displayed by the adapter using DiffUtil for efficient updates.
     *
     * @param newParagraphs The new list of transcription paragraphs.
     */
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

    /**
     * Toggles audio playback for the paragraph at the specified position.
     *
     * @param position   The position of the paragraph in the list.
     * @param playButton The button that triggered the playback toggle.
     */
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

    /**
     * Starts audio playback for the paragraph at the specified position.
     *
     * @param position   The position of the paragraph in the list.
     * @param playButton The button that triggered the playback.
     */
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

    /**
     * Stops current audio playback and resets the playback state.
     */
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
        private final MaterialButton btnCopy;
        private final MaterialButtonToggleGroup toggleGroup;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.paragraphText);
            btnPlay = view.findViewById(R.id.btnPlay);
            btnRaw = view.findViewById(R.id.btnRaw);
            btnSmart = view.findViewById(R.id.btnSmart);
            btnCopy = view.findViewById(R.id.btnCopy);
            toggleGroup = view.findViewById(R.id.toggleGroup);
        }

        void bind(TranscriptionParagraph paragraph, int position) {
            textView.setText(paragraph.getDisplayText());

            boolean showPlayback = persistency == null || persistency.isShowPlaybackButton();
            boolean showCopy = persistency == null || persistency.isShowCopyButton();

            // Play button handling
            btnPlay.setVisibility(showPlayback ? View.VISIBLE : View.GONE);
            if (showPlayback) {
                if (paragraph.getAudioFilePath() == null) {
                    btnPlay.setEnabled(false);
                    btnPlay.setIconResource(R.drawable.ic_play_arrow);
                    btnPlay.setAlpha(0.38f); // Standard disabled alpha
                } else {
                    btnPlay.setEnabled(true);
                    btnPlay.setAlpha(1.0f);
                    boolean isCurrent = (currentlyPlayingPosition == position);
                    boolean isPlaying = isCurrent && mediaPlayer != null && mediaPlayer.isPlaying();
                    btnPlay.setIconResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
                    btnPlay.setOnClickListener(v -> togglePlayback(position, btnPlay));
                }
            }

            // Copy button handling
            btnCopy.setVisibility(showCopy ? View.VISIBLE : View.GONE);
            if (showCopy) {
                btnCopy.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Transcription", paragraph.getDisplayText());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(v.getContext(), R.string.message_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Text toggle handling
            boolean showRaw = persistency == null || persistency.isShowRawText();
            boolean showSmart = persistency == null || persistency.isShowSmartText();

            boolean hasFormattedText = paragraph.getFormattedText() != null && !paragraph.getFormattedText().isEmpty();

            // Force state if one is hidden
            if (!showRaw) {
                paragraph.setShowFormatted(true);
            } else if (!showSmart || !hasFormattedText) {
                paragraph.setShowFormatted(false);
            }

            textView.setText(paragraph.getDisplayText());

            btnRaw.setVisibility(showRaw ? View.VISIBLE : View.GONE);
            btnSmart.setVisibility(showSmart ? View.VISIBLE : View.GONE);

            btnRaw.setEnabled(true); // Raw is always available
            btnSmart.setEnabled(hasFormattedText);
            btnSmart.setAlpha(hasFormattedText ? 1.0f : 0.38f);

            toggleGroup.clearOnButtonCheckedListeners();
            toggleGroup.check(paragraph.isShowFormatted() && hasFormattedText ? R.id.btnSmart : R.id.btnRaw);

            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    boolean showFormatted = (checkedId == R.id.btnSmart);

                    // Only allow toggling to Smart if it's available
                    if (showFormatted && !hasFormattedText) {
                        group.check(R.id.btnRaw);
                        return;
                    }

                    if (paragraph.isShowFormatted() != showFormatted) {
                        paragraph.setShowFormatted(showFormatted);
                        textView.setText(paragraph.getDisplayText());
                    }
                }
            });
        }
    }
}
