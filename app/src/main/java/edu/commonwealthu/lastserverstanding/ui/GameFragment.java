package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.view.GameView;

/**
 * Game Fragment - Main gameplay screen
 * Contains GameView and HUD overlay
 */
public class GameFragment extends Fragment {
    
    private GameView gameView;
    private GameEngine gameEngine;
    
    // HUD elements
    private TextView resourcesText;
    private LinearProgressIndicator healthBar;
    private TextView waveText;
    private FloatingActionButton pauseFab;
    private View emergencyBanner;
    
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
        pauseFab = view.findViewById(R.id.fab_pause);
        emergencyBanner = view.findViewById(R.id.emergency_banner);
        
        // Initialize game engine
        gameEngine = new GameEngine();
        
        // TODO: Load saved game if continuing
        if (continueGame) {
            loadSavedGame();
        }
        
        // Create and add GameView programmatically
        FrameLayout gameContainer = view.findViewById(R.id.game_view_container);
        gameView = new GameView(requireContext());
        gameView.setGameEngine(gameEngine);
        gameContainer.addView(gameView, 0); // Add below HUD
        
        // Set up pause button
        pauseFab.setOnClickListener(v -> togglePause());
        
        // Set up HUD update callback
        setupHUDUpdates();
        
        // Hide emergency banner initially
        emergencyBanner.setVisibility(View.GONE);
    }
    
    /**
     * Toggle game pause state
     */
    private void togglePause() {
        if (gameEngine != null) {
            boolean isPaused = !gameEngine.isPaused();
            gameEngine.setPaused(isPaused);
            
            // Update FAB icon
            pauseFab.setImageResource(isPaused ? 
                R.drawable.ic_play_arrow : R.drawable.ic_pause);
        }
    }
    
    /**
     * Set up periodic HUD updates
     */
    private void setupHUDUpdates() {
        // Create a handler to update HUD every 100ms
        // This keeps UI responsive without overwhelming it
        
        // TODO: Use a Handler or LiveData to update HUD
        // For now, this is a placeholder
    }
    
    /**
     * Update HUD elements with current game state
     */
    private void updateHUD() {
        if (gameEngine == null) return;
        
        // Update resources
        resourcesText.setText(String.format("Resources: %d", gameEngine.getResources()));
        
        // Update health bar
        int health = gameEngine.getDataCenterHealth();
        healthBar.setProgress(health);
        
        // Update health bar color based on health
        if (health > 60) {
            healthBar.setIndicatorColor(getResources().getColor(R.color.health_high, null));
        } else if (health > 30) {
            healthBar.setIndicatorColor(getResources().getColor(R.color.health_medium, null));
        } else {
            healthBar.setIndicatorColor(getResources().getColor(R.color.health_low, null));
        }
        
        // Update wave counter
        waveText.setText(String.format("Wave %d", gameEngine.getCurrentWave()));
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
     * Load saved game state
     */
    private void loadSavedGame() {
        // TODO: Implement save game loading
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.startGameLoop();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.stopGameLoop();
        }
        // Auto-pause game when leaving
        if (gameEngine != null) {
            gameEngine.setPaused(true);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameView != null) {
            gameView.stopGameLoop();
        }
    }
}
