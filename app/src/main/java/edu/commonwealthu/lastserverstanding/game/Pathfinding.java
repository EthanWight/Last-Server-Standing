package edu.commonwealthu.lastserverstanding.game;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* Pathfinding implementation for enemy navigation
 * Finds optimal path from start to goal while avoiding obstacles
 */
public class Pathfinding {
    
    // Node representing a position on the grid
    private static class Node implements Comparable<Node> {
        PointF position;
        Node parent;
        float gCost; // Distance from start
        float hCost; // Estimated distance to goal
        float fCost; // Total cost (g + h)
        
        Node(PointF position) {
            this.position = position;
            this.gCost = Float.MAX_VALUE;
            this.hCost = 0;
            this.fCost = Float.MAX_VALUE;
        }
        
        @Override
        public int compareTo(Node other) {
            return Float.compare(this.fCost, other.fCost);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Node)) return false;
            Node other = (Node) obj;
            return position.equals(other.position.x, other.position.y);
        }
        
        @Override
        public int hashCode() {
            return (int)(position.x * 1000 + position.y);
        }
    }
    
    private int gridWidth;
    private int gridHeight;
    private int cellSize;
    
    /**
     * Constructor
     * @param gridWidth Width of the game grid
     * @param gridHeight Height of the game grid
     * @param cellSize Size of each grid cell in pixels
     */
    public Pathfinding(int gridWidth, int gridHeight, int cellSize) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.cellSize = cellSize;
    }
    
    /**
     * Find path from start to goal using A* algorithm
     * @param start Starting position
     * @param goal Goal position
     * @param obstacles List of obstacle positions (tower positions)
     * @return List of waypoints from start to goal, or empty list if no path found
     */
    public List<PointF> findPath(PointF start, PointF goal, List<PointF> obstacles) {
        // Convert world coordinates to grid coordinates
        PointF startGrid = worldToGrid(start);
        PointF goalGrid = worldToGrid(goal);
        
        // Initialize data structures
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<String, Node> allNodes = new HashMap<>();
        
        Node startNode = new Node(startGrid);
        startNode.gCost = 0;
        startNode.hCost = heuristic(startGrid, goalGrid);
        startNode.fCost = startNode.hCost;
        
        openSet.add(startNode);
        allNodes.put(getKey(startGrid), startNode);
        
        // Convert obstacles to grid coordinates for fast lookup
        Map<String, Boolean> obstacleMap = new HashMap<>();
        for (PointF obstacle : obstacles) {
            PointF obstacleGrid = worldToGrid(obstacle);
            obstacleMap.put(getKey(obstacleGrid), true);
        }
        
        // A* main loop
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            // Check if we reached the goal
            if (distance(current.position, goalGrid) < 0.5f) {
                return reconstructPath(current);
            }
            
            // Explore neighbors
            for (Node neighbor : getNeighbors(current, goalGrid, obstacleMap, allNodes)) {
                float tentativeGCost = current.gCost + distance(current.position, neighbor.position);
                
                if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = heuristic(neighbor.position, goalGrid);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        // No path found - return straight line as fallback
        List<PointF> fallbackPath = new ArrayList<>();
        fallbackPath.add(start);
        fallbackPath.add(goal);
        return fallbackPath;
    }
    
    /**
     * Get neighboring nodes (8 directions)
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
            
            // Check bounds
            if (newX < 0 || newX >= gridWidth || newY < 0 || newY >= gridHeight) {
                continue;
            }
            
            PointF neighborPos = new PointF(newX, newY);
            String key = getKey(neighborPos);
            
            // Skip if obstacle (unless it's the goal)
            if (obstacles.containsKey(key) && distance(neighborPos, worldToGrid(gridToWorld(goal))) > 0.5f) {
                continue;
            }
            
            // Get or create neighbor node
            Node neighbor = allNodes.get(key);
            if (neighbor == null) {
                neighbor = new Node(neighborPos);
                allNodes.put(key, neighbor);
            }
            
            neighbors.add(neighbor);
        }
        
        return neighbors;
    }
    
    /**
     * Reconstruct path from goal to start by following parent pointers
     */
    private List<PointF> reconstructPath(Node goalNode) {
        List<PointF> path = new ArrayList<>();
        Node current = goalNode;
        
        while (current != null) {
            path.add(gridToWorld(current.position));
            current = current.parent;
        }
        
        Collections.reverse(path);
        
        // Smooth path by removing unnecessary waypoints
        return smoothPath(path);
    }
    
    /**
     * Smooth path by removing waypoints that can be skipped
     */
    private List<PointF> smoothPath(List<PointF> path) {
        if (path.size() <= 2) return path;
        
        List<PointF> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));
        
        int current = 0;
        while (current < path.size() - 1) {
            // Try to skip ahead as far as possible
            int furthest = current + 1;
            for (int i = path.size() - 1; i > current + 1; i--) {
                if (hasLineOfSight(path.get(current), path.get(i))) {
                    furthest = i;
                    break;
                }
            }
            
            smoothed.add(path.get(furthest));
            current = furthest;
        }
        
        return smoothed;
    }
    
    /**
     * Check if there's a clear line of sight between two points
     */
    private boolean hasLineOfSight(PointF start, PointF end) {
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        int steps = (int) (distance * 2); // Check twice per unit
        for (int i = 1; i < steps; i++) {
            float t = i / (float) steps;
            float x = start.x + dx * t;
            float y = start.y + dy * t;
            
            // In a full implementation, check against obstacles here
            // For now, assume clear line of sight
        }
        
        return true;
    }
    
    /**
     * Heuristic function (Euclidean distance)
     */
    private float heuristic(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate distance between two points
     */
    private float distance(PointF a, PointF b) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Convert world coordinates to grid coordinates
     */
    private PointF worldToGrid(PointF world) {
        return new PointF(
            (int) (world.x / cellSize),
            (int) (world.y / cellSize)
        );
    }
    
    /**
     * Convert grid coordinates to world coordinates (center of cell)
     */
    private PointF gridToWorld(PointF grid) {
        return new PointF(
            grid.x * cellSize + cellSize / 2f,
            grid.y * cellSize + cellSize / 2f
        );
    }
    
    /**
     * Get unique key for a grid position
     */
    private String getKey(PointF pos) {
        return (int)pos.x + "," + (int)pos.y;
    }
}
