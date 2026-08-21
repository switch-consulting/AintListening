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

public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ViewHolder> {

    public interface InteractionListener {
        void onDownloadClicked(int index);
        void onDeleteClicked(int index, String displayName);
    }

    private final List<ModelInfo> models;
    private final InteractionListener listener;
    private boolean isBusy = false;

    public ModelAdapter(List<ModelInfo> models, InteractionListener listener) {
        this.models = models;
        this.listener = listener;
    }

    public void setBusy(boolean busy) {
        if (this.isBusy != busy) {
            this.isBusy = busy;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView nameText;
        private final TextView statusText;
        private final MaterialButton downloadButton;
        private final MaterialButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.modelStatusIcon);
            nameText = itemView.findViewById(R.id.modelNameText);
            statusText = itemView.findViewById(R.id.modelStatusText);
            downloadButton = itemView.findViewById(R.id.inlineDownloadButton);
            deleteButton = itemView.findViewById(R.id.inlineDeleteButton);
        }

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
