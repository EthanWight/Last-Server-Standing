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

    private final long timestamp;
    private final String gameStateJson; // Serialized GameState
    private final boolean isAutoSave;
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

    public String getGameStateJson() { return gameStateJson; }

    public boolean isAutoSave() { return isAutoSave; }

    public int getWave() { return wave; }
    public void setWave(int wave) { this.wave = wave; }

    public long getScore() { return score; }
    public void setScore(long score) { this.score = score; }
}