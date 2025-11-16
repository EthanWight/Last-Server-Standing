package edu.commonwealthu.lastserverstanding.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.commonwealthu.lastserverstanding.game.GameEngine;

/**
 * Custom SurfaceView for rendering the game at 60 FPS using Choreographer
 * Handles all game rendering and input
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Choreographer.FrameCallback {

    private static final String TAG = "GameView";

    // Removed fixed aspect ratio - use device's natural landscape dimensions

    // Choreographer for vsync-based rendering
    private Choreographer choreographer;
    private volatile boolean isRunning;
    private long lastFrameTimeNanos;

    // Game engine reference
    private GameEngine gameEngine;

    // Rendering
    private Paint paint;
    private Paint gridPaint; // Separate paint for grid to avoid reconfiguration
    private Canvas canvas;

    // Performance optimization - cache grid
    private boolean gridNeedsRedraw = true;
    private android.graphics.Bitmap gridCache;
    private Canvas gridCacheCanvas;

    // Grid settings
    private final int gridSize = 64; // Size of each grid cell in pixels
    private int gridWidth;
    private int gridHeight;
    
    // Camera
    private PointF cameraOffset;
    private final float cameraZoom = 1.0f;
    
    // Touch input
    private PointF lastTouchPoint;

    // Tap listener for tower placement
    private OnTapListener tapListener;

    // Drag preview
    private boolean showDragPreview = false;
    private final PointF dragPreviewPosition = new PointF();
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
    public void showDragPreview(int iconRes) {
        this.showDragPreview = true;
        this.dragPreviewIconRes = iconRes;
    }

    /**
     * Hide tower drag preview
     */
    public void hideDragPreview() {
        this.showDragPreview = false;
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
        init();
    }

    /**
     * Constructor for programmatic creation
     */
    public GameView(Context context) {
        super(context);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // Use the device's natural dimensions without forcing an aspect ratio
        Log.d(TAG, "onMeasure: using natural dimensions: " + width + "x" + height);
        setMeasuredDimension(width, height);
    }
    
    /**
     * Initialize the view
     */
    private void init() {
        // Set up surface holder callbacks
        getHolder().addCallback(this);

        // Initialize paint for game objects
        paint = new Paint();
        paint.setAntiAlias(true);

        // Initialize separate paint for grid (optimization)
        gridPaint = new Paint();
        gridPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.grid_line));
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
        int newGridWidth = width / gridSize;
        int newGridHeight = height / gridSize;

        // Check if dimensions changed
        if (newGridWidth != gridWidth || newGridHeight != gridHeight) {
            gridWidth = newGridWidth;
            gridHeight = newGridHeight;
            gridNeedsRedraw = true; // Mark grid for redraw when dimensions change

            // Clean up old cache
            if (gridCache != null && !gridCache.isRecycled()) {
                gridCache.recycle();
                gridCache = null;
            }
        }
    }
    
    @Override
    public void surfaceCreated(@androidx.annotation.NonNull SurfaceHolder holder) {
        // Start game loop when surface is ready
        calculateGridDimensions(getWidth(), getHeight());
        startGameLoop();
    }

    @Override
    public void surfaceChanged(@androidx.annotation.NonNull SurfaceHolder holder, int format, int width, int height) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // Set the surface to the same size as the view, respecting the aspect ratio.
        // This is crucial to prevent the canvas from stretching.
        if (viewWidth > 0 && viewHeight > 0) {
            holder.setFixedSize(viewWidth, viewHeight);
        }

        Log.d(TAG, "Surface changed: surface=" + width + "x" + height + ", view=" + viewWidth + "x" + viewHeight + ". Fixed size set.");
        calculateGridDimensions(viewWidth, viewHeight);
    }
    
    @Override
    public void surfaceDestroyed(@androidx.annotation.NonNull SurfaceHolder holder) {
        // Stop game loop when surface is destroyed
        stopGameLoop();

        // Clean up grid cache
        if (gridCache != null && !gridCache.isRecycled()) {
            gridCache.recycle();
            gridCache = null;
        }
        gridCacheCanvas = null;
    }
    
    /**
     * Start the game loop using Choreographer
     */
    public void startGameLoop() {
        if (!isRunning) {
            isRunning = true;
            choreographer = Choreographer.getInstance();
            lastFrameTimeNanos = System.nanoTime();
            choreographer.postFrameCallback(this);
        }
    }

    /**
     * Stop the game loop
     */
    public void stopGameLoop() {
        isRunning = false;
        if (choreographer != null) {
            choreographer.removeFrameCallback(this);
        }
    }
    
    /**
     * Choreographer frame callback - called on each vsync (typically 60 FPS)
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!isRunning) {
            return;
        }

        // Calculate delta time
        long deltaTimeNs = frameTimeNanos - lastFrameTimeNanos;
        float deltaTime = deltaTimeNs / 1000000000f; // Convert to seconds

        // Update game state
        if (gameEngine != null) {
            gameEngine.update(deltaTime);
        }

        // Render frame
        render();

        lastFrameTimeNanos = frameTimeNanos;

        // Request next frame
        if (isRunning && choreographer != null) {
            choreographer.postFrameCallback(this);
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
            canvas.drawColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.background_deep));
            
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
     * Draw the game grid (optimized with caching)
     */
    private void drawGrid() {
        int gridPixelWidth = gridWidth * gridSize;
        int gridPixelHeight = gridHeight * gridSize;

        // Redraw grid to cache if needed
        if (gridNeedsRedraw && gridPixelWidth > 0 && gridPixelHeight > 0) {
            try {
                // Create bitmap for caching if needed
                if (gridCache == null || gridCache.isRecycled() ||
                    gridCache.getWidth() != gridPixelWidth ||
                    gridCache.getHeight() != gridPixelHeight) {

                    // Clean up old bitmap
                    if (gridCache != null && !gridCache.isRecycled()) {
                        gridCache.recycle();
                    }

                    // Create new bitmap and canvas for grid
                    gridCache = android.graphics.Bitmap.createBitmap(
                        gridPixelWidth, gridPixelHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    );
                    gridCacheCanvas = new Canvas(gridCache);
                }

                // Clear cache
                gridCacheCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

                // Draw grid to cache
                // Vertical lines
                for (int x = 0; x <= gridWidth; x++) {
                    float xPos = x * gridSize;
                    gridCacheCanvas.drawLine(xPos, 0, xPos, gridPixelHeight, gridPaint);
                }

                // Horizontal lines
                for (int y = 0; y <= gridHeight; y++) {
                    float yPos = y * gridSize;
                    gridCacheCanvas.drawLine(0, yPos, gridPixelWidth, yPos, gridPaint);
                }

                gridNeedsRedraw = false;
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory creating grid cache, falling back to direct draw", e);
                gridCache = null;
                gridCacheCanvas = null;
            }
        }

        // Draw cached grid or fall back to direct draw
        if (gridCache != null && !gridCache.isRecycled()) {
            canvas.drawBitmap(gridCache, 0, 0, null);
        } else {
            // Fallback: draw directly if cache failed
            drawGridDirect();
        }
    }

    /**
     * Fallback method to draw grid directly (if caching fails)
     */
    private void drawGridDirect() {
        // Vertical lines
        for (int x = 0; x <= gridWidth; x++) {
            float xPos = x * gridSize;
            canvas.drawLine(xPos, 0, xPos, gridHeight * gridSize, gridPaint);
        }

        // Horizontal lines
        for (int y = 0; y <= gridHeight; y++) {
            float yPos = y * gridSize;
            canvas.drawLine(0, yPos, gridWidth * gridSize, yPos, gridPaint);
        }
    }
    
    /**
     * Draw drag preview of tower being placed
     */
    private void drawDragPreview() {
        float radius = gridSize / 2f;

        // Draw semi-transparent background circle
        paint.setStyle(Paint.Style.FILL);
        if (isDragPreviewValid) {
            paint.setColor(Color.argb(100, 0, 255, 0)); // Green with transparency
        } else {
            paint.setColor(Color.argb(100, 255, 0, 0)); // Red with transparency
        }
        canvas.drawCircle(dragPreviewPosition.x, dragPreviewPosition.y, radius, paint);

        // Draw tower icon if available
        if (dragPreviewIconRes != 0) {
            try {
                android.graphics.drawable.Drawable drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(getContext(), dragPreviewIconRes);
                if (drawable != null) {
                    // Calculate icon bounds centered on preview position
                    int iconSize = (int) (gridSize * 0.6f); // Icon is 60% of grid size
                    int left = (int) (dragPreviewPosition.x - iconSize / 2f);
                    int top = (int) (dragPreviewPosition.y - iconSize / 2f);
                    int right = left + iconSize;
                    int bottom = top + iconSize;

                    drawable.setBounds(left, top, right, bottom);

                    // Set alpha based on validity
                    drawable.setAlpha(isDragPreviewValid ? 255 : 180);

                    // Draw the icon
                    drawable.draw(canvas);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to draw drag preview icon", e);
            }
        }

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
                32f, paint); // Half of minimum spacing (64f / 2)

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
                    // Store touch point for performClick to use
                    lastTouchPoint.set(touchPoint.x, touchPoint.y);
                    performClick();
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
     * Handle click for accessibility
     */
    @Override
    public boolean performClick() {
        super.performClick();

        // Handle tap at last touch point
        PointF worldPos = screenToWorld(lastTouchPoint);
        if (tapListener != null) {
            tapListener.onTap(worldPos);
        } else if (gameEngine != null) {
            gameEngine.handleTap(worldPos);
        }

        return true;
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

    // Getters
    public int getGridSize() { return gridSize; }
}
