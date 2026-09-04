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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter for displaying transcription paragraphs in a RecyclerView.
 * Allows toggling each paragraph between raw and smart formatted text.
 */
public class TranscriptionAdapter extends RecyclerView.Adapter<TranscriptionAdapter.ViewHolder> {

    private final List<TranscriptionParagraph> paragraphs = new ArrayList<>();

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
                // For simplicity, we assume items are the same if their raw text is the same.
                // In a more complex app, we might use unique IDs.
                return Objects.equals(paragraphs.get(oldItemPosition).getRawText(),
                        newParagraphs.get(newItemPosition).getRawText());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                TranscriptionParagraph oldItem = paragraphs.get(oldItemPosition);
                TranscriptionParagraph newItem = newParagraphs.get(newItemPosition);
                return Objects.equals(oldItem.getRawText(), newItem.getRawText()) &&
                        Objects.equals(oldItem.getFormattedText(), newItem.getFormattedText()) &&
                        oldItem.isShowFormatted() == newItem.isShowFormatted();
            }
        });

        paragraphs.clear();
        paragraphs.addAll(newParagraphs);
        diffResult.dispatchUpdatesTo(this);
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
        holder.bind(paragraph);
    }

    @Override
    public int getItemCount() {
        return paragraphs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final MaterialButtonToggleGroup toggleGroup;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.paragraphText);
            toggleGroup = view.findViewById(R.id.toggleGroup);
        }

        void bind(TranscriptionParagraph paragraph) {
            textView.setText(paragraph.getDisplayText());

            // If no formatted text is available, hide the toggle group
            if (paragraph.getFormattedText() == null || paragraph.getFormattedText().isEmpty()) {
                toggleGroup.setVisibility(View.GONE);
            } else {
                toggleGroup.setVisibility(View.VISIBLE);
                
                // Set the selection without triggering the listener
                toggleGroup.clearOnButtonCheckedListeners();
                toggleGroup.check(paragraph.isShowFormatted() ? R.id.btnSmart : R.id.btnRaw);
                
                toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                    if (isChecked) {
                        boolean showFormatted = (checkedId == R.id.btnSmart);
                        if (paragraph.isShowFormatted() != showFormatted) {
                            paragraph.setShowFormatted(showFormatted);
                            textView.setText(paragraph.getDisplayText());
                        }
                    }
                });
            }
        }
    }
}
