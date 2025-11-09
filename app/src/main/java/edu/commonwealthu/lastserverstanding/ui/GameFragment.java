package edu.commonwealthu.lastserverstanding.ui;

import android.content.res.ColorStateList;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.model.towers.FirewallTower;
import edu.commonwealthu.lastserverstanding.model.towers.HoneypotTower;
import edu.commonwealthu.lastserverstanding.model.towers.JammerTower;
import edu.commonwealthu.lastserverstanding.view.GameView;

/**
 * Game Fragment - Main gameplay screen
 * Contains GameView and HUD overlay
 */
public class GameFragment extends Fragment {

    private static final String TAG = "GameFragment";
    private static final int HUD_UPDATE_INTERVAL_MS = 100; // Update HUD 10 times per second

    private GameView gameView;
    private GameEngine gameEngine;

    // HUD elements
    private TextView resourcesText;
    private LinearProgressIndicator healthBar;
    private TextView waveText;
    private TextView fpsText;
    private FloatingActionButton nextWaveFab;
    private FloatingActionButton fabFirewall;
    private FloatingActionButton fabHoneypot;
    private FloatingActionButton fabJammer;
    private View emergencyBanner;

    // HUD update handler
    private Handler hudHandler;
    private Runnable hudUpdater;

    // Tower selection
    private TowerOption selectedTower;
    private List<TowerOption> availableTowers;
    private FloatingActionButton selectedFab;

    private boolean continueGame;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get arguments
        if (getArguments() != null) {
            continueGame = getArguments().getBoolean("continue_game", false);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize HUD views
        resourcesText = view.findViewById(R.id.text_resources);
        healthBar = view.findViewById(R.id.health_bar);
        waveText = view.findViewById(R.id.text_wave);
        fpsText = view.findViewById(R.id.text_fps);
        nextWaveFab = view.findViewById(R.id.fab_next_wave);
        fabFirewall = view.findViewById(R.id.fab_tower_firewall);
        fabHoneypot = view.findViewById(R.id.fab_tower_honeypot);
        fabJammer = view.findViewById(R.id.fab_tower_jammer);
        emergencyBanner = view.findViewById(R.id.emergency_banner);
        
        // Initialize game engine
        gameEngine = new GameEngine();
        gameEngine.setContext(requireContext());

        // TODO: Load saved game if continuing
        if (continueGame) {
            loadSavedGame();
        }
        
        // Create and add GameView programmatically
        FrameLayout gameContainer = view.findViewById(R.id.game_view_container);
        gameView = new GameView(requireContext());
        gameView.setGameEngine(gameEngine);
        gameContainer.addView(gameView, 0); // Add below HUD

        // Set world dimensions after view is laid out
        gameView.post(() -> {
            int width = gameView.getWidth();
            int height = gameView.getHeight();
            int gridSize = gameView.getGridSize();

            if (width > 0 && height > 0) {
                gameEngine.setWorldDimensions(width, height, gridSize);
                Log.d(TAG, String.format("World dimensions set: %dx%d, grid size: %d", width, height, gridSize));

                // Start game paused so player can place initial towers
                gameEngine.setPaused(true);
                Log.d(TAG, "Game initialized - paused for tower placement");

                // Show next wave button for player to start wave 1
                nextWaveFab.setVisibility(View.VISIBLE);
            }
        });

        // Set up next wave button
        nextWaveFab.setOnClickListener(v -> startNextWave());

        // Set up tower selection buttons
        fabFirewall.setOnClickListener(v -> selectTower(0, fabFirewall));
        fabFirewall.setOnLongClickListener(v -> {
            showTowerInfoDialog(0);
            return true;
        });

        fabHoneypot.setOnClickListener(v -> selectTower(1, fabHoneypot));
        fabHoneypot.setOnLongClickListener(v -> {
            showTowerInfoDialog(1);
            return true;
        });

        fabJammer.setOnClickListener(v -> selectTower(2, fabJammer));
        fabJammer.setOnLongClickListener(v -> {
            showTowerInfoDialog(2);
            return true;
        });

        // Set up tap listener for tower placement
        gameView.setOnTapListener(this::handleTowerPlacement);

        // Initialize available towers
        initializeTowerOptions();

        // Select Firewall by default
        selectTower(0, fabFirewall);

        // Set up HUD update callback
        setupHUDUpdates();

        // Hide emergency banner initially
        emergencyBanner.setVisibility(View.GONE);

        Log.d(TAG, "GameFragment view created");
    }
    
    /**
     * Start the next wave
     */
    private void startNextWave() {
        if (gameEngine != null) {
            gameEngine.startNextWave();
            nextWaveFab.setVisibility(View.GONE);

            // Auto-unpause the game when starting a wave
            gameEngine.setPaused(false);

            Toast.makeText(requireContext(),
                "Wave " + gameEngine.getCurrentWave() + " started!",
                Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Wave " + gameEngine.getCurrentWave() + " started");
        }
    }
    
    /**
     * Set up periodic HUD updates
     */
    private void setupHUDUpdates() {
        hudHandler = new Handler();
        hudUpdater = new Runnable() {
            @Override
            public void run() {
                if (gameEngine != null && isAdded()) {
                    updateHUD();
                    hudHandler.postDelayed(this, HUD_UPDATE_INTERVAL_MS);
                }
            }
        };
        hudHandler.post(hudUpdater);
    }
    
    /**
     * Update HUD elements with current game state
     */
    private void updateHUD() {
        if (gameEngine == null) return;

        int currentResources = gameEngine.getResources();

        // Update resources (just the number, icon is in layout)
        resourcesText.setText(String.valueOf(currentResources));

        // Update health bar
        int health = gameEngine.getDataCenterHealth();
        healthBar.setProgress(health);

        // Update health bar color based on health
        if (health > 60) {
            healthBar.setIndicatorColor(getResources().getColor(R.color.health_high, requireContext().getTheme()));
        } else if (health > 30) {
            healthBar.setIndicatorColor(getResources().getColor(R.color.health_medium, requireContext().getTheme()));
        } else {
            healthBar.setIndicatorColor(getResources().getColor(R.color.health_low, requireContext().getTheme()));
        }

        // Update wave counter (just the number)
        waveText.setText(String.valueOf(gameEngine.getCurrentWave()));

        // Update FPS counter
        fpsText.setText(String.valueOf(gameEngine.getFPS()));

        // Show/hide next wave button based on wave state
        if (!gameEngine.getWaveManager().isWaveActive() && gameEngine.getCurrentWave() > 0) {
            // Wave is complete, show next wave button
            nextWaveFab.setVisibility(View.VISIBLE);
        } else if (gameEngine.getWaveManager().isWaveActive()) {
            // Wave is active, hide button
            nextWaveFab.setVisibility(View.GONE);
        }
    }
    
    /**
     * Show emergency alert banner
     */
    public void showEmergencyAlert(String message) {
        emergencyBanner.setVisibility(View.VISIBLE);
        TextView bannerText = emergencyBanner.findViewById(R.id.banner_text);
        bannerText.setText(message);

        // Auto-hide after 3 seconds
        emergencyBanner.postDelayed(() -> {
            emergencyBanner.setVisibility(View.GONE);
        }, 3000);
    }

    /**
     * Initialize available tower options
     */
    private void initializeTowerOptions() {
        availableTowers = new ArrayList<>();

        // Firewall Tower - Basic defense (unlocked)
        availableTowers.add(new TowerOption(
                "Firewall",
                "Firewall",
                100,
                15f,
                150f,
                2.0f,
                R.drawable.ic_tower_firewall,
                false,
                "Basic defense tower with high fire rate. Good all-around tower for early game."
        ));

        // Honeypot Tower (unlocked)
        availableTowers.add(new TowerOption(
                "Honeypot",
                "Honeypot",
                150,
                25f,
                120f,
                1.5f,
                R.drawable.ic_tower_honeypot,
                false,
                "Slows enemies and deals damage over time. Effective for controlling enemy movement."
        ));

        // Jammer Tower (unlocked)
        availableTowers.add(new TowerOption(
                "Jammer",
                "Jammer",
                200,
                10f,
                200f,
                3.0f,
                R.drawable.ic_tower_jammer,
                false,
                "Fast attack speed with wide range. Best for hitting multiple targets."
        ));
    }

    /**
     * Select a tower by index
     */
    private void selectTower(int index, FloatingActionButton fab) {
        if (index < 0 || index >= availableTowers.size()) {
            return;
        }

        TowerOption tower = availableTowers.get(index);

        // Check if tower is locked
        if (tower.isLocked()) {
            Toast.makeText(requireContext(),
                    tower.getName() + " is locked! Complete more waves to unlock.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Deselect previous tower
        if (selectedFab != null) {
            selectedFab.setBackgroundTintList(ColorStateList.valueOf(
                    getResources().getColor(R.color.electric_blue, requireContext().getTheme())));
        }

        // Select new tower
        selectedTower = tower;
        selectedFab = fab;

        // Highlight selected tower with different color
        fab.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(R.color.success_green, requireContext().getTheme())));

        // Get current resources
        int resources = gameEngine != null ? gameEngine.getResources() : 0;
        boolean canAfford = resources >= tower.getCost();

        // Show selection feedback
        String message = tower.getName() + " selected (" + tower.getCost() + " resources)";
        if (!canAfford) {
            message += " - Need " + (tower.getCost() - resources) + " more!";
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Tower selected: " + tower.getName());
    }

    /**
     * Show info dialog for a tower
     */
    private void showTowerInfoDialog(int index) {
        if (index < 0 || index >= availableTowers.size()) {
            return;
        }

        TowerOption tower = availableTowers.get(index);

        // Build the message
        String message = tower.getDescription() + "\n\n" +
                "Cost: " + tower.getCost() + " resources\n" +
                "Damage: " + String.format("%.0f", tower.getDamage()) + "\n" +
                "Range: " + String.format("%.0f", tower.getRange()) + "\n" +
                "Fire Rate: " + String.format("%.1f", tower.getFireRate()) + " attacks/sec";

        if (tower.isLocked()) {
            message += "\n\nStatus: LOCKED\nComplete more waves to unlock this tower.";
        }

        // Show AlertDialog
        new AlertDialog.Builder(requireContext())
                .setTitle(tower.getName() + " Tower")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setIcon(tower.getIconResId())
                .show();
    }

    /**
     * Handle tap event for tower placement
     */
    private void handleTowerPlacement(PointF worldPosition) {
        if (selectedTower == null) {
            Log.d(TAG, "No tower selected - tap ignored");
            return;
        }

        if (gameEngine == null) {
            Log.e(TAG, "GameEngine is null!");
            return;
        }

        // Check if player can afford the tower
        if (gameEngine.getResources() < selectedTower.getCost()) {
            Toast.makeText(requireContext(),
                    "Not enough resources! Need " + selectedTower.getCost(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create and place the tower based on type
            boolean placed = false;
            switch (selectedTower.getType()) {
                case "Firewall":
                    Log.d(TAG, "Creating Firewall tower at " + worldPosition);
                    FirewallTower firewallTower = new FirewallTower(worldPosition);
                    placed = gameEngine.addTower(firewallTower);
                    break;
                case "Honeypot":
                    Log.d(TAG, "Creating Honeypot tower at " + worldPosition);
                    HoneypotTower honeypotTower = new HoneypotTower(worldPosition);
                    placed = gameEngine.addTower(honeypotTower);
                    break;
                case "Jammer":
                    Log.d(TAG, "Creating Jammer tower at " + worldPosition);
                    JammerTower jammerTower = new JammerTower(worldPosition);
                    placed = gameEngine.addTower(jammerTower);
                    break;
                default:
                    Log.e(TAG, "Unknown tower type: " + selectedTower.getType());
            }

            if (placed) {
                Log.d(TAG, String.format("Tower placed successfully at (%.0f, %.0f)", worldPosition.x, worldPosition.y));
                Toast.makeText(requireContext(),
                        selectedTower.getName() + " placed!",
                        Toast.LENGTH_SHORT).show();
                // Don't clear selection - allow multiple placements
            } else {
                Log.w(TAG, "Tower placement failed - not enough resources");
                Toast.makeText(requireContext(),
                        "Not enough resources!",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error placing tower", e);
            Toast.makeText(requireContext(),
                    "Error placing tower: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load saved game state
     */
    private void loadSavedGame() {
        // TODO: Implement save game loading
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Fragment paused - stopping game loop");

        if (gameView != null) {
            gameView.stopGameLoop();
        }

        // Stop HUD updates
        if (hudHandler != null && hudUpdater != null) {
            hudHandler.removeCallbacks(hudUpdater);
        }

        // Auto-pause game when leaving
        if (gameEngine != null) {
            gameEngine.setPaused(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed - starting game loop");

        if (gameView != null) {
            gameView.startGameLoop();
        }

        // Resume HUD updates
        if (hudHandler != null && hudUpdater != null) {
            hudHandler.post(hudUpdater);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment destroyed - cleaning up");

        if (gameView != null) {
            gameView.stopGameLoop();
        }

        // Clean up HUD handler to prevent memory leaks
        if (hudHandler != null && hudUpdater != null) {
            hudHandler.removeCallbacks(hudUpdater);
            hudHandler = null;
            hudUpdater = null;
        }
    }
}
