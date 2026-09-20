package com.example.acyclic;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.List;

/** Hosts the existing custom canvas game and its level-select home screen. */
public class MainActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView();
        setContentView(gameView);
    }

    @Override
    public void onBackPressed() {
        if (gameView.showHome()) return;
        super.onBackPressed();
    }

    public final class GameView extends View {
        private static final int CYAN = Color.rgb(0, 210, 255);
        private static final int PANEL = Color.rgb(12, 24, 35);
        private static final int BACKGROUND = Color.rgb(18, 18, 22);
        private static final int GRID_COLUMNS = 5;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ProgressManager progressManager = new ProgressManager(MainActivity.this);
        private GameState gameState;
        private int currentLevel = 1;
        private boolean showingHome = true;
        private boolean gameWon;
        private boolean gameOver;
        private boolean cycleWarning;
        private boolean paused;
        private float warningTime;
        private float animationTime;
        private float overlayAlpha;
        private float cellSize, startX, startY, playerX, playerY;
        private float touchStartX, touchStartY, levelScroll;

        GameView() {
            super(MainActivity.this);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        boolean showHome() {
            if (!showingHome) {
                showingHome = true;
                paused = false;
                gameWon = false;
                gameOver = false;
                invalidate();
                return true;
            }
            return false;
        }

        void startLevel(int level) {
            if (!progressManager.isLevelUnlocked(level)) return;
            currentLevel = level;
            gameState = new GameState(LevelManager.getLevel(level).createBoard());
            showingHome = false;
            gameWon = gameOver = cycleWarning = paused = false;
            warningTime = overlayAlpha = 0f;
            calculateGrid();
            playerX = getX(gameState.getCurrentCol());
            playerY = getY(gameState.getCurrentRow());
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(BACKGROUND);
            animationTime += .06f;
            if (showingHome) {
                drawHome(canvas);
            } else {
                calculateGrid();
                drawHeader(canvas);
                drawGrid(canvas);
                drawPath(canvas);
                drawCurrentNode(canvas);
                if (cycleWarning) drawCycleWarning(canvas);
                else if (gameWon) drawWin(canvas);
                else if (gameOver) drawGameOver(canvas);
                else if (paused) drawPause(canvas);
            }
            postInvalidateOnAnimation();
        }

        private void drawHome(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            float left = 24, right = w - 24, top = 24, bottom = h - 24;
            panel(canvas, left, top, right, bottom, 30, CYAN);
            centered(canvas, "ACYCLIC", w / 2, top + 58, 34, Color.WHITE, true);
            centered(canvas, "A PUZZLE GAME", w / 2, top + 88, 16, CYAN, true);
            centered(canvas, "Collect all the nodes • Don't create a cycle", w / 2, top + 119, 14, Color.LTGRAY, false);
            button(canvas, w / 2 - 105, top + 140, w / 2 + 105, top + 194, "PLAY", Color.rgb(0, 170, 220));
            centered(canvas, "LEVEL SELECT", w / 2, top + 228, 18, CYAN, true);

            float gridTop = top + 246 - levelScroll;
            float margin = 38, gap = 10, size = (w - margin * 2 - gap * 4) / GRID_COLUMNS;
            for (int level = 1; level <= LevelManager.LEVEL_COUNT; level++) {
                int index = level - 1, row = index / GRID_COLUMNS, col = index % GRID_COLUMNS;
                float x = margin + col * (size + gap), y = gridTop + row * (size + gap);
                if (y + size < top + 238 || y > bottom - 8) continue;
                boolean completed = progressManager.isLevelCompleted(level);
                boolean unlocked = progressManager.isLevelUnlocked(level);
                int fill = completed ? Color.rgb(0, 130, 105) : unlocked ? Color.rgb(0, 105, 145) : Color.rgb(38, 48, 58);
                panel(canvas, x, y, x + size, y + size, 14, unlocked ? CYAN : Color.rgb(75, 95, 105));
                paint.setStyle(Paint.Style.FILL); paint.setColor(fill);
                canvas.drawRoundRect(x + 3, y + 3, x + size - 3, y + size - 3, 12, 12, paint);
                centered(canvas, String.valueOf(level), x + size / 2, y + size / 2 + 7, 19, Color.WHITE, true);
                if (completed) centered(canvas, "✓", x + size - 14, y + 18, 17, Color.WHITE, true);
                else if (!unlocked) drawLock(canvas, x + size - 16, y + 11);
            }
        }

        private void drawLock(Canvas canvas, float x, float y) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.5f); paint.setColor(Color.LTGRAY);
            canvas.drawRoundRect(x, y + 7, x + 12, y + 17, 2, 2, paint);
            canvas.drawArc(x + 2, y, x + 10, y + 12, 180, -180, false, paint);
        }

        private void calculateGrid() {
            if (gameState == null) return;
            Board board = gameState.getBoard();
            float topSpace = 260, available = Math.max(80, getHeight() - topSpace - 30);
            cellSize = Math.min(getWidth() / (board.getCols() + 1f), available / (board.getRows() + 1f));
            startX = (getWidth() - (board.getCols() - 1) * cellSize) / 2f;
            startY = topSpace + (available - (board.getRows() - 1) * cellSize) / 2f;
        }

        private void drawHeader(Canvas canvas) {
            panel(canvas, 20, 20, getWidth() - 20, 230, 24, CYAN);
            centered(canvas, "ACYCLIC", getWidth() / 2f, 72, 30, Color.WHITE, true);
            centered(canvas, "LEVEL " + currentLevel + "  •  SCORE: " + gameState.getScore(), getWidth() / 2f, 122, 21, Color.WHITE, true);
            centered(canvas, "Swipe to move • Don't create a cycle", getWidth() / 2f, 170, 16, Color.LTGRAY, false);
            button(canvas, 30, 180, 155, 218, "MENU", Color.rgb(35, 70, 85));
            button(canvas, getWidth() - 135, 180, getWidth() - 30, 218, "PAUSE", Color.rgb(35, 70, 85));
        }

        private void drawGrid(Canvas canvas) {
            Board board = gameState.getBoard();
            for (int r = 0; r < board.getRows(); r++) for (int c = 0; c < board.getCols(); c++) {
                float x = getX(c), y = getY(r);
                if (!board.isAvailable(r, c)) {
                    paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(20, 39, 50)); canvas.drawCircle(x, y, 12, paint);
                    paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(Color.rgb(48, 91, 105));
                    canvas.drawLine(x - 5, y - 5, x + 5, y + 5, paint); canvas.drawLine(x + 5, y - 5, x - 5, y + 5, paint);
                } else {
                    paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(55, 0, 210, 255)); canvas.drawCircle(x, y, 12, paint);
                    paint.setColor(Color.rgb(125, 150, 160)); canvas.drawCircle(x, y, 5, paint);
                }
            }
        }

        private void drawPath(Canvas canvas) {
            List<GameState.Position> path = gameState.getPath();
            if (path.size() < 2) return;
            Path line = new Path(); line.moveTo(getX(path.get(0).getCol()), getY(path.get(0).getRow()));
            for (int i = 1; i < path.size(); i++) line.lineTo(getX(path.get(i).getCol()), getY(path.get(i).getRow()));
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(18); paint.setColor(Color.argb(80, 0, 210, 255)); canvas.drawPath(line, paint);
            paint.setStrokeWidth(10); paint.setColor(Color.rgb(0, 200, 255)); canvas.drawPath(line, paint);
        }

        private void drawCurrentNode(Canvas canvas) {
            float targetX = getX(gameState.getCurrentCol()), targetY = getY(gameState.getCurrentRow());
            playerX += (targetX - playerX) * .25f; playerY += (targetY - playerY) * .25f;
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); canvas.drawCircle(playerX, playerY, 16, paint);
            paint.setColor(Color.rgb(0, 180, 255)); canvas.drawCircle(playerX, playerY, 10, paint);
        }

        private void drawCycleWarning(Canvas canvas) {
            warningTime += .06f;
            overlay(canvas, "CYCLE DETECTED!", "This connection creates a cycle", Color.RED, null);
            if (warningTime >= .7f) { cycleWarning = false; gameOver = true; Toast.makeText(MainActivity.this, "Cycle detected!", Toast.LENGTH_SHORT).show(); }
        }
        private void drawGameOver(Canvas canvas) { overlay(canvas, "GAME OVER", "Tap RESTART to try again", Color.RED, "RESTART"); }
        private void drawPause(Canvas canvas) { overlay(canvas, "PAUSED", "", CYAN, "RESUME"); }
        private void drawWin(Canvas canvas) {
            overlay(canvas, "YOU WIN!", "Level " + currentLevel + " completed", Color.rgb(0, 230, 150), currentLevel < LevelManager.LEVEL_COUNT ? "NEXT LEVEL" : "LEVEL MENU");
            button(canvas, getWidth() / 2f - 105, getHeight() / 2f + 103, getWidth() / 2f + 105, getHeight() / 2f + 151, "LEVEL MENU", Color.rgb(35, 70, 85));
        }
        private void overlay(Canvas canvas, String title, String message, int color, String action) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(205, 0, 0, 0)); canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            float l = 35, t = getHeight() / 2f - 145, r = getWidth() - 35, b = t + 290;
            panel(canvas, l, t, r, b, 26, color); centered(canvas, title, getWidth() / 2f, t + 70, 38, color, true);
            centered(canvas, message, getWidth() / 2f, t + 120, 18, Color.WHITE, false);
            if (action != null) button(canvas, getWidth() / 2f - 105, t + 165, getWidth() / 2f + 105, t + 215, action, color);
        }

        private void panel(Canvas canvas, float l, float t, float r, float b, float radius, int border) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(PANEL); canvas.drawRoundRect(l, t, r, b, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3); paint.setColor(border); canvas.drawRoundRect(l, t, r, b, radius, radius, paint);
        }
        private void button(Canvas canvas, float l, float t, float r, float b, String text, int color) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color); canvas.drawRoundRect(l, t, r, b, 16, 16, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(Color.rgb(120, 235, 255)); canvas.drawRoundRect(l, t, r, b, 16, 16, paint);
            centered(canvas, text, (l + r) / 2f, (t + b) / 2f + 7, 17, Color.WHITE, true);
        }
        private void centered(Canvas canvas, String text, float x, float y, float size, int color, boolean bold) {
            paint.setStyle(Paint.Style.FILL); paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            paint.setTextSize(size); paint.setColor(color); canvas.drawText(text, x - paint.measureText(text) / 2f, y, paint);
        }
        private float getX(int col) { return startX + col * cellSize; }
        private float getY(int row) { return startY + row * cellSize; }

        private void movePlayer(int row, int col) {
            if (gameOver || gameWon || paused) return;
            GameState.MoveResult result = gameState.moveTo(row, col);
            if (result == GameState.MoveResult.CYCLE_DETECTED) { cycleWarning = true; warningTime = 0f; }
            if (result == GameState.MoveResult.WON) {
                progressManager.completeLevel(currentLevel);
                gameWon = true;
                Toast.makeText(MainActivity.this, "You Win!", Toast.LENGTH_SHORT).show();
            }
            invalidate();
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) { touchStartX = event.getX(); touchStartY = event.getY(); return true; }
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            float x = event.getX(), y = event.getY(), dx = x - touchStartX, dy = y - touchStartY;
            if (showingHome) {
                if (Math.abs(dy) > 30) { levelScroll = Math.max(0, levelScroll - dy); invalidate(); return true; }
                float top = 24;
                if (inside(x, y, getWidth() / 2f - 105, top + 140, getWidth() / 2f + 105, top + 194)) { startLevel(1); return true; }
                float size = (getWidth() - 76 - 40) / 5f, gridTop = top + 246 - levelScroll;
                int col = (int) ((x - 38) / (size + 10)), row = (int) ((y - gridTop) / (size + 10));
                if (col >= 0 && col < 5 && row >= 0) { int level = row * 5 + col + 1; if (level <= 30 && progressManager.isLevelUnlocked(level)) startLevel(level); }
                return true;
            }
            if (cycleWarning) return true;
            if (gameWon) {
                float mid = getHeight() / 2f;
                if (inside(x, y, getWidth()/2f-105, mid+103, getWidth()/2f+105, mid+151)) { showHome(); }
                else if (inside(x, y, getWidth()/2f-105, mid+20, getWidth()/2f+105, mid+70)) {
                    if (currentLevel == LevelManager.LEVEL_COUNT) showHome();
                    else startLevel(currentLevel + 1);
                }
                return true;
            }
            if (gameOver) { startLevel(currentLevel); return true; }
            if (paused) { if (y > getHeight()/2f + 20) startLevel(currentLevel); else paused = false; invalidate(); return true; }
            if (inside(x, y, 30, 180, 155, 218)) { showHome(); return true; }
            if (inside(x, y, getWidth()-135, 180, getWidth()-30, 218)) { paused = true; invalidate(); return true; }
            if (Math.abs(dx) < 50 && Math.abs(dy) < 50) return true;
            if (Math.abs(dx) > Math.abs(dy)) movePlayer(gameState.getCurrentRow(), gameState.getCurrentCol() + (dx > 0 ? 1 : -1));
            else movePlayer(gameState.getCurrentRow() + (dy > 0 ? 1 : -1), gameState.getCurrentCol());
            return true;
        }
        private boolean inside(float x, float y, float l, float t, float r, float b) { return x >= l && x <= r && y >= t && y <= b; }
    }
}
