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
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Core game engine managing all game logic and state
 */
public class GameEngine {

    // Game listener for events
    private GameListener gameListener;

    // Game state
    private final List<Tower> towers;
    private final List<Enemy> enemies;
    private final List<Projectile> projectiles;

    private int currentWave;
    private int resources;
    private int dataCenterHealth;
    private int previousHealth;
    private long score;
    private boolean isPaused;
    private boolean isGameOver;
    
    // FPS tracking
    private int fps;
    private long lastFpsTime;
    private int frameCount;
    
    // Game systems
    private CollisionSystem collisionSystem;
    private Pathfinding pathfinding;
    private final WaveManager waveManager;
    private GameMap gameMap;

    // World dimensions
    private int worldWidth;
    private int worldHeight;
    private int gridSize;
    
    // Game constants
    private static final int STARTING_RESOURCES = 500;
    private static final int STARTING_HEALTH = 100;

    // Tower icon cache
    private final Map<String, Bitmap> towerIcons;
    private Context context;

    // Settings
    private boolean vibrationEnabled = true;
    private boolean soundEnabled = true;

    // Object pooling for performance (reduce GC)
    private final Rect tempRect = new Rect();

    // Reusable lists for rendering (avoid allocation per frame)
    private final List<Tower> renderTowersCopy = new ArrayList<>();
    private final List<Enemy> renderEnemiesCopy = new ArrayList<>();
    private final List<Projectile> renderProjectilesCopy = new ArrayList<>();

    // Reusable lists for batched tile rendering (store grid coordinates as longs)
    private final List<Long> pathTiles = new ArrayList<>();
    private final List<Long> buildableTiles = new ArrayList<>();
    private final List<Long> spawnTiles = new ArrayList<>();
    private final List<Long> datacenterTiles = new ArrayList<>();

    // Cached rendering resources (avoid per-frame allocation/lookup)
    private static final android.graphics.ColorFilter CORRUPTION_FILTER =
        new android.graphics.PorterDuffColorFilter(
            android.graphics.Color.RED,
            android.graphics.PorterDuff.Mode.MULTIPLY);

    private int pathColor;
    private int wallColor;
    private int spawnColor;
    private int datacenterColor;

    /**
     * Constructor
     */
    public GameEngine() {
        towerIcons = new HashMap<>();
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();

        // Initialize temp paint
        Paint tempPaint = new Paint();
        tempPaint.setAntiAlias(true);
        
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
     * Set context for loading resources
     */
    public void setContext(Context context) {
        this.context = context;

        // Cache color values to avoid per-frame resource lookups
        if (context != null) {
            pathColor = androidx.core.content.ContextCompat.getColor(context, R.color.path_gray);
            wallColor = androidx.core.content.ContextCompat.getColor(context, R.color.wall_dark_gray);
            spawnColor = androidx.core.content.ContextCompat.getColor(context, R.color.spawn_green);
            datacenterColor = androidx.core.content.ContextCompat.getColor(context, R.color.datacenter_blue);
        }
    }

    /**
     * Set world dimensions (called by GameView after initialization)
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
        int gridWidth = width / cellSize;
        int gridHeight = height / cellSize;
        gameMap = GameMap.createSimpleMap(gridWidth, gridHeight, cellSize);

        // Center the map on screen
        gameMap.centerOnScreen(width, height);
    }
    
    /**
     * Update game state
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
                tower.update(deltaTime);

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
     * Render all game elements
     * @param canvas Canvas to draw on
     * @param paint Paint object for drawing
     */
    public void render(Canvas canvas, Paint paint) {
        // Draw map first (underneath everything)
        renderMap(canvas, paint);

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

        // Draw towers
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < renderTowersCopy.size(); i++) {
            drawTower(canvas, paint, renderTowersCopy.get(i));
        }

        // Draw enemies
        for (int i = 0; i < renderEnemiesCopy.size(); i++) {
            drawEnemy(canvas, paint, renderEnemiesCopy.get(i));
        }

        // Draw projectiles
        for (int i = 0; i < renderProjectilesCopy.size(); i++) {
            drawProjectile(canvas, paint, renderProjectilesCopy.get(i));
        }
    }

    /**
     * Render the game map with different colors for different tile types
     * Optimized to batch paint operations by color
     */
    private void renderMap(Canvas canvas, Paint paint) {
        if (gameMap == null) {
            return; // Guard against null gameMap
        }

        paint.setStyle(Paint.Style.FILL);

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
            paint.setColor(pathColor);
            for (long coords : pathTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, paint);
            }
        }

        if (!buildableTiles.isEmpty()) {
            paint.setColor(wallColor);
            for (long coords : buildableTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, paint);
            }
        }

        if (!spawnTiles.isEmpty()) {
            paint.setColor(spawnColor);
            for (long coords : spawnTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, paint);
            }
        }

        if (!datacenterTiles.isEmpty()) {
            paint.setColor(datacenterColor);
            for (long coords : datacenterTiles) {
                int x = (int) (coords >> 32);
                int y = (int) coords;
                float left = x * gridSize + offsetX;
                float top = y * gridSize + offsetY;
                canvas.drawRect(left, top, left + gridSize, top + gridSize, paint);
            }
        }
    }
    
    /**
     * Get tower icon resource ID based on type
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
     * Load and cache tower icon
     * Converts vector drawables to bitmaps for rendering
     */
    private Bitmap getTowerIcon(String towerType) {
        if (!towerIcons.containsKey(towerType) && context != null) {
            try {
                int resourceId = getTowerIconResource(towerType);

                // Load the drawable (works for both vector and bitmap drawables)
                Drawable drawable = ContextCompat.getDrawable(context, resourceId);

                if (drawable != null) {
                    // Create a bitmap to draw the vector drawable into
                    int size = 48; // Icon size in pixels
                    Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
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
     * Draw a tower with its icon (optimized to reduce object allocation)
     */
    private void drawTower(Canvas canvas, Paint paint, Tower tower) {
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

            // Apply corruption tint if corrupted (use cached filter)
            if (tower.isCorrupted()) {
                paint.setColorFilter(CORRUPTION_FILTER);
            }

            canvas.drawBitmap(icon, null, tempRect, paint);

            // Clear color filter
            paint.setColorFilter(null);
        } else {
            // Fallback to circle if icon not available
            paint.setColor(tower.isCorrupted() ? Color.RED : Color.CYAN);
            canvas.drawCircle(pos.x, pos.y, 24, paint);
        }

    }
    
    /**
     * Draw an enemy (placeholder - will be enhanced later)
     */
    private void drawEnemy(Canvas canvas, Paint paint, Enemy enemy) {
        if (enemy == null) return;
        PointF pos = enemy.getPosition();
        if (pos == null) return;
        
        // Draw enemy body
        paint.setColor(Color.RED);
        canvas.drawCircle(pos.x, pos.y, 15, paint);
        
        // Draw health bar
        paint.setColor(Color.GREEN);
        float healthBarWidth = 30 * enemy.getHealthPercentage();
        canvas.drawRect(
            pos.x - 15, 
            pos.y - 25, 
            pos.x - 15 + healthBarWidth, 
            pos.y - 20, 
            paint
        );
    }
    
    /**
     * Draw a projectile (placeholder - will be enhanced later)
     */
    private void drawProjectile(Canvas canvas, Paint paint, Projectile projectile) {
        if (projectile == null) return;
        PointF pos = projectile.getPosition();
        if (pos == null) return;
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(pos.x, pos.y, 5, paint);
    }
    
    /**
     * Update FPS counter
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
     * Handle tap on game view
     */
    public void handleTap(PointF worldPos) {
        // Will be implemented for tower placement
        // For now, just log the position
        android.util.Log.d("GameEngine", "Tapped at: " + worldPos.x + ", " + worldPos.y);
    }
    
    /**
     * Check if a tower can be placed at the given position
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
                if (distance < 64f) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Add a tower to the game
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
     * Add an enemy to the game
     */
    public void addEnemy(Enemy enemy) {
        synchronized (enemies) {
            enemies.add(enemy);
        }
    }

    /**
     * Get tower at a specific position (grid coordinates)
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
     * Upgrade a tower
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
            if (tower.upgrade()) {
                resources -= upgradeCost;
                notifyStatsChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * Add resources
     */
    public void addResources(int amount) {
        resources += amount;
        notifyStatsChanged();
    }
    
    /**
     * Add score
     */
    public void addScore(long amount) {
        score += amount;
        notifyStatsChanged();
    }

    /**
     * Notify listener that stats have changed
     */
    private void notifyStatsChanged() {
        if (gameListener != null) {
            gameListener.onStatsChanged(currentWave, resources, dataCenterHealth, score);
        }
    }

    /**
     * Handle game over
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
     * Start next wave
     */
    public void startNextWave() {
        waveManager.startNextWave(this);
        currentWave = waveManager.getCurrentWave();
        notifyStatsChanged();
    }

    /**
     * Capture current game state for saving
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
                    tower.getLevel(),
                    tower.isCorrupted()
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
     * Restore game state from saved data
     */
    public void restoreGameState(edu.commonwealthu.lastserverstanding.data.models.GameState state) {
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
     * Helper to create tower from saved data
     */
    private Tower createTowerFromData(edu.commonwealthu.lastserverstanding.data.models.GameState.TowerData data) {
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
            tower.upgrade();
        }
        tower.setCorrupted(data.isCorrupted);
        return tower;
    }

    /**
     * Helper to create enemy from saved data
     */
    private Enemy createEnemyFromData(edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData data) {
        // Reconstruct path
        List<PointF> path = new ArrayList<>();
        for (edu.commonwealthu.lastserverstanding.data.models.GameState.EnemyData.Point p : data.path) {
            path.add(new PointF(p.x, p.y));
        }

        // For now, only support DataCrawler (expand later)
        if ("Data Crawler".equals(data.type)) {
            edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler enemy = new edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler(path);
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
     * Try to upgrade tower at grid position
     * @return true if upgrade successful
     */
    public boolean tryUpgradeTowerAt(PointF gridPos) {
        if (gameMap == null) return false;

        synchronized (towers) {
            for (Tower tower : towers) {
                PointF towerGrid = gameMap.worldToGrid(tower.getPosition());
                if (Math.abs(towerGrid.x - gridPos.x) < 0.5f && Math.abs(towerGrid.y - gridPos.y) < 0.5f) {
                    int upgradeCost = tower.getUpgradeCost();
                    if (resources >= upgradeCost) {
                        boolean upgraded = tower.upgrade();
                        if (upgraded) {
                            resources -= upgradeCost;
                            notifyStatsChanged();
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Trigger emergency alert with haptic and audio feedback
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
                    VibrationEffect effect = VibrationEffect.createWaveform(timings, amplitudes, -1);
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
     * Play emergency alert sound using ToneGenerator
     */
    private void playAlertSound() {
        new Thread(() -> {
            android.media.ToneGenerator toneGen = null;
            try {
                toneGen = new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100);
                // Play a series of urgent tones (high pitched beeps)
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                Thread.sleep(300);
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400);
                Thread.sleep(500);
                toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                Thread.sleep(300);
            } catch (Exception e) {
                // Silently fail if sound cannot be played
                android.util.Log.e("GameEngine", "Failed to play alert sound: " + e.getMessage(), e);
            } finally {
                // Always release ToneGenerator to prevent resource leak
                if (toneGen != null) {
                    try {
                        toneGen.release();
                    } catch (Exception e) {
                        android.util.Log.e("GameEngine", "Error releasing ToneGenerator: " + e.getMessage(), e);
                    }
                }
            }
        }).start();
    }

    /**
     * Set vibration enabled state from settings
     */
    public void setVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
    }

    /**
     * Set sound enabled state from settings
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
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
     * Interface for game event callbacks
     */
    public interface GameListener {
        void onGameOver(int finalWave);
        void onStatsChanged(int wave, int resources, int health, long score);
    }
}
