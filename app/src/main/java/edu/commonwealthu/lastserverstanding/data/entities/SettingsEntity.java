package edu.commonwealthu.lastserverstanding.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Singleton entity for app settings
 */
@Entity(tableName = "settings")
public class SettingsEntity {

    @PrimaryKey
    private int id = 1; // Always 1 (singleton)

    private boolean soundEnabled;
    private boolean vibrationEnabled;
    private float accelerometerSensitivity;

    public SettingsEntity() {
        // Default values
        this.soundEnabled = true;
        this.vibrationEnabled = true;
        this.accelerometerSensitivity = 0.5f;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }

    public boolean isVibrationEnabled() { return vibrationEnabled; }
    public void setVibrationEnabled(boolean vibrationEnabled) { this.vibrationEnabled = vibrationEnabled; }

    public float getAccelerometerSensitivity() { return accelerometerSensitivity; }
    public void setAccelerometerSensitivity(float sensitivity) { this.accelerometerSensitivity = sensitivity; }
}