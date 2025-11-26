package edu.commonwealthu.lastserverstanding.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.commonwealthu.lastserverstanding.R;
import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.StatusEffect;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Manages the core game loop, entity updates, and rendering for the tower defense game.
 * Coordinates towers, enemies, projectiles, wave management, and player resources.
 * Uses object pooling and batched rendering for optimal performance.
 *
 * @author Ethan Wight
 */
public class GameEngine {

    /** Listener for game events and state changes. */
    private GameListener gameListener;

    // ==================== Game State ====================

    /** All active towers (synchronized for thread safety). */
    private final List<Tower> towers;

    /** All active enemies (synchronized for thread safety). */
    private final List<Enemy> enemies;

    /** All active projectiles (synchronized for thread safety). */
    private final List<Projectile> projectiles;

    /** Current wave number (0-indexed). */
    private int currentWave;

    /** Available resources for building and upgrading towers. */
    private int resources;

    /** Datacenter health (game over when reaches 0). */
    private int dataCenterHealth;

    /** Previous health value for detecting health loss. */
    private int previousHealth;

    /** Player's accumulated score. */
    private long score;

    /** Whether the game is currently paused. */
    private boolean isPaused;

    /** Whether the game has ended. */
    private boolean isGameOver;

    // ==================== FPS Tracking ====================

    /** Frames per second counter for performance monitoring. */
    private int fps;

    /** Timestamp of last FPS calculation in milliseconds. */
    private long lastFpsTime;

    /** Number of frames since last FPS calculation. */
    private int frameCount;

    // ==================== Game Systems ====================

    /** Spatial partitioning system for efficient collision detection. */
    private CollisionSystem collisionSystem;

    /** Pathfinding system using A* for enemy path generation. */
    private Pathfinding pathfinding;

    /** Wave management system controlling enemy spawning. */
    private final WaveManager waveManager;

    /** Game map with tile types and buildable areas. */
    private GameMap gameMap;

    /** Sound manager for playing tower attack sound effects. */
    private SoundManager soundManager;

    // ==================== World Dimensions ====================

    /** Width of the game world in pixels. */
    private int worldWidth;

    /** Height of the game world in pixels. */
    private int worldHeight;

    /** Size of each grid cell in pixels. */
    private int gridSize;

    // ==================== Game Constants ====================

    /** Initial resources when starting a new game. */
    private static final int STARTING_RESOURCES = 500;

    /** Initial datacenter health when starting a new game. */
    private static final int STARTING_HEALTH = 100;

    /** Size in pixels for rendered tower icons. */
    private static final int TOWER_ICON_SIZE = 48;

    /** Size in pixels for rendered datacenter icon. */
    private static final int DATACENTER_ICON_SIZE = 96;

    // ==================== Icon Caching ====================

    /**
     * Cache mapping tower type names to pre-rendered bitmap icons.
     * Avoids repeated vector drawable conversion during rendering.
     */
    private final Map<String, Bitmap> towerIcons;

    /**
     * Cache mapping enemy type names to pre-rendered bitmap icons.
     * Avoids repeated vector drawable conversion during rendering.
     */
    private final Map<String, Bitmap> enemyIcons;

    /**
     * Cached bitmap icon for the datacenter goal.
     * Loaded once and reused for all datacenter rendering.
     */
    private Bitmap dataCenterIcon;

    /**
     * Android context for accessing resources and system services.
     * Required for loading drawables, colors, and vibration services.
     */
    private Context context;

    // ==================== Settings ====================

    /**
     * Whether haptic feedback is enabled for game events.
     * When true, device vibrates on datacenter damage (if supported).
     */
    private boolean vibrationEnabled = true;

    /**
     * Whether sound effects are enabled for game events.
     * When true, plays alert sounds on datacenter damage.
     */
    private boolean soundEnabled = true;

    /**
     * Whether to show tower range indicators during gameplay.
     * When true, renders semi-transparent circles around towers.
     */
    private boolean showTowerRanges = true;

    // ==================== Object Pooling for Performance ====================

    /**
     * Reusable Rect object for bitmap rendering to avoid per-frame allocation.
     * Reduces garbage collection pressure in tight render loops.
     */
    private final Rect tempRect = new Rect();

    /**
     * Dedicated Paint object for rendering map tiles.
     * Pre-configured for non-antialiased fill style to maximize tile rendering performance.
     */
    private final Paint mapPaint = new Paint();

    /**
     * Dedicated Paint object for rendering tower icons and fallback circles.
     * Pre-configured with antialiasing for smooth circular shapes.
     */
    private final Paint towerPaint = new Paint();

    /**
     * Dedicated Paint object for rendering tower range indicators.
     * Pre-configured as semi-transparent stroke with antialiasing.
     */
    private final Paint towerRangePaint = new Paint();

    /**
     * Dedicated Paint object for rendering enemy icons and fallback circles.
     * Pre-configured with antialiasing for smooth shapes.
     */
    private final Paint enemyPaint = new Paint();

    /**
     * Dedicated Paint object for rendering projectiles.
     * Pre-configured with antialiasing and color changes per projectile type.
     */
    private final Paint projectilePaint = new Paint();

    /**
     * Dedicated Paint object for rendering enemy health bar fill.
     * Color set from resources once context is available.
     */
    private final Paint healthBarPaint = new Paint();

    /**
     * Dedicated Paint object for rendering enemy health bar background.
     * Color set from resources once context is available.
     */
    private final Paint healthBarBgPaint = new Paint();

    // ==================== Render Buffers ====================

    /**
     * Reusable list for defensive copy of towers during rendering.
     * Pre-allocated to avoid per-frame list allocation and garbage collection.
     */
    private final List<Tower> renderTowersCopy = new ArrayList<>();

    /**
     * Reusable list for defensive copy of enemies during rendering.
     * Pre-allocated to avoid per-frame list allocation and garbage collection.
     */
    private final List<Enemy> renderEnemiesCopy = new ArrayList<>();

    /**
     * Reusable list for defensive copy of projectiles during rendering.
     * Pre-allocated to avoid per-frame list allocation and garbage collection.
     */
    private final List<Projectile> renderProjectilesCopy = new ArrayList<>();

    // ==================== Batched Tile Rendering ====================

    /**
     * Reusable list storing grid coordinates of path tiles as long values.
     * Each long encodes (x, y) coordinates to avoid object allocation in render loop.
     * Cleared and repopulated each frame for batched color rendering.
     */
    private final List<Long> pathTiles = new ArrayList<>();

    /**
     * Reusable list storing grid coordinates of buildable tiles as long values.
     * Used for batched rendering of all buildable tiles with a single color set.
     */
    private final List<Long> buildableTiles = new ArrayList<>();

    /**
     * Reusable list storing grid coordinates of spawn tiles as long values.
     * Used for batched rendering of all spawn tiles with a single color set.
     */
    private final List<Long> spawnTiles = new ArrayList<>();

    /**
     * Reusable list storing grid coordinates of datacenter tiles as long values.
     * Used for batched rendering of all datacenter tiles with a single color set.
     */
    private final List<Long> datacenterTiles = new ArrayList<>();

    // ==================== Cached Colors ====================

    /**
     * Cached color value for path tiles (resolved from resources once).
     * Avoids repeated resource lookups during rendering.
     */
    private int pathColor;

    /**
     * Cached color value for wall/buildable tiles (resolved from resources once).
     * Avoids repeated resource lookups during rendering.
     */
    private int wallColor;

    /**
     * Cached color value for spawn tiles (resolved from resources once).
     * Avoids repeated resource lookups during rendering.
     */
    private int spawnColor;

    /**
     * Cached color value for datacenter tiles (resolved from resources once).
     * Avoids repeated resource lookups during rendering.
     */
    private int datacenterColor;

    /**
     * Constructor.
     */
    public GameEngine() {
        towerIcons = new HashMap<>();
        enemyIcons = new HashMap<>();
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();

        // Initialize dedicated Paint objects
        initializePaints();

        currentWave = 0;
        resources = STARTING_RESOURCES;
        dataCenterHealth = STARTING_HEALTH;
        previousHealth = STARTING_HEALTH;
        score = 0;
        isPaused = false;
        isGameOver = false;
        
        fps = 0;
        lastFpsTime = System.currentTimeMillis();
        frameCount = 0;
        
        // Initialize world dimensions (will be set by GameView)
        worldWidth = 1280;
        worldHeight = 720;
        gridSize = 64;
        
        // Initialize game systems
        collisionSystem = new CollisionSystem(worldWidth, worldHeight, gridSize * 2);
        pathfinding = new Pathfinding(worldWidth / gridSize, worldHeight / gridSize, gridSize);
        waveManager = new WaveManager();
    }

    /**
     * Initialize dedicated Paint objects to reduce configuration changes during rendering.
     */
    private void initializePaints() {
        // Map paint - for tiles
        mapPaint.setStyle(Paint.Style.FILL);
        mapPaint.setAntiAlias(false); // No AA needed for grid tiles

        // Tower paint - for drawing towers
        towerPaint.setStyle(Paint.Style.FILL);
        towerPaint.setAntiAlias(true);

        // Tower range paint - for showing tower ranges
        towerRangePaint.setStyle(Paint.Style.STROKE);
        towerRangePaint.setStrokeWidth(2);
        towerRangePaint.setAntiAlias(true);
        towerRangePaint.setAlpha(128); // 50% transparent

        // Enemy paint - for drawing enemies
        enemyPaint.setStyle(Paint.Style.FILL);
        enemyPaint.setAntiAlias(true);

        // Projectile paint - for drawing projectiles
        projectilePaint.setStyle(Paint.Style.FILL);
        projectilePaint.setAntiAlias(true);

        // Health bar paint - for enemy health bars
        healthBarPaint.setStyle(Paint.Style.FILL);
        healthBarPaint.setAntiAlias(false); // No AA for rectangles
        // Color will be set in setContext() after context is available

        // Health bar background - dark background for health bars
        healthBarBgPaint.setStyle(Paint.Style.FILL);
        healthBarBgPaint.setAntiAlias(false);
        // Color will be set in setContext() after context is available
    }

    /**
     * Set context for loading resources.
     *
     * @param context The application context
     */
    public void setContext(Context context) {
        this.context = context;

        // Cache color values to avoid per-frame resource lookups
        if (context != null) {
            pathColor = androidx.core.content.ContextCompat.getColor(context, R.color.path_gray);
            wallColor = androidx.core.content.ContextCompat.getColor(context, R.color.wall_dark_gray);
            spawnColor = androidx.core.content.ContextCompat.getColor(context, R.color.spawn_green);
            datacenterColor = androidx.core.content.ContextCompat.getColor(context, R.color.datacenter_blue);

            // Set health bar colors now that context is available
            healthBarPaint.setColor(ContextCompat.getColor(context, R.color.health_bar_fill));
            healthBarBgPaint.setColor(ContextCompat.getColor(context, R.color.health_bar_background));

            // Initialize sound manager
            soundManager = new SoundManager(context);
            soundManager.setSoundEnabled(soundEnabled);
        }
    }

    /**
     * Set world dimensions (called by GameView after initialization).
     *
     * @param width World width in pixels
     * @param height World height in pixels
     * @param cellSize Grid cell size in pixels
     */
    public void setWorldDimensions(int width, int height, int cellSize) {
        if (width <= 0 || height <= 0 || cellSize <= 0) {
            throw new IllegalArgumentException(
                "World dimensions must be positive: width=" + width +
                ", height=" + height + ", cellSize=" + cellSize
            );
        }

        this.worldWidth = width;
        this.worldHeight = height;
        this.gridSize = cellSize;

        // Reinitialize systems with correct dimensions
        collisionSystem = new CollisionSystem(width, height, cellSize * 2);
        pathfinding = new Pathfinding(width / cellSize, height / cellSize, cellSize);

        // Initialize game map
        gameMap = GameMap.createSimpleMap(cellSize);

        // Center the map on screen
        gameMap.centerOnScreen(width, height);
    }
    
    /**
     * Update game state.
     *
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (isPaused) return;
        
        // Update FPS counter
        updateFPS();
        
        // Update collision system
        collisionSystem.update(towers, enemies, projectiles);
        
        // Update wave manager
        waveManager.update(deltaTime, this);
        
        // Update all towers with collision system for better targeting
        synchronized (towers) {
            for (Tower tower : towers) {
                tower.update();

                // Use collision system for more efficient target acquisition
                if (tower.getTarget() == null || !tower.getTarget().isAlive()) {
                    Enemy closestEnemy = collisionSystem.getClosestEnemy(tower.getPosition(), tower.getRange());
                    tower.setTarget(closestEnemy);
                }

                // Fire if possible
                Projectile projectile = tower.fire();
                if (projectile != null) {
                    synchronized (projectiles) {
                        projectiles.add(projectile);
                    }

                    // Play tower-specific sound effect
                    playTowerSound(tower.getType());
                }
            }
        }

        // Update all enemies
        synchronized (enemies) {
            Iterator<Enemy> enemyIterator = enemies.iterator();
            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();
                enemy.update(deltaTime);

                // Check if enemy reached end
                if (enemy.hasReachedEnd()) {
                    dataCenterHealth -= enemy.getDamage();
                    notifyStatsChanged();
                    enemyIterator.remove();
                    continue;
                }

                // Remove dead enemies
                if (!enemy.isAlive()) {
                    resources += enemy.getReward();
                    score += enemy.getReward() * 10L;
                    notifyStatsChanged();
                    enemyIterator.remove();
                }
            }
        }

        // Update all projectiles
        synchronized (projectiles) {
            Iterator<Projectile> projectileIterator = projectiles.iterator();
            while (projectileIterator.hasNext()) {
                Projectile projectile = projectileIterator.next();
                projectile.update(deltaTime);

                // Remove projectiles that have hit
                if (projectile.hasHit()) {
                    projectileIterator.remove();
                }
            }
        }

        // Check if health decreased and trigger alert
        if (dataCenterHealth < previousHealth) {
            triggerEmergencyAlert();
            previousHealth = dataCenterHealth;
        }

        // Check for game over
        if (dataCenterHealth <= 0) {
            gameOver();
        }
    }
    
    /**
     * Render all game elements.
     *
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     */
    public void render(Canvas canvas, Paint paint) {
        // Draw map first (underneath everything)
        renderMap(canvas);

        // Draw data center goals
        drawGoals(canvas, paint);

        // Create defensive copies to avoid ConcurrentModificationException
        // Reuse pre-allocated lists instead of creating new ones each frame
        renderTowersCopy.clear();
        renderEnemiesCopy.clear();
        renderProjectilesCopy.clear();

        synchronized (towers) {
            renderTowersCopy.addAll(towers);
        }
        synchronized (enemies) {
            renderEnemiesCopy.addAll(enemies);
        }
        synchronized (projectiles) {
            renderProjectilesCopy.addAll(projectiles);
        }

        // Draw tower ranges first (underneath towers) if enabled
        if (showTowerRanges) {
            for (int i = 0; i < renderTowersCopy.size(); i++) {
                drawTowerRange(canvas, renderTowersCopy.get(i));
            }
        }

        // Draw towers
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < renderTowersCopy.size(); i++) {
            drawTower(canvas, renderTowersCopy.get(i));
        }

        // Draw enemies
        for (int i = 0; i < renderEnemiesCopy.size(); i++) {
            drawEnemy(canvas, paint, renderEnemiesCopy.get(i));
        }

        // Draw projectiles
        for (int i = 0; i < renderProjectilesCopy.size(); i++) {
            drawProjectile(canvas, renderProjectilesCopy.get(i));
        }
    }

    /**
     * Render the game map with different colors for different tile types.
     * Optimized to batch paint operations by color.
     *
     * @param canvas Canvas to draw on
     */
    private void renderMap(Canvas canvas) {
        if (gameMap == null) {
            return; // Guard against null gameMap
        }

        // Use dedicated mapPaint instead of generic paint

        float offsetX = gameMap.getOffsetX();
        float offsetY = gameMap.getOffsetY();

        // Clear reusable lists
        pathTiles.clear();
        buildableTiles.clear();
        spawnTiles.clear();
        datacenterTiles.clear();

        // First pass: categorize tiles by type (store grid coordinates as long)
        for (int y = 0; y < gameMap.getHeight(); y++) {
            for (int x = 0; x < gameMap.getWidth(); x++) {
                TileType tile = gameMap.getTileAt(x, y);

                if (tile == TileType.EMPTY) {
                    continue; // Don't draw empty tiles
                }

                // Encode grid coordinates as single long (no allocation!)
                long coords = ((long) x << 32) | (y & 0xFFFFFFFFL);

                switch (tile) {
                    case PATH:
                        pathTiles.add(coords);
                        break;
                    case BUILDABLE:
                        buildableTiles.add(coords);
                        break;
                    case SPAWN:
                        spawnTiles.add(coords);
                        break;
                    case DATACENTER:
                        datacenterTiles.add(coords);
                        break;
                }
            }
        }

        // Second pass: draw all tiles of each type with single color set (use cached colors)
        if (!pathTiles.isEmpty()) {
            mapPaint.setColor(pathColor);
            for (long coords : pathTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, mapPaint);
            }
        }

        if (!buildableTiles.isEmpty()) {
            mapPaint.setColor(wallColor);
            for (long coords : buildableTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, mapPaint);
            }
        }

        if (!spawnTiles.isEmpty()) {
            mapPaint.setColor(spawnColor);
            for (long coords : spawnTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, mapPaint);
            }
        }

        if (!datacenterTiles.isEmpty()) {
            mapPaint.setColor(datacenterColor);
            for (long coords : datacenterTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, mapPaint);
            }
        }
    }
    
    /**
     * Get tower icon resource ID based on type.
     *
     * @param towerType The type of tower
     * @return The resource ID for the tower icon
     */
    private int getTowerIconResource(String towerType) {
        return switch (towerType) {
            case "Firewall" -> R.drawable.ic_tower_firewall;
            case "Honeypot" -> R.drawable.ic_tower_honeypot;
            case "Jammer" -> R.drawable.ic_tower_jammer;
            default -> {
                // Unknown tower type, log warning and use firewall as fallback
                android.util.Log.w("GameEngine", "Unknown tower type: " + towerType);
                yield R.drawable.ic_tower_firewall;
            }
        };
    }

    /**
     * Load and cache tower icon.
     * Converts vector drawables to bitmaps for rendering.
     *
     * @param towerType The type of tower
     * @return The cached tower icon bitmap, or null if loading failed
     */
    private Bitmap getTowerIcon(String towerType) {
        if (!towerIcons.containsKey(towerType) && context != null) {
            try {
                int resourceId = getTowerIconResource(towerType);

                // Load the drawable (works for both vector and bitmap drawables)
                Drawable drawable = ContextCompat.getDrawable(context, resourceId);

                if (drawable != null) {
                    // Create a bitmap to draw the vector drawable into
                    Bitmap bitmap = Bitmap.createBitmap(
                            TOWER_ICON_SIZE, TOWER_ICON_SIZE, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    // Set the bounds and draw
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);

                    towerIcons.put(towerType, bitmap);
                } else {
                    // If drawable is null, cache null to avoid repeated attempts
                    towerIcons.put(towerType, null);
                }
            } catch (Exception e) {
                // If loading fails, cache null to avoid repeated attempts
                towerIcons.put(towerType, null);
            }
        }
        return towerIcons.get(towerType);
    }

    /**
     * Load and cache enemy icon.
     * Converts vector drawables to bitmaps for rendering.
     *
     * @param enemy The enemy to get the icon for
     * @return The cached enemy icon bitmap, or null if loading failed
     */
    private Bitmap getEnemyIcon(Enemy enemy) {
        String enemyType = enemy.getType();
        if (!enemyIcons.containsKey(enemyType) && context != null) {
            try {
                int resourceId = enemy.getIconResId();

                // Load the drawable (works for both vector and bitmap drawables)
                Drawable drawable = ContextCompat.getDrawable(context, resourceId);

                if (drawable != null) {
                    // Create bitmap and canvas
                    Bitmap bitmap = Bitmap.createBitmap(
                            TOWER_ICON_SIZE, TOWER_ICON_SIZE, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    // Set the bounds and draw
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);

                    enemyIcons.put(enemyType, bitmap);
                } else {
                    // If drawable is null, cache null to avoid repeated attempts
                    enemyIcons.put(enemyType, null);
                }
            } catch (Exception e) {
                // If loading fails, cache null to avoid repeated attempts
                enemyIcons.put(enemyType, null);
            }
        }
        return enemyIcons.get(enemyType);
    }

    /**
     * Load and cache data center goal icon.
     * Converts vector drawable to bitmap for rendering.
     *
     * @return The cached datacenter icon bitmap, or null if loading failed
     */
    private Bitmap getDataCenterIcon() {
        if (dataCenterIcon == null && context != null) {
            try {
                // Load the data center drawable
                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_datacenter);

                if (drawable != null) {
                    // Create a larger bitmap for the goal icon (more visible)
                    Bitmap bitmap = Bitmap.createBitmap(
                            DATACENTER_ICON_SIZE, DATACENTER_ICON_SIZE,
                            Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    // Set the bounds and draw
                    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    drawable.draw(canvas);

                    dataCenterIcon = bitmap;
                }
            } catch (Exception e) {
                // If loading fails, set to null
                dataCenterIcon = null;
            }
        }
        return dataCenterIcon;
    }

    /**
     * Draw data center icon at the map exit/goal position.
     *
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     */
    private void drawGoals(Canvas canvas, Paint paint) {
        if (gameMap == null) return;

        Bitmap icon = getDataCenterIcon();
        if (icon == null) return;

        PointF dataCenterPos = gameMap.getDataCenterPoint();
        if (dataCenterPos == null) return;

        // Calculate position to center the icon on the data center tile
        float left = dataCenterPos.x - icon.getWidth() / 2f;
        float top = dataCenterPos.y - icon.getHeight() / 2f;

        // Draw the data center icon
        canvas.drawBitmap(icon, left, top, paint);
    }

    /**
     * Draw tower range indicator (semi-transparent circle).
     *
     * @param canvas Canvas to draw on
     * @param tower The tower to draw range for
     */
    private void drawTowerRange(Canvas canvas, Tower tower) {
        if (tower == null) return;
        PointF pos = tower.getPosition();
        if (pos == null) return;

        // Use dedicated range paint (already configured with stroke, alpha, etc.)
        // Draw range circle border
        towerRangePaint.setColor(ContextCompat.getColor(context, R.color.tower_range_indicator));
        canvas.drawCircle(pos.x, pos.y, tower.getRange(), towerRangePaint);
    }

    /**
     * Draw a tower with its icon (optimized to reduce object allocation).
     *
     * @param canvas Canvas to draw on
     * @param tower The tower to draw
     */
    private void drawTower(Canvas canvas, Tower tower) {
        if (tower == null) return;
        PointF pos = tower.getPosition();
        if (pos == null) return;

        // Get tower icon
        Bitmap icon = getTowerIcon(tower.getType());

        if (icon != null) {
            // Draw icon centered on position - reuse tempRect to avoid allocation
            int halfSize = icon.getWidth() / 2;
            tempRect.set(
                    (int) (pos.x - halfSize),
                    (int) (pos.y - halfSize),
                    (int) (pos.x + halfSize),
                    (int) (pos.y + halfSize)
            );

            canvas.drawBitmap(icon, null, tempRect, towerPaint);
        } else {
            // Fallback to circle if icon not available - use dedicated tower paint
            towerPaint.setColor(ContextCompat.getColor(context, R.color.tower_default));
            canvas.drawCircle(pos.x, pos.y, 24, towerPaint);
        }

    }
    
    /**
     * Draw an enemy with its icon.
     *
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     * @param enemy The enemy to draw
     */
    private void drawEnemy(Canvas canvas, Paint paint, Enemy enemy) {
        if (enemy == null) return;
        PointF pos = enemy.getPosition();
        if (pos == null) return;

        // Get enemy icon
        Bitmap icon = getEnemyIcon(enemy);

        if (icon != null) {
            // Draw icon centered on position - reuse tempRect to avoid allocation
            int halfSize = icon.getWidth() / 2;
            tempRect.set(
                    (int) (pos.x - halfSize),
                    (int) (pos.y - halfSize),
                    (int) (pos.x + halfSize),
                    (int) (pos.y + halfSize)
            );

            canvas.drawBitmap(icon, null, tempRect, enemyPaint);
        } else {
            // Fallback to circle if icon not available - use dedicated enemy paint
            enemyPaint.setColor(enemy.getColor());
            canvas.drawCircle(pos.x, pos.y, 15, enemyPaint);
        }

        // Draw status effect animations
        drawStatusEffects(canvas, paint, enemy);

        // Draw health bar above the enemy - use dedicated health bar paints
        float healthBarWidth = 30 * enemy.getHealthPercentage();
        // Draw background
        canvas.drawRect(
            pos.x - 15,
            pos.y - 30,
            pos.x + 15,
            pos.y - 25,
            healthBarBgPaint
        );
        // Draw health
        canvas.drawRect(
            pos.x - 15,
            pos.y - 30,
            pos.x - 15 + healthBarWidth,
            pos.y - 25,
            healthBarPaint
        );
    }

    /**
     * Draw status effect animations around an enemy.
     *
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     * @param enemy The enemy to draw effects for
     */
    private void drawStatusEffects(Canvas canvas, Paint paint, Enemy enemy) {
        if (enemy == null) return;
        PointF pos = enemy.getPosition();
        if (pos == null) return;

        List<StatusEffect> effects = enemy.getStatusEffects();
        if (effects == null || effects.isEmpty()) return;

        // Save current paint settings
        int originalColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();
        float originalStrokeWidth = paint.getStrokeWidth();

        // Check for each effect type and draw appropriate animation
        for (StatusEffect effect : effects) {
            switch (effect.getType()) {
                case BURN:
                    drawBurnEffect(canvas, paint, pos);
                    break;
                case SLOW:
                    drawSlowEffect(canvas, paint, pos);
                    break;
                case STUN:
                    drawStunEffect(canvas, paint, pos);
                    break;
            }
        }

        // Restore paint settings
        paint.setColor(originalColor);
        paint.setStyle(originalStyle);
        paint.setStrokeWidth(originalStrokeWidth);
    }

    /**
     * Draw burn effect (fire particles).
     *
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     * @param pos Position to draw effect at
     */
    private void drawBurnEffect(Canvas canvas, Paint paint, PointF pos) {
        paint.setStyle(Paint.Style.FILL);
        long time = System.currentTimeMillis();

        ContextCompat.getColor(context, R.color.effect_burn_base);

        // Animated fire particles rising up
        for (int i = 0; i < 4; i++) {
            float angle = (float) ((time / 100.0 + i * 90) % 360);
            float radius = 20 + (float) Math.sin(time / 200.0 + i) * 5;
            float offsetY = -10 - (float) ((time / 50.0 + i * 10) % 20);

            float x = pos.x + (float) Math.cos(Math.toRadians(angle)) * (radius / 2);
            float y = pos.y + offsetY;

            // Gradient from base color with varying alpha and green component for gradient effect
            int alpha = (int) (100 + Math.sin(time / 100.0 + i) * 50);
            paint.setColor(Color.argb(alpha, 255, 100 + i * 30, 0));
            canvas.drawCircle(x, y, 3 - i * 0.5f, paint);
        }
    }

    /**
     * Draw slow effect (dripping glue/honey).
     *
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     * @param pos Position to draw effect at
     */
    private void drawSlowEffect(Canvas canvas, Paint paint, PointF pos) {
        long time = System.currentTimeMillis();

        // Get base colors for slow effect
        int amberColor = ContextCompat.getColor(context, R.color.effect_slow_amber);
        int amberDarkColor = ContextCompat.getColor(context, R.color.effect_slow_amber_dark);
        int amberDropColor = ContextCompat.getColor(context, R.color.effect_slow_amber_drop);

        // Dripping glue effect with honey/amber color
        paint.setStyle(Paint.Style.FILL);

        // Draw main glue blob on enemy
        int alpha = 180;
        paint.setColor(Color.argb(alpha, Color.red(amberColor), Color.green(amberColor), Color.blue(amberColor)));
        canvas.drawCircle(pos.x, pos.y, 12, paint);

        // Add darker center for depth
        paint.setColor(Color.argb(alpha, Color.red(amberDarkColor), Color.green(amberDarkColor), Color.blue(amberDarkColor)));
        canvas.drawCircle(pos.x, pos.y, 8, paint);

        // Draw dripping glue drops
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 4; i++) {
            // Slow dripping animation
            float dropProgress = (float) ((time / 150.0 + i * 3) % 12);
            float angle = i * 90; // Four drips evenly spaced

            float dropX = pos.x + (float) Math.cos(Math.toRadians(angle)) * 10;
            float dropY = pos.y + dropProgress;

            // Draw elongated drip
            int dropAlpha = (int) (150 * (1.0f - dropProgress / 12));
            paint.setColor(Color.argb(dropAlpha, Color.red(amberDropColor), Color.green(amberDropColor), Color.blue(amberDropColor)));
            canvas.drawCircle(dropX, dropY, 3, paint);

            // Draw connecting string from blob to drip
            paint.setStrokeWidth(1.5f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.argb(dropAlpha, Color.red(amberDropColor), Color.green(amberDropColor), Color.blue(amberDropColor)));
            canvas.drawLine(pos.x, pos.y + 8, dropX, dropY, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    /**
     * Draw stun effect (electric sparks).
     *
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     * @param pos Position to draw effect at
     */
    private void drawStunEffect(Canvas canvas, Paint paint, PointF pos) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        long time = System.currentTimeMillis();

        // Get stun effect colors
        int stunYellowColor = ContextCompat.getColor(context, R.color.effect_stun_yellow);
        int stunGlowColor = ContextCompat.getColor(context, R.color.effect_stun_yellow_glow);

        // Electric lightning bolts
        for (int i = 0; i < 5; i++) {
            float angle = (float) ((time / 20.0 + i * 72) % 360);
            float radius = 22;

            // Jagged lightning effect
            float prevX = pos.x;
            float prevY = pos.y;

            for (int j = 0; j < 3; j++) {
                float segmentAngle = angle + (float) (Math.random() * 30 - 15);
                float segmentRadius = radius * (j + 1) / 3;
                float x = pos.x + (float) Math.cos(Math.toRadians(segmentAngle)) * segmentRadius;
                float y = pos.y + (float) Math.sin(Math.toRadians(segmentAngle)) * segmentRadius;

                int alpha = (int) (150 + Math.sin(time / 50.0 + i) * 100);
                paint.setColor(Color.argb(alpha, Color.red(stunYellowColor), Color.green(stunYellowColor), Color.blue(stunYellowColor)));
                canvas.drawLine(prevX, prevY, x, y, paint);

                prevX = x;
                prevY = y;
            }
        }

        // Outer glow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(50, Color.red(stunGlowColor), Color.green(stunGlowColor), Color.blue(stunGlowColor)));
        canvas.drawCircle(pos.x, pos.y, 25, paint);
    }
    
    /**
     * Draw a projectile.
     *
     * @param canvas Canvas to draw on
     * @param projectile The projectile to draw
     */
    private void drawProjectile(Canvas canvas, Projectile projectile) {
        if (projectile == null) return;
        PointF pos = projectile.getPosition();
        if (pos == null) return;

        // Color projectile based on status effect type
        StatusEffect effect = projectile.getStatusEffect();
        if (effect != null) {
            switch (effect.getType()) {
                case BURN:
                    // Firewall - Red/Orange
                    projectilePaint.setColor(ContextCompat.getColor(context, R.color.projectile_burn));
                    break;
                case SLOW:
                    // Honeypot - Amber/Honey
                    projectilePaint.setColor(ContextCompat.getColor(context, R.color.projectile_slow));
                    break;
                case STUN:
                    // Jammer - Blue/Electric
                    projectilePaint.setColor(ContextCompat.getColor(context, R.color.projectile_stun));
                    break;
                default:
                    projectilePaint.setColor(ContextCompat.getColor(context, R.color.projectile_default));
                    break;
            }
        } else {
            projectilePaint.setColor(ContextCompat.getColor(context, R.color.projectile_default));
        }

        canvas.drawCircle(pos.x, pos.y, 5, projectilePaint);
    }
    
    /**
     * Update FPS counter.
     */
    private void updateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }
    
    /**
     * Handle tap on game view.
     *
     * @param worldPos The world position that was tapped
     */
    public void handleTap(PointF worldPos) {
        // Will be implemented for tower placement
        // For now, just log the position
        android.util.Log.d("GameEngine", "Tapped at: " + worldPos.x + ", " + worldPos.y);
    }
    
    /**
     * Check if a tower can be placed at the given position.
     *
     * @param position World position to check
     * @return true if placement is valid
     */
    public boolean isValidTowerPlacement(PointF position) {
        if (gameMap == null) return false;

        // Check if position is on a buildable tile
        PointF gridPos = gameMap.worldToGrid(position);
        if (!gameMap.isBuildable(gridPos)) {
            return false;
        }

        // Check spacing from other towers
        synchronized (towers) {
            for (Tower existingTower : towers) {
                float dx = existingTower.getPosition().x - position.x;
                float dy = existingTower.getPosition().y - position.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                // Enforce minimum spacing between towers (one grid cell)
                if (distance < gridSize) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Add a tower to the game.
     *
     * @param tower The tower to add
     * @return true if tower was added successfully
     */
    public boolean addTower(Tower tower) {
        if (tower == null) {
            return false;
        }

        // Check if tower can be placed at this position
        if (!isValidTowerPlacement(tower.getPosition())) {
            android.util.Log.w("GameEngine", "Cannot place tower - invalid position or too close to another tower");
            return false;
        }

        if (resources >= tower.getCost()) {
            synchronized (towers) {
                towers.add(tower);
            }
            resources -= tower.getCost();
            notifyStatsChanged();
            return true;
        }
        return false;
    }

    /**
     * Remove a tower from the game and refund partial resources.
     *
     * @param tower The tower to remove
     * @return true if tower was successfully removed
     */
    public boolean removeTower(Tower tower) {
        if (tower == null) {
            return false;
        }

        synchronized (towers) {
            if (towers.remove(tower)) {
                // Refund 50% of total investment (base cost + all upgrades)
                int refund = tower.getTotalInvestment() / 2;
                resources += refund;
                notifyStatsChanged();
                android.util.Log.d("GameEngine",
                        "Tower removed, refunded " + refund + " resources (50% of "
                        + tower.getTotalInvestment() + " total investment)");
                return true;
            }
        }
        return false;
    }

    /**
     * Add an enemy to the game.
     *
     * @param enemy The enemy to add
     */
    public void addEnemy(Enemy enemy) {
        synchronized (enemies) {
            enemies.add(enemy);
        }
    }

    /**
     * Get tower at a specific position (grid coordinates).
     *
     * @param worldPos World position to check
     * @return Tower at that position, or null if none found
     */
    public Tower getTowerAt(PointF worldPos) {
        if (gameMap == null) return null;

        PointF gridPos = gameMap.worldToGrid(worldPos);
        synchronized (towers) {
            for (Tower tower : towers) {
                PointF towerGridPos = gameMap.worldToGrid(tower.getPosition());
                if (Math.abs(towerGridPos.x - gridPos.x) < 0.5f &&
                    Math.abs(towerGridPos.y - gridPos.y) < 0.5f) {
                    return tower;
                }
            }
        }
        return null;
    }

    /**
     * Upgrade a tower.
     *
     * @param tower Tower to upgrade
     * @return true if upgrade successful
     */
    public boolean upgradeTower(Tower tower) {
        if (tower == null) return false;

        int upgradeCost = tower.getUpgradeCost();
        if (upgradeCost == 0) {
            return false; // Already at max level
        }

        if (resources >= upgradeCost) {
            if (tower.upgrade(upgradeCost)) {
                resources -= upgradeCost;
                notifyStatsChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Add resources.
     *
     * @param amount Amount of resources to add
     */
    public void addResources(int amount) {
        resources += amount;
        notifyStatsChanged();
    }
    
    /**
     * Add score.
     *
     * @param amount Amount of score to add
     */
    public void addScore(long amount) {
        score += amount;
        notifyStatsChanged();
    }

    /**
     * Notify listener that stats have changed.
     */
    private void notifyStatsChanged() {
        if (gameListener != null) {
            gameListener.onStatsChanged(currentWave, resources, dataCenterHealth, score);
        }
    }

    /**
     * Handle game over.
     */
    private void gameOver() {
        if (isGameOver) return; // Prevent multiple game over calls

        isGameOver = true;
        isPaused = true;

        // Notify listener
        if (gameListener != null) {
            gameListener.onGameOver(currentWave);
        }
    }
    
    /**
     * Start next wave.
     */
    public void startNextWave() {
        waveManager.startNextWave(this);
        currentWave = waveManager.getCurrentWave();
        notifyStatsChanged();
    }

    /**
     * Capture current game state for saving.
     *
     * @return The captured game state
     */
    public edu.commonwealthu.lastserverstanding.data.models.GameState captureGameState() {
        edu.commonwealthu.lastserverstanding.data.models.GameState state = new edu.commonwealthu.lastserverstanding.data.models.GameState();
        state.currentWave = currentWave;
        state.resources = resources;
        state.dataCenterHealth = dataCenterHealth;
        state.score = score;

        // Capture tower data
        synchronized (towers) {
            for (Tower tower : towers) {
                state.towers.add(new edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData(
                    tower.getType(),
                    tower.getPosition(),
                    tower.getLevel()
                ));
            }
        }

        // Capture enemy data
        synchronized (enemies) {
            for (Enemy enemy : enemies) {
                state.enemies.add(new edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData(
                    enemy.getType(),
                    enemy.getPosition(),
                    enemy.getHealth(),
                    enemy.getCurrentPathIndex(),
                    enemy.getPath()
                ));
            }
        }

        return state;
    }

    /**
     * Restore game state from saved data.
     *
     * @param state The saved game state to restore
     */
    public void restoreGameState(
            edu.commonwealthu.lastserverstanding.data.models.GameState state) {
        // Clear current state
        synchronized (towers) {
            towers.clear();
        }
        synchronized (enemies) {
            enemies.clear();
        }
        synchronized (projectiles) {
            projectiles.clear();
        }

        // Restore basic values
        currentWave = state.currentWave;
        resources = state.resources;
        dataCenterHealth = state.dataCenterHealth;
        previousHealth = state.dataCenterHealth; // Initialize to avoid false alert on load
        score = state.score;

        // Restore wave manager state to match saved wave
        waveManager.restoreWaveState(state.currentWave);

        // Restore towers
        synchronized (towers) {
            for (edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData towerData : state.towers) {
                Tower tower = createTowerFromData(towerData);
                if (tower != null) {
                    towers.add(tower);
                }
            }
        }

        // Restore enemies
        synchronized (enemies) {
            for (edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData enemyData : state.enemies) {
                Enemy enemy = createEnemyFromData(enemyData);
                if (enemy != null) {
                    enemies.add(enemy);
                }
            }
        }

        // Notify that stats have been restored
        notifyStatsChanged();
    }

    /**
     * Helper to create tower from saved data.
     *
     * @param data The saved tower data
     * @return The recreated tower, or null if type is unknown
     */
    private Tower createTowerFromData(
            edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData data) {
        PointF pos = new PointF(data.x, data.y);
        Tower tower;

        // Create tower based on type
        switch (data.type) {
            case "Firewall":
                tower = new edu.commonwealthu.lastserverstanding.model.towers.FirewallTower(pos);
                break;
            case "Honeypot":
                tower = new edu.commonwealthu.lastserverstanding.model.towers.HoneypotTower(pos);
                break;
            case "Jammer":
                tower = new edu.commonwealthu.lastserverstanding.model.towers.JammerTower(pos);
                break;
            default:
                // Unknown tower type - skip it
                return null;
        }

        // Upgrade to saved level
        for (int i = 1; i < data.level; i++) {
            int upgradeCost = tower.getUpgradeCost();
            tower.upgrade(upgradeCost);
        }
        return tower;
    }

    /**
     * Helper to create enemy from saved data.
     *
     * @param data The saved enemy data
     * @return The recreated enemy, or null if type is unsupported
     */
    private Enemy createEnemyFromData(
            edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData data) {
        // Reconstruct path
        List<PointF> path = new ArrayList<>();
        for (edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData.Point p : data.path) {
            path.add(new PointF(p.x(), p.y()));
        }

        // For now, only support DataCrawler (expand later)
        if ("Data Crawler".equals(data.type)) {
            edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler enemy =
                    new edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler(path);
            // Set position and health
            enemy.setPosition(new PointF(data.x, data.y));
            enemy.setCurrentPathIndex(data.currentPathIndex);
            float damage = enemy.getMaxHealth() - data.health;
            enemy.takeDamage(damage);
            return enemy;
        }

        return null;
    }


    /**
     * Trigger emergency alert with haptic and audio feedback.
     */
    public void triggerEmergencyAlert() {
        android.util.Log.d("GameEngine", "Emergency alert triggered!");

        // Play alert sound if enabled
        if (soundEnabled) {
            playAlertSound();
        }

        // Trigger haptic feedback if enabled
        if (vibrationEnabled && context != null) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Modern vibration pattern for emergency alert
                    // Pattern: short-long-short (SOS-like)
                    long[] timings = {0, 200, 100, 400, 100, 200};
                    int[] amplitudes = {0, 255, 0, 255, 0, 255};
                    VibrationEffect effect = VibrationEffect.createWaveform(
                            timings, amplitudes, -1);
                    vibrator.vibrate(effect);
                } else {
                    // Fallback for older devices
                    long[] pattern = {0, 200, 100, 400, 100, 200};
                    vibrator.vibrate(pattern, -1);
                }
            }
        }
    }

    /**
     * Play emergency alert sound using ToneGenerator.
     */
    private void playAlertSound() {
        new Thread(() -> {
            android.media.ToneGenerator toneGen = null;
            try {
                toneGen = new android.media.ToneGenerator(
                        android.media.AudioManager.STREAM_ALARM, 100);
                // Play a series of urgent tones (high pitched beeps)
                toneGen.startTone(
                        android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                Thread.sleep(300);
                toneGen.startTone(
                        android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400);
                Thread.sleep(500);
                toneGen.startTone(
                        android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                Thread.sleep(300);
            } catch (Exception e) {
                // Silently fail if sound cannot be played
                android.util.Log.e("GameEngine",
                        "Failed to play alert sound: " + e.getMessage(), e);
            } finally {
                // Always release ToneGenerator to prevent resource leak
                if (toneGen != null) {
                    try {
                        toneGen.release();
                    } catch (Exception e) {
                        android.util.Log.e("GameEngine",
                                "Error releasing ToneGenerator: " + e.getMessage(), e);
                    }
                }
            }
        }).start();
    }

    /**
     * Set vibration enabled state from settings.
     *
     * @param enabled True to enable vibration, false to disable
     */
    public void setVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
    }

    /**
     * Set sound enabled state from settings.
     *
     * @param enabled True to enable sound, false to disable
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        if (soundManager != null) {
            soundManager.setSoundEnabled(enabled);
        }
    }

    /**
     * Set tower range visibility from settings.
     *
     * @param enabled True to show tower ranges, false to hide
     */
    public void setShowTowerRanges(boolean enabled) {
        this.showTowerRanges = enabled;
    }

    // Getters and Setters
    public int getCurrentWave() { return currentWave; }
    public int getResources() { return resources; }
    public long getScore() { return score; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    public int getFPS() { return fps; }
    public boolean isGameOver() { return isGameOver; }

    public List<Tower> getTowers() { return towers; }
    public List<Enemy> getEnemies() { return enemies; }

    public Pathfinding getPathfinding() { return pathfinding; }
    public WaveManager getWaveManager() { return waveManager; }
    public GameMap getGameMap() { return gameMap; }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
        // Immediately notify with current stats
        if (listener != null) {
            listener.onStatsChanged(currentWave, resources, dataCenterHealth, score);
        }
    }

    /**
     * Notify listener that a wave has been completed.
     *
     * @param waveNumber The number of the completed wave
     */
    public void notifyWaveComplete(int waveNumber) {
        if (gameListener != null) {
            gameListener.onWaveComplete(waveNumber);
        }
    }

    /**
     * Plays the appropriate sound effect for a tower type.
     *
     * @param towerType The type of tower ("Firewall", "Honeypot", "Jammer")
     */
    private void playTowerSound(String towerType) {
        if (soundManager == null) {
            android.util.Log.w("GameEngine", "SoundManager is null, cannot play sound for " + towerType);
            return;
        }

        switch (towerType) {
            case "Firewall":
                android.util.Log.d("GameEngine", "Playing Firewall burn sound");
                soundManager.playSound(SoundManager.SoundType.FIREWALL_BURN);
                break;
            case "Honeypot":
                android.util.Log.d("GameEngine", "Playing Honeypot sticky sound");
                soundManager.playSound(SoundManager.SoundType.HONEYPOT_STICKY);
                break;
            case "Jammer":
                android.util.Log.d("GameEngine", "Playing Jammer zap sound");
                soundManager.playSound(SoundManager.SoundType.JAMMER_ZAP);
                break;
        }
    }

    /**
     * Cleanup method to release resources when game is destroyed.
     * Should be called from Fragment/Activity onDestroy().
     */
    public void cleanup() {
        if (soundManager != null) {
            soundManager.release();
            soundManager = null;
        }
    }

    /**
     * Interface for game event callbacks.
     */
    public interface GameListener {
        void onGameOver(int finalWave);
        void onStatsChanged(int wave, int resources, int health, long score);
        void onWaveComplete(int waveNumber);
    }
}
