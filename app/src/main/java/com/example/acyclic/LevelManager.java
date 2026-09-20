package com.example.acyclic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Central catalogue of the game's playable boards. */
public final class LevelManager {
    public static final int LEVEL_COUNT = 30;
    private static final List<LevelDefinition> LEVELS = buildLevels();

    private LevelManager() { }

    public static LevelDefinition getLevel(int level) {
        if (level < 1 || level > LEVEL_COUNT) {
            throw new IllegalArgumentException("Unknown level: " + level);
        }
        return LEVELS.get(level - 1);
    }

    public static List<LevelDefinition> getLevels() { return LEVELS; }

    private static List<LevelDefinition> buildLevels() {
        List<LevelDefinition> levels = new ArrayList<>();
        // Level 1 and the pre-existing Level 2 layout intentionally retain their original boards.
        levels.add(level(1, 8, 6, 4, 3));
        levels.add(level(2, 8, 6, 4, 3, 7, 13, 19, 25, 46, 47));

        int[][] sizes = {
            {4, 4}, {4, 5}, {5, 5}, {5, 6}, {6, 6}, {6, 7}, {7, 7}, {7, 8},
            {8, 8}, {8, 9}, {9, 9}, {9, 10}, {10, 10}, {8, 10}, {9, 11}, {10, 11},
            {10, 12}, {11, 11}, {11, 12}, {12, 12}, {10, 13}, {11, 13}, {12, 13},
            {12, 14}, {13, 13}, {13, 14}, {14, 14}, {14, 15}
        };
        for (int number = 3; number <= LEVEL_COUNT; number++) {
            int[] size = sizes[number - 3];
            int blocks = Math.max(0, (number - 5) / 2);
            levels.add(levelWithDetours(number, size[0], size[1], blocks));
        }
        return Collections.unmodifiableList(levels);
    }

    private static LevelDefinition levelWithDetours(int number, int rows, int cols, int blocks) {
        Set<Integer> unavailable = new HashSet<>();
        // Each later board has a distinct, deterministic obstacle pattern. The start stays at an
        // outer endpoint, leaving a clear opening into the board while route choices get denser.
        for (int i = 0; i < blocks; i++) {
            int row = 1 + ((number * 3 + i * 2) % (rows - 2));
            int col = 1 + ((number * 5 + i * 3) % (cols - 2));
            unavailable.add(row * cols + col);
        }
        return new LevelDefinition(number, rows, cols, 0, 0, unavailable);
    }

    private static LevelDefinition level(int number, int rows, int cols, int startRow, int startCol,
            int... unavailableIds) {
        Set<Integer> unavailable = new HashSet<>();
        for (int id : unavailableIds) { unavailable.add(id); }
        return new LevelDefinition(number, rows, cols, startRow, startCol, unavailable);
    }
}
