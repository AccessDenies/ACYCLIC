package com.example.acyclic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression coverage for completion accounting and solvability of every shipped board. */
public class WinConditionAuditTest {
    @Test
    public void everyLevelHasABoardDerivedTargetAndCanReachItWithoutCycles() {
        for (int level = 1; level <= LevelManager.LEVEL_COUNT; level++) {
            LevelDefinition definition = LevelManager.getLevel(level);
            Board board = definition.createBoard();
            GameState state = new GameState(board);

            assertEquals(board.getRows() * board.getCols(), board.getTotalPositionCount());
            assertEquals(board.getUnavailableNodeIds().size(), board.getBlockedNodeCount());
            assertEquals(board.getTotalPositionCount() - board.getBlockedNodeCount(), board.getPlayableNodeCount());
            assertEquals(board.getPlayableNodeCount(), definition.getWinTarget());
            assertTrue(board.isAvailable(board.getStartRow(), board.getStartCol()));
            assertEquals(1, state.getVisitedNodeCount());
            assertTrue("A level cannot be complete immediately after its start node", state.getVisitedNodeCount() < definition.getWinTarget());

            CompletionTrace trace = visitAllReachableNodes(state);
            assertEquals("Level " + level + " must have one connected playable graph", board.getPlayableNodeCount(), state.getVisitedNodeCount());
            assertTrue("Level " + level + " must reach its board-derived target", trace.won);
            assertEquals(board.getPlayableNodeCount(), trace.winVisitedCount);
            assertFalse("A valid depth-first traversal should not create a cycle", trace.cycleDetected);
        }
    }

    @Test
    public void levelOneRequiresAllFortyEightUniquePlayableNodes() {
        Board board = Board.forLevel(1);
        GameState state = new GameState(board);

        assertEquals(48, board.getTotalPositionCount());
        assertEquals(0, board.getBlockedNodeCount());
        assertEquals(48, board.getPlayableNodeCount());

        CompletionTrace trace = visitAllReachableNodes(state);
        assertTrue(trace.sawFortySixNodesWithoutWin);
        assertTrue(trace.won);
        assertEquals(48, trace.winVisitedCount);
    }

    private CompletionTrace visitAllReachableNodes(GameState state) {
        CompletionTrace trace = new CompletionTrace();
        explore(state, state.getBoard(), trace);
        return trace;
    }

    private void explore(GameState state, Board board, CompletionTrace trace) {
        int row = state.getCurrentRow();
        int col = state.getCurrentCol();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] direction : directions) {
            int nextRow = row + direction[0];
            int nextCol = col + direction[1];
            if (!board.isAvailable(nextRow, nextCol) || state.hasVisited(nextRow, nextCol)) {
                continue;
            }

            GameState.MoveResult result = state.moveTo(nextRow, nextCol);
            if (result == GameState.MoveResult.CYCLE_DETECTED) {
                trace.cycleDetected = true;
                return;
            }
            assertTrue(result == GameState.MoveResult.MOVED || result == GameState.MoveResult.WON);
            if (state.getVisitedNodeCount() == 46) {
                trace.sawFortySixNodesWithoutWin = result != GameState.MoveResult.WON;
            }
            if (result == GameState.MoveResult.WON) {
                trace.won = true;
                trace.winVisitedCount = state.getVisitedNodeCount();
                return;
            }

            explore(state, board, trace);
            if (trace.won || trace.cycleDetected) return;
            assertEquals(GameState.MoveResult.BACKTRACKED, state.moveTo(row, col));
        }
    }

    private static final class CompletionTrace {
        boolean cycleDetected;
        boolean sawFortySixNodesWithoutWin;
        boolean won;
        int winVisitedCount;
    }
}
