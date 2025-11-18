package edu.commonwealthu.lastserverstanding.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Random;

import edu.commonwealthu.lastserverstanding.R;

/**
 * Matrix-style binary rain animation background
 * Displays falling columns of 0s and 1s with random flickering
 */
public class BinaryRainView extends View {

    private static final int COLUMN_COUNT = 10; // Number of vertical columns (reduced for performance)
    private static final int UPDATE_INTERVAL_MS = 50; // Update every 50ms (20 FPS for efficiency)
    private static final float FALL_SPEED = 16f; // Pixels per update (compensate for slower updates)
    private static final int MAX_TRAIL_LENGTH = 10; // Maximum digits in trail (reduced for performance)
    private static final float FLICKER_CHANCE = 0.2f; // 20% chance to flicker per update

    private final Paint paint;
    private final Random random;
    private BinaryColumn[] columns;
    private float columnWidth;
    private float textSize;
    private boolean isRunning;
    private final Runnable updateRunnable;

    public BinaryRainView(Context context) {
        this(context, null);
    }

    public BinaryRainView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BinaryRainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(ContextCompat.getColor(context, R.color.electric_blue));
        paint.setTextAlign(Paint.Align.CENTER);

        random = new Random();
        columns = new BinaryColumn[COLUMN_COUNT];

        // Runnable for periodic updates
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    updateColumns();
                    invalidate();
                    postDelayed(this, UPDATE_INTERVAL_MS);
                }
            }
        };
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate column spacing and text size
        columnWidth = (float) w / COLUMN_COUNT;
        textSize = columnWidth * 0.6f; // Text size based on column width
        paint.setTextSize(textSize);

        // Initialize columns with random starting positions
        columns = new BinaryColumn[COLUMN_COUNT];
        for (int i = 0; i < COLUMN_COUNT; i++) {
            columns[i] = new BinaryColumn(i, h);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (columns == null) {
            return; // Not initialized yet
        }

        // Draw each column
        for (BinaryColumn column : columns) {
            if (column != null) {
                column.draw(canvas, paint, columnWidth, textSize);
            }
        }
    }

    /**
     * Update all columns - move them down and handle flickering
     */
    private void updateColumns() {
        for (BinaryColumn column : columns) {
            if (column != null) {
                column.update(getHeight(), random);
            }
        }
    }

    /**
     * Start the animation
     */
    public void startAnimation() {
        if (!isRunning) {
            isRunning = true;
            post(updateRunnable);
        }
    }

    /**
     * Stop the animation
     */
    public void stopAnimation() {
        isRunning = false;
        removeCallbacks(updateRunnable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    /**
     * Represents a single column of falling binary digits
     */
    private static class BinaryColumn {
        private final int columnIndex;
        private float y; // Current y position of the head
        private final StringBuilder digits; // The binary digits in this column
        private int trailLength; // Current length of the trail

        public BinaryColumn(int columnIndex, int screenHeight) {
            this.columnIndex = columnIndex;
            this.digits = new StringBuilder();

            // Start at random position - some on screen, some above
            Random rand = new Random();
            this.y = rand.nextFloat() * screenHeight * 1.5f - screenHeight * 0.5f; // Range from -50% to +100% of screen
            this.trailLength = 5 + rand.nextInt(MAX_TRAIL_LENGTH - 5);

            // Initialize with random binary digits
            for (int i = 0; i < trailLength; i++) {
                digits.append(rand.nextInt(2));
            }
        }

        public void update(int screenHeight, Random random) {
            // Move down
            y += FALL_SPEED;

            // Random flickering - change some digits
            if (random.nextFloat() < FLICKER_CHANCE) {
                int flickerIndex = random.nextInt(digits.length());
                digits.setCharAt(flickerIndex, random.nextInt(2) == 0 ? '0' : '1');
            }

            // If column has fallen off screen, reset to top
            if (y - (trailLength * 30) > screenHeight) { // 30 is approximate char height
                y = -50;
                trailLength = 5 + random.nextInt(MAX_TRAIL_LENGTH - 5);

                // Generate new random digits
                digits.setLength(0);
                for (int i = 0; i < trailLength; i++) {
                    digits.append(random.nextInt(2));
                }
            }
        }

        public void draw(Canvas canvas, Paint paint, float columnWidth, float textSize) {
            float x = columnIndex * columnWidth + columnWidth / 2;

            // Draw each digit in the trail with decreasing alpha
            for (int i = 0; i < digits.length(); i++) {
                float digitY = y - (i * textSize);

                // Skip if above screen
                if (digitY < 0) continue;

                // Calculate alpha - brighter at the head, fading toward tail
                float alphaFactor = 1.0f - ((float) i / digits.length());
                alphaFactor = Math.max(0.2f, alphaFactor); // Minimum 20% alpha

                int alpha = (int) (255 * alphaFactor * 0.6f); // 0.6f for overall dimness
                paint.setAlpha(alpha);

                // Draw the digit
                char digit = digits.charAt(i);
                canvas.drawText(String.valueOf(digit), x, digitY, paint);
            }

            // Reset alpha for next column
            paint.setAlpha(255);
        }
    }
}
