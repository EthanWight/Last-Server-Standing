package edu.commonwealthu.lastserverstanding;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

/**
 * Custom Application class to initialize Firebase and other app-wide components
 */
public class LastServerStandingApplication extends Application {

    private static final String TAG = "LSSApplication";
    private static LastServerStandingApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
        }
    }

    /**
     * Get application instance for context access
     */
    public static LastServerStandingApplication getInstance() {
        return instance;
    }
}
