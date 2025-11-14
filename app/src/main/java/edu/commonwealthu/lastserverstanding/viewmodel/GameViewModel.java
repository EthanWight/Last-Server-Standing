package edu.commonwealthu.lastserverstanding.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import edu.commonwealthu.lastserverstanding.data.entities.SaveGameEntity;
import edu.commonwealthu.lastserverstanding.data.entities.SettingsEntity;
import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;
import edu.commonwealthu.lastserverstanding.game.GameEngine;
import java.util.List;

/**
 * ViewModel for managing game data with lifecycle awareness
 */
public class GameViewModel extends AndroidViewModel {

    private GameRepository repository;

    // LiveData from repository
    private LiveData<List<SaveGameEntity>> allSaves;
    private LiveData<SettingsEntity> settings;

    // Game state LiveData
    private MutableLiveData<Integer> currentWave = new MutableLiveData<>(0);
    private MutableLiveData<Integer> resources = new MutableLiveData<>(500);
    private MutableLiveData<Integer> health = new MutableLiveData<>(100);
    private MutableLiveData<Long> score = new MutableLiveData<>(0L);

    // Game engine instance (persists across navigation and config changes)
    private GameEngine gameEngine;

    public GameViewModel(Application application) {
        super(application);
        repository = new GameRepository(application);
        allSaves = repository.getAllSaves();
        settings = repository.getSettings();
    }

    /**
     * Save current game state
     */
    public void saveGame(GameState gameState, boolean isAutoSave, GameRepository.SaveCallback callback) {
        repository.saveGame(gameState, isAutoSave, callback);
    }

    /**
     * Load saved game by ID
     */
    public void loadGame(int saveId, GameRepository.LoadCallback callback) {
        repository.loadGame(saveId, callback);
    }

    /**
     * Load most recent auto-save
     */
    public void loadLatestAutoSave(GameRepository.LoadCallback callback) {
        repository.loadLatestAutoSave(callback);
    }

    /**
     * Delete save
     */
    public void deleteSave(SaveGameEntity save, GameRepository.DeleteCallback callback) {
        repository.deleteSave(save, callback);
    }

    /**
     * Update settings
     */
    public void updateSettings(SettingsEntity settings) {
        repository.updateSettings(settings);
    }

    /**
     * Update game stats (called from GameEngine)
     */
    public void updateGameStats(int wave, int res, int hp, long sc) {
        currentWave.postValue(wave);
        resources.postValue(res);
        health.postValue(hp);
        score.postValue(sc);
    }

    // Getters for LiveData
    public LiveData<List<SaveGameEntity>> getAllSaves() { return allSaves; }
    public LiveData<SettingsEntity> getSettings() { return settings; }
    public LiveData<Integer> getCurrentWave() { return currentWave; }
    public LiveData<Integer> getResources() { return resources; }
    public LiveData<Integer> getHealth() { return health; }
    public LiveData<Long> getScore() { return score; }

    /**
     * Get or create the game engine instance
     * This ensures the same instance is used across navigation
     */
    public GameEngine getGameEngine() {
        if (gameEngine == null) {
            gameEngine = new GameEngine();
        }
        return gameEngine;
    }

    /**
     * Reset game engine (for starting a new game)
     */
    public void resetGameEngine() {
        gameEngine = new GameEngine();
    }

    /**
     * Check if game engine exists (has an active game)
     */
    public boolean hasActiveGame() {
        return gameEngine != null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shutdown repository executor service to prevent resource leaks
        if (repository != null) {
            repository.shutdown();
        }
    }
}
