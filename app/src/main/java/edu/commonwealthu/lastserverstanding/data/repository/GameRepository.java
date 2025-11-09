package edu.commonwealthu.lastserverstanding.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import edu.commonwealthu.lastserverstanding.data.GameDatabase;
import edu.commonwealthu.lastserverstanding.data.dao.SaveGameDao;
import edu.commonwealthu.lastserverstanding.data.dao.SettingsDao;
import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;
import edu.commonwealthu.lastserverstanding.data.entities.SettingsEntity;
import edu.commonwealthu.lastserverstanding.data.models.GameState;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing data operations
 * Provides clean API between ViewModels and data sources
 */
public class GameRepository {

    private SaveGameDao saveGameDao;
    private SettingsDao settingsDao;
    private ExecutorService executorService;

    // LiveData
    private LiveData<List<SaveGameEntity>> allSaves;
    private LiveData<SettingsEntity> settings;

    public GameRepository(Application application) {
        GameDatabase db = GameDatabase.getInstance(application);
        saveGameDao = db.saveGameDao();
        settingsDao = db.settingsDao();
        executorService = Executors.newFixedThreadPool(2);

        allSaves = saveGameDao.getAllSaves();
        settings = settingsDao.getSettings();

        // Initialize default settings if needed
        initializeDefaultSettings();
    }

    /**
     * Save game state
     */
    public void saveGame(GameState gameState, boolean isAutoSave, SaveCallback callback) {
        executorService.execute(() -> {
            try {
                String json = gameState.toJson();
                SaveGameEntity entity = new SaveGameEntity(
                    System.currentTimeMillis(),
                    json,
                    isAutoSave,
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

    /**
     * Load game state
     */
    public void loadGame(int saveId, LoadCallback callback) {
        executorService.execute(() -> {
            try {
                LiveData<SaveGameEntity> saveData = saveGameDao.getSaveById(saveId);
                // This is simplified - in real implementation, observe LiveData
                // For now, just notify success
                if (callback != null) {
                    callback.onSuccess(null); // Will fix this properly
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Delete save game
     */
    public void deleteSave(SaveGameEntity save, DeleteCallback callback) {
        executorService.execute(() -> {
            try {
                saveGameDao.delete(save);
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Update settings
     */
    public void updateSettings(SettingsEntity settings) {
        executorService.execute(() -> {
            settingsDao.update(settings);
        });
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
    public LiveData<List<SaveGameEntity>> getAllSaves() {
        return allSaves;
    }

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
