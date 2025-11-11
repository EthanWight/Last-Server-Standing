package edu.commonwealthu.lastserverstanding.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import edu.commonwealthu.lastserverstanding.game.GameEngine;

/**
 * Enhanced GameView with comprehensive gesture handling
 * Supports: tap, long press, pan, pinch zoom, double tap, and swipe
 */
public class EnhancedGameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "EnhancedGameView";

    // Game loop thread
    private Thread gameThread;
    private volatile boolean isRunning;
    
    // Target frame rate
    private static final int TARGET_FPS = 60;
    private static final long TARGET_FRAME_TIME = 1000 / TARGET_FPS;
    
    // Game engine reference
    private GameEngine gameEngine;
    
    // Rendering
    private Paint paint;
    private Canvas canvas;
    
    // Grid settings
    private final int gridSize = 64;
    private int gridWidth;
    private int gridHeight;
    
    // Camera
    private PointF cameraOffset;
    private float cameraZoom = 1.0f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    
    // Gesture detectors
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleDetector;
    
    // Selected grid cell
    private PointF selectedCell;
    
    // Gesture listeners
    private OnGridTapListener gridTapListener;
    private OnTowerLongPressListener towerLongPressListener;
    private OnSwipeUpListener swipeUpListener;
    
    /**
     * Interfaces for gesture callbacks
     */
    public interface OnGridTapListener {
        void onGridTapped(PointF gridPosition);
    }
    
    public interface OnTowerLongPressListener {
        void onTowerLongPressed(PointF gridPosition);
    }
    
    public interface OnSwipeUpListener {
        void onSwipeUp();
    }
    
    /**
     * Constructor for XML inflation
     */
    public EnhancedGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    /**
     * Constructor for programmatic creation
     */
    public EnhancedGameView(Context context) {
        super(context);
        init(context);
    }
    
    /**
     * Initialize the view
     */
    private void init(Context context) {
        getHolder().addCallback(this);
        
        paint = new Paint();
        paint.setAntiAlias(true);
        
        cameraOffset = new PointF(0, 0);
        selectedCell = null;
        
        setFocusable(true);
        // Mark view as clickable so performClick is meaningful for accessibility
        setClickable(true);
        
        // Initialize gesture detectors
        setupGestureDetectors(context);
    }

    /**
     * Set up gesture detection
     */
    private void setupGestureDetectors(Context context) {
        // Standard gesture detector for taps, scrolls, flings
        gestureDetector = new GestureDetector(context,
            new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                    handleSingleTap(e.getX(), e.getY());
                    // Announce click for accessibility
                    performClick();
                    return true;
                }

                @Override
                public void onLongPress(@NonNull MotionEvent e) {
                    handleLongPress(e.getX(), e.getY());
                }

                @Override
                public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2,
                                       float distanceX, float distanceY) {
                    handlePan(distanceX, distanceY);
                    return true;
                }

                @Override
                public boolean onDoubleTap(@NonNull MotionEvent e) {
                    handleDoubleTap(e.getX(), e.getY());
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2,
                                      float velocityX, float velocityY) {
                    handleFling(velocityX, velocityY);
                    return true;
                }
            });

        // Scale gesture detector for pinch zoom
        scaleDetector = new ScaleGestureDetector(context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(@NonNull ScaleGestureDetector detector) {
                    handlePinchZoom(detector);
                    return true;
                }
            });
    }
    
    /**
     * Handle single tap gesture
     */
    private void handleSingleTap(float screenX, float screenY) {
        PointF worldPos = screenToWorld(new PointF(screenX, screenY));
        PointF gridPos = worldToGrid(worldPos);
        
        // Update selected cell
        selectedCell = gridPos;
        
        // Notify listener
        if (gridTapListener != null) {
            gridTapListener.onGridTapped(gridPos);
        }
        
        // Also notify game engine
        if (gameEngine != null) {
            gameEngine.handleTap(worldPos);
        }
    }
    
    /**
     * Handle long press gesture
     */
    private void handleLongPress(float screenX, float screenY) {
        PointF worldPos = screenToWorld(new PointF(screenX, screenY));
        PointF gridPos = worldToGrid(worldPos);
        
        // Trigger haptic feedback
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        
        // Notify listener
        if (towerLongPressListener != null) {
            towerLongPressListener.onTowerLongPressed(gridPos);
        }
    }
    
    /**
     * Handle pan gesture
     */
    private void handlePan(float distanceX, float distanceY) {
        // Move camera in opposite direction of pan
        cameraOffset.x -= distanceX;
        cameraOffset.y -= distanceY;
        
        // Apply bounds to prevent panning too far
        applyPanBounds();
    }
    
    /**
     * Handle pinch zoom gesture
     */
    private void handlePinchZoom(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        float newZoom = cameraZoom * scaleFactor;
        
        // Clamp zoom level
        cameraZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        
        // Zoom toward gesture center point
        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();
        adjustCameraForZoom(focusX, focusY, scaleFactor);
    }
    
    /**
     * Handle double tap gesture
     */
    private void handleDoubleTap(float screenX, float screenY) {
        PointF worldPos = screenToWorld(new PointF(screenX, screenY));
        PointF gridPos = worldToGrid(worldPos);

        // Try to upgrade tower at this position
        if (gameEngine != null) {
            boolean upgraded = gameEngine.tryUpgradeTowerAt(gridPos);

            if (upgraded) {
                // Success feedback (use API level check for newer constants)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                } else {
                    performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                }
            } else {
                // Failure feedback (use API level check for newer constants)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.REJECT);
                } else {
                    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                }
            }
        }
    }
    
    /**
     * Handle fling gesture
     */
    private void handleFling(float velocityX, float velocityY) {
        // Check for upward swipe from bottom of screen
        // Use absolute value of velocityX to ensure it's primarily vertical
        if (velocityY < -2000 && Math.abs(velocityX) < Math.abs(velocityY)) {
            if (swipeUpListener != null) {
                swipeUpListener.onSwipeUp();
            }
        }
    }
    
    /**
     * Apply bounds to camera panning
     */
    private void applyPanBounds() {
        float worldWidth = gridWidth * gridSize * cameraZoom;
        float worldHeight = gridHeight * gridSize * cameraZoom;
        
        // Limit panning to world boundaries
        cameraOffset.x = Math.max(-(worldWidth - getWidth()), 
                                  Math.min(0, cameraOffset.x));
        cameraOffset.y = Math.max(-(worldHeight - getHeight()), 
                                  Math.min(0, cameraOffset.y));
    }
    
    /**
     * Adjust camera position when zooming
     */
    private void adjustCameraForZoom(float focusX, float focusY, float scaleFactor) {
        // Zoom toward the focus point
        float dx = focusX - cameraOffset.x;
        float dy = focusY - cameraOffset.y;
        
        cameraOffset.x -= dx * (scaleFactor - 1);
        cameraOffset.y -= dy * (scaleFactor - 1);
        
        applyPanBounds();
    }
    
    /**
     * Convert screen coordinates to world coordinates
     */
    private PointF screenToWorld(PointF screenPos) {
        return new PointF(
            (screenPos.x - cameraOffset.x) / cameraZoom,
            (screenPos.y - cameraOffset.y) / cameraZoom
        );
    }
    
    /**
     * Convert world coordinates to grid coordinates
     */
    private PointF worldToGrid(PointF worldPos) {
        return new PointF(
            (int)(worldPos.x / gridSize),
            (int)(worldPos.y / gridSize)
        );
    }

    @Override
    @SuppressWarnings("ClickableViewAccessibility") // performClick is called via gestureDetector
    public boolean onTouchEvent(MotionEvent event) {
        // Process gestures in order of priority
        boolean handled = scaleDetector.onTouchEvent(event);
        if (!scaleDetector.isInProgress()) {
            handled |= gestureDetector.onTouchEvent(event);
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        // Call super to handle accessibility events
        super.performClick();
        return true;
    }
    
    /**
     * Set game engine
     */
    @SuppressWarnings("unused") // Public API
    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
    }

    /**
     * Set gesture listeners
     */
    @SuppressWarnings("unused") // Public API
    public void setOnGridTapListener(OnGridTapListener listener) {
        this.gridTapListener = listener;
    }

    @SuppressWarnings("unused") // Public API
    public void setOnTowerLongPressListener(OnTowerLongPressListener listener) {
        this.towerLongPressListener = listener;
    }

    @SuppressWarnings("unused") // Public API
    public void setOnSwipeUpListener(OnSwipeUpListener listener) {
        this.swipeUpListener = listener;
    }
    
    // Game loop and rendering methods (same as original GameView)

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        calculateGridDimensions(getWidth(), getHeight());
        startGameLoop();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        calculateGridDimensions(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopGameLoop();
    }
    
    private void calculateGridDimensions(int width, int height) {
        gridWidth = width / gridSize;
        gridHeight = height / gridSize;
    }
    
    public void startGameLoop() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }
    
    public void stopGameLoop() {
        isRunning = false;
        if (gameThread != null) {
            Thread threadToJoin = gameThread;
            gameThread = null;
            try {
                threadToJoin.join(1000); // Wait max 1 second
            } catch (InterruptedException e) {
                Log.w(TAG, "Game loop thread interrupted while stopping", e);
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
        }
    }
    
    @Override
    public void run() {
        long lastFrameTime = System.currentTimeMillis();

        while (isRunning) {
            long startTime = System.currentTimeMillis();
            long deltaTime = startTime - lastFrameTime;

            if (gameEngine != null) {
                gameEngine.update(deltaTime / 1000f);
            }

            render();

            lastFrameTime = startTime;

            long frameTime = System.currentTimeMillis() - startTime;
            long sleepTime = TARGET_FRAME_TIME - frameTime;
            if (sleepTime > 0) {
                try {
                    //noinspection BusyWait - Intentional frame rate control
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Game loop sleep interrupted", e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break; // Exit loop when interrupted
                }
            }
        }
    }
    
    private void render() {
        if (!getHolder().getSurface().isValid()) {
            return;
        }
        
        canvas = getHolder().lockCanvas();
        if (canvas == null) return;
        
        try {
            canvas.drawColor(Color.parseColor("#0D1117"));
            
            canvas.save();
            canvas.translate(cameraOffset.x, cameraOffset.y);
            canvas.scale(cameraZoom, cameraZoom);
            
            drawGrid();
            drawSelectedCell();
            
            if (gameEngine != null) {
                gameEngine.render(canvas, paint);
            }
            
            canvas.restore();
            drawHUD();
            
        } finally {
            getHolder().unlockCanvasAndPost(canvas);
        }
    }
    
    private void drawGrid() {
        paint.setColor(Color.parseColor("#1F2937"));
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        
        for (int x = 0; x <= gridWidth; x++) {
            float xPos = x * gridSize;
            canvas.drawLine(xPos, 0, xPos, gridHeight * gridSize, paint);
        }
        
        for (int y = 0; y <= gridHeight; y++) {
            float yPos = y * gridSize;
            canvas.drawLine(0, yPos, gridWidth * gridSize, yPos, paint);
        }
    }
    
    private void drawSelectedCell() {
        if (selectedCell != null) {
            paint.setColor(Color.parseColor("#2196F3"));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            
            float x = selectedCell.x * gridSize;
            float y = selectedCell.y * gridSize;
            canvas.drawRect(x, y, x + gridSize, y + gridSize, paint);
        }
    }
    
    private void drawHUD() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        
        if (gameEngine != null) {
            String fpsText = "FPS: " + gameEngine.getFPS();
            canvas.drawText(fpsText, getWidth() - 200, 120, paint);
        }
    }

    @SuppressWarnings("unused") // Public API
    public int getGridSize() { return gridSize; }

    @SuppressWarnings("unused") // Public API
    public int getGridWidth() { return gridWidth; }

    @SuppressWarnings("unused") // Public API
    public int getGridHeight() { return gridHeight; }
}
