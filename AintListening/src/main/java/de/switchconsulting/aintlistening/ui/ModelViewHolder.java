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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;

import de.switchconsulting.aintlistening.R;
import de.switchconsulting.aintlistening.data.LanguageSupport;
import de.switchconsulting.aintlistening.data.ModelInfo;
import de.switchconsulting.aintlistening.data.ModelManager;
import de.switchconsulting.aintlistening.data.Persistency;
import de.switchconsulting.aintlistening.transcription.Transcriber;
import de.switchconsulting.aintlistening.transcription.TranscriberRegistry;
import de.switchconsulting.aintlistening.transcription.TranscriberType;

/**
 * ViewHolder class for language group items in ModelAdapter.
 */
public class ModelViewHolder extends RecyclerView.ViewHolder {
    private final TextView languageNameText;
    private final MaterialSwitch languageEnabledSwitch;
    private final ViewGroup transcriberModelsContainer;
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
        languageEnabledSwitch = itemView.findViewById(R.id.languageEnabledSwitch);
        transcriberModelsContainer = itemView.findViewById(R.id.transcriberModelsContainer);
        formattingRow = itemView.findViewById(R.id.formattingModelRow);
        formattingNotSupportedText = itemView.findViewById(R.id.formattingNotSupportedText);
    }

    /**
     * Binds language support data to the view.
     *
     * @param language            The language support information.
     * @param transcriberRegistry The registry for transcription engines.
     * @param isBusy              Whether the adapter is currently busy.
     * @param listener            The listener for interaction events.
     */
    public void bind(LanguageSupport language, TranscriberRegistry transcriberRegistry, boolean isBusy, ModelInteractionListener listener) {
        languageNameText.setText(language.getLocale().getDisplayName());

        Context context = itemView.getContext();
        Persistency persistency = new Persistency(context);
        boolean isEnabled = persistency.isLanguageEnabled(language.getLocale());
        boolean hasTranscription = language.hasTranscriptionModelDownloaded(context);
        TranscriberType activeType = language.getActiveTranscriberType(context, persistency);

        languageEnabledSwitch.setOnCheckedChangeListener(null);
        languageEnabledSwitch.setChecked(isEnabled && hasTranscription);
        languageEnabledSwitch.setEnabled(hasTranscription && !isBusy);
        languageEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onLanguageEnabledChanged(language, isChecked));

        // Optimize: Reuse existing views to prevent "flashing" during re-bind
        int childCount = transcriberModelsContainer.getChildCount();
        int index = 0;
        LayoutInflater inflater = LayoutInflater.from(context);

        for (ModelInfo transcriptionInfo : language.getTranscriptionModels()) {
            View row;
            if (index < childCount) {
                row = transcriberModelsContainer.getChildAt(index);
            } else {
                row = inflater.inflate(R.layout.item_model_status, transcriberModelsContainer, false);
                transcriberModelsContainer.addView(row);
            }
            bindModelRow(language, row, transcriptionInfo, activeType, true, isBusy, transcriberRegistry, listener);
            index++;
        }

        // Remove any excess views if the model count changed (unlikely in this app)
        while (transcriberModelsContainer.getChildCount() > index) {
            transcriberModelsContainer.removeViewAt(index);
        }

        if (language.getFormattingModel() != null) {
            formattingRow.setVisibility(View.VISIBLE);
            formattingNotSupportedText.setVisibility(View.GONE);
            bindModelRow(language, formattingRow, language.getFormattingModel(), activeType, false, isBusy, transcriberRegistry, listener);
        } else {
            formattingRow.setVisibility(View.GONE);
            formattingNotSupportedText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Binds model information to a specific row (transcription or formatting).
     *
     * @param language            The language support information.
     * @param rowView             The row view.
     * @param info                The model information.
     * @param activeType          The currently active transcriber type for this language.
     * @param isTranscriber       Whether this row is for a transcriber (vs formatting).
     * @param isBusy              Whether the adapter is currently busy.
     * @param transcriberRegistry The registry for transcription engines.
     * @param listener            The listener for user interactions.
     */
    private void bindModelRow(LanguageSupport language, View rowView, ModelInfo info, TranscriberType activeType, boolean isTranscriber, boolean isBusy, TranscriberRegistry transcriberRegistry, ModelInteractionListener listener) {
        Context context = rowView.getContext();
        ImageView icon = rowView.findViewById(R.id.modelStatusIcon);
        MaterialRadioButton radioButton = rowView.findViewById(R.id.modelSelectedRadio);
        MaterialCheckBox checkBox = rowView.findViewById(R.id.modelSelectedCheck);
        TextView nameText = rowView.findViewById(R.id.modelNameText);
        TextView statusText = rowView.findViewById(R.id.modelStatusText);
        MaterialButton downloadButton = rowView.findViewById(R.id.inlineDownloadButton);
        MaterialButton deleteButton = rowView.findViewById(R.id.inlineDeleteButton);

        Persistency persistency = new Persistency(context);
        boolean isDownloaded = ModelManager.INSTANCE.isModelDownloaded(context, info);

        if (isTranscriber) {
            nameText.setText(ModelManager.getEngineNameResId(info.type()));
            radioButton.setVisibility(View.VISIBLE);
            checkBox.setVisibility(View.GONE);

            radioButton.setOnCheckedChangeListener(null);
            radioButton.setChecked(info.type() == activeType);
            radioButton.setEnabled(!isBusy && isDownloaded);
            radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    listener.onTranscriberSelected(language, info.type());
                }
            });
        } else {
            nameText.setText(info.locale().getDisplayName());
            radioButton.setVisibility(View.GONE);
            checkBox.setVisibility(View.VISIBLE);

            Transcriber engine = transcriberRegistry.getTranscriber(activeType);
            boolean isPunctuationProvidedByEngine = engine != null && engine.providesPunctuation();
            boolean isEnabled = persistency.isSmartFormattingEnabled(language.getLocale());

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(isEnabled && !isPunctuationProvidedByEngine);
            checkBox.setEnabled(!isBusy && isDownloaded && !isPunctuationProvidedByEngine);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onSmartFormattingToggled(language, isChecked));
        }

        if (!isDownloaded) {
            icon.setImageResource(R.drawable.ic_error);
            icon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            statusText.setText(context.getString(R.string.status_unavailable_with_size, info.size()));
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
