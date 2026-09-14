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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.transcription.TranscriptionParagraph;

/**
 * ViewHolder class for transcription paragraph items in TranscriptionAdapter.
 */
public class TranscriptionViewHolder extends RecyclerView.ViewHolder {
    private final TextView textView;
    private final MaterialButton btnPlay;
    private final MaterialButton btnRaw;
    private final MaterialButton btnSmart;
    private final MaterialButton btnCopy;
    private final MaterialButtonToggleGroup toggleGroup;
    private final TranscriptionAdapter adapter;

    public TranscriptionViewHolder(View view, TranscriptionAdapter adapter) {
        super(view);
        this.textView = view.findViewById(R.id.paragraphText);
        this.btnPlay = view.findViewById(R.id.btnPlay);
        this.btnRaw = view.findViewById(R.id.btnRaw);
        this.btnSmart = view.findViewById(R.id.btnSmart);
        this.btnCopy = view.findViewById(R.id.btnCopy);
        this.toggleGroup = view.findViewById(R.id.toggleGroup);
        this.adapter = adapter;
    }

    void bind(TranscriptionParagraph paragraph, int position) {
        textView.setText(paragraph.getDisplayText());

        boolean showPlayback = adapter.persistency == null || adapter.persistency.isShowPlaybackButton();
        boolean showCopy = adapter.persistency == null || adapter.persistency.isShowCopyButton();

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
                boolean isCurrent = (adapter.currentlyPlayingPosition == position);
                boolean isPlaying = isCurrent && adapter.mediaPlayer != null && adapter.mediaPlayer.isPlaying();
                btnPlay.setIconResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
                btnPlay.setOnClickListener(v -> adapter.togglePlayback(position, btnPlay));
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
        boolean showRaw = adapter.persistency == null || adapter.persistency.isShowRawText();
        boolean showSmart = adapter.persistency == null || adapter.persistency.isShowSmartText();

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
