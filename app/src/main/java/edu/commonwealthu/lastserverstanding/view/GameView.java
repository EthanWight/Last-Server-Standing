package edu.commonwealthu.lastserverstanding.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.commonwealthu.lastserverstanding.game.GameEngine;

/**
 * Custom SurfaceView for rendering the game at 60 FPS
 * Handles all game rendering and input
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    
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
    private int gridSize = 64; // Size of each grid cell in pixels
    private int gridWidth;
    private int gridHeight;
    
    // Camera
    private PointF cameraOffset;
    private float cameraZoom = 1.0f;
    
    // Touch input
    private PointF lastTouchPoint;
    
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
        
        // Initialize paint
        paint = new Paint();
        paint.setAntiAlias(true);
        
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
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Main game loop - runs at 60 FPS
     */
    @Override
    public void run() {
        long lastFrameTime = System.currentTimeMillis();
        
        while (isRunning) {
            long startTime = System.currentTimeMillis();
            long deltaTime = startTime - lastFrameTime;
            
            // Update game state
            if (gameEngine != null) {
                gameEngine.update(deltaTime / 1000f); // Convert to seconds
            }
            
            // Render frame
            render();
            
            lastFrameTime = startTime;
            
            // Sleep to maintain target frame rate
            long frameTime = System.currentTimeMillis() - startTime;
            long sleepTime = TARGET_FRAME_TIME - frameTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
     * Draw the game grid
     */
    private void drawGrid() {
        paint.setColor(Color.parseColor("#1F2937"));
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        
        // Vertical lines
        for (int x = 0; x <= gridWidth; x++) {
            float xPos = x * gridSize;
            canvas.drawLine(xPos, 0, xPos, gridHeight * gridSize, paint);
        }
        
        // Horizontal lines
        for (int y = 0; y <= gridHeight; y++) {
            float yPos = y * gridSize;
            canvas.drawLine(0, yPos, gridWidth * gridSize, yPos, paint);
        }
    }
    
    /**
     * Draw heads-up display
     */
    private void drawHUD() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        
        // Draw FPS counter (top-right)
        if (gameEngine != null) {
            String fpsText = "FPS: " + gameEngine.getFPS();
            canvas.drawText(fpsText, getWidth() - 200, 120, paint);
        }
        
        // Additional HUD elements will be added here
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
                // Handle tap
                if (gameEngine != null) {
                    PointF worldPos = screenToWorld(touchPoint);
                    gameEngine.handleTap(worldPos);
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                // Handle pan
                float dx = touchPoint.x - lastTouchPoint.x;
                float dy = touchPoint.y - lastTouchPoint.y;
                cameraOffset.x += dx;
                cameraOffset.y += dy;
                lastTouchPoint.set(touchPoint.x, touchPoint.y);
                return true;
                
            case MotionEvent.ACTION_UP:
                return true;
        }
        
        return super.onTouchEvent(event);
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
