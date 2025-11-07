package edu.commonwealthu.lastserverstanding.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.Projectile;
import edu.commonwealthu.lastserverstanding.model.Tower;

/**
 * Core game engine managing all game logic and state
 */
public class GameEngine {
    
    // Game state
    private List<Tower> towers;
    private List<Enemy> enemies;
    private List<Projectile> projectiles;
    
    private int currentWave;
    private int resources;
    private int dataCenterHealth;
    private long score;
    private boolean isPaused;
    
    // FPS tracking
    private int fps;
    private long lastFpsTime;
    private int frameCount;
    
    // Game constants
    private static final int STARTING_RESOURCES = 500;
    private static final int STARTING_HEALTH = 100;
    
    /**
     * Constructor
     */
    public GameEngine() {
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
        projectiles = new ArrayList<>();
        
        currentWave = 0;
        resources = STARTING_RESOURCES;
        dataCenterHealth = STARTING_HEALTH;
        score = 0;
        isPaused = false;
        
        fps = 0;
        lastFpsTime = System.currentTimeMillis();
        frameCount = 0;
    }
    
    /**
     * Update game state
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (isPaused) return;
        
        // Update FPS counter
        updateFPS();
        
        // Update all towers
        for (Tower tower : towers) {
            tower.update(deltaTime);
            tower.acquireTarget(enemies);
            
            // Fire if possible
            Projectile projectile = tower.fire();
            if (projectile != null) {
                projectiles.add(projectile);
            }
        }
        
        // Update all enemies
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.update(deltaTime);
            
            // Check if enemy reached end
            if (enemy.hasReachedEnd()) {
                dataCenterHealth -= enemy.getDamage();
                enemyIterator.remove();
            }
            
            // Remove dead enemies
            if (!enemy.isAlive()) {
                resources += enemy.getReward();
                score += enemy.getReward() * 10;
                enemyIterator.remove();
            }
        }
        
        // Update all projectiles
        Iterator<Projectile> projectileIterator = projectiles.iterator();
        while (projectileIterator.hasNext()) {
            Projectile projectile = projectileIterator.next();
            projectile.update(deltaTime);
            
            // Remove projectiles that have hit
            if (projectile.hasHit()) {
                projectileIterator.remove();
            }
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
        // Draw towers
        paint.setStyle(Paint.Style.FILL);
        for (Tower tower : towers) {
            drawTower(canvas, paint, tower);
        }
        
        // Draw enemies
        for (Enemy enemy : enemies) {
            drawEnemy(canvas, paint, enemy);
        }
        
        // Draw projectiles
        for (Projectile projectile : projectiles) {
            drawProjectile(canvas, paint, projectile);
        }
    }
    
    /**
     * Draw a tower (placeholder - will be enhanced later)
     */
    private void drawTower(Canvas canvas, Paint paint, Tower tower) {
        PointF pos = tower.getPosition();
        
        // Draw tower body
        paint.setColor(tower.isCorrupted() ? Color.RED : Color.CYAN);
        canvas.drawCircle(pos.x, pos.y, 20, paint);
        
        // Draw range indicator if selected
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(50, 0, 255, 255));
        paint.setStrokeWidth(2);
        canvas.drawCircle(pos.x, pos.y, tower.getRange(), paint);
        paint.setStyle(Paint.Style.FILL);
    }
    
    /**
     * Draw an enemy (placeholder - will be enhanced later)
     */
    private void drawEnemy(Canvas canvas, Paint paint, Enemy enemy) {
        PointF pos = enemy.getPosition();
        
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
        PointF pos = projectile.getPosition();
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
        System.out.println("Tapped at: " + worldPos.x + ", " + worldPos.y);
    }
    
    /**
     * Add a tower to the game
     */
    public boolean addTower(Tower tower) {
        if (resources >= tower.getCost()) {
            towers.add(tower);
            resources -= tower.getCost();
            return true;
        }
        return false;
    }
    
    /**
     * Add an enemy to the game
     */
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }
    
    /**
     * Handle game over
     */
    private void gameOver() {
        isPaused = true;
        // Game over logic will be implemented later
        System.out.println("Game Over! Final Score: " + score);
    }
    
    /**
     * Start next wave
     */
    public void startNextWave() {
        currentWave++;
        // Wave spawning logic will be implemented later
    }
    
    // Getters and Setters
    public int getCurrentWave() { return currentWave; }
    public int getResources() { return resources; }
    public int getDataCenterHealth() { return dataCenterHealth; }
    public long getScore() { return score; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    public int getFPS() { return fps; }
    
    public List<Tower> getTowers() { return towers; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
}
