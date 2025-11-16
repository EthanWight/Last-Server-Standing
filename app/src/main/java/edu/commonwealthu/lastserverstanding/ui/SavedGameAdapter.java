package edu.commonwealthu.lastserverstanding.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;

/**
 * Adapter for displaying saved games in a RecyclerView
 */
public class SavedGameAdapter extends RecyclerView.Adapter<SavedGameAdapter.SavedGameViewHolder> {

    private List<SaveGameEntity> savedGames;
    private final OnSaveGameClickListener clickListener;

    public interface OnSaveGameClickListener {
        void onLoadSave(SaveGameEntity save);
        void onDeleteSave(SaveGameEntity save);
    }

    public SavedGameAdapter(OnSaveGameClickListener clickListener) {
        this.savedGames = new ArrayList<>();
        this.clickListener = clickListener;
    }

    public void setSavedGames(List<SaveGameEntity> newSavedGames) {
        List<SaveGameEntity> newList = newSavedGames != null ? newSavedGames : new ArrayList<>();

        // Use DiffUtil for efficient updates
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return savedGames.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // Items are the same if they have the same ID
                return savedGames.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                SaveGameEntity oldSave = savedGames.get(oldItemPosition);
                SaveGameEntity newSave = newList.get(newItemPosition);

                // Compare all relevant fields
                return oldSave.getId() == newSave.getId() &&
                       oldSave.getWave() == newSave.getWave() &&
                       oldSave.getScore() == newSave.getScore() &&
                       oldSave.getTimestamp() == newSave.getTimestamp() &&
                       oldSave.isAutoSave() == newSave.isAutoSave();
            }
        });

        this.savedGames = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public SavedGameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_game, parent, false);
        return new SavedGameViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedGameViewHolder holder, int position) {
        SaveGameEntity save = savedGames.get(position);
        holder.bind(save);
    }

    @Override
    public int getItemCount() {
        return savedGames.size();
    }

    public static class SavedGameViewHolder extends RecyclerView.ViewHolder {
        private final TextView waveText;
        private final TextView scoreText;
        private final TextView timestampText;
        private final TextView autosaveBadge;
        private final ImageButton deleteButton;
        private final OnSaveGameClickListener clickListener;

        public SavedGameViewHolder(@NonNull View itemView, OnSaveGameClickListener clickListener) {
            super(itemView);
            this.clickListener = clickListener;

            waveText = itemView.findViewById(R.id.text_wave);
            scoreText = itemView.findViewById(R.id.text_score);
            timestampText = itemView.findViewById(R.id.text_timestamp);
            autosaveBadge = itemView.findViewById(R.id.badge_autosave);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(SaveGameEntity save) {
            // Set wave
            waveText.setText(itemView.getContext().getString(R.string.wave_format, save.getWave()));

            // Set score
            scoreText.setText(itemView.getContext().getString(R.string.score_format, save.getScore()));

            // Set timestamp
            timestampText.setText(formatTimestamp(save.getTimestamp()));

            // Show/hide autosave badge
            autosaveBadge.setVisibility(save.isAutoSave() ? View.VISIBLE : View.GONE);

            // Set click listener for loading the save
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onLoadSave(save);
                }
            });

            // Set click listener for deleting the save
            deleteButton.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onDeleteSave(save);
                }
            });
        }

        private String formatTimestamp(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
