package edu.commonwealthu.lastserverstanding.data.models;

/**
 * Settings data model (POJO)
 * Replaced SettingsEntity after moving to Firebase
 */
public class Settings {

    private boolean soundEnabled;
    private boolean vibrationEnabled;

    public Settings() {
        // Default values
        this.soundEnabled = true;
        this.vibrationEnabled = true;
    }

    public Settings(boolean soundEnabled, boolean vibrationEnabled) {
        this.soundEnabled = soundEnabled;
        this.vibrationEnabled = vibrationEnabled;
    }

    // Getters and Setters
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }
}
