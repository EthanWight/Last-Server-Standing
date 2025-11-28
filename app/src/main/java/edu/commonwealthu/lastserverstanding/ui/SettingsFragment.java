package edu.commonwealthu.lastserverstanding.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.models.Settings;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;
import edu.commonwealthu.lastserverstanding.game.GameEngine;
import edu.commonwealthu.lastserverstanding.viewmodel.GameViewModel;

/**
 * Settings Fragment - User preferences and configuration UI.
 * Allows users to customize player name, audio settings, and gameplay preferences.
 * Changes are saved to Firebase and persisted across sessions.
 *
 * @author Ethan Wight
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    /** ViewModel for managing game state and settings. */
    private GameViewModel viewModel;

    /** Text input for player name. */
    private com.google.android.material.textfield.TextInputEditText playerNameEdit;

    /** Toggle switch for sound effects. */
    private SwitchMaterial soundSwitch;

    /** Toggle switch for haptic feedback. */
    private SwitchMaterial vibrationSwitch;

    /** Toggle switch for tower range indicators. */
    private SwitchMaterial towerRangesSwitch;

    /** Current settings loaded from database. */
    private Settings currentSettings;

    /**
     * Creates and returns the settings layout.
     *
     * @param inflater Layout inflater for creating views
     * @param container Parent view container
     * @param savedInstanceState Saved state from previous instance
     * @return Inflated settings view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    /**
     * Initializes UI components and sets up event listeners.
     * Loads current settings from ViewModel and player name from SharedPreferences.
     *
     * @param view The created view
     * @param savedInstanceState Saved state from previous instance
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // Initialize views
        playerNameEdit = view.findViewById(R.id.edit_player_name);
        soundSwitch = view.findViewById(R.id.switch_sound);
        vibrationSwitch = view.findViewById(R.id.switch_vibration);
        towerRangesSwitch = view.findViewById(R.id.switch_tower_ranges);
        MaterialButton saveButton = view.findViewById(R.id.btn_save);
        MaterialButton backButton = view.findViewById(R.id.btn_back);
        MaterialButton mainMenuButton = view.findViewById(R.id.btn_main_menu);
        View infoContainer = view.findViewById(R.id.info_container);

        // Load player name from SharedPreferences
        loadPlayerName();

        // Load current settings
        loadSettings();

        // Set up listeners
        saveButton.setOnClickListener(v -> saveSettings());
        backButton.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        mainMenuButton.setOnClickListener(v -> goToMainMenu());
        infoContainer.setOnClickListener(v -> showGameInfo());
    }

    /**
     * Loads player name from SharedPreferences and populates the text field.
     * Uses device model as default if no name is set.
     */
    private void loadPlayerName() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("game_prefs", android.content.Context.MODE_PRIVATE);
        String playerName = prefs.getString("player_name", "");

        // If no name set, use device model as default
        if (playerName.isEmpty()) {
            playerName = android.os.Build.MODEL;
        }

        playerNameEdit.setText(playerName);
    }

    /**
     * Loads settings from ViewModel and updates UI switches.
     * Creates default settings if none exist.
     */
    private void loadSettings() {
        viewModel.getSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null) {
                currentSettings = settings;
                soundSwitch.setChecked(settings.isSoundEnabled());
                vibrationSwitch.setChecked(settings.isVibrationEnabled());
                towerRangesSwitch.setChecked(settings.isShowTowerRanges());
            } else {
                // Use defaults if no settings exist
                currentSettings = new Settings();
                soundSwitch.setChecked(true);
                vibrationSwitch.setChecked(true);
                towerRangesSwitch.setChecked(true);
            }
        });
    }

    /**
     * Saves all settings to database and SharedPreferences.
     * Updates player name in Firebase repository and navigates back.
     */
    private void saveSettings() {
        // Save player name to SharedPreferences
        String playerName = "";
        if (playerNameEdit != null && playerNameEdit.getText() != null) {
            playerName = playerNameEdit.getText().toString().trim();
        }
        if (playerName.isEmpty()) {
            playerName = android.os.Build.MODEL;
        }

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("game_prefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putString("player_name", playerName).apply();

        Log.d(TAG, "Player name saved: " + playerName);

        if (currentSettings == null) {
            currentSettings = new Settings();
        }

        // Update settings from UI
        currentSettings.setSoundEnabled(soundSwitch.isChecked());
        currentSettings.setVibrationEnabled(vibrationSwitch.isChecked());
        currentSettings.setShowTowerRanges(towerRangesSwitch.isChecked());

        // Save via ViewModel
        viewModel.updateSettings(currentSettings);
        
        // Refresh player name in Firebase repository for save continuity
        viewModel.refreshPlayerName();

        // Show confirmation
        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();

        // Navigate back
        Navigation.findNavController(requireView()).navigateUp();
    }

    /**
     * Navigates to main menu, silently saving game progress if present.
     * No popup is shown - save happens in background.
     */
    private void goToMainMenu() {
        GameEngine gameEngine = viewModel.getGameEngine();

        if (gameEngine != null && gameEngine.getCurrentWave() > 0) {
            // There's actual game progress to save - do it silently
            GameState gameState = gameEngine.captureGameState();
            Log.d(TAG, "Silently saving game state before returning to menu - Wave: " + gameState.currentWave);

            viewModel.saveGame(gameState, true, new GameRepository.SaveCallback() {
                @Override
                public void onSuccess(int saveId) {
                    Log.d(TAG, "Game auto-saved successfully - SaveID: " + saveId);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to save game: " + error);
                }
            });
        } else {
            Log.d(TAG, "No game progress to save");
        }

        // Navigate to main menu immediately (don't wait for save to complete)
        Navigation.findNavController(requireView()).navigate(R.id.action_settings_to_menu);
    }

    /**
     * Show game info dialog with tower and enemy information
     */
    private void showGameInfo() {
        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_game_info)
                .create();

        // Show dialog
        dialog.show();

        // Set up close button after dialog is shown
        if (dialog.getWindow() != null) {
            View dialogView = dialog.getWindow().getDecorView();
            MaterialButton closeButton = dialogView.findViewById(R.id.btn_close);
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> dialog.dismiss());
            }
        }
    }
}
