package edu.commonwealthu.lastserverstanding.game;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.commonwealthu.lastserverstanding.model.Enemy;
import edu.commonwealthu.lastserverstanding.model.enemies.DataCrawler;
import edu.commonwealthu.lastserverstanding.model.enemies.TrojanHorse;
import edu.commonwealthu.lastserverstanding.model.enemies.VirusBot;

/**
 * Wave manager handles enemy spawning with progressive difficulty.
 * Generates waves of enemies with varying compositions and timing.
 *
 * @author Ethan Wight
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
    private final List<PointF> spawnPoints;
    private final List<PointF> goalPoints;
    private final Random random;
    
    // Difficulty scaling
    private float difficultyMultiplier;
    private static final float DIFFICULTY_INCREASE_PER_WAVE = 0.15f;
    
    /**
     * Constructor for WaveManager.
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

        // Default goal point (single data center location - right side, centered)
        goalPoints.add(new PointF(1200, 360));
    }
    
    /**
     * Update wave manager.
     *
     * @param deltaTime Time since last update in seconds.
     * @param gameEngine Reference to game engine for spawning enemies.
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
     * Start the next wave.
     *
     * @param gameEngine The game engine instance.
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
     * Calculate wave parameters based on wave number.
     */
    private void calculateWaveParameters() {
        // Base enemies: 5 + 3 per wave
        totalEnemiesThisWave = 5 + (currentWave * 3);
        
        // Spawn interval decreases slightly each wave (faster spawning)
        spawnInterval = Math.max(0.3f, 1.0f - (currentWave * 0.05f));
    }
    
    /**
     * Spawn an enemy.
     *
     * @param gameEngine The game engine instance.
     */
    private void spawnEnemy(GameEngine gameEngine) {
        List<PointF> path;

        // Use map path if available, otherwise fall back to default behavior
        GameMap map = gameEngine.getGameMap();
        if (map != null && !map.getEnemyPath().isEmpty()) {
            // Use the predefined path from the map
            path = map.getEnemyPath();
        } else {
            // Fallback to path generation using map's data center if available
            PointF spawnPoint = spawnPoints.get(random.nextInt(spawnPoints.size()));
            PointF goalPoint;
            if (map != null && map.getDataCenterPoint() != null) {
                goalPoint = map.getDataCenterPoint();
            } else {
                goalPoint = goalPoints.isEmpty() ? new PointF(1200, 360) : goalPoints.get(random.nextInt(goalPoints.size()));
            }
            path = generatePath(spawnPoint, goalPoint, gameEngine);
        }

        // Create enemy based on wave number and difficulty
        Enemy enemy = createEnemyForWave(path);

        // Add enemy to game
        gameEngine.addEnemy(enemy);
    }
    
    /**
     * Generate path from spawn to goal using pathfinding.
     *
     * @param start The starting position.
     * @param goal The goal position.
     * @param gameEngine The game engine instance.
     * @return The path from start to goal.
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
     * Create enemy appropriate for current wave.
     * Enemy variety increases with wave progression:
     * - Waves 1-4: Data Crawlers (red) only
     * - Waves 5-9: Data Crawlers and Virus Bots (blue)
     * - Wave 10+: All three types with increasing difficulty
     *
     * @param path The path for the enemy to follow.
     * @return The created enemy.
     */
    private Enemy createEnemyForWave(List<PointF> path) {
        // Waves 1-4: Only Data Crawlers (basic red enemies)
        if (currentWave < 5) {
            return new DataCrawler(path);
        }

        // Waves 5-9: Introduce Virus Bots (blue)
        if (currentWave < 10) {
            // 70% Data Crawlers, 30% Virus Bots
            int roll = random.nextInt(100);
            if (roll < 70) {
                return new DataCrawler(path);
            } else {
                return new VirusBot(path);
            }
        }

        // Wave 10+: All three enemy types
        // Distribution shifts toward stronger enemies as waves progress
        int roll = random.nextInt(100);

        // Calculate distribution based on wave (higher waves = more strong enemies)
        int crawlerChance = Math.max(20, 60 - (currentWave - 10) * 2); // Decreases from 60% to 20%
        int virusBotChance = 30; // Remains steady at 30%
        // Trojan Horse gets the remaining percentage (increases from 10% to 50%)

        if (roll < crawlerChance) {
            return new DataCrawler(path);
        } else if (roll < crawlerChance + virusBotChance) {
            return new VirusBot(path);
        } else {
            return new TrojanHorse(path);
        }
    }
    
    /**
     * End current wave.
     *
     * @param gameEngine The game engine instance.
     */
    private void endWave(GameEngine gameEngine) {
        isWaveActive = false;

        // Award bonus resources for completing wave
        int waveBonus = 100 + (currentWave * 25);
        gameEngine.addResources(waveBonus);

        // Award score bonus
        long scoreBonus = (long)(500 * currentWave * difficultyMultiplier);
        gameEngine.addScore(scoreBonus);

        // Notify wave completion (for autosave)
        gameEngine.notifyWaveComplete(currentWave);
    }

    /**
     * Get current wave number.
     *
     * @return The current wave number.
     */
    public int getCurrentWave() { return currentWave; }

    /**
     * Check if a wave is currently active.
     *
     * @return True if wave is active, false otherwise.
     */
    public boolean isWaveActive() { return isWaveActive; }

    /**
     * Restore wave state from saved game.
     *
     * @param wave The wave number to restore.
     */
    public void restoreWaveState(int wave) {
        this.currentWave = wave;
        this.isWaveActive = false; // Wave is paused when loading
        this.enemiesSpawnedThisWave = 0;
        this.timeSinceLastSpawn = 0;
        // Restore difficulty multiplier for the current wave
        this.difficultyMultiplier = 1.0f + (wave - 1) * DIFFICULTY_INCREASE_PER_WAVE;
    }
}
