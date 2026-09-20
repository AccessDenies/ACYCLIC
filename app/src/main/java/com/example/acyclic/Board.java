package com.example.acyclic;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Immutable definition of a playable grid and its unavailable cells. */
public final class Board {
    private final int rows;
    private final int cols;
    private final Set<Integer> unavailableNodeIds;
    private final int startRow;
    private final int startCol;

    public Board(int rows, int cols, Set<Integer> unavailableNodeIds) {
        this(rows, cols, unavailableNodeIds, rows / 2, cols / 2);
    }

    public Board(int rows, int cols, Set<Integer> unavailableNodeIds, int startRow, int startCol) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }

        this.rows = rows;
        this.cols = cols;
        this.unavailableNodeIds = new HashSet<>(unavailableNodeIds);
        for (int nodeId : this.unavailableNodeIds) {
            if (nodeId < 0 || nodeId >= rows * cols) {
                throw new IllegalArgumentException("Unavailable node is outside the board");
            }
        }
        if (!isInside(startRow, startCol) || this.unavailableNodeIds.contains(startRow * cols + startCol)) {
            throw new IllegalArgumentException("The board start position must be available");
        }
        this.startRow = startRow;
        this.startCol = startCol;
    }

    public static Board forLevel(int level) {
        return LevelManager.getLevel(level).createBoard();
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getStartRow() { return startRow; }

    public int getStartCol() { return startCol; }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean isAvailable(int row, int col) {
        return isInside(row, col) && !unavailableNodeIds.contains(getNodeId(row, col));
    }

    public int getNodeId(int row, int col) {
        if (!isInside(row, col)) {
            throw new IllegalArgumentException("Position is outside the board");
        }
        return row * cols + col;
    }

    public int getAvailableNodeCount() {
        return getPlayableNodeCount();
    }

    /** Total grid positions before blocked cells are excluded. */
    public int getTotalPositionCount() { return rows * cols; }

    /** Number of explicitly unavailable positions. */
    public int getBlockedNodeCount() { return unavailableNodeIds.size(); }

    /** Unique playable nodes required by the win condition. */
    public int getPlayableNodeCount() { return getTotalPositionCount() - getBlockedNodeCount(); }

    public Set<Integer> getUnavailableNodeIds() {
        return Collections.unmodifiableSet(unavailableNodeIds);
    }
}
