package edu.commonwealthu.lastserverstanding.game;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler;

/**
 * Wave manager handles enemy spawning with progressive difficulty
 * Generates waves of enemies with varying compositions and timing
 */
public class WaveManager {
    
    // Wave configuration
    private int currentWave;
    private boolean isWaveActive;
    private float timeSinceLastSpawn;
    private int enemiesSpawnedThisWave;
    private int totalEnemiesThisWave;
    
    // Spawn settings
    private float spawnInterval; // Seconds between enemy spawns
    private List<PointF> spawnPoints;
    private List<PointF> goalPoints;
    private Random random;
    
    // Difficulty scaling
    private float difficultyMultiplier;
    private static final float DIFFICULTY_INCREASE_PER_WAVE = 0.15f;
    
    /**
     * Constructor
     */
    public WaveManager() {
        this.currentWave = 0;
        this.isWaveActive = false;
        this.timeSinceLastSpawn = 0;
        this.enemiesSpawnedThisWave = 0;
        this.spawnInterval = 1.0f;
        this.difficultyMultiplier = 1.0f;
        this.random = new Random();
        
        // Initialize spawn and goal points
        spawnPoints = new ArrayList<>();
        goalPoints = new ArrayList<>();
        
        // Default spawn points (left side of screen)
        spawnPoints.add(new PointF(50, 150));
        spawnPoints.add(new PointF(50, 300));
        spawnPoints.add(new PointF(50, 450));
        
        // Default goal points (right side of screen)
        goalPoints.add(new PointF(1200, 200));
        goalPoints.add(new PointF(1200, 400));
    }
    
    /**
     * Update wave manager
     * @param deltaTime Time since last update in seconds
     * @param gameEngine Reference to game engine for spawning enemies
     */
    public void update(float deltaTime, GameEngine gameEngine) {
        if (!isWaveActive) {
            return;
        }
        
        timeSinceLastSpawn += deltaTime;
        
        // Check if it's time to spawn next enemy
        if (timeSinceLastSpawn >= spawnInterval && enemiesSpawnedThisWave < totalEnemiesThisWave) {
            spawnEnemy(gameEngine);
            timeSinceLastSpawn = 0;
            enemiesSpawnedThisWave++;
        }
        
        // Check if wave is complete (all enemies spawned and no enemies left)
        if (enemiesSpawnedThisWave >= totalEnemiesThisWave && gameEngine.getEnemies().isEmpty()) {
            endWave(gameEngine);
        }
    }
    
    /**
     * Start the next wave
     */
    public void startNextWave(GameEngine gameEngine) {
        if (isWaveActive) {
            return; // Wave already in progress
        }
        
        currentWave++;
        isWaveActive = true;
        timeSinceLastSpawn = 0;
        enemiesSpawnedThisWave = 0;
        
        // Calculate wave parameters with difficulty scaling
        calculateWaveParameters();
        
        // Update difficulty multiplier
        difficultyMultiplier = 1.0f + (currentWave - 1) * DIFFICULTY_INCREASE_PER_WAVE;
        
        // Award resources for starting new wave
        gameEngine.addResources(50 + currentWave * 10);
    }
    
    /**
     * Calculate wave parameters based on wave number
     */
    private void calculateWaveParameters() {
        // Base enemies: 5 + 3 per wave
        totalEnemiesThisWave = 5 + (currentWave * 3);
        
        // Spawn interval decreases slightly each wave (faster spawning)
        spawnInterval = Math.max(0.3f, 1.0f - (currentWave * 0.05f));
    }
    
    /**
     * Spawn an enemy
     */
    private void spawnEnemy(GameEngine gameEngine) {
        // Choose random spawn point
        PointF spawnPoint = spawnPoints.get(random.nextInt(spawnPoints.size()));
        
        // Choose random goal point
        PointF goalPoint = goalPoints.get(random.nextInt(goalPoints.size()));
        
        // Generate path from spawn to goal
        List<PointF> path = generatePath(spawnPoint, goalPoint, gameEngine);
        
        // Create enemy based on wave number and difficulty
        Enemy enemy = createEnemyForWave(path);
        
        // Add enemy to game
        gameEngine.addEnemy(enemy);
    }
    
    /**
     * Generate path from spawn to goal using pathfinding
     */
    private List<PointF> generatePath(PointF start, PointF goal, GameEngine gameEngine) {
        // Get tower positions as obstacles
        List<PointF> obstacles = new ArrayList<>();
        for (var tower : gameEngine.getTowers()) {
            obstacles.add(tower.getPosition());
        }
        
        // Use pathfinding if available, otherwise create simple path
        Pathfinding pathfinding = gameEngine.getPathfinding();
        if (pathfinding != null) {
            return pathfinding.findPath(start, goal, obstacles);
        } else {
            // Fallback: simple path with one waypoint
            List<PointF> simplePath = new ArrayList<>();
            simplePath.add(start);
            simplePath.add(new PointF((start.x + goal.x) / 2, (start.y + goal.y) / 2));
            simplePath.add(goal);
            return simplePath;
        }
    }
    
    /**
     * Create enemy appropriate for current wave
     * Later waves introduce new enemy types
     */
    private Enemy createEnemyForWave(List<PointF> path) {
        // For now, only Data Crawlers are implemented
        // Future waves will introduce other enemy types
        
        DataCrawler enemy = new DataCrawler(path);
        
        // Scale enemy stats based on difficulty
        scaleEnemyStats(enemy);
        
        return enemy;
    }
    
    /**
     * Scale enemy stats based on difficulty multiplier
     */
    private void scaleEnemyStats(Enemy enemy) {
        // Increase health based on difficulty
        // This would require adding a method to Enemy class or using reflection
        // For now, this is a placeholder for the scaling logic
        
        // TODO: Implement stat scaling when Enemy class supports it
        // enemy.scaleHealth(difficultyMultiplier);
        // enemy.scaleSpeed(1.0f + (difficultyMultiplier - 1.0f) * 0.3f); // Speed increases slower
        // enemy.scaleReward((int)(enemy.getReward() * difficultyMultiplier));
    }
    
    /**
     * End current wave
     */
    private void endWave(GameEngine gameEngine) {
        isWaveActive = false;
        
        // Award bonus resources for completing wave
        int waveBonus = 100 + (currentWave * 25);
        gameEngine.addResources(waveBonus);
        
        // Award score bonus
        long scoreBonus = (long)(500 * currentWave * difficultyMultiplier);
        gameEngine.addScore(scoreBonus);
    }
    
    /**
     * Set custom spawn points
     */
    public void setSpawnPoints(List<PointF> points) {
        if (points != null && !points.isEmpty()) {
            this.spawnPoints = new ArrayList<>(points);
        }
    }
    
    /**
     * Set custom goal points
     */
    public void setGoalPoints(List<PointF> points) {
        if (points != null && !points.isEmpty()) {
            this.goalPoints = new ArrayList<>(points);
        }
    }
    
    /**
     * Get progress through current wave (0 to 1)
     */
    public float getWaveProgress() {
        if (totalEnemiesThisWave == 0) return 0;
        return enemiesSpawnedThisWave / (float) totalEnemiesThisWave;
    }
    
    /**
     * Get time until next spawn
     */
    public float getTimeUntilNextSpawn() {
        if (!isWaveActive) return 0;
        return Math.max(0, spawnInterval - timeSinceLastSpawn);
    }
    
    // Getters
    public int getCurrentWave() { return currentWave; }
    public boolean isWaveActive() { return isWaveActive; }
    public int getEnemiesSpawnedThisWave() { return enemiesSpawnedThisWave; }
    public int getTotalEnemiesThisWave() { return totalEnemiesThisWave; }
    public int getEnemiesRemainingToSpawn() { return totalEnemiesThisWave - enemiesSpawnedThisWave; }
    public float getDifficultyMultiplier() { return difficultyMultiplier; }
}
