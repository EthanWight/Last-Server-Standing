package edu.commonwealthu.lastserverstanding.game;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the game map with tiles defining where enemies walk and towers can be placed
 */
public class GameMap {
    private final int width;
    private final int height;
    private final int gridSize;
    private final TileType[][] tiles;
    private final List<PointF> pathPoints;
    private PointF spawnPoint;
    private PointF dataCenterPoint;
    private float offsetX;
    private float offsetY;

    public GameMap(int width, int height, int gridSize) {
        this.width = width;
        this.height = height;
        this.gridSize = gridSize;
        this.tiles = new TileType[height][width];
        this.pathPoints = new ArrayList<>();
        this.offsetX = 0;
        this.offsetY = 0;

        // Initialize all tiles as empty
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = TileType.EMPTY;
            }
        }
    }

    /**
     * Get tile type at grid position
     */
    public TileType getTileAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return TileType.EMPTY;
        }
        return tiles[y][x];
    }

    /**
     * Set tile type at grid position
     */
    public void setTileAt(int x, int y, TileType type) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            tiles[y][x] = type;
        }
    }

    /**
     * Check if a tile can have a tower built on it
     */
    public boolean isBuildable(int x, int y) {
        return getTileAt(x, y) == TileType.BUILDABLE;
    }

    /**
     * Check if a tile can have a tower built on it (PointF version)
     */
    public boolean isBuildable(PointF gridPos) {
        return isBuildable((int) gridPos.x, (int) gridPos.y);
    }

    /**
     * Convert grid coordinates to world coordinates (center of tile)
     */
    public PointF gridToWorld(int x, int y) {
        return new PointF(
            x * gridSize + gridSize / 2f + offsetX,
            y * gridSize + gridSize / 2f + offsetY
        );
    }

    /**
     * Convert world coordinates to grid coordinates
     */
    public PointF worldToGrid(PointF worldPos) {
        return new PointF(
            (int)((worldPos.x - offsetX) / gridSize),
            (int)((worldPos.y - offsetY) / gridSize)
        );
    }

    /**
     * Set the offset to center the map on screen
     * Recalculates all path points with new offset
     */
    public void centerOnScreen(int screenWidth, int screenHeight) {
        int mapPixelWidth = width * gridSize;
        int mapPixelHeight = height * gridSize;
        float oldOffsetX = offsetX;
        float oldOffsetY = offsetY;
        offsetX = (screenWidth - mapPixelWidth) / 2f;
        offsetY = (screenHeight - mapPixelHeight) / 2f;

        // Recalculate path points with new offset
        if (!pathPoints.isEmpty()) {
            float deltaX = offsetX - oldOffsetX;
            float deltaY = offsetY - oldOffsetY;
            for (PointF point : pathPoints) {
                point.x += deltaX;
                point.y += deltaY;
            }
        }

        // Update spawn and datacenter positions
        if (spawnPoint != null) {
            spawnPoint.x += offsetX - oldOffsetX;
            spawnPoint.y += offsetY - oldOffsetY;
        }
        if (dataCenterPoint != null) {
            dataCenterPoint.x += offsetX - oldOffsetX;
            dataCenterPoint.y += offsetY - oldOffsetY;
        }
    }

    /**
     * Get the path that enemies should follow
     * Returns unmodifiable view to avoid defensive copy allocation
     */
    public List<PointF> getEnemyPath() {
        return java.util.Collections.unmodifiableList(pathPoints);
    }

    /**
     * Add a path point for enemies to follow (grid coordinates)
     */
    public void addPathPoint(int x, int y) {
        pathPoints.add(gridToWorld(x, y));
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }

    public void setSpawnPoint(int x, int y) { this.spawnPoint = gridToWorld(x, y); }
    public void setDataCenterPoint(int x, int y) { this.dataCenterPoint = gridToWorld(x, y); }

    /**
     * Create a butterfly-shaped map with a beautiful symmetric design
     * The path traces through both wings of the butterfly
     */
    @SuppressWarnings("unused") // gridWidth and gridHeight parameters kept for API compatibility
    public static GameMap createSimpleMap(int gridWidth, int gridHeight, int gridSize) {
        // Create a smaller map (18x12) for better centering
        int mapWidth = 18;
        int mapHeight = 12;
        GameMap map = new GameMap(mapWidth, mapHeight, gridSize);

        // Butterfly-shaped path - ONLY horizontal/vertical moves, ZERO backtracking
        // Each wing uses distinct columns to prevent any tile from being visited twice
        // Upper left (cols 2-3), Lower left (cols 4-5), Center (cols 6-11), Lower right (cols 12-13), Upper right (cols 14-15)
        int[][] pathRoute = {
            // Spawn
            {1, 6},

            // === UPPER LEFT WING (columns 2-3, rows 2-6) ===
            {2, 6}, {2, 5}, {2, 4}, {2, 3}, {2, 2},  // Go up column 2
            {3, 2}, {4, 2},  // Across to column 4
            {4, 3}, {4, 4}, {4, 5}, {4, 6},  // Down column 3

            // === LOWER LEFT WING (columns 4-5, rows 6-10) ===
            {4, 7}, {4, 8}, {4, 9}, {4, 10},  // Down column 4
            {5, 10}, {6, 10},  // Across to column 6
            {6, 9}, {6, 8}, {6, 7}, {6, 6},  // Up column 5

            // === CENTER BODY (columns 6-11, row 6) ===
            {7, 6}, {8, 6}, {9, 6}, {10, 6}, {11, 6},  // Across center

            // === LOWER RIGHT WING (columns 12-13, rows 6-10) ===
            {11, 7}, {11, 8}, {11, 9}, {11, 10},  // Down column 11
            {12, 10}, {13, 10},  // Across to column 13
            {13, 9}, {13, 8}, {13, 7},  // Up column 13

            // === UPPER RIGHT WING (columns 13-15, rows 2-6) ===
            {13, 6}, {13, 5}, {13, 4}, {13, 3}, {13, 2},  // Up column 14
            {14, 2}, {15, 2},  // Across to column 15
            {15, 3}, {15, 4}, {15, 5}, {15, 6},  // Down column 15

            // === EXIT ===
            {16, 6}
        };

        // Mark path tiles
        for (int[] coord : pathRoute) {
            int x = coord[0];
            int y = coord[1];
            if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
                map.setTileAt(x, y, TileType.PATH);
                map.addPathPoint(x, y);
            }
        }

        // Set spawn and datacenter
        map.setTileAt(1, 6, TileType.SPAWN);
        map.setSpawnPoint(1, 6);
        map.setTileAt(16, 6, TileType.DATACENTER);
        map.setDataCenterPoint(16, 6);

        // Create buildable walls around the path
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                TileType current = map.getTileAt(x, y);
                if (current == TileType.EMPTY) {
                    // Check if adjacent to path
                    boolean adjacentToPath = false;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            TileType neighbor = map.getTileAt(x + dx, y + dy);
                            if (neighbor == TileType.PATH || neighbor == TileType.SPAWN || neighbor == TileType.DATACENTER) {
                                adjacentToPath = true;
                                break;
                            }
                        }
                        if (adjacentToPath) break;
                    }
                    if (adjacentToPath) {
                        map.setTileAt(x, y, TileType.BUILDABLE);
                    }
                }
            }
        }

        return map;
    }
}
