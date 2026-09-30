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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelCatalogRepository;
import de.switchconsulting.aintlistening.data.PreferencesDataSource;
import de.switchconsulting.aintlistening.transcription.TranscriberRegistry;

/**
 * A RecyclerView adapter for displaying language-grouped speech models and their current status.
 * It provides buttons for downloading or deleting transcription and formatting models.
 */
public class ModelAdapter extends RecyclerView.Adapter<ModelViewHolder> {

    private final List<LanguageSupport> languages;
    private final ModelCatalogRepository modelRepository;
    private final TranscriberRegistry transcriberRegistry;
    private final PreferencesDataSource preferencesDataSource;
    private final ModelInteractionListener listener;
    private boolean isBusy = false;

    /**
     * Constructs a new ModelAdapter.
     *
     * @param languages             The list of supported languages to display.
     * @param modelRepository       The repository for model metadata and disk checks.
     * @param transcriberRegistry   The registry for transcription engines.
     * @param preferencesDataSource The preferences data source.
     * @param listener              The listener for interaction events.
     */
    public ModelAdapter(List<LanguageSupport> languages,
                        ModelCatalogRepository modelRepository,
                        TranscriberRegistry transcriberRegistry,
                        PreferencesDataSource preferencesDataSource,
                        ModelInteractionListener listener) {
        this.languages = languages;
        this.modelRepository = modelRepository;
        this.transcriberRegistry = transcriberRegistry;
        this.preferencesDataSource = preferencesDataSource;
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
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language_support, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        LanguageSupport language = languages.get(position);
        holder.bind(language, modelRepository, transcriberRegistry, preferencesDataSource, isBusy, listener);
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }
}
