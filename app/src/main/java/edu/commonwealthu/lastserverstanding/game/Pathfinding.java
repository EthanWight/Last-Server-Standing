package edu.commonwealthu.lastserverstanding.game;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* pathfinding implementation for optimal enemy navigation around obstacles.
 * Uses grid-based search with Euclidean heuristic for 8-directional movement.
 * Returns smoothed paths with fallback to direct line if completely blocked.
 *
 * @author Ethan Wight
 */
public record Pathfinding(
        int gridWidth,
        int gridHeight,
        int cellSize) {

    /**
     * A node in the A* pathfinding grid.
     * Maintains g-cost, h-cost, and f-cost for algorithm prioritization.
     */
    private static class Node implements Comparable<Node> {
        final PointF position;
        Node parent;
        float gCost;
        float hCost;
        float fCost;

        /**
         * Creates a new node at the specified grid position.
         *
         * @param position the grid position of this node
         */
        Node(PointF position) {
            this.position = position;
            this.gCost = Float.MAX_VALUE;
            this.hCost = 0;
            this.fCost = Float.MAX_VALUE;
        }

        /**
         * Compares nodes by f-cost for priority queue ordering.
         *
         * @param other the node to compare to
         * @return negative if lower f-cost, positive if higher, zero if equal
         */
        @Override
        public int compareTo(Node other) {
            return Float.compare(this.fCost, other.fCost);
        }

        /**
         * Checks equality based on grid position with epsilon tolerance.
         *
         * @param obj the object to compare with
         * @return true if nodes occupy the same grid cell
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Node other)) return false;
            return Math.abs(position.x - other.position.x) < 0.01f &&
                    Math.abs(position.y - other.position.y) < 0.01f;
        }

        /**
         * Generates hash code from grid position.
         *
         * @return hash code value for this node
         */
        @Override
        public int hashCode() {
            return (int) (position.x * 1000 + position.y);
        }
    }

    /**
     * Finds optimal path from start to goal using A* algorithm.
     *
     * @param start starting position in world coordinates
     * @param goal goal position in world coordinates
     * @param obstacles list of obstacle positions to avoid
     * @return smoothed list of waypoints, or direct line if blocked
     */
    public List<PointF> findPath(PointF start, PointF goal, List<PointF> obstacles) {
        PointF startGrid = worldToGrid(start);
        PointF goalGrid = worldToGrid(goal);

        // Initialize open set with priority queue and tracking structures
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> openSetKeys = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startGrid);
        startNode.gCost = 0;
        startNode.hCost = heuristic(startGrid, goalGrid);
        startNode.fCost = startNode.hCost;

        openSet.add(startNode);
        openSetKeys.add(getKey(startGrid));
        allNodes.put(getKey(startGrid), startNode);

        // Build obstacle map for quick lookup
        Map<String, Boolean> obstacleMap = new HashMap<>();
        for (PointF obstacle : obstacles) {
            PointF obstacleGrid = worldToGrid(obstacle);
            obstacleMap.put(getKey(obstacleGrid), true);
        }

        // Main A* loop
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current == null) continue;

            String currentKey = getKey(current.position);
            openSetKeys.remove(currentKey);

            // Check if goal reached
            if (distance(current.position, goalGrid) < 0.5f) {
                return reconstructPath(current);
            }

            // Evaluate neighbors
            for (Node neighbor : getNeighbors(current, goalGrid, obstacleMap, allNodes)) {
                float tentativeGCost = current.gCost + distance(current.position, neighbor.position);

                if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = heuristic(neighbor.position, goalGrid);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;

                    String neighborKey = getKey(neighbor.position);
                    if (!openSetKeys.contains(neighborKey)) {
                        openSet.add(neighbor);
                        openSetKeys.add(neighborKey);
                    }
                }
            }
        }

        // Fallback: return direct line if no path found
        List<PointF> fallbackPath = new ArrayList<>();
        fallbackPath.add(start);
        fallbackPath.add(goal);
        return fallbackPath;
    }

    /**
     * Get neighboring nodes (8 directions).
     *
     * @param current The current node.
     * @param goal The goal position.
     * @param obstacles Map of obstacle positions.
     * @param allNodes Map of all nodes.
     * @return List of neighboring nodes.
     */
    private List<Node> getNeighbors(Node current, PointF goal,
                                    Map<String, Boolean> obstacles,
                                    Map<String, Node> allNodes) {
        List<Node> neighbors = new ArrayList<>();

        // 8 directions: N, NE, E, SE, S, SW, W, NW
        int[][] directions = {
                {0, -1}, {1, -1}, {1, 0}, {1, 1},
                {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
        };

        for (int[] dir : directions) {
            float newX = current.position.x + dir[0];
            float newY = current.position.y + dir[1];

            if (newX < 0 || newX >= gridWidth || newY < 0 || newY >= gridHeight) {
                continue;
            }

            PointF neighborPos = new PointF(newX, newY);
            String key = getKey(neighborPos);

            // Skip if obstacle (unless it's the goal)
            if (obstacles.containsKey(key) && distance(neighborPos, goal) > 0.5f) {
                continue;
            }

            Node neighbor = allNodes.computeIfAbsent(key, k -> new Node(neighborPos));

            neighbors.add(neighbor);
        }

        return neighbors;
    }

    /**
     * Reconstruct path from goal to start by following parent pointers.
     *
     * @param goalNode The goal node.
     * @return The reconstructed path.
     */
    private List<PointF> reconstructPath(Node goalNode) {
        List<PointF> path = new ArrayList<>();
        Node current = goalNode;

        while (current != null) {
            path.add(gridToWorld(current.position));
            current = current.parent;
        }

        Collections.reverse(path);
        return smoothPath(path);
    }

    /**
     * Smooth path by removing waypoints that can be skipped.
     *
     * @param path The path to smooth.
     * @return The smoothed path.
     */
    private List<PointF> smoothPath(List<PointF> path) {
        if (path.size() <= 2) return path;

        List<PointF> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        int current = 0;
        while (current < path.size() - 1) {
            // Skip ahead to furthest point - assume clear line of sight (no obstacle checking)
            int furthest = Math.max(path.size() - 1, current + 1);

            smoothed.add(path.get(furthest));
            current = furthest;
        }

        return smoothed;
    }

    /**
     * Heuristic function (Euclidean distance).
     *
     * @param a The first position.
     * @param b The second position.
     * @return The Euclidean distance between the positions.
     */
    private float heuristic(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate distance between two points.
     *
     * @param a The first position.
     * @param b The second position.
     * @return The distance between the positions.
     */
    private float distance(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Convert world coordinates to grid coordinates.
     *
     * @param world The world coordinates.
     * @return The grid coordinates.
     */
    private PointF worldToGrid(PointF world) {
        return new PointF(
                (int) (world.x / cellSize),
                (int) (world.y / cellSize)
        );
    }

    /**
     * Convert grid coordinates to world coordinates (center of cell).
     *
     * @param grid The grid coordinates.
     * @return The world coordinates.
     */
    private PointF gridToWorld(PointF grid) {
        return new PointF(
                grid.x * cellSize + cellSize / 2f,
                grid.y * cellSize + cellSize / 2f
        );
    }

    /**
     * Get unique key for a grid position.
     *
     * @param pos The grid position.
     * @return The unique key for the position.
     */
    private String getKey(PointF pos) {
        return (int) pos.x + "," + (int) pos.y;
    }
}
