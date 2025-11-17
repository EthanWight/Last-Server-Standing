package edu.commonwealthu.lastserverstanding.ui;

import android.content.res.ColorStateList;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;
import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.model.Tower;
import edu.commonwealthu.lastserverstanding.model.towers.FirewallTower;
import edu.commonwealthu.lastserverstanding.model.towers.HoneypotTower;
import edu.commonwealthu.lastserverstanding.model.towers.JammerTower;
import edu.commonwealthu.lastserverstanding.view.GameView;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;

/**
 * Game Fragment - Main gameplay screen
 * Contains GameView and HUD overlay
 * Save/Load Flow:
 * - Auto-save occurs in onPause() whenever there's progress (wave > 0)
 * - This saves when: going to settings, main menu, background, or exiting app
 * - When user clicks "Continue" from main menu, saved game is loaded
 * - When user clicks "New Game" from main menu, saves are deleted and fresh game starts
 * - When returning from settings, existing game state is preserved in ViewModel
 */
public class GameFragment extends Fragment {

    private static final String TAG = "GameFragment";
    private static final int HUD_UPDATE_INTERVAL_MS = 100; // Update HUD 10 times per second

    private GameView gameView;
    private GameEngine gameEngine;
    private GameViewModel viewModel;

    // HUD elements
    private TextView resourcesText;
    private LinearProgressIndicator healthBar;
    private TextView waveText;
    private TextView fpsText;
    private ImageButton nextWaveFab;

    // HUD update handler
    private Handler hudHandler;
    private Runnable hudUpdater;

    // Tower selection
    private TowerOption selectedTower;
    private List<TowerOption> availableTowers;
    private AccessibleImageButton selectedFab;
    private boolean isDragging = false;
    private TowerOption draggedTower = null;

    private boolean continueGame;
    private boolean isNewGame;
    private boolean hasLoadedGame = false;

    // Key to save state across configuration changes and fragment recreation
    private static final String KEY_HAS_LOADED = "has_loaded_game";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore state from savedInstanceState (survives configuration changes and recreation)
        if (savedInstanceState != null) {
            hasLoadedGame = savedInstanceState.getBoolean(KEY_HAS_LOADED, false);
            Log.d(TAG, "onCreate - restored hasLoadedGame: " + hasLoadedGame);
        }

        // Get arguments
        if (getArguments() != null) {
            continueGame = getArguments().getBoolean("continue_game", false);
            // Check if this is explicitly a new game
            isNewGame = !continueGame && getArguments().containsKey("continue_game");

            // If new game is explicitly requested, clear hasLoadedGame flag
            // This ensures new game works even if returning from main menu
            if (isNewGame) {
                hasLoadedGame = false;
            }

            Log.d(TAG, "onCreate - continueGame: " + continueGame + ", isNewGame: " + isNewGame);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save state so it survives configuration changes and fragment recreation
        outState.putBoolean(KEY_HAS_LOADED, hasLoadedGame);
        Log.d(TAG, "onSaveInstanceState - saving hasLoadedGame: " + hasLoadedGame);
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

        // Initialize ViewModel (use activity scope to persist across navigation)
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // Initialize HUD views
        resourcesText = view.findViewById(R.id.text_resources);
        healthBar = view.findViewById(R.id.health_bar);
        waveText = view.findViewById(R.id.text_wave);
        fpsText = view.findViewById(R.id.text_fps);
        nextWaveFab = view.findViewById(R.id.fab_next_wave);
        ImageButton settingsFab = view.findViewById(R.id.fab_settings);
        AccessibleImageButton fabFirewall = view.findViewById(R.id.fab_tower_firewall);
        AccessibleImageButton fabHoneypot = view.findViewById(R.id.fab_tower_honeypot);
        AccessibleImageButton fabJammer = view.findViewById(R.id.fab_tower_jammer);

        // Get the game engine first
        gameEngine = viewModel.getGameEngine();

        // Set up game event listener
        gameEngine.setGameListener(new GameEngine.GameListener() {
            @Override
            public void onGameOver(int finalWave) {
                handleGameOver(finalWave);
            }

            @Override
            public void onStatsChanged(int wave, int resources, int health, long score) {
                if (viewModel != null) {
                    viewModel.updateGameStats(wave, resources, health, score);
                }
            }
        });

        // Check if the game engine is already initialized (has active game state)
        // This prevents resetting an active game when returning from settings
        boolean isGameAlreadyActive = viewModel.hasActiveGame() && gameEngine.getCurrentWave() > 0;

        // If user clicked "New Game" from main menu, reset the game
        // Only reset if we haven't already loaded this session (prevents reset when returning from settings)
        if (isNewGame && !hasLoadedGame) {
            Log.d(TAG, "New game requested - deleting save and starting fresh");
            deleteAutoSave();
            viewModel.resetGameEngine();
            gameEngine = viewModel.getGameEngine();
            // Re-set the listener after reset
            gameEngine.setGameListener(new GameEngine.GameListener() {
                @Override
                public void onGameOver(int finalWave) {
                    handleGameOver(finalWave);
                }

                @Override
                public void onStatsChanged(int wave, int resources, int health, long score) {
                    if (viewModel != null) {
                        viewModel.updateGameStats(wave, resources, health, score);
                    }
                }
            });
            hasLoadedGame = true; // Mark as loaded to prevent re-initialization
            Log.d(TAG, "New game initialized - Wave: " + gameEngine.getCurrentWave() + ", Resources: " + gameEngine.getResources());
        } else if (continueGame && !hasLoadedGame && !isGameAlreadyActive) {
            // User clicked "Continue" from main menu - reset and load saved game
            Log.d(TAG, "Continue game requested - will reset and load from save");
            viewModel.resetGameEngine();
            gameEngine = viewModel.getGameEngine();
            // Re-set the listener after reset
            gameEngine.setGameListener(new GameEngine.GameListener() {
                @Override
                public void onGameOver(int finalWave) {
                    handleGameOver(finalWave);
                }

                @Override
                public void onStatsChanged(int wave, int resources, int health, long score) {
                    if (viewModel != null) {
                        viewModel.updateGameStats(wave, resources, health, score);
                    }
                }
            });
            // DON'T set hasLoadedGame here - it will be set after loadSavedGame() completes
        } else {
            // Returning from settings or resuming - keep existing game
            Log.d(TAG, "Resuming existing game - Wave: " + gameEngine.getCurrentWave() +
                    ", Resources: " + gameEngine.getResources());
            hasLoadedGame = true; // Mark as loaded since we're using existing game
        }

        // Set context (safe to call multiple times)
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

        // Create and add GameView programmatically
        FrameLayout gameContainer = view.findViewById(R.id.game_view_container);
        gameView = new GameView(requireContext());
        gameView.setGameEngine(gameEngine);
        gameContainer.addView(gameView, 0); // Add below HUD, view handles its own sizing

        // Set world dimensions after view is laid out
        gameView.post(() -> {
            int width = gameView.getWidth();
            int height = gameView.getHeight();
            int gridSize = gameView.getGridSize();

            if (width > 0 && height > 0) {
                gameEngine.setWorldDimensions(width, height, gridSize);
                Log.d(TAG, String.format(Locale.getDefault(), "World dimensions set: %dx%d, grid size: %d", width, height, gridSize));

                // Load saved game only if continuing from main menu (after world setup)
                if (continueGame && !hasLoadedGame && !isGameAlreadyActive) {
                    Log.d(TAG, "Loading saved game from main menu");
                    loadSavedGame();
                    // hasLoadedGame will be set to true in the load callback
                } else {
                    Log.d(TAG, "Using existing game engine - current wave: " + gameEngine.getCurrentWave());

                    // Start game paused so player can place initial towers
                    gameEngine.setPaused(true);
                    Log.d(TAG, "Game initialized - paused for tower placement");

                    // Show next wave button for player to start wave 1
                    if (gameEngine.getCurrentWave() == 0) {
                        nextWaveFab.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // Set up next wave button
        nextWaveFab.setOnClickListener(v -> startNextWave());

        // Set up settings button
        settingsFab.setOnClickListener(v -> openSettings());

        // Set up tower selection buttons with drag and drop
        setupTowerDragAndDrop(fabFirewall, 0);
        setupTowerDragAndDrop(fabHoneypot, 1);
        setupTowerDragAndDrop(fabJammer, 2);

        // Set up tap listener for tower placement or upgrade
        gameView.setOnTapListener(this::handleTowerPlacement);

        // Initialize available towers
        initializeTowerOptions();

        // Set up LiveData observers for HUD
        setupLiveDataObservers();

        // Set up periodic FPS updates only
        setupFPSUpdates();

        Log.d(TAG, "GameFragment view created");
    }
    
    /**
     * Start the next wave, pause, or resume
     */
    private void startNextWave() {
        if (gameEngine != null) {
            boolean isWaveActive = gameEngine.getWaveManager().isWaveActive();
            boolean isPaused = gameEngine.isPaused();

            // If wave is active and not paused, pause the game
            if (isWaveActive && !isPaused) {
                gameEngine.setPaused(true);
                Toast.makeText(requireContext(), "Game paused", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Game paused");
                return;
            }

            // If game is paused AND wave is active, resume
            // (If paused but no wave active, fall through to start wave instead)
            if (isPaused && isWaveActive) {
                gameEngine.setPaused(false);
                Toast.makeText(requireContext(), "Game resumed!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Game resumed");
                return;
            }

            // Otherwise, start the next wave (and unpause if needed)
            // Auto-save before starting next wave (if there's progress)
            if (gameEngine.getCurrentWave() > 0) {
                saveGameState();
            }

            gameEngine.startNextWave();

            // Auto-unpause the game when starting a wave
            gameEngine.setPaused(false);

            Toast.makeText(requireContext(),
                "Wave " + gameEngine.getCurrentWave() + " started!",
                Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Wave " + gameEngine.getCurrentWave() + " started");
        }

        // Update button icon after state change
        updatePlayPauseButton();
    }

    /**
     * Update play/pause button icon based on game state
     */
    private void updatePlayPauseButton() {
        if (gameEngine != null && nextWaveFab != null) {
            boolean isWaveActive = gameEngine.getWaveManager().isWaveActive();
            boolean isPaused = gameEngine.isPaused();

            // Show pause icon if wave is active and not paused
            // Show play icon in all other cases
            if (isWaveActive && !isPaused) {
                nextWaveFab.setImageResource(R.drawable.ic_pause);
                nextWaveFab.setContentDescription("Pause game");
            } else {
                nextWaveFab.setImageResource(R.drawable.ic_play_arrow);
                // If paused AND wave is active, show "Resume"
                // Otherwise (paused with no wave, or not paused with no wave), show "Start next wave"
                if (isPaused && isWaveActive) {
                    nextWaveFab.setContentDescription("Resume game");
                } else {
                    nextWaveFab.setContentDescription("Start next wave");
                }
            }
        }
    }

    /**
     * Open settings menu
     */
    private void openSettings() {
        if (gameEngine != null) {
            // Don't manually pause - the game loop will naturally pause when fragment pauses
            // Navigate to settings (game engine persists in ViewModel)
            Navigation.findNavController(requireView()).navigate(R.id.action_game_to_settings);
            Log.d(TAG, "Navigating to settings");
        }
    }

    /**
     * Set up drag and drop for tower button
     */
    private void setupTowerDragAndDrop(AccessibleImageButton fab, int towerIndex) {
        // Add click listener for accessibility support
        fab.setOnClickListener(v -> {
            // Ignore click if it's from a drag operation
            if (isDragging) {
                isDragging = false;
                return;
            }

            // Simple click selects/deselects the tower
            if (selectedTower != null && selectedFab == fab) {
                deselectTower();
            } else if (towerIndex < availableTowers.size()) {
                selectTower(towerIndex, fab);
            }
        });

        // Set up touch listener for drag-and-drop functionality
        // performClick() is called in ACTION_UP for accessibility
        fab.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Start drag
                    isDragging = true;
                    if (towerIndex < availableTowers.size()) {
                        TowerOption tower = availableTowers.get(towerIndex);
                        draggedTower = tower;
                        gameView.showDragPreview(tower.getIconResId());
                        updateDragPreview(event.getRawX(), event.getRawY());
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Update drag preview position
                    updateDragPreview(event.getRawX(), event.getRawY());
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Drop tower
                    gameView.hideDragPreview();
                    handleTowerDrop(event.getRawX(), event.getRawY());
                    draggedTower = null;
                    // Call performClick for accessibility
                    v.performClick();
                    return true;
            }
            return false;
        });

        // Long press shows info
        fab.setOnLongClickListener(v -> {
            showTowerInfoDialog(towerIndex);
            return true;
        });
    }

    /**
     * Update drag preview position and validity
     */
    private void updateDragPreview(float screenX, float screenY) {
        // Convert screen coordinates to game view coordinates
        int[] location = new int[2];
        gameView.getLocationOnScreen(location);
        float viewX = screenX - location[0];
        float viewY = screenY - location[1];

        PointF screenPos = new PointF(viewX, viewY);
        PointF worldPos = gameView.screenToWorld(screenPos);

        // Check if placement is valid
        boolean isValid = gameEngine != null && gameEngine.isValidTowerPlacement(worldPos);

        gameView.updateDragPreview(screenPos, isValid);
    }

    /**
     * Handle tower drop
     */
    private void handleTowerDrop(float screenX, float screenY) {
        // Convert screen coordinates to game view coordinates
        int[] location = new int[2];
        gameView.getLocationOnScreen(location);
        float viewX = screenX - location[0];
        float viewY = screenY - location[1];

        PointF screenPos = new PointF(viewX, viewY);
        PointF worldPos = gameView.screenToWorld(screenPos);

        // Try to place the tower
        handleTowerPlacement(worldPos);
    }

    /**
     * Set up LiveData observers for HUD stats
     */
    private void setupLiveDataObservers() {
        // Observe wave changes
        viewModel.getCurrentWave().observe(getViewLifecycleOwner(), wave -> {
            if (waveText != null && wave != null) {
                waveText.setText(String.valueOf(wave));

                // Always show button once game has started (wave > 0)
                // Button will toggle between: Start Wave / Pause / Resume
                if (gameEngine != null) {
                    if (wave > 0) {
                        nextWaveFab.setVisibility(View.VISIBLE);
                    } else if (wave == 0) {
                        // Show for initial wave start
                        nextWaveFab.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        // Observe resources changes
        viewModel.getResources().observe(getViewLifecycleOwner(), resources -> {
            if (resourcesText != null && resources != null) {
                resourcesText.setText(String.valueOf(resources));
            }
        });

        // Observe health changes
        viewModel.getHealth().observe(getViewLifecycleOwner(), health -> {
            if (healthBar != null && health != null) {
                healthBar.setProgress(health);

                // Update health bar color based on health
                if (health > 60) {
                    healthBar.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.health_high));
                } else if (health > 30) {
                    healthBar.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.health_medium));
                } else {
                    healthBar.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.health_low));
                }
            }
        });

        // Score is observed but not displayed in HUD (shown at game over)
        // viewModel.getScore().observe(getViewLifecycleOwner(), score -> { ... });
    }

    /**
     * Set up periodic FPS updates
     */
    private void setupFPSUpdates() {
        hudHandler = new Handler();
        hudUpdater = new Runnable() {
            @Override
            public void run() {
                if (gameEngine != null && isAdded() && fpsText != null) {
                    fpsText.setText(String.valueOf(gameEngine.getFPS()));
                    updatePlayPauseButton(); // Keep play/pause icon in sync
                    hudHandler.postDelayed(this, HUD_UPDATE_INTERVAL_MS);
                }
            }
        };
        hudHandler.post(hudUpdater);
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
    private void selectTower(int index, AccessibleImageButton fab) {
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
                    ContextCompat.getColor(requireContext(), R.color.electric_blue)));
        }

        // Select new tower
        selectedTower = tower;
        selectedFab = fab;

        // Highlight selected tower with different color
        fab.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.success_green)));

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
     * Deselect the currently selected tower
     */
    private void deselectTower() {
        if (selectedFab != null) {
            // Reset button color to default
            selectedFab.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.electric_blue)));
        }

        selectedTower = null;
        selectedFab = null;
        Log.d(TAG, "Tower deselected");
    }

    /**
     * Show info dialog for a tower using the tower card layout
     */
    private void showTowerInfoDialog(int index) {
        if (index < 0 || index >= availableTowers.size()) {
            return;
        }

        TowerOption tower = availableTowers.get(index);

        // Inflate the tower card layout
        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_tower_card, null);

        // Get references to views
        android.widget.ImageView iconView = cardView.findViewById(R.id.tower_icon);
        TextView nameView = cardView.findViewById(R.id.tower_name);
        TextView costView = cardView.findViewById(R.id.tower_cost);
        TextView descriptionView = cardView.findViewById(R.id.tower_description);
        com.google.android.material.chip.Chip damageChip = cardView.findViewById(R.id.chip_damage);
        com.google.android.material.chip.Chip rangeChip = cardView.findViewById(R.id.chip_range);
        com.google.android.material.chip.Chip fireRateChip = cardView.findViewById(R.id.chip_fire_rate);
        View lockOverlay = cardView.findViewById(R.id.lock_overlay);
        View infoButton = cardView.findViewById(R.id.btn_info);
        com.google.android.material.button.MaterialButton closeButton = cardView.findViewById(R.id.btn_close);

        // Populate the card with tower data
        iconView.setImageResource(tower.getIconResId());
        nameView.setText(getString(R.string.tower_name_format, tower.getName()));
        costView.setText(String.valueOf(tower.getCost()));
        descriptionView.setText(tower.getDescription());
        damageChip.setText(String.format(Locale.getDefault(), "%.0f", tower.getDamage()));
        rangeChip.setText(String.format(Locale.getDefault(), "%.0f", tower.getRange()));
        fireRateChip.setText(String.format(Locale.getDefault(), "%.1f", tower.getFireRate()));

        // Show/hide elements for dialog mode
        lockOverlay.setVisibility(tower.isLocked() ? View.VISIBLE : View.GONE);
        infoButton.setVisibility(View.GONE); // Hide info button in dialog
        closeButton.setVisibility(View.VISIBLE); // Show close button in dialog

        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(cardView)
                .create();

        // Set up close button to dismiss dialog
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Show upgrade dialog for an existing tower
     */
    private void showTowerUpgradeDialog(Tower tower) {
        if (tower == null) return;

        // Inflate the tower card layout
        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_tower_card, null);

        // Get references to views
        android.widget.ImageView iconView = cardView.findViewById(R.id.tower_icon);
        TextView nameView = cardView.findViewById(R.id.tower_name);
        TextView levelView = cardView.findViewById(R.id.tower_level);
        TextView descriptionView = cardView.findViewById(R.id.tower_description);
        com.google.android.material.chip.Chip damageChip = cardView.findViewById(R.id.chip_damage);
        com.google.android.material.chip.Chip rangeChip = cardView.findViewById(R.id.chip_range);
        com.google.android.material.chip.Chip fireRateChip = cardView.findViewById(R.id.chip_fire_rate);
        View infoButton = cardView.findViewById(R.id.btn_info);
        com.google.android.material.button.MaterialButton upgradeButton = cardView.findViewById(R.id.btn_upgrade);
        com.google.android.material.button.MaterialButton closeButton = cardView.findViewById(R.id.btn_close);
        View lockOverlay = cardView.findViewById(R.id.lock_overlay);
        TextView costView = cardView.findViewById(R.id.tower_cost);

        // Get tower type name
        String towerType = tower.getType();

        // Populate the card with tower data
        iconView.setImageResource(getTowerIconResource(towerType));
        nameView.setText(getString(R.string.tower_name_format, towerType));
        levelView.setText(getString(R.string.tower_level_format, tower.getLevel()));
        levelView.setVisibility(View.VISIBLE); // Show level for existing tower
        descriptionView.setText(getTowerDescription(towerType));
        damageChip.setText(String.format(Locale.getDefault(), "%.0f", tower.getDamage()));
        rangeChip.setText(String.format(Locale.getDefault(), "%.0f", tower.getRange()));
        fireRateChip.setText(String.format(Locale.getDefault(), "%.1f", tower.getFireRate()));

        // Hide cost and lock overlay for existing towers
        costView.setVisibility(View.GONE);
        lockOverlay.setVisibility(View.GONE);
        infoButton.setVisibility(View.GONE);

        // Show upgrade button
        upgradeButton.setVisibility(View.VISIBLE);
        closeButton.setVisibility(View.VISIBLE);

        int upgradeCost = tower.getUpgradeCost();
        if (upgradeCost > 0) {
            upgradeButton.setText(getString(R.string.upgrade_button_format, upgradeCost));
            upgradeButton.setEnabled(gameEngine.getResources() >= upgradeCost);
        } else {
            upgradeButton.setText(getString(R.string.max_level));
            upgradeButton.setEnabled(false);
        }

        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(cardView)
                .create();

        // Set up upgrade button click
        upgradeButton.setOnClickListener(v -> {
            if (gameEngine.upgradeTower(tower)) {
                Toast.makeText(requireContext(),
                        getString(R.string.tower_upgraded_format, towerType, tower.getLevel()),
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, towerType + " upgraded - New stats: Damage=" + tower.getDamage() +
                        ", Range=" + tower.getRange() + ", FireRate=" + tower.getFireRate());
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(),
                        getString(R.string.not_enough_resources),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Set up close button
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Get tower icon resource by type
     */
    private int getTowerIconResource(String towerType) {
        return switch (towerType) {
            case "Firewall" -> R.drawable.ic_tower_firewall;
            case "Honeypot" -> R.drawable.ic_tower_honeypot;
            case "Jammer" -> R.drawable.ic_tower_jammer;
            default -> {
                Log.w(TAG, "Unknown tower type: " + towerType + ", using Firewall icon as default");
                yield R.drawable.ic_tower_firewall;
            }
        };
    }

    /**
     * Get tower description by type
     */
    private String getTowerDescription(String towerType) {
        return switch (towerType) {
            case "Firewall" -> "Basic defense tower with high fire rate. Good all-around tower for early game.";
            case "Honeypot" -> "Slows enemies and deals damage over time. Effective for controlling enemy movement.";
            case "Jammer" -> "Fast attack speed with wide range. Best for hitting multiple targets.";
            default -> "Unknown tower type.";
        };
    }

    /**
     * Handle tap event for tower placement or upgrade
     */
    private void handleTowerPlacement(PointF worldPosition) {
        if (gameEngine == null) {
            Log.e(TAG, "GameEngine is null!");
            return;
        }

        // First, check if there's an existing tower at this position
        Tower existingTower = gameEngine.getTowerAt(worldPosition);
        if (existingTower != null) {
            // Show upgrade dialog for existing tower
            showTowerUpgradeDialog(existingTower);
            return;
        }

        // Use draggedTower if available (from drag operation), otherwise use selectedTower (from click)
        TowerOption towerToPlace = draggedTower != null ? draggedTower : selectedTower;

        // No existing tower, proceed with placement
        if (towerToPlace == null) {
            Log.d(TAG, "No tower selected - tap ignored");
            return;
        }

        // Check if player can afford the tower
        if (gameEngine.getResources() < towerToPlace.getCost()) {
            Toast.makeText(requireContext(),
                    "Not enough resources! Need " + towerToPlace.getCost(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create and place the tower based on type
            boolean placed = false;
            switch (towerToPlace.getType()) {
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
                    Log.e(TAG, "Unknown tower type: " + towerToPlace.getType());
            }

            if (placed) {
                Log.d(TAG, String.format(Locale.getDefault(), "Tower placed successfully at (%.0f, %.0f)", worldPosition.x, worldPosition.y));
                Toast.makeText(requireContext(),
                        towerToPlace.getName() + " placed!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Tower placement failed - invalid spot");
                Toast.makeText(requireContext(),
                        "Invalid spot! Place towers on walls only.",
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

        Log.d(TAG, "Loading latest auto-save");
        viewModel.loadLatestAutoSave(createLoadCallback());
    }

    /**
     * Create a load callback for saved games
     */
    private GameRepository.LoadCallback createLoadCallback() {
        return new GameRepository.LoadCallback() {
            @Override
            public void onSuccess(GameState gameState) {
                if (gameEngine != null && gameState != null) {
                    // Check if the saved game is a game-over state (0 health)
                    if (gameState.dataCenterHealth <= 0) {
                        Log.d(TAG, "Saved game was game-over state. Starting fresh game instead.");
                        hasLoadedGame = true;

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(),
                                        "Previous game ended at Wave " + gameState.currentWave + ". Starting new game.",
                                        Toast.LENGTH_LONG).show();

                                // Delete the old save and start fresh
                                deleteAutoSave();
                                gameEngine.setPaused(true);
                                nextWaveFab.setVisibility(View.VISIBLE);
                            });
                        }
                        return;
                    }

                    // Restore the game state to the engine
                    gameEngine.restoreGameState(gameState);
                    hasLoadedGame = true; // Mark as loaded to prevent re-initialization
                    Log.d(TAG, "Game loaded successfully - Wave: " + gameState.currentWave +
                            ", Resources: " + gameState.resources);

                    // Update UI on main thread
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            // Set pause state
                            gameEngine.setPaused(true);

                            // Show next wave button if wave is complete
                            if (!gameEngine.getWaveManager().isWaveActive()) {
                                nextWaveFab.setVisibility(View.VISIBLE);
                            }

                            Toast.makeText(requireContext(),
                                    "Game loaded - Wave " + gameState.currentWave,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load game: " + error);
                hasLoadedGame = true; // Mark as loaded to prevent re-initialization
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                "No saved game found. Starting new game.",
                                Toast.LENGTH_SHORT).show();

                        // Set up for new game
                        gameEngine.setPaused(true);
                        nextWaveFab.setVisibility(View.VISIBLE);
                    });
                }
            }
        };
    }

    /**
     * Delete auto-save when starting a new game
     */
    private void deleteAutoSave() {
        if (viewModel == null) {
            return;
        }

        // Delete auto-save from Firebase
        viewModel.deleteAutoSave(new GameRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Firebase auto-save deleted successfully");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to delete Firebase auto-save: " + error);
            }
        });
    }

    /**
     * Handle game over event
     */
    private void handleGameOver(int finalWave) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Game Over! Final Wave: " + finalWave);

            // Keep the auto-save in database for record-keeping
            // The game state is preserved so users can see their final stats
            // Note: When continuing, the load logic will detect 0 health and start fresh

            // Get player name from SharedPreferences or use default
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("game_prefs", android.content.Context.MODE_PRIVATE);
            String playerName = prefs.getString("player_name", android.os.Build.MODEL); // Use device model as default

            // Get the final score from game engine
            long finalScore = gameEngine != null ? gameEngine.getScore() : 0;

            // Ensure user is authenticated and get userId for leaderboard submission
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                // User already authenticated, submit score
                submitScoreToLeaderboard(currentUser.getUid(), playerName, finalWave);
            } else {
                // Need to authenticate anonymously first
                auth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && auth.getCurrentUser() != null) {
                            String userId = auth.getCurrentUser().getUid();
                            submitScoreToLeaderboard(userId, playerName, finalWave);
                        } else {
                            Log.e(TAG, "Failed to authenticate for leaderboard submission");
                            if (isAdded()) {
                                Toast.makeText(requireContext(),
                                    "Warning: Failed to submit score to leaderboard",
                                    Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            }

            // Show game over dialog
            showGameOverDialog(finalWave, finalScore);
        });
    }

    /**
     * Submit score to leaderboard with userId
     */
    private void submitScoreToLeaderboard(String userId, String playerName, int wave) {
        edu.commonwealthu.lastserverstanding.data.firebase.FirebaseManager.getInstance()
            .submitHighScore(userId, playerName, wave, new edu.commonwealthu.lastserverstanding.data.firebase.FirebaseManager.LeaderboardCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Score submitted successfully to leaderboard - UserId: " + userId + ", Player: " + playerName + ", Wave: " + wave);
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                            "Score submitted to leaderboard!",
                            Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Failed to submit score: " + message);
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                            "Warning: Failed to submit score to leaderboard",
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    /**
     * Show game over dialog
     */
    private void showGameOverDialog(int finalWave, long finalScore) {
        if (!isAdded()) return;

        String message = String.format(Locale.getDefault(),
            "You reached Wave %d!\nFinal Score: %,d\n\nYour score has been submitted to the leaderboard.",
            finalWave,
            finalScore
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Game Over!")
                .setMessage(message)
                .setPositiveButton("Main Menu", (dialog, which) -> {
                    // Clear the game engine to prevent continuing
                    viewModel.resetGameEngine();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setNegativeButton("New Game", (dialog, which) -> {
                    // Reset game and start fresh
                    viewModel.resetGameEngine();
                    gameEngine = viewModel.getGameEngine();
                    gameEngine.setContext(requireContext());

                    // Re-initialize the game
                    if (gameView != null) {
                        gameView.setGameEngine(gameEngine);
                    }

                    // Set up game event listener
                    gameEngine.setGameListener(new GameEngine.GameListener() {
                        @Override
                        public void onGameOver(int finalWave) {
                            handleGameOver(finalWave);
                        }

                        @Override
                        public void onStatsChanged(int wave, int resources, int health, long score) {
                            if (viewModel != null) {
                                viewModel.updateGameStats(wave, resources, health, score);
                            }
                        }
                    });

                    // Mark as loaded to prevent re-initialization
                    hasLoadedGame = true;

                    // Start paused for tower placement
                    gameEngine.setPaused(true);
                    nextWaveFab.setVisibility(View.VISIBLE);

                    Log.d(TAG, "New game started after game over");
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Fragment paused - stopping game loop and auto-saving");

        if (gameView != null) {
            gameView.stopGameLoop();
        }

        // Stop HUD updates
        if (hudHandler != null && hudUpdater != null) {
            hudHandler.removeCallbacks(hudUpdater);
        }

        // Auto-pause game and auto-save when leaving GameFragment
        if (gameEngine != null) {
            gameEngine.setPaused(true);

            // Auto-save game state - this ensures progress is saved when:
            // - Going to settings
            // - Going to main menu
            // - App going to background
            // - User exiting the app
            saveGameState();
        }
    }

    /**
     * Save current game state as auto-save
     */
    private void saveGameState() {
        if (gameEngine == null || viewModel == null) {
            Log.w(TAG, "Cannot save game - engine or viewModel is null");
            return;
        }

        // Don't save if game is over - prevents saving with 0 health
        if (gameEngine.isGameOver()) {
            Log.d(TAG, "Skipping save - game is over");
            return;
        }

        int currentWave = gameEngine.getCurrentWave();
        Log.d(TAG, "Auto-save called - currentWave: " + currentWave);

        // Only save if there's actual progress (wave > 0)
        if (currentWave == 0) {
            Log.d(TAG, "Skipping save - no progress to save (wave 0)");
            return;
        }

        // Capture game state from engine
        GameState gameState = gameEngine.captureGameState();

        // Double-check health - don't save if health is 0 or less
        if (gameState.dataCenterHealth <= 0) {
            Log.d(TAG, "Skipping save - datacenter health is 0 or less");
            return;
        }

        Log.d(TAG, "Saving game state - Wave: " + gameState.currentWave +
                ", Resources: " + gameState.resources +
                ", Health: " + gameState.dataCenterHealth);

        // Save via ViewModel (always auto-save)
        viewModel.saveGame(gameState, true, new GameRepository.SaveCallback() {
            @Override
            public void onSuccess(int saveId) {
                Log.d(TAG, "Auto-save successful - Wave: " +
                        gameState.currentWave + ", SaveID: " + saveId);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to auto-save game: " + error);
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

        // Log current game state
        if (gameEngine != null) {
            Log.d(TAG, "Game state on resume - Wave: " + gameEngine.getCurrentWave() +
                    ", Resources: " + gameEngine.getResources() +
                    ", Paused: " + gameEngine.isPaused());
        }

        // Update play/pause button to reflect current state
        updatePlayPauseButton();
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
