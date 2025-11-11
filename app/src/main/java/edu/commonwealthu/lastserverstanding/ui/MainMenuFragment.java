package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;

/**
 * Main Menu Fragment - Entry point for the application
 * Provides navigation to game, leaderboards, and settings
 */
public class MainMenuFragment extends Fragment {

    private static final String TAG = "MainMenuFragment";
    private MaterialButton continueButton;
    private MaterialButton newGameButton;
    private MaterialButton leaderboardButton;
    private GameViewModel viewModel;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_menu, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        // Initialize views
        continueButton = view.findViewById(R.id.btn_continue);
        newGameButton = view.findViewById(R.id.btn_new_game);
        leaderboardButton = view.findViewById(R.id.btn_leaderboard);
        View settingsButton = view.findViewById(R.id.btn_settings);

        // Hide continue button initially
        continueButton.setVisibility(View.GONE);

        // Observe saved games and show continue button if any exist with progress
        viewModel.getAllSaves().observe(getViewLifecycleOwner(), saves -> {
            Log.d(TAG, "Checking saved games - count: " + (saves != null ? saves.size() : 0));

            // Only show continue button if there are saves with actual game progress
            boolean hasValidSaves = saves != null && !saves.isEmpty();

            // Additional check: make sure saves have actual progress (not just initialization)
            if (hasValidSaves && saves != null) {
                hasValidSaves = false;
                for (int i = 0; i < saves.size(); i++) {
                    int wave = saves.get(i).getWave();
                    boolean isAutoSave = saves.get(i).isAutoSave();
                    Log.d(TAG, "Save " + i + " - Wave: " + wave + ", AutoSave: " + isAutoSave);

                    // Check if save has any meaningful progress (wave > 0)
                    if (wave > 0) {
                        hasValidSaves = true;
                        Log.d(TAG, "Found valid save with wave " + wave);
                        break;
                    }
                }
            }

            Log.d(TAG, "Continue button visibility: " + (hasValidSaves ? "VISIBLE" : "GONE"));
            continueButton.setVisibility(hasValidSaves ? View.VISIBLE : View.GONE);
        });
        
        // Set up button listeners
        continueButton.setOnClickListener(v -> {
            Log.d(TAG, "Continue button clicked - navigating to game");
            // Navigate to game with saved state
            Bundle args = new Bundle();
            args.putBoolean("continue_game", true);
            Navigation.findNavController(v).navigate(R.id.action_menu_to_game, args);
        });

        newGameButton.setOnClickListener(v -> {
            Log.d(TAG, "New Game button clicked - navigating to game");
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
     * Start animated background effect
     */
    private void startBackgroundAnimation(View view) {
        // TODO: Implement circuit board animation
        // This will be a custom animated drawable or canvas animation
    }
}
