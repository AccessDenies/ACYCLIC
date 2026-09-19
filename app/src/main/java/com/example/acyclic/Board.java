package com.example.acyclic;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Immutable definition of a playable grid and its unavailable cells. */
public final class Board {
    private final int rows;
    private final int cols;
    private final Set<Integer> unavailableNodeIds;

    public Board(int rows, int cols, Set<Integer> unavailableNodeIds) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }

        this.rows = rows;
        this.cols = cols;
        this.unavailableNodeIds = new HashSet<>(unavailableNodeIds);
    }

    public static Board forLevel(int level) {
        Set<Integer> unavailable = new HashSet<>();

        if (level == 2) {
            addUnavailable(unavailable, 6, 1, 1);
            addUnavailable(unavailable, 6, 2, 1);
            addUnavailable(unavailable, 6, 3, 1);
            addUnavailable(unavailable, 6, 4, 1);
            addUnavailable(unavailable, 6, 7, 4);
            addUnavailable(unavailable, 6, 7, 5);
        }

        return new Board(8, 6, unavailable);
    }

    private static void addUnavailable(Set<Integer> unavailable, int cols, int row, int col) {
        unavailable.add(row * cols + col);
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

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
        return rows * cols - unavailableNodeIds.size();
    }

    public Set<Integer> getUnavailableNodeIds() {
        return Collections.unmodifiableSet(unavailableNodeIds);
    }
}
