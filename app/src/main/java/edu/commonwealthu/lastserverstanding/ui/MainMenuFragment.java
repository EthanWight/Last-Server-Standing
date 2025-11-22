package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;

/**
 * Main Menu Fragment - Entry point for the application UI.
 * Serves as the primary navigation hub providing buttons for starting a new game, continuing a saved game,
 * viewing leaderboards, and accessing settings. The continue button dynamically appears when a saved game
 * with progress is detected in Firebase, and an animated binary rain background creates a cybersecurity-themed atmosphere.
 *
 * @author Ethan Wight
 */
public class MainMenuFragment extends Fragment {

    private static final String TAG = "MainMenuFragment";
    private MaterialButton continueButton;
    private GameViewModel viewModel;

    /**
     * Creates and returns the view hierarchy associated with this fragment.
     *
     * @param inflater The LayoutInflater used to inflate views in this fragment
     * @param container The parent view that this fragment's UI should be attached to
     * @param savedInstanceState Previous state data (not used in this fragment)
     * @return The inflated view for the main menu UI
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_menu, container, false);
    }

    /**
     * Called immediately after onCreateView to perform final initialization.
     * Initializes the ViewModel, sets up button listeners, and checks Firebase for saved games.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState Previous state data (not used in this fragment)
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel (use activity scope for consistency)
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // Initialize views
        continueButton = view.findViewById(R.id.btn_continue);
        MaterialButton newGameButton = view.findViewById(R.id.btn_new_game);
        MaterialButton leaderboardButton = view.findViewById(R.id.btn_leaderboard);
        View settingsButton = view.findViewById(R.id.btn_settings);

        // Hide continue button initially
        continueButton.setVisibility(View.GONE);

        // Check Firebase for autosaves to show/hide continue button
        checkForFirebaseSave(viewModel);

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

        // Background animation (BinaryRainView) starts automatically when attached to window
    }

    /**
     * Called when the fragment becomes visible to the user.
     * Refreshes the continue button visibility to reflect the current save state.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Refresh continue button when returning to main menu
        if (viewModel != null) {
            checkForFirebaseSave(viewModel);
        }
    }

    /**
     * Checks Firebase for auto-saved games and updates continue button visibility.
     * Shows the button only if a save with progress exists.
     *
     * @param viewModel The GameViewModel to use for loading and checking saved games
     */
    private void checkForFirebaseSave(GameViewModel viewModel) {
        Log.d(TAG, "==========================================");
        Log.d(TAG, "checkForFirebaseSave: START");
        Log.d(TAG, "==========================================");

        viewModel.loadLatestAutoSave(new GameRepository.LoadCallback() {
            @Override
            public void onSuccess(
                    edu.commonwealthu.lastserverstanding.data.models.GameState gameState) {
                Log.d(TAG, "checkForFirebaseSave: onSuccess callback received");
                Log.d(TAG, "checkForFirebaseSave: gameState != null? "
                        + (gameState != null));

                // Found a save - show continue button if it has progress
                if (gameState != null && gameState.currentWave > 0) {
                    Log.d(TAG, "checkForFirebaseSave: ✓✓✓ SHOWING CONTINUE BUTTON");
                    Log.d(TAG, "checkForFirebaseSave: Wave: " + gameState.currentWave
                            + ", Score: " + gameState.score);
                    continueButton.setVisibility(View.VISIBLE);
                    Log.d(TAG, "checkForFirebaseSave: Button visibility set to VISIBLE");
                } else {
                    Log.d(TAG, "checkForFirebaseSave: ✗ Autosave has no progress (wave 0)");
                    continueButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                // No save found or error - hide continue button
                Log.e(TAG, "checkForFirebaseSave: ✗✗✗ ERROR - Hiding continue button");
                Log.e(TAG, "checkForFirebaseSave: Error message: " + error);
                continueButton.setVisibility(View.GONE);
            }
        });

        Log.d(TAG, "checkForFirebaseSave: Load request sent, waiting for callback...");
    }
}
