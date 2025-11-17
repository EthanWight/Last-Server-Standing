package edu.commonwealthu.lastserverstanding.data.repository;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing user settings in Firebase Firestore
 * Uses anonymous authentication to sync settings across sessions
 */
public class FirebaseSettingsRepository {

    private static final String TAG = "FirebaseSettingsRepo";
    private static final String COLLECTION_SETTINGS = "user_settings";
    private static final String FIELD_SOUND_ENABLED = "soundEnabled";
    private static final String FIELD_VIBRATION_ENABLED = "vibrationEnabled";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    // Default settings
    private static final boolean DEFAULT_SOUND_ENABLED = true;
    private static final boolean DEFAULT_VIBRATION_ENABLED = true;

    public FirebaseSettingsRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Ensure user is authenticated (anonymously)
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
     * Load settings from Firestore
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
                callback.onSuccess(DEFAULT_SOUND_ENABLED, DEFAULT_VIBRATION_ENABLED);
            }
        });
    }

    /**
     * Perform the actual load operation
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

                        // Use defaults if fields are missing
                        boolean sound = soundEnabled != null ? soundEnabled : DEFAULT_SOUND_ENABLED;
                        boolean vibration = vibrationEnabled != null ? vibrationEnabled : DEFAULT_VIBRATION_ENABLED;

                        Log.d(TAG, "Loaded settings for user " + userId);
                        callback.onSuccess(sound, vibration);
                    } else {
                        // No settings found, return defaults
                        Log.d(TAG, "No settings found for user, using defaults");
                        callback.onSuccess(DEFAULT_SOUND_ENABLED, DEFAULT_VIBRATION_ENABLED);
                    }
                } else {
                    Log.e(TAG, "Failed to load settings", task.getException());
                    // On error, return defaults
                    callback.onSuccess(DEFAULT_SOUND_ENABLED, DEFAULT_VIBRATION_ENABLED);
                }
            });
    }

    /**
     * Save settings to Firestore
     */
    public void saveSettings(boolean soundEnabled, boolean vibrationEnabled, SaveCallback callback) {
        ensureAuthenticated(new AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                performSave(userId, soundEnabled, vibrationEnabled, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError("Authentication failed: " + error);
            }
        });
    }

    /**
     * Perform the actual save operation
     */
    private void performSave(String userId, boolean soundEnabled, boolean vibrationEnabled, SaveCallback callback) {
        Map<String, Object> settingsData = new HashMap<>();
        settingsData.put(FIELD_SOUND_ENABLED, soundEnabled);
        settingsData.put(FIELD_VIBRATION_ENABLED, vibrationEnabled);

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

    // Callback interfaces
    public interface AuthCallback {
        void onSuccess(String userId);
        void onError(String error);
    }

    public interface LoadCallback {
        void onSuccess(boolean soundEnabled, boolean vibrationEnabled);
    }

    public interface SaveCallback {
        void onSuccess();
        void onError(String error);
    }
}
