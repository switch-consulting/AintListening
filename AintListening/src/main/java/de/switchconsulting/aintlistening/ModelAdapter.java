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

/**
 * A RecyclerView adapter for displaying a list of speech models and their current status (installed or not).
 * It provides buttons for downloading or deleting models.
 */
public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {

    /**
     * Interface for handling interactions with model items in the list.
     */
    public interface InteractionListener {
        /**
         * Called when the download button is clicked for a model.
         *
         * @param index The index of the model in the list.
         */
        void onDownloadClicked(int index);

        /**
         * Called when the delete button is clicked for a model.
         *
         * @param index       The index of the model in the list.
         * @param displayName The display name of the model.
         */
        void onDeleteClicked(int index, String displayName);
    }

    private final List<ModelInfo> models;
    private final InteractionListener listener;
    private boolean isBusy = false;

    /**
     * Constructs a new ModelAdapter.
     *
     * @param models   The list of models to display.
     * @param listener The listener for interaction events.
     */
    public ModelAdapter(List<ModelInfo> models, InteractionListener listener) {
        this.models = models;
        this.listener = listener;
    }

    /**
     * Sets whether the adapter is in a busy state (e.g., during a download).
     * Disables or enables interaction buttons accordingly.
     *
     * @param busy True if the adapter should be busy, false otherwise.
     */
    public void setBusy(boolean busy) {
        if (this.isBusy != busy) {
            this.isBusy = busy;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    /**
     * Refreshes the entire list of models.
     */
    public void refresh() {
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_model_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelInfo model = models.get(position);
        holder.bind(model, position, isBusy, listener);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    /**
     * ViewHolder class for model list items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView nameText;
        private final TextView statusText;
        private final MaterialButton downloadButton;
        private final MaterialButton deleteButton;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The view for the list item.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.modelStatusIcon);
            nameText = itemView.findViewById(R.id.modelNameText);
            statusText = itemView.findViewById(R.id.modelStatusText);
            downloadButton = itemView.findViewById(R.id.inlineDownloadButton);
            deleteButton = itemView.findViewById(R.id.inlineDeleteButton);
        }

        /**
         * Binds a model's data to the view.
         *
         * @param info     The model information.
         * @param index    The index of the model in the list.
         * @param isBusy   Whether the adapter is currently busy.
         * @param listener The listener for interaction events.
         */
        public void bind(ModelInfo info, int index, boolean isBusy, InteractionListener listener) {
            Context context = itemView.getContext();
            nameText.setText(info.displayName);

            boolean isDownloaded = ModelManager.isModelDownloaded(context, index);

            if (!isDownloaded) {
                icon.setImageResource(R.drawable.ic_error);
                icon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                statusText.setText(context.getString(R.string.status_unavailable_with_size, info.size));
                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setEnabled(!isBusy);
                downloadButton.setOnClickListener(v -> listener.onDownloadClicked(index));
                deleteButton.setVisibility(View.GONE);
            } else {
                icon.setImageResource(R.drawable.ic_check_circle);
                icon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                statusText.setText(R.string.status_available);
                downloadButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setEnabled(!isBusy);
                deleteButton.setOnClickListener(v -> listener.onDeleteClicked(index, info.displayName));
            }
        }
    }
}
