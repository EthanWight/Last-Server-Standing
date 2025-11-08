package edu.commonwealthu.lastserverstanding.ui;

import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.model.Tower;
import edu.commonwealthu.lastserverstanding.model.towers.FirewallTower;

/**
 * Bottom Sheet for tower selection and purchase
 * Displays available towers in a grid layout
 */
public class TowerSelectionBottomSheet extends BottomSheetDialogFragment {
    
    private RecyclerView towerRecyclerView;
    private TowerAdapter towerAdapter;
    private GameEngine gameEngine;
    private PointF selectedPosition;
    
    private OnTowerSelectedListener listener;
    
    /**
     * Interface for tower selection callback
     */
    public interface OnTowerSelectedListener {
        void onTowerSelected(Tower tower, PointF position);
    }
    
    /**
     * Create new instance with position and game engine
     */
    public static TowerSelectionBottomSheet newInstance(PointF position, GameEngine gameEngine) {
        TowerSelectionBottomSheet sheet = new TowerSelectionBottomSheet();
        sheet.selectedPosition = position;
        sheet.gameEngine = gameEngine;
        return sheet;
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
        
        // Set up RecyclerView
        towerRecyclerView = view.findViewById(R.id.tower_recycler_view);
        towerRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        
        // Create tower list
        List<TowerInfo> towers = createTowerList();
        
        // Set up adapter
        towerAdapter = new TowerAdapter(towers, this::onTowerClicked);
        towerRecyclerView.setAdapter(towerAdapter);
    }
    
    /**
     * Create list of available towers
     */
    private List<TowerInfo> createTowerList() {
        List<TowerInfo> towers = new ArrayList<>();
        
        // Firewall Tower
        towers.add(new TowerInfo(
            "Firewall",
            "Basic defense tower with high fire rate",
            R.drawable.ic_tower_firewall,
            100,
            15f,  // damage
            150f, // range
            2.0f, // fire rate
            true  // unlocked
        ));
        
        // TODO: Add more tower types
        // Honeypot Tower
        towers.add(new TowerInfo(
            "Honeypot",
            "Lures and distracts enemies",
            R.drawable.ic_tower_honeypot,
            150,
            0f,   // no damage
            200f, // large distraction radius
            0f,   // doesn't fire
            false // locked initially
        ));
        
        // Neural Jammer
        towers.add(new TowerInfo(
            "Neural Jammer",
            "Slows enemy processing speed",
            R.drawable.ic_tower_jammer,
            200,
            10f,
            175f,
            1.5f,
            false // locked
        ));
        
        return towers;
    }
    
    /**
     * Handle tower click
     */
    private void onTowerClicked(TowerInfo towerInfo) {
        if (!towerInfo.isUnlocked) {
            // Show locked message
            // TODO: Show snackbar or toast
            return;
        }
        
        // Check if player has enough resources
        if (gameEngine != null && gameEngine.getResources() < towerInfo.cost) {
            // Show insufficient resources message
            // TODO: Show snackbar
            return;
        }
        
        // Create tower instance based on type
        Tower tower = createTowerFromInfo(towerInfo);
        
        // Notify listener
        if (listener != null && tower != null) {
            listener.onTowerSelected(tower, selectedPosition);
        }
        
        // Dismiss bottom sheet
        dismiss();
    }
    
    /**
     * Create tower instance from tower info
     */
    private Tower createTowerFromInfo(TowerInfo info) {
        switch (info.name) {
            case "Firewall":
                return new FirewallTower(selectedPosition);
            // TODO: Add cases for other tower types
            default:
                return null;
        }
    }
    
    /**
     * Set the tower selection listener
     */
    public void setOnTowerSelectedListener(OnTowerSelectedListener listener) {
        this.listener = listener;
    }
    
    /**
     * Inner class for tower information
     */
    static class TowerInfo {
        String name;
        String description;
        int iconResId;
        int cost;
        float damage;
        float range;
        float fireRate;
        boolean isUnlocked;
        
        TowerInfo(String name, String description, int iconResId, int cost,
                  float damage, float range, float fireRate, boolean isUnlocked) {
            this.name = name;
            this.description = description;
            this.iconResId = iconResId;
            this.cost = cost;
            this.damage = damage;
            this.range = range;
            this.fireRate = fireRate;
            this.isUnlocked = isUnlocked;
        }
    }
    
    /**
     * RecyclerView Adapter for towers
     */
    static class TowerAdapter extends RecyclerView.Adapter<TowerAdapter.TowerViewHolder> {
        
        private List<TowerInfo> towers;
        private OnTowerClickListener clickListener;
        
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
         * ViewHolder for tower cards
         */
        static class TowerViewHolder extends RecyclerView.ViewHolder {
            // TODO: Add view references
            
            TowerViewHolder(@NonNull View itemView) {
                super(itemView);
                // TODO: Initialize views
            }
            
            void bind(TowerInfo tower, OnTowerClickListener listener) {
                // TODO: Bind tower data to views
                
                itemView.setOnClickListener(v -> listener.onTowerClick(tower));
                
                // Apply locked state if necessary
                itemView.setAlpha(tower.isUnlocked ? 1.0f : 0.5f);
            }
        }
    }
}
