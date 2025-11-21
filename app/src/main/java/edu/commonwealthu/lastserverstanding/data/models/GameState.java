package edu.commonwealthu.lastserverstanding.data.models;

import android.graphics.PointF;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable representation of complete game state.
 *
 * @author Ethan Wight
 */
public class GameState {

    public int currentWave;
    public int resources;
    public int dataCenterHealth;
    public long score;

    // Serialized tower and enemy data
    public final List<TowerData> towers;
    public final List<EnemyData> enemies;

    public GameState() {
        towers = new ArrayList<>();
        enemies = new ArrayList<>();
    }

    /**
     * Serialize to JSON string.
     *
     * @return The JSON representation of this game state.
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Deserialize from JSON string.
     *
     * @param json The JSON string to deserialize.
     * @return The deserialized game state.
     */
    public static GameState fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, GameState.class);
    }

    /**
     * Nested class for tower data.
     *
     * @author Ethan Wight
     */
    public static class TowerData {
        public final String type;
        public final float x;
        public final float y;
        public final int level;

        public TowerData(String type, PointF position, int level) {
            this.type = type;
            this.x = position.x;
            this.y = position.y;
            this.level = level;
        }
    }

    /**
     * Nested class for enemy data.
     *
     * @author Ethan Wight
     */
    public static class EnemyData {
        public final String type;
        public final float x;
        public final float y;
        public final float health;
        public final int currentPathIndex;
        public final List<Point> path;

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

        /**
                 * Point class for path coordinates.
                 *
                 * @author Ethan Wight
                 */
                public record Point(float x, float y) {
            /**
             * Constructor for point.
             *
             * @param x The x coordinate.
             * @param y The y coordinate.
             */
            public Point {
            }
                }
    }
}
