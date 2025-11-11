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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;
import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.model.towers.FirewallTower;
import edu.commonwealthu.lastserverstanding.model.towers.HoneypotTower;
import edu.commonwealthu.lastserverstanding.model.towers.JammerTower;
import edu.commonwealthu.lastserverstanding.view.GameView;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;

/**
 * Game Fragment - Main gameplay screen
 * Contains GameView and HUD overlay
 */
public class GameFragment extends Fragment {

    private static final String TAG = "GameFragment";
    private static final int HUD_UPDATE_INTERVAL_MS = 100; // Update HUD 10 times per second

    private GameView gameView;
    private GameEngine gameEngine;
    private GameViewModel viewModel;

    // HUD elements
    private TextView resourcesText;
    private TextView scoreText;
    private LinearProgressIndicator healthBar;
    private TextView waveText;
    private TextView fpsText;
    private FloatingActionButton nextWaveFab;
    private FloatingActionButton pauseFab;
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

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        // Initialize HUD views
        resourcesText = view.findViewById(R.id.text_resources);
        scoreText = view.findViewById(R.id.text_score);
        healthBar = view.findViewById(R.id.health_bar);
        waveText = view.findViewById(R.id.text_wave);
        fpsText = view.findViewById(R.id.text_fps);
        nextWaveFab = view.findViewById(R.id.fab_next_wave);
        pauseFab = view.findViewById(R.id.fab_pause);
        fabFirewall = view.findViewById(R.id.fab_tower_firewall);
        fabHoneypot = view.findViewById(R.id.fab_tower_honeypot);
        fabJammer = view.findViewById(R.id.fab_tower_jammer);
        emergencyBanner = view.findViewById(R.id.emergency_banner);

        // Initialize game engine
        gameEngine = new GameEngine();
        gameEngine.setContext(requireContext());

        // Observe settings and update game engine
        viewModel.getSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null && gameEngine != null) {
                gameEngine.setSoundEnabled(settings.isSoundEnabled());
                gameEngine.setVibrationEnabled(settings.isVibrationEnabled());
                Log.d(TAG, "Settings updated - Sound: " + settings.isSoundEnabled() +
                        ", Vibration: " + settings.isVibrationEnabled());
            }
        });

        // Load saved game if continuing
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
                updatePauseButton();
                Log.d(TAG, "Game initialized - paused for tower placement");

                // Show next wave button for player to start wave 1
                nextWaveFab.setVisibility(View.VISIBLE);
            }
        });

        // Set up next wave button
        nextWaveFab.setOnClickListener(v -> startNextWave());

        // Set up pause button
        pauseFab.setOnClickListener(v -> togglePause());

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
            updatePauseButton();

            Toast.makeText(requireContext(),
                "Wave " + gameEngine.getCurrentWave() + " started!",
                Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Wave " + gameEngine.getCurrentWave() + " started");
        }
    }

    /**
     * Toggle pause state
     */
    private void togglePause() {
        if (gameEngine != null) {
            boolean isPaused = gameEngine.isPaused();
            gameEngine.setPaused(!isPaused);
            updatePauseButton();

            String message = isPaused ? "Game resumed" : "Game paused";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, message);
        }
    }

    /**
     * Update pause button icon based on game state
     */
    private void updatePauseButton() {
        if (gameEngine != null && pauseFab != null) {
            boolean isPaused = gameEngine.isPaused();
            int iconRes = isPaused ? R.drawable.ic_play_arrow : R.drawable.ic_pause;
            pauseFab.setImageResource(iconRes);
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

        // Update score
        long score = gameEngine.getScore();
        scoreText.setText(String.valueOf(score));

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
     * Load saved game state from most recent auto-save
     */
    private void loadSavedGame() {
        if (viewModel == null) {
            Log.e(TAG, "ViewModel is null, cannot load game");
            return;
        }

        // Load the most recent save (ID = 1 for auto-save)
        viewModel.loadGame(1, new GameRepository.LoadCallback() {
            @Override
            public void onSuccess(GameState gameState) {
                if (gameEngine != null && gameState != null) {
                    // Restore the game state to the engine
                    gameEngine.restoreGameState(gameState);
                    Log.d(TAG, "Game loaded successfully - Wave: " + gameState.currentWave +
                            ", Resources: " + gameState.resources);

                    Toast.makeText(requireContext(),
                            "Game loaded - Wave " + gameState.currentWave,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load game: " + error);
                Toast.makeText(requireContext(),
                        "No saved game found. Starting new game.",
                        Toast.LENGTH_SHORT).show();
            }
        });
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

            // Auto-save game state when going to background
            saveGameState(true);
        }
    }

    /**
     * Save current game state
     * @param isAutoSave true for auto-save, false for manual save
     */
    private void saveGameState(boolean isAutoSave) {
        if (gameEngine == null || viewModel == null) {
            Log.w(TAG, "Cannot save game - engine or viewModel is null");
            return;
        }

        // Only save if there's actual progress (wave > 0)
        if (gameEngine.getCurrentWave() == 0) {
            Log.d(TAG, "Skipping save - no progress to save");
            return;
        }

        // Capture game state from engine
        GameState gameState = gameEngine.captureGameState();

        // Save via ViewModel
        viewModel.saveGame(gameState, isAutoSave, new GameRepository.SaveCallback() {
            @Override
            public void onSuccess(int saveId) {
                Log.d(TAG, (isAutoSave ? "Auto" : "Manual") + " save successful - Wave: " +
                        gameState.currentWave + ", Score: " + gameState.score + ", SaveID: " + saveId);

                if (!isAutoSave && isAdded()) {
                    Toast.makeText(requireContext(),
                            "Game saved!",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to save game: " + error);

                if (!isAutoSave && isAdded()) {
                    Toast.makeText(requireContext(),
                            "Failed to save game: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
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

        // Update pause button to reflect current state
        updatePauseButton();
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
