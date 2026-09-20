package com.example.acyclic;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Immutable, data-only description of one ACYCLIC board. */
public final class LevelDefinition {
    private final int number;
    private final int rows;
    private final int cols;
    private final int startRow;
    private final int startCol;
    private final Set<Integer> unavailableNodeIds;

    public LevelDefinition(int number, int rows, int cols, int startRow, int startCol,
            Set<Integer> unavailableNodeIds) {
        if (number < 1 || rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Level number and board dimensions must be positive");
        }
        this.number = number;
        this.rows = rows;
        this.cols = cols;
        this.startRow = startRow;
        this.startCol = startCol;
        this.unavailableNodeIds = Collections.unmodifiableSet(new HashSet<>(unavailableNodeIds));
        createBoard(); // Validate the configuration once at construction.
    }

    public int getNumber() { return number; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public Set<Integer> getUnavailableNodeIds() { return unavailableNodeIds; }

    /** The board-derived number of nodes a player must uniquely visit to complete this level. */
    public int getWinTarget() { return rows * cols - unavailableNodeIds.size(); }

    public int getTotalPositionCount() { return rows * cols; }

    public int getBlockedNodeCount() { return unavailableNodeIds.size(); }

    public Board createBoard() {
        return new Board(rows, cols, unavailableNodeIds, startRow, startCol);
    }
}
