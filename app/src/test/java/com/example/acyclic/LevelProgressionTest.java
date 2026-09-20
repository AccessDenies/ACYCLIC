package com.example.acyclic;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class LevelProgressionTest {
    @Test public void freshProgressOnlyUnlocksLevelOne() {
        ProgressManager progress = progress();
        assertTrue(progress.isLevelUnlocked(1));
        for (int level = 2; level <= 30; level++) assertFalse(progress.isLevelUnlocked(level));
    }
    @Test public void completionUnlocksNextAndIsRetained() {
        ProgressManager progress = progress();
        progress.completeLevel(1);
        assertTrue(progress.isLevelCompleted(1)); assertTrue(progress.isLevelUnlocked(2));
        for (int level = 2; level <= 5; level++) progress.completeLevel(level);
        assertTrue(progress.isLevelCompleted(5)); assertTrue(progress.isLevelUnlocked(6)); assertTrue(progress.isLevelUnlocked(1));
    }
    @Test public void progressSurvivesNewManagerAndLevelThirtyHasNoSuccessor() {
        MemoryStore store = new MemoryStore(); ProgressManager first = new ProgressManager(store);
        for (int level = 1; level <= 30; level++) first.completeLevel(level);
        ProgressManager second = new ProgressManager(store);
        assertTrue(second.isLevelCompleted(30)); assertEquals(30, second.getHighestUnlockedLevel());
    }
    @Test public void invalidLevelsAreSafe() {
        ProgressManager progress = progress(); progress.completeLevel(0); progress.completeLevel(31);
        assertFalse(progress.isLevelUnlocked(0)); assertFalse(progress.isLevelCompleted(31)); assertEquals(1, progress.getHighestUnlockedLevel());
    }
    @Test public void allThirtyDefinitionsCreateDistinctValidBoards() {
        assertEquals(30, LevelManager.getLevels().size());
        Set<String> configurations = new HashSet<>();
        for (int level = 1; level <= 30; level++) {
            LevelDefinition definition = LevelManager.getLevel(level); Board board = definition.createBoard();
            assertEquals(level, definition.getNumber()); assertTrue(board.isAvailable(board.getStartRow(), board.getStartCol()));
            assertTrue(board.getAvailableNodeCount() > 1);
            assertTrue(configurations.add(board.getRows() + "x" + board.getCols() + ":" + board.getUnavailableNodeIds()));
            assertEquals(board.getUnavailableNodeIds(), Board.forLevel(level).getUnavailableNodeIds());
        }
    }
    @Test public void levelSelectionLoadsTheRequestedConfiguration() {
        for (int level = 1; level <= 30; level++) {
            GameState state = new GameState(LevelManager.getLevel(level).createBoard());
            assertEquals(LevelManager.getLevel(level).getRows(), state.getBoard().getRows());
            assertEquals(LevelManager.getLevel(level).getCols(), state.getBoard().getCols());
        }
    }
    private ProgressManager progress() { return new ProgressManager(new MemoryStore()); }
    private static final class MemoryStore implements ProgressManager.Store {
        final Map<String, Integer> values = new HashMap<>();
        public int getInt(String key, int defaultValue) { return values.containsKey(key) ? values.get(key) : defaultValue; }
        public void putInt(String key, int value) { values.put(key, value); }
    }
}
