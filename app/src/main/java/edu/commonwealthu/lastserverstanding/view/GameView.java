package edu.commonwealthu.lastserverstanding.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.commonwealthu.lastserverstanding.game.GameEngine;

/**
 * Custom SurfaceView for rendering the game at 60 FPS
 * Handles all game rendering and input
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "GameView";

    // Game loop thread
    private Thread gameThread;
    private volatile boolean isRunning;
    
    // Target frame rate
    private static final int TARGET_FPS = 60;
    private static final long TARGET_FRAME_TIME_NS = 1000000000L / TARGET_FPS; // Use nanoseconds for precision

    // Game engine reference
    private GameEngine gameEngine;

    // Rendering
    private Paint paint;
    private Paint gridPaint; // Separate paint for grid to avoid reconfiguration
    private Canvas canvas;

    // Performance optimization - cache grid
    private boolean gridNeedsRedraw = true;
    
    // Grid settings
    private int gridSize = 64; // Size of each grid cell in pixels
    private int gridWidth;
    private int gridHeight;
    
    // Camera
    private PointF cameraOffset;
    private float cameraZoom = 1.0f;
    
    // Touch input
    private PointF lastTouchPoint;

    // Tap listener for tower placement
    private OnTapListener tapListener;

    // Drag preview
    private boolean showDragPreview = false;
    private PointF dragPreviewPosition = new PointF();
    private String dragPreviewTowerType = null;
    private int dragPreviewIconRes = 0;
    private boolean isDragPreviewValid = false;

    public interface OnTapListener {
        void onTap(PointF worldPosition);
    }

    public void setOnTapListener(OnTapListener listener) {
        this.tapListener = listener;
    }

    /**
     * Show tower drag preview
     */
    public void showDragPreview(String towerType, int iconRes) {
        this.showDragPreview = true;
        this.dragPreviewTowerType = towerType;
        this.dragPreviewIconRes = iconRes;
    }

    /**
     * Hide tower drag preview
     */
    public void hideDragPreview() {
        this.showDragPreview = false;
        this.dragPreviewTowerType = null;
    }

    /**
     * Update drag preview position and validity
     */
    public void updateDragPreview(PointF screenPos, boolean isValid) {
        PointF worldPos = screenToWorld(screenPos);
        dragPreviewPosition.set(worldPos.x, worldPos.y);
        isDragPreviewValid = isValid;
    }

    /**
     * Constructor for XML inflation
     */
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    /**
     * Constructor for programmatic creation
     */
    public GameView(Context context) {
        super(context);
        init(context);
    }
    
    /**
     * Initialize the view
     */
    private void init(Context context) {
        // Set up surface holder callbacks
        getHolder().addCallback(this);

        // Initialize paint for game objects
        paint = new Paint();
        paint.setAntiAlias(true);

        // Initialize separate paint for grid (optimization)
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#1F2937"));
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(false); // Grid doesn't need anti-aliasing

        // Initialize camera
        cameraOffset = new PointF(0, 0);
        lastTouchPoint = new PointF(0, 0);

        // Make view focusable
        setFocusable(true);
    }
    
    /**
     * Set the game engine
     */
    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
    }
    
    /**
     * Calculate grid dimensions based on view size
     */
    private void calculateGridDimensions(int width, int height) {
        gridWidth = width / gridSize;
        gridHeight = height / gridSize;
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Start game loop when surface is ready
        calculateGridDimensions(getWidth(), getHeight());
        startGameLoop();
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        calculateGridDimensions(width, height);
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Stop game loop when surface is destroyed
        stopGameLoop();
    }
    
    /**
     * Start the game loop thread
     */
    public void startGameLoop() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }
    
    /**
     * Stop the game loop thread
     */
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
    
    /**
     * Main game loop - runs at 60 FPS with high precision timing
     */
    @Override
    public void run() {
        long lastFrameTime = System.nanoTime();

        while (isRunning) {
            long startTime = System.nanoTime();
            long deltaTimeNs = startTime - lastFrameTime;
            float deltaTime = deltaTimeNs / 1000000000f; // Convert to seconds

            // Update game state
            if (gameEngine != null) {
                gameEngine.update(deltaTime);
            }

            // Render frame
            render();

            lastFrameTime = startTime;

            // Calculate sleep time for 60 FPS
            long frameTimeNs = System.nanoTime() - startTime;
            long sleepTimeNs = TARGET_FRAME_TIME_NS - frameTimeNs;

            if (sleepTimeNs > 0) {
                // Use a combination of sleep and busy wait for more accurate timing
                long sleepMs = sleepTimeNs / 1000000L;
                int sleepNs = (int) (sleepTimeNs % 1000000L);

                try {
                    if (sleepMs > 0) {
                        Thread.sleep(sleepMs, sleepNs);
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "Game loop sleep interrupted", e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break; // Exit loop when interrupted
                }
            }
        }
    }
    
    /**
     * Render the game
     */
    private void render() {
        if (!getHolder().getSurface().isValid()) {
            return;
        }
        
        // Lock canvas for drawing
        canvas = getHolder().lockCanvas();
        if (canvas == null) return;
        
        try {
            // Clear screen
            canvas.drawColor(Color.parseColor("#0D1117")); // Dark background
            
            // Save canvas state
            canvas.save();
            
            // Apply camera transform
            canvas.translate(cameraOffset.x, cameraOffset.y);
            canvas.scale(cameraZoom, cameraZoom);
            
            // Draw grid
            drawGrid();
            
            // Draw game elements
            if (gameEngine != null) {
                gameEngine.render(canvas, paint);
            }

            // Draw drag preview (if active)
            if (showDragPreview) {
                drawDragPreview();
            }

            // Restore canvas state
            canvas.restore();

            // Draw HUD (not affected by camera)
            drawHUD();
            
        } finally {
            // Unlock and post canvas
            getHolder().unlockCanvasAndPost(canvas);
        }
    }
    
    /**
     * Draw the game grid (optimized to use pre-configured paint)
     */
    private void drawGrid() {
        // Use pre-configured gridPaint for better performance
        // Draw fewer lines by using larger step size (every 2 or 4 cells) for distant zoom levels
        int step = cameraZoom < 0.5f ? 4 : (cameraZoom < 0.75f ? 2 : 1);

        // Vertical lines
        for (int x = 0; x <= gridWidth; x += step) {
            float xPos = x * gridSize;
            canvas.drawLine(xPos, 0, xPos, gridHeight * gridSize, gridPaint);
        }

        // Horizontal lines
        for (int y = 0; y <= gridHeight; y += step) {
            float yPos = y * gridSize;
            canvas.drawLine(0, yPos, gridWidth * gridSize, yPos, gridPaint);
        }
    }
    
    /**
     * Draw drag preview of tower being placed
     */
    private void drawDragPreview() {
        if (dragPreviewPosition == null) return;

        // Draw semi-transparent circle for tower preview
        paint.setStyle(Paint.Style.FILL);

        // Set color based on validity (red if invalid, green if valid)
        if (isDragPreviewValid) {
            paint.setColor(Color.argb(100, 0, 255, 0)); // Green with transparency
        } else {
            paint.setColor(Color.argb(100, 255, 0, 0)); // Red with transparency
        }

        // Draw tower circle
        float radius = gridSize / 2f;
        canvas.drawCircle(dragPreviewPosition.x, dragPreviewPosition.y, radius, paint);

        // Draw border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        if (isDragPreviewValid) {
            paint.setColor(Color.argb(200, 0, 255, 0)); // Solid green border
        } else {
            paint.setColor(Color.argb(200, 255, 0, 0)); // Solid red border
        }
        canvas.drawCircle(dragPreviewPosition.x, dragPreviewPosition.y, radius, paint);

        // Draw spacing border (shows minimum spacing)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(Color.argb(80, 255, 255, 255)); // Light white
        canvas.drawCircle(dragPreviewPosition.x, dragPreviewPosition.y,
                edu.commonwealthu.lastserverstanding.model.Tower.getMinTowerSpacing() / 2f, paint);

        // Reset paint style
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
    }

    /**
     * Draw heads-up display
     */
    private void drawHUD() {
        // HUD elements are now handled by GameFragment overlay
        // This method can be used for debug overlays if needed
    }
    
    /**
     * Handle touch input
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF touchPoint = new PointF(event.getX(), event.getY());
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchPoint.set(touchPoint.x, touchPoint.y);
                return true;

            case MotionEvent.ACTION_UP:
                // Check if this was a tap (minimal movement)
                float dx = touchPoint.x - lastTouchPoint.x;
                float dy = touchPoint.y - lastTouchPoint.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < 20) { // Threshold for tap vs drag
                    // Handle tap
                    PointF worldPos = screenToWorld(touchPoint);
                    if (tapListener != null) {
                        tapListener.onTap(worldPos);
                    } else if (gameEngine != null) {
                        gameEngine.handleTap(worldPos);
                    }
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                // Handle pan (disabled for now to make tapping easier)
                // Can re-enable with two-finger pan later
                return true;
        }
        
        return super.onTouchEvent(event);
    }
    
    /**
     * Convert screen coordinates to world coordinates
     */
    public PointF screenToWorld(PointF screenPos) {
        return new PointF(
            (screenPos.x - cameraOffset.x) / cameraZoom,
            (screenPos.y - cameraOffset.y) / cameraZoom
        );
    }
    
    /**
     * Convert world coordinates to grid coordinates
     */
    public PointF worldToGrid(PointF worldPos) {
        return new PointF(
            (int)(worldPos.x / gridSize),
            (int)(worldPos.y / gridSize)
        );
    }
    
    // Getters
    public int getGridSize() { return gridSize; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }
}
