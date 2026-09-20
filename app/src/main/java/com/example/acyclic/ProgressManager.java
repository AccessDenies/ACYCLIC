package com.example.acyclic;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists only level completion, deriving unlocks from the contiguous progression. */
public final class ProgressManager {
    interface Store { int getInt(String key, int defaultValue); void putInt(String key, int value); }
    private static final String KEY_HIGHEST_COMPLETED = "highest_completed_level";
    private final Store store;

    public ProgressManager(Context context) {
        this(new SharedPreferencesStore(context.getSharedPreferences("acyclic_progress", Context.MODE_PRIVATE)));
    }

    ProgressManager(Store store) { this.store = store; }

    public boolean isLevelUnlocked(int level) {
        return isValid(level) && level <= getHighestUnlockedLevel();
    }

    public boolean isLevelCompleted(int level) {
        return isValid(level) && level <= getHighestCompletedLevel();
    }

    public void completeLevel(int level) {
        if (!isValid(level)) { return; }
        int completed = getHighestCompletedLevel();
        if (level == completed + 1) {
            store.putInt(KEY_HIGHEST_COMPLETED, level);
        }
    }

    public int getHighestUnlockedLevel() {
        return Math.min(LevelManager.LEVEL_COUNT, getHighestCompletedLevel() + 1);
    }

    private int getHighestCompletedLevel() {
        return Math.max(0, Math.min(LevelManager.LEVEL_COUNT, store.getInt(KEY_HIGHEST_COMPLETED, 0)));
    }

    private boolean isValid(int level) { return level >= 1 && level <= LevelManager.LEVEL_COUNT; }

    private static final class SharedPreferencesStore implements Store {
        private final SharedPreferences preferences;
        SharedPreferencesStore(SharedPreferences preferences) { this.preferences = preferences; }
        public int getInt(String key, int defaultValue) { return preferences.getInt(key, defaultValue); }
        public void putInt(String key, int value) { preferences.edit().putInt(key, value).commit(); }
    }
}
