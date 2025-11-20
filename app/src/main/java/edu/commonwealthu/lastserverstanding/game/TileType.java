package edu.commonwealthu.lastserverstanding.game;

/**
 * Types of tiles in the game map.
 *
 * @author Ethan Wight
 */
public enum TileType {
    /**
     * Empty space - no building, no walking.
     */
    EMPTY,

    /**
     * Path where enemies walk.
     */
    PATH,

    /**
     * Walls where towers can be built.
     */
    BUILDABLE,

    /**
     * The datacenter that enemies are trying to reach.
     */
    DATACENTER,

    /**
     * Enemy spawn point.
     */
    SPAWN
}
