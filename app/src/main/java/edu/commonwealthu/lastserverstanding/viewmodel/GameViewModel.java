package edu.commonwealthu.lastserverstanding.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import edu.commonwealthu.lastserverstanding.data.models.GameState;
import edu.commonwealthu.lastserverstanding.data.models.Settings;
import edu.commonwealthu.lastserverstanding.data.repository.GameRepository;
import edu.commonwealthu.lastserverstanding.game.GameEngine;

/**
 * ViewModel for managing game data with lifecycle awareness
 */
public class GameViewModel extends AndroidViewModel {

    private final GameRepository repository;

    // LiveData from repository
    private final LiveData<Settings> settings;

    // Game state LiveData
    private final MutableLiveData<Integer> currentWave = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> resources = new MutableLiveData<>(500);
    private final MutableLiveData<Integer> health = new MutableLiveData<>(100);
    private final MutableLiveData<Long> score = new MutableLiveData<>(0L);

    // Game engine instance (persists across navigation and config changes)
    private GameEngine gameEngine;

    public GameViewModel(Application application) {
        super(application);
        repository = new GameRepository();
        settings = repository.getSettings();
    }

    /**
     * Save current game state
     */
    public void saveGame(GameState gameState, boolean isAutoSave, GameRepository.SaveCallback callback) {
        repository.saveGame(gameState, isAutoSave, callback);
    }

    /**
     * Load most recent auto-save
     */
    public void loadLatestAutoSave(GameRepository.LoadCallback callback) {
        repository.loadLatestAutoSave(callback);
    }

    /**
     * Delete autosave from Firebase
     */
    public void deleteAutoSave(GameRepository.DeleteCallback callback) {
        repository.deleteAutoSave(callback);
    }

    /**
     * Update settings
     */
    public void updateSettings(Settings settings) {
        repository.updateSettings(settings);
    }

    /**
     * Refresh player name for Firebase saves
     */
    public void refreshPlayerName() {
        repository.refreshPlayerName();
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
    public LiveData<Settings> getSettings() { return settings; }
    public LiveData<Integer> getCurrentWave() { return currentWave; }
    public LiveData<Integer> getResources() { return resources; }
    public LiveData<Integer> getHealth() { return health; }

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
