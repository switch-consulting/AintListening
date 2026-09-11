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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelInfo;
import de.switchconsulting.aintlistening.data.ModelManager;

/**
 * A RecyclerView adapter for displaying language-grouped speech models and their current status.
 * It provides buttons for downloading or deleting transcription and formatting models.
 */
public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {

    /**
     * Interface for handling interactions with model items in the list.
     */
    public interface InteractionListener {
        /**
         * Called when the download button is clicked for a model.
         *
         * @param info The model information.
         */
        void onDownloadClicked(ModelInfo info);

        /**
         * Called when the delete button is clicked for a model.
         *
         * @param info The model information.
         */
        void onDeleteClicked(ModelInfo info);
    }

    private final List<LanguageSupport> languages;
    private final InteractionListener listener;
    private boolean isBusy = false;

    /**
     * Constructs a new ModelAdapter.
     *
     * @param languages The list of supported languages to display.
     * @param listener  The listener for interaction events.
     */
    public ModelAdapter(List<LanguageSupport> languages, InteractionListener listener) {
        this.languages = languages;
        this.listener = listener;
    }

    /**
     * Sets the busy state of the adapter. While busy, interaction elements are disabled.
     *
     * @param busy True if the adapter is busy (e.g., during a download).
     */
    public void setBusy(boolean busy) {
        if (this.isBusy != busy) {
            this.isBusy = busy;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    /**
     * Refreshes the adapter by notifying that the dataset has changed.
     */
    public void refresh() {
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language_support, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LanguageSupport language = languages.get(position);
        holder.bind(language, isBusy, listener);
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    /**
     * ViewHolder class for language group items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView languageNameText;
        private final View transcriberRow;
        private final View formattingRow;
        private final TextView formattingNotSupportedText;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The view for the list item.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            languageNameText = itemView.findViewById(R.id.languageNameText);
            transcriberRow = itemView.findViewById(R.id.transcriberModelRow);
            formattingRow = itemView.findViewById(R.id.formattingModelRow);
            formattingNotSupportedText = itemView.findViewById(R.id.formattingNotSupportedText);
        }

        /**
         * Binds language support data to the view.
         *
         * @param language The language support information.
         * @param isBusy   Whether the adapter is currently busy.
         * @param listener The listener for interaction events.
         */
        public void bind(LanguageSupport language, boolean isBusy, InteractionListener listener) {
            languageNameText.setText(language.getLocale().getDisplayName());

            bindModelRow(transcriberRow, language.getTranscriptionModel(), isBusy, listener);

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
        private void bindModelRow(View rowView, ModelInfo info, boolean isBusy, InteractionListener listener) {
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
}
