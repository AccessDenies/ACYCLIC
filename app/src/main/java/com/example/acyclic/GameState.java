package com.example.acyclic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable game rules and graph state, deliberately independent from Android UI classes.
 */
public final class GameState {
    public enum MoveResult {
        INVALID,
        BACKTRACKED,
        MOVED,
        CYCLE_DETECTED,
        WON
    }

    public static final class Position {
        private final int row;
        private final int col;

        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }
    }

    private final Board board;
    private final List<Position> path = new ArrayList<>();
    private final Set<Integer> graphNodes = new HashSet<>();
    private final Set<Long> graphEdges = new HashSet<>();
    private int currentRow;
    private int currentCol;
    private int score;

    public GameState(Board board) {
        this.board = board;
        reset();
    }

    public void reset() {
        path.clear();
        graphNodes.clear();
        graphEdges.clear();
        score = 0;
        currentRow = board.getStartRow();
        currentCol = board.getStartCol();

        if (!board.isAvailable(currentRow, currentCol)) {
            throw new IllegalStateException("The board start position must be available");
        }

        path.add(new Position(currentRow, currentCol));
        graphNodes.add(board.getNodeId(currentRow, currentCol));
    }

    public MoveResult moveTo(int newRow, int newCol) {
        if (!board.isAvailable(newRow, newCol)
                || Math.abs(newRow - currentRow) + Math.abs(newCol - currentCol) != 1) {
            return MoveResult.INVALID;
        }

        int currentId = board.getNodeId(currentRow, currentCol);
        int nextId = board.getNodeId(newRow, newCol);

        if (path.size() >= 2) {
            Position previous = path.get(path.size() - 2);
            if (previous.row == newRow && previous.col == newCol) {
                graphEdges.remove(getEdgeKey(currentId, nextId));
                path.remove(path.size() - 1);
                currentRow = newRow;
                currentCol = newCol;
                if (score > 0) {
                    score--;
                }
                return MoveResult.BACKTRACKED;
            }
        }

        if (wouldCreateCycle(currentId, nextId)) {
            return MoveResult.CYCLE_DETECTED;
        }

        graphNodes.add(nextId);
        graphEdges.add(getEdgeKey(currentId, nextId));
        currentRow = newRow;
        currentCol = newCol;
        path.add(new Position(currentRow, currentCol));
        score++;

        // Completion is based on unique visited playable nodes, not path length. Backtracking
        // deliberately shortens the rendered path while retaining collection progress.
        return graphNodes.size() == board.getPlayableNodeCount()
                ? MoveResult.WON
                : MoveResult.MOVED;
    }

    public Board getBoard() {
        return board;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public int getCurrentCol() {
        return currentCol;
    }

    public int getScore() {
        return score;
    }

    public List<Position> getPath() {
        return Collections.unmodifiableList(path);
    }

    public boolean hasVisited(int row, int col) {
        return board.isInside(row, col) && graphNodes.contains(board.getNodeId(row, col));
    }

    public int getVisitedNodeCount() {
        return graphNodes.size();
    }

    public int getEdgeCount() {
        return graphEdges.size();
    }

    private long getEdgeKey(int nodeA, int nodeB) {
        int smaller = Math.min(nodeA, nodeB);
        int larger = Math.max(nodeA, nodeB);
        return (((long) smaller) << 32) | (larger & 0xffffffffL);
    }

    private boolean edgeExists(int nodeA, int nodeB) {
        return graphEdges.contains(getEdgeKey(nodeA, nodeB));
    }

    private boolean wouldCreateCycle(int currentId, int nextId) {
        return !edgeExists(currentId, nextId) && canReachNode(currentId, nextId);
    }

    private boolean canReachNode(int startId, int targetId) {
        Set<Integer> visited = new HashSet<>();
        List<Integer> stack = new ArrayList<>();
        stack.add(startId);

        while (!stack.isEmpty()) {
            int current = stack.remove(stack.size() - 1);
            if (current == targetId) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }

            for (long edge : graphEdges) {
                int nodeA = (int) (edge >> 32);
                int nodeB = (int) edge;
                if (nodeA == current && !visited.contains(nodeB)) {
                    stack.add(nodeB);
                }
                if (nodeB == current && !visited.contains(nodeA)) {
                    stack.add(nodeA);
                }
            }
        }

        return false;
    }
}
