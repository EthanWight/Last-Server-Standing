package edu.commonwealthu.lastserverstanding.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;

import edu.commonwealthu.lastserverstanding.R;

/**
 * Adapter for displaying tower options in the bottom sheet
 */
public class TowerAdapter extends RecyclerView.Adapter<TowerAdapter.TowerViewHolder> {

    private final List<TowerOption> towerOptions;
    private final OnTowerSelectedListener listener;
    private int currentResources;

    public interface OnTowerSelectedListener {
        void onTowerSelected(TowerOption tower);
        void onTowerInfoClicked(TowerOption tower);
    }

    public TowerAdapter(List<TowerOption> towerOptions, OnTowerSelectedListener listener) {
        this.towerOptions = towerOptions;
        this.listener = listener;
        this.currentResources = 0;
    }

    public void updateResources(int resources) {
        this.currentResources = resources;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TowerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tower_card, parent, false);
        return new TowerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TowerViewHolder holder, int position) {
        TowerOption tower = towerOptions.get(position);
        holder.bind(tower);
    }

    @Override
    public int getItemCount() {
        return towerOptions.size();
    }

    class TowerViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final ImageView icon;
        private final TextView name;
        private final TextView cost;
        private final Chip damageChip;
        private final Chip rangeChip;
        private final Chip fireRateChip;
        private final ImageButton infoButton;
        private final FrameLayout lockOverlay;

        TowerViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            icon = itemView.findViewById(R.id.tower_icon);
            name = itemView.findViewById(R.id.tower_name);
            cost = itemView.findViewById(R.id.tower_cost);
            damageChip = itemView.findViewById(R.id.chip_damage);
            rangeChip = itemView.findViewById(R.id.chip_range);
            fireRateChip = itemView.findViewById(R.id.chip_fire_rate);
            infoButton = itemView.findViewById(R.id.btn_info);
            lockOverlay = itemView.findViewById(R.id.lock_overlay);
        }

        void bind(TowerOption tower) {
            // Set tower info
            icon.setImageResource(tower.getIconResId());
            name.setText(tower.getName());
            cost.setText(String.valueOf(tower.getCost()));
            damageChip.setText(String.format("%.0f", tower.getDamage()));
            rangeChip.setText(String.format("%.0f", tower.getRange()));
            fireRateChip.setText(String.format("%.1f", tower.getFireRate()));

            // Show/hide lock overlay
            lockOverlay.setVisibility(tower.isLocked() ? View.VISIBLE : View.GONE);

            // Determine if tower is affordable
            boolean canAfford = currentResources >= tower.getCost();
            boolean enabled = !tower.isLocked() && canAfford;

            // Update card appearance
            card.setEnabled(enabled);
            card.setAlpha(enabled ? 1.0f : 0.5f);

            // Set click listeners
            if (enabled) {
                card.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTowerSelected(tower);
                    }
                });
            } else {
                card.setOnClickListener(null);
            }

            infoButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTowerInfoClicked(tower);
                }
            });
        }
    }
}
