package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

import edu.commonwealthu.lastserverstanding.R;

/**
 * Main Menu Fragment - Entry point for the application
 * Provides navigation to game, leaderboards, and settings
 */
public class MainMenuFragment extends Fragment {
    
    private MaterialButton continueButton;
    private MaterialButton newGameButton;
    private MaterialButton leaderboardButton;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_menu, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        continueButton = view.findViewById(R.id.btn_continue);
        newGameButton = view.findViewById(R.id.btn_new_game);
        leaderboardButton = view.findViewById(R.id.btn_leaderboard);
        View settingsButton = view.findViewById(R.id.btn_settings);
        
        // Check if saved game exists
        boolean hasSavedGame = checkForSavedGame();
        continueButton.setVisibility(hasSavedGame ? View.VISIBLE : View.GONE);
        
        // Set up button listeners
        continueButton.setOnClickListener(v -> {
            // Navigate to game with saved state
            Bundle args = new Bundle();
            args.putBoolean("continue_game", true);
            Navigation.findNavController(v).navigate(R.id.action_menu_to_game, args);
        });
        
        newGameButton.setOnClickListener(v -> {
            // Navigate to game with new state
            Bundle args = new Bundle();
            args.putBoolean("continue_game", false);
            Navigation.findNavController(v).navigate(R.id.action_menu_to_game, args);
        });
        
        leaderboardButton.setOnClickListener(v -> {
            // Navigate to stats/leaderboard
            Navigation.findNavController(v).navigate(R.id.action_menu_to_stats);
        });
        
        settingsButton.setOnClickListener(v -> {
            // Navigate to settings
            Navigation.findNavController(v).navigate(R.id.action_menu_to_settings);
        });
        
        // Start background animation
        startBackgroundAnimation(view);
    }
    
    /**
     * Check if a saved game exists
     * @return true if save file exists
     */
    private boolean checkForSavedGame() {
        // TODO: Implement save game detection
        // For now, return false
        return false;
    }
    
    /**
     * Start animated background effect
     */
    private void startBackgroundAnimation(View view) {
        // TODO: Implement circuit board animation
        // This will be a custom animated drawable or canvas animation
    }
}
