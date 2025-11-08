package edu.commonwealthu.lastserverstanding.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages accelerometer sensor for shake detection
 * Triggers emergency alerts when device is shaken
 */
public class AccelerometerManager implements SensorEventListener {
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;
    private ShakeListener shakeListener;
    
    // Shake detection parameters
    private static final float GRAVITY = 9.8f;
    private static final long SHAKE_WINDOW_MS = 500;
    private static final int REQUIRED_PEAKS = 3;
    
    private float sensitivityThreshold = 2.5f; // Adjustable by user
    private List<Long> shakePeaks;
    private boolean isListening;
    
    /**
     * Interface for shake detection callback
     */
    public interface ShakeListener {
        void onShakeDetected();
    }
    
    /**
     * Constructor
     */
    public AccelerometerManager(Context context, ShakeListener listener) {
        this.shakeListener = listener;
        this.shakePeaks = new ArrayList<>();
        
        // Get sensor manager
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        // Get accelerometer sensor
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        
        // Get vibrator
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    /**
     * Start listening to accelerometer
     */
    public void startListening() {
        if (accelerometer != null && !isListening) {
            sensorManager.registerListener(this, accelerometer, 
                SensorManager.SENSOR_DELAY_GAME);
            isListening = true;
        }
    }
    
    /**
     * Stop listening to accelerometer
     */
    public void stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this);
            isListening = false;
        }
    }
    
    /**
     * Set sensitivity threshold (1.0 = very sensitive, 5.0 = not sensitive)
     */
    public void setSensitivity(float sensitivity) {
        this.sensitivityThreshold = sensitivity;
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        
        // Get acceleration values
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate acceleration magnitude
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
        
        // Remove gravity component
        float netAcceleration = Math.abs(acceleration - GRAVITY);
        
        // Check if acceleration exceeds threshold
        if (netAcceleration > sensitivityThreshold) {
            long currentTime = System.currentTimeMillis();
            shakePeaks.add(currentTime);
            
            // Remove old peaks outside the time window
            removeOldPeaks(currentTime);
            
            // Check if we have enough peaks for a shake
            if (shakePeaks.size() >= REQUIRED_PEAKS) {
                onShakeDetected();
                shakePeaks.clear(); // Reset after detection
            }
        }
    }
    
    /**
     * Remove peaks that are outside the shake window
     */
    private void removeOldPeaks(long currentTime) {
        shakePeaks.removeIf(peakTime -> 
            currentTime - peakTime > SHAKE_WINDOW_MS);
    }
    
    /**
     * Called when a shake is detected
     */
    private void onShakeDetected() {
        // Trigger vibration feedback
        triggerVibration();
        
        // Notify listener
        if (shakeListener != null) {
            shakeListener.onShakeDetected();
        }
    }
    
    /**
     * Trigger haptic feedback
     */
    private void triggerVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            // Pattern: wait 0ms, vibrate 100ms, wait 50ms, vibrate 100ms
            long[] pattern = {0, 100, 50, 100};
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for accelerometer
    }
    
    /**
     * Check if accelerometer is available
     */
    public boolean isAccelerometerAvailable() {
        return accelerometer != null;
    }
    
    /**
     * Release resources
     */
    public void release() {
        stopListening();
    }
}
