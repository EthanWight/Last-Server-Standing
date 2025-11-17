package edu.commonwealthu.lastserverstanding.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.commonwealthu.lastserverstanding.data.GameDatabase;
import edu.commonwealthu.lastserverstanding.data.dao.SaveGameDao;
import edu.commonwealthu.lastserverstanding.data.dao.SettingsDao;
import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;
import edu.commonwealthu.lastserverstanding.data.entities.SettingsEntity;
import edu.commonwealthu.lastserverstanding.data.models.GameState;

/**
 * Repository for managing data operations
 * Provides clean API between ViewModels and data sources
 * Now uses Firebase for cloud-based autosaves
 */
public class GameRepository {

    private final SaveGameDao saveGameDao;
    private final SettingsDao settingsDao;
    private final ExecutorService executorService;
    private final FirebaseSaveRepository firebaseSaveRepository;

    // LiveData
    private final LiveData<SettingsEntity> settings;

    public GameRepository(Application application) {
        GameDatabase db = GameDatabase.getInstance(application);
        saveGameDao = db.saveGameDao();
        settingsDao = db.settingsDao();
        executorService = Executors.newFixedThreadPool(2);
        firebaseSaveRepository = new FirebaseSaveRepository();

        settings = settingsDao.getSettings();

        // Initialize default settings if needed
        initializeDefaultSettings();
    }

    /**
     * Save game state
     * Autosaves go to Firebase (cloud), manual saves go to local Room database
     */
    public void saveGame(GameState gameState, boolean isAutoSave, SaveCallback callback) {
        if (isAutoSave) {
            // Use Firebase for autosaves - persists even when app is killed
            firebaseSaveRepository.saveGame(gameState, true, new FirebaseSaveRepository.SaveCallback() {
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
        } else {
            // Use Room database for manual saves (if needed in the future)
            executorService.execute(() -> {
                try {
                    String json = gameState.toJson();
                    SaveGameEntity entity = new SaveGameEntity(
                        System.currentTimeMillis(),
                        json,
                        false, // Manual save, not auto-save
                        gameState.currentWave,
                        gameState.score
                    );

                    long id = saveGameDao.insert(entity);

                    if (callback != null) {
                        callback.onSuccess((int)id);
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }
            });
        }
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
    public void updateSettings(SettingsEntity settings) {
        executorService.execute(() -> settingsDao.update(settings));
    }

    /**
     * Initialize default settings if not exists
     */
    private void initializeDefaultSettings() {
        executorService.execute(() -> {
            SettingsEntity existing = settingsDao.getSettingsSync();
            if (existing == null) {
                settingsDao.insert(new SettingsEntity());
            }
        });
    }

    // LiveData getters
    public LiveData<SettingsEntity> getSettings() {
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
