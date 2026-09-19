package com.example.acyclic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class GameStateTest {
    @Test
    public void rejectsOutOfBoundsBlockedAndNonAdjacentMoves() {
        Set<Integer> unavailable = new HashSet<>();
        unavailable.add(1); // (0, 1)
        GameState state = new GameState(new Board(3, 3, unavailable));

        assertEquals(GameState.MoveResult.INVALID, state.moveTo(-1, 1));
        assertEquals(GameState.MoveResult.INVALID, state.moveTo(0, 1));
        assertEquals(GameState.MoveResult.INVALID, state.moveTo(0, 0));
        assertEquals(1, state.getVisitedNodeCount());
        assertEquals(0, state.getScore());
    }

    @Test
    public void levelTwoKeepsItsDefinedUnavailableNodesNonPlayable() {
        Board board = Board.forLevel(2);

        assertFalse(board.isAvailable(1, 1));
        assertFalse(board.isAvailable(2, 1));
        assertFalse(board.isAvailable(3, 1));
        assertFalse(board.isAvailable(4, 1));
        assertFalse(board.isAvailable(7, 4));
        assertFalse(board.isAvailable(7, 5));
        assertEquals(42, board.getAvailableNodeCount());
        assertTrue(board.isAvailable(1, 2));
    }

    @Test
    public void backtrackingRemovesTheLatestEdgeButKeepsCollectionProgress() {
        GameState state = new GameState(new Board(3, 3, Collections.<Integer>emptySet()));

        assertEquals(GameState.MoveResult.MOVED, state.moveTo(1, 2));
        assertEquals(GameState.MoveResult.BACKTRACKED, state.moveTo(1, 1));

        assertEquals(1, state.getCurrentRow());
        assertEquals(1, state.getCurrentCol());
        assertEquals(0, state.getScore());
        assertEquals(0, state.getEdgeCount());
        assertEquals(2, state.getVisitedNodeCount());
        assertTrue(state.hasVisited(1, 2));
        assertEquals(1, state.getPath().size());
    }

    @Test
    public void rejectsAnEdgeThatClosesAnUndirectedCycle() {
        GameState state = new GameState(new Board(3, 3, Collections.<Integer>emptySet()));

        assertEquals(GameState.MoveResult.MOVED, state.moveTo(0, 1));
        assertEquals(GameState.MoveResult.MOVED, state.moveTo(0, 0));
        assertEquals(GameState.MoveResult.MOVED, state.moveTo(1, 0));

        assertEquals(GameState.MoveResult.CYCLE_DETECTED, state.moveTo(1, 1));
        assertEquals(1, state.getCurrentRow());
        assertEquals(0, state.getCurrentCol());
        assertEquals(3, state.getEdgeCount());
        assertEquals(4, state.getVisitedNodeCount());
    }

    @Test
    public void allowsReenteringAnAlreadyCollectedNodeWhenItDoesNotCreateACycle() {
        GameState state = new GameState(new Board(3, 3, Collections.<Integer>emptySet()));

        assertEquals(GameState.MoveResult.MOVED, state.moveTo(0, 1));
        assertEquals(GameState.MoveResult.BACKTRACKED, state.moveTo(1, 1));
        assertTrue(state.hasVisited(0, 1));

        assertEquals(GameState.MoveResult.MOVED, state.moveTo(1, 0));
        assertEquals(GameState.MoveResult.MOVED, state.moveTo(0, 0));
        assertEquals(GameState.MoveResult.MOVED, state.moveTo(0, 1));

        assertEquals(4, state.getVisitedNodeCount());
        assertEquals(3, state.getScore());
    }

    @Test
    public void reportsWinAfterEveryAvailableNodeHasBeenVisited() {
        GameState state = new GameState(new Board(2, 2, Collections.<Integer>emptySet()));

        assertEquals(GameState.MoveResult.MOVED, state.moveTo(0, 1));
        assertEquals(GameState.MoveResult.MOVED, state.moveTo(0, 0));
        assertEquals(GameState.MoveResult.WON, state.moveTo(1, 0));
        assertEquals(4, state.getVisitedNodeCount());
    }

    @Test
    public void resetRestoresTheInitialNodeAndClearsGraphProgress() {
        GameState state = new GameState(new Board(3, 3, Collections.<Integer>emptySet()));
        state.moveTo(1, 2);
        state.moveTo(0, 2);

        state.reset();

        assertEquals(1, state.getCurrentRow());
        assertEquals(1, state.getCurrentCol());
        assertEquals(0, state.getScore());
        assertEquals(1, state.getVisitedNodeCount());
        assertEquals(0, state.getEdgeCount());
        assertEquals(1, state.getPath().size());
        assertFalse(state.hasVisited(1, 2));
    }
}
