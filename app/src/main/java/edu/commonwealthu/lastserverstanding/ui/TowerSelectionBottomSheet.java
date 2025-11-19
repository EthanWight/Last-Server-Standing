package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.commonwealthu.lastserverstanding.R;

/**
 * Bottom Sheet for tower selection and purchase.
 * Displays available towers in a 2-column grid layout with stats and pricing.
 *
 * <p>Usage example:
 * <pre>
 * TowerSelectionBottomSheet bottomSheet = TowerSelectionBottomSheet.newInstance(
 *     towerInfo -> {
 *         // Handle tower selection
 *         placeTower(towerInfo.getName(), towerInfo.getCost());
 *     }
 * );
 * bottomSheet.show(getSupportFragmentManager(), "tower_selection");
 * </pre>
 */
public class TowerSelectionBottomSheet extends BottomSheetDialogFragment {

    private TowerSelectionListener selectionListener;

    /**
     * Listener interface for tower selection events.
     */
    public interface TowerSelectionListener {
        /**
         * Called when a tower is selected from the bottom sheet.
         *
         * @param towerInfo Information about the selected tower
         */
        void onTowerSelected(TowerInfo towerInfo);
    }

    /**
     * Creates a new instance of TowerSelectionBottomSheet with a selection listener.
     *
     * @param listener The listener to be notified when a tower is selected
     * @return A new instance of TowerSelectionBottomSheet
     */
    @SuppressWarnings("unused")
    public static TowerSelectionBottomSheet newInstance(TowerSelectionListener listener) {
        TowerSelectionBottomSheet sheet = new TowerSelectionBottomSheet();
        sheet.setSelectionListener(listener);
        return sheet;
    }

    /**
     * Sets the tower selection listener.
     *
     * @param listener The listener to be notified when a tower is selected
     */
    public void setSelectionListener(TowerSelectionListener listener) {
        this.selectionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_tower_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView towerRecyclerView = view.findViewById(R.id.tower_recycler_view);
        towerRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        List<TowerInfo> towers = createTowerList();
        TowerAdapter towerAdapter = new TowerAdapter(towers, this::onTowerClicked);
        towerRecyclerView.setAdapter(towerAdapter);
    }

    /**
     * Creates the list of available towers with their stats and pricing.
     *
     * @return List of tower information
     */
    private List<TowerInfo> createTowerList() {
        List<TowerInfo> towers = new ArrayList<>();

        towers.add(new TowerInfo(
                "Firewall",
                "Basic defense tower with high fire rate",
                R.drawable.ic_tower_firewall,
                100,
                15f,
                150f,
                2.0f,
                true
        ));

        towers.add(new TowerInfo(
                "Honeypot",
                "Lures and distracts enemies",
                R.drawable.ic_tower_honeypot,
                150,
                0f,
                200f,
                0f,
                false
        ));

        towers.add(new TowerInfo(
                "Neural Jammer",
                "Slows enemy processing speed",
                R.drawable.ic_tower_jammer,
                200,
                10f,
                175f,
                1.5f,
                false
        ));

        return towers;
    }

    /**
     * Handles tower click events. Validates unlock status and notifies listener.
     *
     * @param towerInfo The tower that was clicked
     */
    private void onTowerClicked(TowerInfo towerInfo) {
        if (!towerInfo.isUnlocked) {
            if (getContext() != null) {
                Toast.makeText(getContext(),
                        towerInfo.name + " is locked!",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (selectionListener != null) {
            selectionListener.onTowerSelected(towerInfo);
        }

        dismiss();
    }

    /**
     * Data class containing tower information for display and purchase.
     *
     * @param name Tower name
     * @param description Tower description
     * @param iconResId Resource ID for tower icon
     * @param cost Purchase cost in resources
     * @param damage Damage per hit
     * @param range Attack range
     * @param fireRate Attacks per second
     * @param isUnlocked Whether the tower is unlocked for purchase
     */
    @SuppressWarnings("unused")
    public record TowerInfo(
            String name,
            String description,
            int iconResId,
            int cost,
            float damage,
            float range,
            float fireRate,
            boolean isUnlocked
    ) {
        // Getter methods with standard naming convention for external API compatibility
        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getIconResId() {
            return iconResId;
        }

        public int getCost() {
            return cost;
        }

        public float getDamage() {
            return damage;
        }

        public float getRange() {
            return range;
        }

        public float getFireRate() {
            return fireRate;
        }

        public boolean isUnlocked() {
            return isUnlocked;
        }
    }

    /**
     * RecyclerView Adapter for displaying towers in a grid.
     */
    static class TowerAdapter extends RecyclerView.Adapter<TowerAdapter.TowerViewHolder> {

        private final List<TowerInfo> towers;
        private final OnTowerClickListener clickListener;

        interface OnTowerClickListener {
            void onTowerClick(TowerInfo tower);
        }

        TowerAdapter(List<TowerInfo> towers, OnTowerClickListener listener) {
            this.towers = towers;
            this.clickListener = listener;
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
            TowerInfo tower = towers.get(position);
            holder.bind(tower, clickListener);
        }

        @Override
        public int getItemCount() {
            return towers.size();
        }

        /**
         * ViewHolder for tower card items.
         */
        static class TowerViewHolder extends RecyclerView.ViewHolder {
            private final ImageView towerIcon;
            private final TextView towerName;
            private final TextView towerCost;
            private final TextView towerDescription;
            private final Chip damageChip;
            private final Chip rangeChip;
            private final Chip fireRateChip;
            private final View lockOverlay;

            TowerViewHolder(@NonNull View itemView) {
                super(itemView);
                towerIcon = itemView.findViewById(R.id.tower_icon);
                towerName = itemView.findViewById(R.id.tower_name);
                towerCost = itemView.findViewById(R.id.tower_cost);
                towerDescription = itemView.findViewById(R.id.tower_description);
                damageChip = itemView.findViewById(R.id.chip_damage);
                rangeChip = itemView.findViewById(R.id.chip_range);
                fireRateChip = itemView.findViewById(R.id.chip_fire_rate);
                lockOverlay = itemView.findViewById(R.id.lock_overlay);
            }

            void bind(TowerInfo tower, OnTowerClickListener listener) {
                towerIcon.setImageResource(tower.iconResId);
                towerName.setText(tower.name);
                towerCost.setText(String.valueOf(tower.cost));
                towerDescription.setText(tower.description);
                damageChip.setText(String.format(Locale.getDefault(), "%.0f", tower.damage));
                rangeChip.setText(String.format(Locale.getDefault(), "%.0f", tower.range));
                fireRateChip.setText(String.format(Locale.getDefault(), "%.1f", tower.fireRate));

                lockOverlay.setVisibility(tower.isUnlocked ? View.GONE : View.VISIBLE);
                itemView.setAlpha(tower.isUnlocked ? 1.0f : 0.5f);
                itemView.setOnClickListener(v -> listener.onTowerClick(tower));
            }
        }
    }
}
