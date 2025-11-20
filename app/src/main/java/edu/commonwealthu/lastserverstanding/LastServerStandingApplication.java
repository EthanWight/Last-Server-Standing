package edu.commonwealthu.lastserverstanding;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

/**
 * Initializes Firebase and manages other app-wide components for the application.
 *
 * @author Ethan Wight
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
     * Returns the application instance for context access.
     *
     * @return the singleton application instance
     */
    public static LastServerStandingApplication getInstance() {
        return instance;
    }
}
