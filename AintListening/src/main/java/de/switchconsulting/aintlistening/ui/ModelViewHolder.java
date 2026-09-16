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

import de.switchconsulting.aintlistening.transcription.TranscriberType;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelInfo;
import de.switchconsulting.aintlistening.data.ModelManager;

/**
 * ViewHolder class for language group items in ModelAdapter.
 */
public class ModelViewHolder extends RecyclerView.ViewHolder {
    private final TextView languageNameText;
    private final View transcriberRow;
    private final View formattingRow;
    private final TextView formattingNotSupportedText;

    /**
     * Constructs a new ViewHolder.
     *
     * @param itemView The view for the list item.
     */
    public ModelViewHolder(@NonNull View itemView) {
        super(itemView);
        languageNameText = itemView.findViewById(R.id.languageNameText);
        transcriberRow = itemView.findViewById(R.id.transcriberModelRow);
        formattingRow = itemView.findViewById(R.id.formattingModelRow);
        formattingNotSupportedText = itemView.findViewById(R.id.formattingNotSupportedText);
    }

    /**
     * Binds language support data to the view.
     *
     * @param language   The language support information.
     * @param activeType The currently active transcriber type.
     * @param isBusy     Whether the adapter is currently busy.
     * @param listener   The listener for interaction events.
     */
    public void bind(LanguageSupport language, TranscriberType activeType, boolean isBusy, ModelInteractionListener listener) {
        languageNameText.setText(language.getLocale().getDisplayName());

        ModelInfo transcriptionInfo = (activeType == TranscriberType.VOSK) ? language.getVoskModel() : language.getWhisperModel();

        if (transcriptionInfo != null) {
            transcriberRow.setVisibility(View.VISIBLE);
            bindModelRow(transcriberRow, transcriptionInfo, isBusy, listener);
        } else {
            transcriberRow.setVisibility(View.GONE);
        }

        if (language.getFormattingModel() != null) {
            formattingRow.setVisibility(View.VISIBLE);
            formattingNotSupportedText.setVisibility(View.GONE);
            bindModelRow(formattingRow, language.getFormattingModel(), isBusy, listener);
        } else {
            formattingRow.setVisibility(View.GONE);
            formattingNotSupportedText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Binds model information to a specific row (transcription or formatting).
     *
     * @param rowView  The row view.
     * @param info     The model information.
     * @param isBusy   Whether the adapter is currently busy.
     * @param listener The listener for user interactions.
     */
    private void bindModelRow(View rowView, ModelInfo info, boolean isBusy, ModelInteractionListener listener) {
        Context context = rowView.getContext();
        ImageView icon = rowView.findViewById(R.id.modelStatusIcon);
        TextView nameText = rowView.findViewById(R.id.modelNameText);
        TextView statusText = rowView.findViewById(R.id.modelStatusText);
        MaterialButton downloadButton = rowView.findViewById(R.id.inlineDownloadButton);
        MaterialButton deleteButton = rowView.findViewById(R.id.inlineDeleteButton);

        nameText.setText(info.locale.getDisplayName());
        boolean isDownloaded = ModelManager.INSTANCE.isModelDownloaded(context, info);

        if (!isDownloaded) {
            icon.setImageResource(R.drawable.ic_error);
            icon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            statusText.setText(context.getString(R.string.status_unavailable_with_size, info.size));
            downloadButton.setVisibility(View.VISIBLE);
            downloadButton.setEnabled(!isBusy);
            downloadButton.setOnClickListener(v -> listener.onDownloadClicked(info));
            deleteButton.setVisibility(View.GONE);
        } else {
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            statusText.setText(R.string.status_available);
            downloadButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setEnabled(!isBusy);
            deleteButton.setOnClickListener(v -> listener.onDeleteClicked(info));
        }
    }
}
