package edu.commonwealthu.lastserverstanding.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import edu.commonwealthu.lastserverstanding.R;

/**
 * Manages sound effects for the game using SoundPool for low-latency playback.
 * Handles loading, playing, and releasing sound resources efficiently.
 * Supports enabling/disabling sounds based on user settings.
 *
 * @author Ethan Wight
 */
public class SoundManager {

    private static final String TAG = "SoundManager";

    /** Maximum simultaneous sound streams. */
    private static final int MAX_STREAMS = 10;

    /** Default sound volume (0.0 to 1.0). */
    private static final float DEFAULT_VOLUME = 1.0f;

    /** SoundPool for efficient sound playback. */
    private SoundPool soundPool;

    /** Map of sound IDs for quick lookup. */
    private final Map<SoundType, Integer> soundIds;

    /** Map to track which sounds have finished loading. */
    private final Map<Integer, Boolean> soundsLoaded;

    /** Whether sound effects are enabled. */
    private boolean soundEnabled = true;

    /** Application context for loading resources. */
    private final Context context;

    /**
     * Enumeration of all available sound effects in the game.
     */
    public enum SoundType {
        FIREWALL_BURN,
        HONEYPOT_STICKY,
        JAMMER_ZAP
    }

    /**
     * Creates a new SoundManager and initializes the SoundPool.
     *
     * @param context Application context for loading sound resources
     */
    public SoundManager(Context context) {
        this.context = context;
        this.soundIds = new HashMap<>();
        this.soundsLoaded = new HashMap<>();
        initializeSoundPool();
        loadSounds();
    }

    /**
     * Initializes the SoundPool with optimal settings for game audio.
     * Uses high-quality audio settings with automatic resampling for compatibility.
     */
    private void initializeSoundPool() {
        // Use USAGE_MEDIA for better audio quality and CONTENT_TYPE_MUSIC for higher fidelity
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build();

        // Set up listener to track when sounds finish loading
        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                soundsLoaded.put(sampleId, true);
                Log.d(TAG, "Sound loaded and resampled successfully: ID " + sampleId);
            } else {
                Log.e(TAG, "Failed to load sound: ID " + sampleId + ", status: " + status);
            }
        });

        Log.d(TAG, "SoundPool initialized with max streams: " + MAX_STREAMS);
    }

    /**
     * Loads all sound effects into memory.
     * Sound files must be placed in res/raw/ directory.
     * Gracefully handles missing sound files.
     */
    private void loadSounds() {
        int loadedCount = 0;

        loadedCount += loadSound(SoundType.FIREWALL_BURN, R.raw.firewall_burn);
        loadedCount += loadSound(SoundType.HONEYPOT_STICKY, R.raw.honeypot_sticky);
        loadedCount += loadSound(SoundType.JAMMER_ZAP, R.raw.jammer_zap);

        Log.d(TAG, "Loaded " + loadedCount + " of " + SoundType.values().length + " sound effects");
    }

    /**
     * Loads a single sound file by resource ID.
     *
     * @param soundType The type of sound to load
     * @param resourceId The resource ID of the raw sound file
     * @return 1 if loaded successfully, 0 otherwise
     */
    private int loadSound(SoundType soundType, int resourceId) {
        try {
            int soundId = soundPool.load(context, resourceId, 1);
            soundIds.put(soundType, soundId);
            soundsLoaded.put(soundId, false);
            Log.d(TAG, "Loading sound: " + soundType + " (ID: " + soundId + ") - will auto-resample if needed");
            return 1;
        } catch (Exception e) {
            Log.e(TAG, "Error loading sound " + soundType + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Plays a sound effect if sounds are enabled.
     *
     * @param soundType The type of sound to play
     */
    public void playSound(SoundType soundType) {
        if (!soundEnabled || soundPool == null) {
            return;
        }

        Integer soundId = soundIds.get(soundType);
        if (soundId != null) {
            // Check if sound has finished loading
            Boolean loaded = soundsLoaded.get(soundId);
            if (loaded != null && loaded) {
                soundPool.play(soundId, DEFAULT_VOLUME, DEFAULT_VOLUME, 1, 0, 1.0f);
            }
            // Silently skip if not loaded yet (avoid spam in logs)
        } else {
            Log.w(TAG, "Sound not found: " + soundType);
        }
    }

    /**
     * Sets whether sound effects are enabled.
     *
     * @param enabled True to enable sounds, false to disable
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        Log.d(TAG, "Sound effects " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Releases all sound resources.
     * Call this when the game is destroyed or no longer needs sounds.
     */
    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundIds.clear();
            Log.d(TAG, "SoundPool released");
        }
    }
}
