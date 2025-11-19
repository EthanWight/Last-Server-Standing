package edu.commonwealthu.lastserverstanding.data.models;

import android.graphics.PointF;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable representation of complete game state
 */
public class GameState {

    public int currentWave;
    public int resources;
    public int dataCenterHealth;
    public long score;

    // Serialized tower and enemy data
    public List<TowerData> towers;
    public List<EnemyData> enemies;

    public GameState() {
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
    }

    /**
     * Serialize to JSON string
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Deserialize from JSON string
     */
    public static GameState fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, GameState.class);
    }

    /**
     * Nested class for tower data
     */
    public static class TowerData {
        public String type;
        public float x;
        public float y;
        public int level;

        public TowerData(String type, PointF position, int level) {
            this.type = type;
            this.x = position.x;
            this.y = position.y;
            this.level = level;
        }
    }

    /**
     * Nested class for enemy data
     */
    public static class EnemyData {
        public String type;
        public float x;
        public float y;
        public float health;
        public int currentPathIndex;
        public List<Point> path;

        public EnemyData(String type, PointF position, float health, int pathIndex, List<PointF> path) {
            this.type = type;
            this.x = position.x;
            this.y = position.y;
            this.health = health;
            this.currentPathIndex = pathIndex;
            this.path = new ArrayList<>();
            for (PointF p : path) {
                this.path.add(new Point(p.x, p.y));
            }
        }

        public static class Point {
            public float x, y;
            public Point(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }
    }
}
