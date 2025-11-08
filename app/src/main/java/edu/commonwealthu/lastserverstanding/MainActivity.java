package edu.commonwealthu.lastserverstanding;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.model.towers.FirewallTower;
import edu.commonwealthu.lastserverstanding.view.GameView;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;
import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;

/**
 * Main Activity for Last Server Standing
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private GameView gameView;
    private GameEngine gameEngine;
    private GameViewModel viewModel;
    private Handler waveHandler;
    private Runnable waveStarter;
    private Button btnNextWave;

    // Test configuration
    private static final boolean AUTO_START_WAVES = false;
    private static final int WAVE_DELAY_MS = 5000; // 5 seconds between waves
    private static final boolean ENABLE_TEST_TOWERS = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== Last Server Standing - Week 2 Test Mode ===");

        // Inflate the layout
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        // Create game engine
        gameEngine = new GameEngine();

        // Create game view
        gameView = new GameView(this);
        gameView.setGameEngine(gameEngine);

        // Add game view to container
        FrameLayout container = findViewById(R.id.game_container);
        container.addView(gameView, 0); // Add at index 0 so button stays on top

        // Set up next wave button
        btnNextWave = findViewById(R.id.btn_next_wave);
        btnNextWave.setText(getString(R.string.start_wave, 1));
        btnNextWave.setOnClickListener(v -> triggerNextWave());

        // Set world dimensions after view is created
        gameView.post(() -> {
            int width = gameView.getWidth();
            int height = gameView.getHeight();
            int gridSize = gameView.getGridSize();
            gameEngine.setWorldDimensions(width, height, gridSize);

            Log.d(TAG, String.format("World dimensions set: %dx%d, grid size: %d", width, height, gridSize));

            // Add test towers after dimensions are set
            if (ENABLE_TEST_TOWERS) {
                addTestTowers();
            }

            // Manual wave control enabled - use button to start waves
            Log.d(TAG, "Manual wave control enabled. Use 'Start Next Wave' button to begin.");
        });

        // Observe game data for future HUD updates
        observeGameData();

        Log.d(TAG, "MainActivity created, game loop starting...");
    }

    /**
     * Observe game data changes for HUD updates
     */
    private void observeGameData() {
        viewModel.getResources().observe(this, resources -> {
            // HUD update will be implemented in Week 4
            Log.d(TAG, "Resources updated: " + resources);
        });

        viewModel.getHealth().observe(this, health -> {
            // HUD update will be implemented in Week 4
            Log.d(TAG, "Health updated: " + health);
        });

        viewModel.getCurrentWave().observe(this, wave -> {
            // HUD update will be implemented in Week 4
            Log.d(TAG, "Wave updated: " + wave);
        });

        viewModel.getScore().observe(this, score -> {
            // HUD update will be implemented in Week 4
            Log.d(TAG, "Score updated: " + score);
        });
    }

    /**
     * Save game state
     */
    private void saveGame(boolean isAutoSave) {
        GameState state = gameEngine.captureGameState();
        viewModel.saveGame(state, isAutoSave, new GameRepository.SaveCallback() {
            @Override
            public void onSuccess(int saveId) {
                runOnUiThread(() -> {
                    String message = isAutoSave ? "Auto-saved!" : "Game saved!";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Game saved with ID: " + saveId);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Save failed: " + message, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Save failed: " + message);
                });
            }
        });
    }

    /**
     * Add test towers to demonstrate pathfinding obstacle avoidance
     * These towers will force enemies to path around them
     */
    private void addTestTowers() {
        Log.d(TAG, "Adding test towers for pathfinding demonstration...");
        
        // Create a defensive line of towers
        // Enemies will need to path around these obstacles
        
        // Tower 1: Left side defense
        FirewallTower tower1 = new FirewallTower(new PointF(300, 200));
        gameEngine.addTower(tower1);
        Log.d(TAG, "Tower 1 placed at (300, 200)");
        
        // Tower 2: Center defense
        FirewallTower tower2 = new FirewallTower(new PointF(500, 300));
        gameEngine.addTower(tower2);
        Log.d(TAG, "Tower 2 placed at (500, 300)");
        
        // Tower 3: Right side defense
        FirewallTower tower3 = new FirewallTower(new PointF(700, 200));
        gameEngine.addTower(tower3);
        Log.d(TAG, "Tower 3 placed at (700, 200)");
        
        // Tower 4: Bottom defense
        FirewallTower tower4 = new FirewallTower(new PointF(400, 450));
        gameEngine.addTower(tower4);
        Log.d(TAG, "Tower 4 placed at (400, 450)");
        
        Log.d(TAG, String.format("Test towers added. Total resources remaining: %d", 
            gameEngine.getResources()));
    }

    /**
     * Start the wave system with automatic progression
     * This tests the WaveManager integration
     */
    private void startWaveSystem() {
        waveHandler = new Handler();
        waveStarter = new Runnable() {
            @Override
            public void run() {
                if (gameEngine == null) return;
                
                // Check if current wave is complete
                if (!gameEngine.getWaveManager().isWaveActive()) {
                    int nextWave = gameEngine.getCurrentWave() + 1;
                    
                    Log.d(TAG, "==========================================");
                    Log.d(TAG, String.format("STARTING WAVE %d", nextWave));
                    Log.d(TAG, String.format("Resources: %d | Health: %d | Score: %d",
                        gameEngine.getResources(),
                        gameEngine.getDataCenterHealth(),
                        gameEngine.getScore()));
                    Log.d(TAG, "==========================================");
                    
                    // Start next wave
                    gameEngine.startNextWave();
                    
                    // Log wave info
                    Log.d(TAG, String.format("Wave %d started - Enemies to spawn: %d",
                        gameEngine.getCurrentWave(),
                        gameEngine.getWaveManager().getTotalEnemiesThisWave()));
                    
                    // Stop auto-progression after wave 5 for testing
                    if (AUTO_START_WAVES && nextWave < 5) {
                        waveHandler.postDelayed(this, WAVE_DELAY_MS);
                    } else if (nextWave >= 5) {
                        Log.d(TAG, "Auto-progression stopped at wave 5. Manual testing from here.");
                    }
                } else {
                    // Wave still active, check again soon
                    waveHandler.postDelayed(this, 1000);
                }
            }
        };
        
        // Start first wave after short delay
        Log.d(TAG, "Wave system initialized. First wave starting in 2 seconds...");
        waveHandler.postDelayed(waveStarter, 2000);
    }

    /**
     * Manual wave trigger (for testing)
     * Can be called from touch input or button later
     */
    public void triggerNextWave() {
        if (gameEngine != null && !gameEngine.getWaveManager().isWaveActive()) {
            Log.d(TAG, "Manual wave trigger!");
            gameEngine.startNextWave();

            // Update button text to show next wave number
            int nextWave = gameEngine.getCurrentWave() + 1;
            btnNextWave.setText(getString(R.string.start_wave, nextWave));
        } else {
            Log.d(TAG, "Cannot start wave - wave already active or game engine null");
        }
    }

    /**
     * Log game state for debugging
     */
    private void logGameState() {
        if (gameEngine == null) return;
        
        Log.d(TAG, "--- Game State ---");
        Log.d(TAG, "Wave: " + gameEngine.getCurrentWave());
        Log.d(TAG, "Resources: " + gameEngine.getResources());
        Log.d(TAG, "Health: " + gameEngine.getDataCenterHealth());
        Log.d(TAG, "Score: " + gameEngine.getScore());
        Log.d(TAG, "Towers: " + gameEngine.getTowers().size());
        Log.d(TAG, "Enemies: " + gameEngine.getEnemies().size());
        Log.d(TAG, "Projectiles: " + gameEngine.getProjectiles().size());
        Log.d(TAG, "FPS: " + gameEngine.getFPS());
        
        if (gameEngine.getWaveManager() != null) {
            Log.d(TAG, "Wave Active: " + gameEngine.getWaveManager().isWaveActive());
            Log.d(TAG, "Wave Progress: " + 
                String.format("%.1f%%", gameEngine.getWaveManager().getWaveProgress() * 100));
        }
        Log.d(TAG, "------------------");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused - stopping game loop");

        if (gameView != null) {
            gameView.stopGameLoop();
        }

        if (waveHandler != null) {
            waveHandler.removeCallbacks(waveStarter);
        }

        // Auto-save when app is paused
        saveGame(true);

        // Log final state
        logGameState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed - restarting game loop");
        
        if (gameView != null) {
            gameView.startGameLoop();
        }
        
        // Resume wave progression if it was active
        if (waveHandler != null && waveStarter != null && AUTO_START_WAVES) {
            waveHandler.postDelayed(waveStarter, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed - cleaning up");
        
        if (gameView != null) {
            gameView.stopGameLoop();
        }
        
        if (waveHandler != null) {
            waveHandler.removeCallbacks(waveStarter);
        }
        
        // Final game statistics
        if (gameEngine != null) {
            Log.d(TAG, "=== Final Game Statistics ===");
            Log.d(TAG, "Waves Completed: " + gameEngine.getCurrentWave());
            Log.d(TAG, "Final Score: " + gameEngine.getScore());
            Log.d(TAG, "Towers Placed: " + gameEngine.getTowers().size());
            Log.d(TAG, "===========================");
        }
    }
}
