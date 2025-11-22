package edu.commonwealthu.lastserverstanding.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.models.Settings;

/**
 * Repository for managing data operations.
 * Provides clean API between ViewModels and data sources.
 * Now uses Firebase for cloud-based autosaves and settings.
 *
 * @author Ethan Wight
 */
public class GameRepository {

    private final ExecutorService executorService;
    private final FirebaseSaveRepository firebaseSaveRepository;
    private final FirebaseSettingsRepository firebaseSettingsRepository;

    // LiveData
    private final MutableLiveData<Settings> settings;

    public GameRepository() {
        executorService = Executors.newFixedThreadPool(2);
        firebaseSaveRepository = new FirebaseSaveRepository();
        firebaseSettingsRepository = new FirebaseSettingsRepository();

        // Initialize settings LiveData with defaults
        settings = new MutableLiveData<>(new Settings());

        // Load settings from Firebase
        loadSettings();
    }

    /**
     * Load settings from Firebase.
     */
    private void loadSettings() {
        firebaseSettingsRepository.loadSettings((soundEnabled, vibrationEnabled, showTowerRanges) -> {
            Settings loadedSettings = new Settings(soundEnabled, vibrationEnabled, showTowerRanges);
            settings.postValue(loadedSettings);
        });
    }

    /**
     * Save game state to Firebase.
     *
     * @param gameState The game state to save.
     * @param isAutoSave Whether this is an autosave.
     * @param callback The callback to handle save result.
     */
    public void saveGame(GameState gameState, boolean isAutoSave, SaveCallback callback) {
        firebaseSaveRepository.saveGame(gameState, isAutoSave, new FirebaseSaveRepository.SaveCallback() {
            @Override
            public void onSuccess(String saveId) {
                if (callback != null) {
                    callback.onSuccess(saveId.hashCode()); // Convert string ID to int for compatibility
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Load most recent auto-save from Firebase.
     *
     * @param callback The callback to handle loaded game state.
     */
    public void loadLatestAutoSave(LoadCallback callback) {
        firebaseSaveRepository.loadLatestAutoSave(new FirebaseSaveRepository.LoadCallback() {
            @Override
            public void onSuccess(GameState gameState) {
                if (callback != null) {
                    callback.onSuccess(gameState);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Delete autosave from Firebase.
     *
     * @param callback The callback to handle delete result.
     */
    public void deleteAutoSave(DeleteCallback callback) {
        firebaseSaveRepository.deleteAutoSave(new FirebaseSaveRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Refresh player name for Firebase saves.
     * Call this when the player changes their name in settings.
     */
    public void refreshPlayerName() {
        firebaseSaveRepository.refreshPlayerName();
    }

    /**
     * Update settings.
     *
     * @param newSettings The new settings to apply.
     */
    public void updateSettings(Settings newSettings) {
        firebaseSettingsRepository.saveSettings(
            newSettings.isSoundEnabled(),
            newSettings.isVibrationEnabled(),
            newSettings.isShowTowerRanges(),
            new FirebaseSettingsRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    // Update local LiveData
                    settings.postValue(newSettings);
                }

                @Override
                public void onError(String error) {
                    // Could notify UI about error, but for now just log
                    android.util.Log.e("GameRepository", "Failed to save settings: " + error);
                }
            }
        );
    }

    /**
     * Get settings LiveData.
     *
     * @return LiveData containing user settings.
     */
    public LiveData<Settings> getSettings() {
        return settings;
    }

    /**
     * Shutdown the executor service to prevent resource leaks.
     * Should be called when the repository is no longer needed.
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Callback interface for save operations.
     *
     * @author Ethan Wight
     */
    public interface SaveCallback {
        /**
         * Called when save succeeds.
         *
         * @param saveId The ID of the saved game.
         */
        void onSuccess(int saveId);

        /**
         * Called when save fails.
         *
         * @param message The error message.
         */
        void onError(String message);
    }

    /**
     * Callback interface for load operations.
     *
     * @author Ethan Wight
     */
    public interface LoadCallback {
        /**
         * Called when load succeeds.
         *
         * @param gameState The loaded game state.
         */
        void onSuccess(GameState gameState);

        /**
         * Called when load fails.
         *
         * @param message The error message.
         */
        void onError(String message);
    }

    /**
     * Callback interface for delete operations.
     *
     * @author Ethan Wight
     */
    public interface DeleteCallback {
        /**
         * Called when delete succeeds.
         */
        void onSuccess();

        /**
         * Called when delete fails.
         *
         * @param message The error message.
         */
        void onError(String message);
    }
}
