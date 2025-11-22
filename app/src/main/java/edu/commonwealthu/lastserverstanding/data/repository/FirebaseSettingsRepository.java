package edu.commonwealthu.lastserverstanding.data.repository;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing user settings in Firebase Firestore.
 * Uses anonymous authentication to sync settings across sessions.
 *
 * @author Ethan Wight
 */
public class FirebaseSettingsRepository {

    private static final String TAG = "FirebaseSettingsRepo";
    private static final String COLLECTION_SETTINGS = "user_settings";
    private static final String FIELD_SOUND_ENABLED = "soundEnabled";
    private static final String FIELD_VIBRATION_ENABLED = "vibrationEnabled";
    private static final String FIELD_SHOW_TOWER_RANGES = "showTowerRanges";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    // Default settings
    private static final boolean DEFAULT_SOUND_ENABLED = true;
    private static final boolean DEFAULT_VIBRATION_ENABLED = true;
    private static final boolean DEFAULT_SHOW_TOWER_RANGES = true;

    public FirebaseSettingsRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Ensure user is authenticated (anonymously).
     *
     * @param callback The callback to handle authentication result.
     */
    public void ensureAuthenticated(AuthCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "User already authenticated: " + currentUser.getUid());
            callback.onSuccess(currentUser.getUid());
            return;
        }

        // Sign in anonymously
        Log.d(TAG, "Signing in anonymously...");
        auth.signInAnonymously()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        Log.d(TAG, "Anonymous sign-in successful: " + user.getUid());
                        callback.onSuccess(user.getUid());
                    } else {
                        callback.onError("Authentication succeeded but user is null");
                    }
                } else {
                    String error = task.getException() != null ?
                        task.getException().getMessage() : "Unknown error";
                    Log.e(TAG, "Anonymous sign-in failed: " + error);
                    callback.onError("Authentication failed: " + error);
                }
            });
    }

    /**
     * Load settings from Firestore.
     *
     * @param callback The callback to handle loaded settings.
     */
    public void loadSettings(LoadCallback callback) {
        ensureAuthenticated(new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                performLoad(userId, callback);
            }

            @Override
            public void onError(String error) {
                // If auth fails, return default settings
                Log.w(TAG, "Authentication failed, using default settings: " + error);
                callback.onSuccess(DEFAULT_SOUND_ENABLED, DEFAULT_VIBRATION_ENABLED, DEFAULT_SHOW_TOWER_RANGES);
            }
        });
    }

    /**
     * Perform the actual load operation.
     *
     * @param userId The user ID to load settings for.
     * @param callback The callback to handle loaded settings.
     */
    private void performLoad(String userId, LoadCallback callback) {
        firestore.collection(COLLECTION_SETTINGS)
            .document(userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        // Load settings from Firestore
                        Boolean soundEnabled = document.getBoolean(FIELD_SOUND_ENABLED);
                        Boolean vibrationEnabled = document.getBoolean(FIELD_VIBRATION_ENABLED);
                        Boolean showTowerRanges = document.getBoolean(FIELD_SHOW_TOWER_RANGES);

                        // Use defaults if fields are missing
                        boolean sound = soundEnabled != null ? soundEnabled : DEFAULT_SOUND_ENABLED;
                        boolean vibration = vibrationEnabled != null ? vibrationEnabled : DEFAULT_VIBRATION_ENABLED;
                        boolean ranges = showTowerRanges != null ? showTowerRanges : DEFAULT_SHOW_TOWER_RANGES;

                        Log.d(TAG, "Loaded settings for user " + userId);
                        callback.onSuccess(sound, vibration, ranges);
                    } else {
                        // No settings found, return defaults
                        Log.d(TAG, "No settings found for user, using defaults");
                        callback.onSuccess(DEFAULT_SOUND_ENABLED, DEFAULT_VIBRATION_ENABLED, DEFAULT_SHOW_TOWER_RANGES);
                    }
                } else {
                    Log.e(TAG, "Failed to load settings", task.getException());
                    // On error, return defaults
                    callback.onSuccess(DEFAULT_SOUND_ENABLED, DEFAULT_VIBRATION_ENABLED, DEFAULT_SHOW_TOWER_RANGES);
                }
            });
    }

    /**
     * Save settings to Firestore.
     *
     * @param soundEnabled Whether sound is enabled.
     * @param vibrationEnabled Whether vibration is enabled.
     * @param showTowerRanges Whether to show tower ranges.
     * @param callback The callback to handle save result.
     */
    public void saveSettings(boolean soundEnabled, boolean vibrationEnabled, boolean showTowerRanges,
                             SaveCallback callback) {
        ensureAuthenticated(new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                performSave(userId, soundEnabled, vibrationEnabled, showTowerRanges, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Authentication failed: " + error);
            }
        });
    }

    /**
     * Perform the actual save operation.
     *
     * @param userId The user ID to save settings for.
     * @param soundEnabled Whether sound is enabled.
     * @param vibrationEnabled Whether vibration is enabled.
     * @param showTowerRanges Whether to show tower ranges.
     * @param callback The callback to handle save result.
     */
    private void performSave(String userId, boolean soundEnabled, boolean vibrationEnabled,
                             boolean showTowerRanges, SaveCallback callback) {
        Map<String, Object> settingsData = new HashMap<>();
        settingsData.put(FIELD_SOUND_ENABLED, soundEnabled);
        settingsData.put(FIELD_VIBRATION_ENABLED, vibrationEnabled);
        settingsData.put(FIELD_SHOW_TOWER_RANGES, showTowerRanges);

        firestore.collection(COLLECTION_SETTINGS)
            .document(userId)
            .set(settingsData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Settings saved successfully for user " + userId);
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to save settings", e);
                if (callback != null) {
                    callback.onError("Failed to save: " + e.getMessage());
                }
            });
    }

    /**
     * Callback interface for authentication operations.
     *
     * @author Ethan Wight
     */
    public interface AuthCallback {
        /**
         * Called when authentication succeeds.
         *
         * @param userId The authenticated user ID.
         */
        void onSuccess(String userId);

        /**
         * Called when authentication fails.
         *
         * @param error The error message.
         */
        void onError(String error);
    }

    /**
     * Callback interface for load operations.
     *
     * @author Ethan Wight
     */
    public interface LoadCallback {
        /**
         * Called when settings load successfully.
         *
         * @param soundEnabled Whether sound is enabled.
         * @param vibrationEnabled Whether vibration is enabled.
         * @param showTowerRanges Whether to show tower ranges.
         */
        void onSuccess(boolean soundEnabled, boolean vibrationEnabled, boolean showTowerRanges);
    }

    /**
     * Callback interface for save operations.
     *
     * @author Ethan Wight
     */
    public interface SaveCallback {
        /**
         * Called when save succeeds.
         */
        void onSuccess();

        /**
         * Called when save fails.
         *
         * @param error The error message.
         */
        void onError(String error);
    }
}
