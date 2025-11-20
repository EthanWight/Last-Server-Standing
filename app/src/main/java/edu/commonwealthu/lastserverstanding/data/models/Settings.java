package edu.commonwealthu.lastserverstanding.data.models;

/**
 * Settings data model (POJO).
 * Replaced SettingsEntity after moving to Firebase.
 *
 * @author Ethan Wight
 */
public class Settings {

    private boolean soundEnabled;
    private boolean vibrationEnabled;
    private boolean showTowerRanges;

    /**
     * Default constructor.
     */
    public Settings() {
        // Default values
        this.soundEnabled = true;
        this.vibrationEnabled = true;
        this.showTowerRanges = true;
    }

    /**
     * Constructor with sound and vibration settings.
     *
     * @param soundEnabled Whether sound is enabled.
     * @param vibrationEnabled Whether vibration is enabled.
     */
    public Settings(boolean soundEnabled, boolean vibrationEnabled) {
        this.soundEnabled = soundEnabled;
        this.vibrationEnabled = vibrationEnabled;
        this.showTowerRanges = true;
    }

    /**
     * Constructor with all settings.
     *
     * @param soundEnabled Whether sound is enabled.
     * @param vibrationEnabled Whether vibration is enabled.
     * @param showTowerRanges Whether to show tower ranges.
     */
    public Settings(boolean soundEnabled, boolean vibrationEnabled, boolean showTowerRanges) {
        this.soundEnabled = soundEnabled;
        this.vibrationEnabled = vibrationEnabled;
        this.showTowerRanges = showTowerRanges;
    }

    /**
     * Check if sound is enabled.
     *
     * @return True if sound is enabled.
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Set sound enabled state.
     *
     * @param soundEnabled Whether sound is enabled.
     */
    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    /**
     * Check if vibration is enabled.
     *
     * @return True if vibration is enabled.
     */
    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    /**
     * Set vibration enabled state.
     *
     * @param vibrationEnabled Whether vibration is enabled.
     */
    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    /**
     * Check if tower ranges should be shown.
     *
     * @return True if tower ranges should be shown.
     */
    public boolean isShowTowerRanges() {
        return showTowerRanges;
    }

    /**
     * Set whether to show tower ranges.
     *
     * @param showTowerRanges Whether to show tower ranges.
     */
    public void setShowTowerRanges(boolean showTowerRanges) {
        this.showTowerRanges = showTowerRanges;
    }
}
