package edu.commonwealthu.lastserverstanding.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a saved game state
 */
@Entity(tableName = "save_games")
public class SaveGameEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private long timestamp;
    private String gameStateJson; // Serialized GameState
    private boolean isAutoSave;
    private int wave;
    private long score;

    // Constructor
    public SaveGameEntity(long timestamp, String gameStateJson, boolean isAutoSave, int wave, long score) {
        this.timestamp = timestamp;
        this.gameStateJson = gameStateJson;
        this.isAutoSave = isAutoSave;
        this.wave = wave;
        this.score = score;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getGameStateJson() { return gameStateJson; }
    public void setGameStateJson(String gameStateJson) { this.gameStateJson = gameStateJson; }

    public boolean isAutoSave() { return isAutoSave; }
    public void setAutoSave(boolean autoSave) { isAutoSave = autoSave; }

    public int getWave() { return wave; }
    public void setWave(int wave) { this.wave = wave; }

    public long getScore() { return score; }
    public void setScore(long score) { this.score = score; }
}