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
 * Custom SurfaceView for rendering the game at 60 FPS using Choreographer.
 * Handles all game rendering and input.
 *
 * @author Ethan Wight
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
    private final PointF tempTouchPoint = new PointF(); // Reusable temp for touch events

    // Tap listener for tower placement
    private OnTapListener tapListener;

    // Drag preview
    private boolean showDragPreview = false;
    private final PointF dragPreviewPosition = new PointF();
    private int dragPreviewIconRes = 0;
    private boolean isDragPreviewValid = false;
    private float dragPreviewRange = 0f;

    public interface OnTapListener {
        @SuppressWarnings("unused") // Used via method reference in GameFragment
        void onTap(PointF worldPosition);
    }

    public void setOnTapListener(OnTapListener listener) {
        this.tapListener = listener;
    }

    /**
     * Show tower drag preview.
     *
     * @param iconRes The icon resource ID.
     * @param range The tower range.
     */
    public void showDragPreview(int iconRes, float range) {
        this.showDragPreview = true;
        this.dragPreviewIconRes = iconRes;
        this.dragPreviewRange = range;
    }

    /**
     * Hide tower drag preview.
     */
    public void hideDragPreview() {
        this.showDragPreview = false;
    }

    /**
     * Update drag preview position and validity.
     *
     * @param screenPos The screen position.
     * @param isValid Whether the placement is valid.
     */
    public void updateDragPreview(PointF screenPos, boolean isValid) {
        PointF worldPos = screenToWorld(screenPos);
        dragPreviewPosition.set(worldPos.x, worldPos.y);
        isDragPreviewValid = isValid;
    }

    /**
     * Constructor for XML inflation.
     *
     * @param context The application context.
     * @param attrs The attribute set.
     */
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor for programmatic creation.
     *
     * @param context The application context.
     */
    public GameView(Context context) {
        super(context);
        init();
    }

    /**
     * Measure the view.
     *
     * @param widthMeasureSpec The width measure specification.
     * @param heightMeasureSpec The height measure specification.
     */
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
     * Initialize the view.
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
     * Set the game engine.
     *
     * @param engine The game engine.
     */
    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
    }
    
    /**
     * Calculate grid dimensions based on view size.
     *
     * @param width The view width.
     * @param height The view height.
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
     * Start the game loop using Choreographer.
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
     * Stop the game loop.
     */
    public void stopGameLoop() {
        isRunning = false;
        if (choreographer != null) {
            choreographer.removeFrameCallback(this);
        }
    }
    
    /**
     * Choreographer frame callback - called on each vsync (typically 60 FPS).
     *
     * @param frameTimeNanos The frame time in nanoseconds.
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
     * Render the game.
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
            
        } finally {
            // Unlock and post canvas
            getHolder().unlockCanvasAndPost(canvas);
        }
    }
    
    /**
     * Draw the game grid (optimized with caching).
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
     * Fallback method to draw grid directly (if caching fails).
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
     * Draw drag preview of tower being placed.
     */
    private void drawDragPreview() {
        float radius = gridSize / 2f;

        // Draw semi-transparent background circle
        paint.setStyle(Paint.Style.FILL);
        if (isDragPreviewValid) {
            paint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.placement_valid_fill));
        } else {
            paint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.placement_invalid_fill));
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
            paint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.placement_valid_border));
        } else {
            paint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.placement_invalid_border));
        }
        canvas.drawCircle(dragPreviewPosition.x, dragPreviewPosition.y, radius, paint);

        // Draw spacing border (shows minimum spacing)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.placement_grid_overlay));
        canvas.drawCircle(dragPreviewPosition.x, dragPreviewPosition.y,
                32f, paint); // Half of minimum spacing (64f / 2)

        // Draw range circle if range is specified
        if (dragPreviewRange > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            if (isDragPreviewValid) {
                paint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.drag_preview_valid));
            } else {
                paint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), edu.commonwealthu.lastserverstanding.R.color.drag_preview_invalid));
            }
            canvas.drawCircle(dragPreviewPosition.x, dragPreviewPosition.y,
                    dragPreviewRange, paint);
        }

        // Reset paint style
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
    }

    /**
     * Handle touch input.
     *
     * @param event The motion event.
     * @return True if the event was handled.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Use temp point to avoid allocation on every touch event
        tempTouchPoint.set(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchPoint.set(tempTouchPoint.x, tempTouchPoint.y);
                return true;

            case MotionEvent.ACTION_UP:
                // Check if this was a tap (minimal movement)
                float dx = tempTouchPoint.x - lastTouchPoint.x;
                float dy = tempTouchPoint.y - lastTouchPoint.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < 20) { // Threshold for tap vs drag
                    // Store touch point for performClick to use
                    lastTouchPoint.set(tempTouchPoint.x, tempTouchPoint.y);
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
     * Handle click for accessibility.
     *
     * @return True if the click was handled.
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
     * Convert screen coordinates to world coordinates.
     *
     * @param screenPos The screen position.
     * @return The world position.
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
