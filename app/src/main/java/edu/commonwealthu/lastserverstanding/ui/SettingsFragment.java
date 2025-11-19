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
 * Settings Fragment - User preferences and configuration
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private GameViewModel viewModel;

    // UI Elements
    private com.google.android.material.textfield.TextInputEditText playerNameEdit;
    private SwitchMaterial soundSwitch;
    private SwitchMaterial vibrationSwitch;
    private SwitchMaterial towerRangesSwitch;

    private Settings currentSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

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
     * Load player name from SharedPreferences
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
     * Load settings from ViewModel
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
     * Save settings to database
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
     * Navigate to main menu (saves current game progress)
     */
    private void goToMainMenu() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Return to Main Menu")
                .setMessage("Return to main menu? Your progress will be saved.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Save current game state before returning to menu
                    saveCurrentGameAndReturnToMenu();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Save current game state and navigate to main menu
     */
    private void saveCurrentGameAndReturnToMenu() {
        GameEngine gameEngine = viewModel.getGameEngine();

        if (gameEngine != null && gameEngine.getCurrentWave() > 0) {
            // There's actual game progress to save
            GameState gameState = gameEngine.captureGameState();

            Log.d(TAG, "Saving game state before returning to menu - Wave: " + gameState.currentWave);

            viewModel.saveGame(gameState, true, new GameRepository.SaveCallback() {
                @Override
                public void onSuccess(int saveId) {
                    Log.d(TAG, "Game auto-saved successfully - SaveID: " + saveId);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Game saved", Toast.LENGTH_SHORT).show();

                            // Navigate to main menu
                            Navigation.findNavController(requireView()).navigate(R.id.action_settings_to_menu);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to save game: " + error);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            // Still navigate even if save failed
                            Toast.makeText(requireContext(), "Warning: Failed to save game", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).navigate(R.id.action_settings_to_menu);
                        });
                    }
                }
            });
        } else {
            // No progress to save, just navigate
            Log.d(TAG, "No game progress to save - returning to menu");
            Navigation.findNavController(requireView()).navigate(R.id.action_settings_to_menu);
        }
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
