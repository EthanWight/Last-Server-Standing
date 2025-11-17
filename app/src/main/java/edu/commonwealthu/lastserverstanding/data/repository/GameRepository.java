package edu.commonwealthu.lastserverstanding.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.models.Settings;

/**
 * Repository for managing data operations
 * Provides clean API between ViewModels and data sources
 * Now uses Firebase for cloud-based autosaves and settings
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
     * Load settings from Firebase
     */
    private void loadSettings() {
        firebaseSettingsRepository.loadSettings((soundEnabled, vibrationEnabled) -> {
            Settings loadedSettings = new Settings(soundEnabled, vibrationEnabled);
            settings.postValue(loadedSettings);
        });
    }

    /**
     * Save game state to Firebase
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
     * Load most recent auto-save from Firebase
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
     * Delete autosave from Firebase
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
     * Refresh player name for Firebase saves
     * Call this when the player changes their name in settings
     */
    public void refreshPlayerName() {
        firebaseSaveRepository.refreshPlayerName();
    }

    /**
     * Update settings
     */
    public void updateSettings(Settings newSettings) {
        firebaseSettingsRepository.saveSettings(
            newSettings.isSoundEnabled(),
            newSettings.isVibrationEnabled(),
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

    // LiveData getters
    public LiveData<Settings> getSettings() {
        return settings;
    }

    /**
     * Shutdown the executor service to prevent resource leaks
     * Should be called when the repository is no longer needed
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    // Callback interfaces
    public interface SaveCallback {
        void onSuccess(int saveId);
        void onError(String message);
    }

    public interface LoadCallback {
        void onSuccess(GameState gameState);
        void onError(String message);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String message);
    }
}
