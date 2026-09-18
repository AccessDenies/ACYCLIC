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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(new GameView());
    }

    public class GameView extends View {

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int rows = 8;
        int cols = 6;

        float cellSize;
        float startX;
        float startY;

        float playerX;
        float playerY;

        boolean isMoving = false;

        int currentRow;
        int currentCol;

        int score = 0;

        boolean gameOver = false;
        boolean gameWon = false;
        boolean gameStarted = false;
        boolean playPressed = false;
        boolean gamePaused = false;

        int currentLevel = 1;

        List<Node> path = new ArrayList<>();

        // ================= GRAPH SYSTEM =================

        // Every node is a vertex.
        // Every player connection is an edge.

        Set<Integer> graphNodes = new HashSet<>();

        // Stores undirected connections between nodes.
        // Example: 1-2 and 2-1 are treated as the same edge.
        Set<Long> graphEdges = new HashSet<>();

        float touchStartX;
        float touchStartY;

        float animationTime = 0f;
        float overlayAlpha = 0f;

        boolean cycleWarning = false;
        float cycleWarningTime = 0f;

        class Node {
            int row;
            int col;

            Node(int row, int col) {
                this.row = row;
                this.col = col;
            }
        }

        boolean isNodeAvailable(int row, int col) {

            // =========================
            // LEVEL 1
            // =========================

            if (currentLevel == 1) {
                return true;
            }

            // =========================
            // LEVEL 2
            // SOLVABLE PUZZLE LAYOUT
            // =========================

            if (currentLevel == 2) {

                if ((row == 1 && col == 1)
                        || (row == 2 && col == 1)
                        || (row == 3 && col == 1)
                        || (row == 4 && col == 1)
                        || (row == 7 && col == 4)
                        || (row == 7 && col == 5)) {

                    return false;
                }

                return true;
            }

            // =========================
            // LEVEL 3
            // =========================

            if (currentLevel == 3) {

                return true;
            }

            return true;
        }

        public GameView() {
            super(MainActivity.this);

            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            gameStarted = false;
        }

        // =========================
        // START / RESTART GAME
        // =========================

        void startNewGame() {
            path.clear();
            graphNodes.clear();
            graphEdges.clear();
            score = 0;
            gameOver = false;
            gameWon = false;
            gameStarted = true;
            playPressed = false;
            gamePaused = false;
            animationTime = 0f;
            overlayAlpha = 0f;

            cycleWarning = false;
            cycleWarningTime = 0f;

            currentRow = rows / 2;
            currentCol = cols / 2;

            path.add(new Node(currentRow, currentCol));

            graphNodes.add(getNodeId(currentRow, currentCol));

            playerX = getX(currentCol);
            playerY = getY(currentRow);
            isMoving = false;

            invalidate();
        }

        // =========================
        // DRAW GAME
        // =========================

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            canvas.drawColor(Color.rgb(18, 18, 22));

            if (!gameStarted) {

                animationTime += 0.08f;

                drawStartScreen(canvas);

                postInvalidateOnAnimation();

                return;
            }

            calculateGrid();

            animationTime += 0.08f;

            if (gameOver || gameWon) {

                overlayAlpha += 12f;

                if (overlayAlpha > 210f) {
                    overlayAlpha = 210f;
                }

            } else {

                overlayAlpha = 0f;
            }

            drawHeader(canvas);
            drawGrid(canvas);
            drawPath(canvas);
            drawCurrentNode(canvas);

            if (cycleWarning) {

                drawCycleWarning(canvas);

            } else if (gameOver) {

                drawGameOver(canvas);

            } else if (gameWon) {

                drawWin(canvas);

            } else if (gamePaused) {

                drawPauseScreen(canvas);
            }

            postInvalidateOnAnimation();
        }

        // =========================
        // GRID POSITION
        // =========================

        void calculateGrid() {

            float width = getWidth();
            float height = getHeight();

            float topSpace = 330;

            float availableHeight = height - topSpace - 40;

            cellSize = Math.min(width / (cols + 1), availableHeight / (rows + 1));

            float gridWidth = (cols - 1) * cellSize;

            float gridHeight = (rows - 1) * cellSize;

            startX = (width - gridWidth) / 2;

            startY = topSpace + (availableHeight - gridHeight) / 2;
        }

        // =========================
        // HEADER
        // =========================

        void drawStartScreen(Canvas canvas) {

            float width = getWidth();
            float height = getHeight();

            // =========================
            // MAIN BOX POSITION
            // =========================

            float boxLeft = 30;
            float boxRight = width - 30;
            float boxTop = height * 0.15f;
            float boxBottom = height * 0.85f;

            // =========================
            // BACKGROUND GLOW
            // =========================

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.argb(30, 0, 210, 255));

            canvas.drawRoundRect(
                    boxLeft - 10, boxTop - 10, boxRight + 10, boxBottom + 10, 42, 42, paint);

            // =========================
            // MAIN BOX
            // =========================

            paint.setColor(Color.rgb(12, 24, 35));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 35, 35, paint);

            // =========================
            // CYAN BORDER
            // =========================

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.rgb(0, 210, 255));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 35, 35, paint);

            // =========================
            // TITLE
            // =========================

            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(38);

            // Title glow
            paint.setColor(Color.argb(70, 0, 210, 255));

            String title = "ACYCLIC";

            float titleWidth = paint.measureText(title);

            canvas.drawText(title, (width - titleWidth) / 2 + 2, boxTop + 92 + 2, paint);

            // Main title
            paint.setColor(Color.WHITE);

            canvas.drawText(title, (width - titleWidth) / 2, boxTop + 90, paint);

            // =========================
            // SUBTITLE
            // =========================

            paint.setTextSize(21);
            paint.setColor(Color.rgb(0, 210, 255));

            String subtitle = "A PUZZLE GAME";

            float subtitleWidth = paint.measureText(subtitle);

            canvas.drawText(subtitle, (width - subtitleWidth) / 2, boxTop + 135, paint);

            // =========================
            // DESCRIPTION
            // =========================

            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            paint.setTextSize(19);
            paint.setColor(Color.LTGRAY);

            String line1 = "Collect all the nodes";
            String line2 = "Don't create a cycle";

            float line1Width = paint.measureText(line1);
            float line2Width = paint.measureText(line2);

            canvas.drawText(line1, (width - line1Width) / 2, boxTop + 195, paint);

            canvas.drawText(line2, (width - line2Width) / 2, boxTop + 230, paint);

            // =========================
            // PLAY BUTTON
            // =========================

            float buttonWidth = 220;
            float buttonHeight = 65;

            float buttonLeft = (width - buttonWidth) / 2;
            float buttonTop = boxTop + 275;
            float buttonRight = buttonLeft + buttonWidth;
            float buttonBottom = buttonTop + buttonHeight;

            // Animated glow
            float glowPulse = (float) ((Math.sin(animationTime * 1.5f) + 1f) / 2f);

            int glowAlpha = playPressed ? 100 : (int) (35 + glowPulse * 35);

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.argb(glowAlpha, 0, 210, 255));

            canvas.drawRoundRect(
                    buttonLeft - 12,
                    buttonTop - 12,
                    buttonRight + 12,
                    buttonBottom + 12,
                    30,
                    30,
                    paint);

            // =========================
            // BUTTON
            // =========================

            if (playPressed) {

                paint.setColor(Color.rgb(0, 130, 175));

            } else {

                paint.setColor(Color.rgb(0, 170, 220));
            }

            canvas.drawRoundRect(buttonLeft, buttonTop, buttonRight, buttonBottom, 22, 22, paint);

            // =========================
            // BUTTON BORDER
            // =========================

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);

            paint.setColor(Color.rgb(120, 235, 255));

            canvas.drawRoundRect(buttonLeft, buttonTop, buttonRight, buttonBottom, 22, 22, paint);

            // =========================
            // PLAY TEXT
            // =========================

            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(28);
            paint.setColor(Color.WHITE);

            String playText = "PLAY";

            float playWidth = paint.measureText(playText);

            Paint.FontMetrics fm = paint.getFontMetrics();

            float buttonCenterY = buttonTop + (buttonHeight / 2);

            float playY = buttonCenterY - (fm.ascent + fm.descent) / 2;

            canvas.drawText(playText, (width - playWidth) / 2, playY, paint);

            // =========================
            // SMALL HINT
            // =========================

            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            paint.setTextSize(15);
            paint.setColor(Color.GRAY);

            String hint = "Swipe to move • Avoid cycles";

            float hintWidth = paint.measureText(hint);

            canvas.drawText(hint, (width - hintWidth) / 2, boxBottom - 35, paint);
        }

        void drawHeader(Canvas canvas) {

            float boxLeft = 25;
            float boxTop = 25;
            float boxRight = getWidth() - 25;
            float boxBottom = 280;

            // Box background
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(12, 24, 35));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // Cyan border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.rgb(0, 210, 255));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // Title
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(34);

            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            String title = "ACYCLIC";

            float titleWidth = paint.measureText(title);

            canvas.drawText(title, (getWidth() - titleWidth) / 2, 90, paint);

            // Score
            paint.setTextSize(24);

            String scoreText = "LEVEL " + currentLevel + "   •   SCORE: " + score;

            float scoreWidth = paint.measureText(scoreText);

            canvas.drawText(scoreText, (getWidth() - scoreWidth) / 2, 155, paint);

            // Instruction
            paint.setTypeface(android.graphics.Typeface.DEFAULT);

            paint.setTextSize(19);
            paint.setColor(Color.LTGRAY);

            String instruction = "Swipe to move • Don't create a cycle !";

            float instructionWidth = paint.measureText(instruction);

            canvas.drawText(instruction, (getWidth() - instructionWidth) / 2, 215, paint);

            // =========================
            // PAUSE BUTTON
            // =========================

            float pauseSize = 55;

            float pauseRight = getWidth() - 40;
            float pauseLeft = pauseRight - pauseSize;

            float pauseTop = 45;
            float pauseBottom = pauseTop + pauseSize;

            // Button glow
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(45, 0, 210, 255));

            canvas.drawRoundRect(
                    pauseLeft - 6, pauseTop - 6, pauseRight + 6, pauseBottom + 6, 18, 18, paint);

            // Button background
            paint.setColor(Color.rgb(18, 55, 70));

            canvas.drawRoundRect(pauseLeft, pauseTop, pauseRight, pauseBottom, 16, 16, paint);

            // Button border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(Color.rgb(0, 210, 255));

            canvas.drawRoundRect(pauseLeft, pauseTop, pauseRight, pauseBottom, 16, 16, paint);

            // Pause symbol
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);

            float barWidth = 7;
            float barHeight = 24;

            float centerX = (pauseLeft + pauseRight) / 2;

            canvas.drawRoundRect(
                    centerX - 12,
                    pauseTop + 15,
                    centerX - 12 + barWidth,
                    pauseTop + 15 + barHeight,
                    3,
                    3,
                    paint);

            canvas.drawRoundRect(
                    centerX + 5,
                    pauseTop + 15,
                    centerX + 5 + barWidth,
                    pauseTop + 15 + barHeight,
                    3,
                    3,
                    paint);
        }

        // =========================
        // DRAW GRID
        // =========================

        void drawGrid(Canvas canvas) {

            paint.setStyle(Paint.Style.FILL);

            float radius = 6;

            for (int r = 0; r < rows; r++) {

                for (int c = 0; c < cols; c++) {

                    float x = getX(c);
                    float y = getY(r);

                    if (isNodeAvailable(r, c)) {
                        canvas.drawCircle(x, y, radius, paint);
                    }

                    // =========================
                    // SUBTLE CYAN GLOW
                    // =========================

                    paint.setColor(Color.argb(35, 0, 210, 255));

                    canvas.drawCircle(x, y, 16, paint);

                    paint.setColor(Color.argb(65, 0, 210, 255));

                    canvas.drawCircle(x, y, 11, paint);

                    // =========================
                    // MAIN NODE
                    // =========================

                    paint.setColor(Color.rgb(100, 120, 130));

                    canvas.drawCircle(x, y, radius, paint);

                    // Small bright center
                    paint.setColor(Color.rgb(150, 180, 190));

                    canvas.drawCircle(x, y, 3, paint);
                }
            }
        }

        // =========================
        // DRAW PLAYER PATH
        // =========================

        void drawPath(Canvas canvas) {

            if (path.size() < 1) {
                return;
            }

            // =========================
            // CYAN PATH GLOW
            // =========================

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            Path line = new Path();

            Node first = path.get(0);

            line.moveTo(getX(first.col), getY(first.row));

            for (int i = 1; i < path.size(); i++) {

                Node node = path.get(i);

                line.lineTo(getX(node.col), getY(node.row));
            }

            // Outer glow
            paint.setStrokeWidth(28);
            paint.setColor(Color.argb(45, 0, 210, 255));

            canvas.drawPath(line, paint);

            // Middle glow
            paint.setStrokeWidth(21);
            paint.setColor(Color.argb(80, 0, 210, 255));

            canvas.drawPath(line, paint);

            // Main path
            paint.setStrokeWidth(14);
            paint.setColor(Color.rgb(0, 200, 255));

            canvas.drawPath(line, paint);

            // =========================
            // COLLECTED NODE GLOW
            // =========================

            paint.setStyle(Paint.Style.FILL);

            for (Node node : path) {

                float x = getX(node.col);
                float y = getY(node.row);

                // Green outer glow
                paint.setColor(Color.argb(40, 0, 255, 150));

                canvas.drawCircle(x, y, 18, paint);

                // Green middle glow
                paint.setColor(Color.argb(80, 0, 255, 150));

                canvas.drawCircle(x, y, 14, paint);

                // Main collected node
                paint.setColor(Color.rgb(0, 230, 150));

                canvas.drawCircle(x, y, 10, paint);
            }
        }

        // =========================
        // CURRENT PLAYER NODE
        // =========================

        void drawCurrentNode(Canvas canvas) {

            float targetX = getX(currentCol);
            float targetY = getY(currentRow);

            // Smooth movement
            if (!isMoving) {
                playerX = targetX;
                playerY = targetY;
            } else {

                float speed = 0.22f;

                playerX += (targetX - playerX) * speed;
                playerY += (targetY - playerY) * speed;

                if (Math.abs(targetX - playerX) < 1f && Math.abs(targetY - playerY) < 1f) {

                    playerX = targetX;
                    playerY = targetY;

                    isMoving = false;
                }
            }

            // Pulse animation
            float pulse = (float) Math.sin(animationTime) * 3f;

            // Outer glow
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(50, 0, 200, 255));

            canvas.drawCircle(playerX, playerY, 25 + pulse, paint);

            // White outer circle
            paint.setColor(Color.WHITE);

            canvas.drawCircle(playerX, playerY, 18 + pulse * 0.5f, paint);

            // Blue inner circle
            paint.setColor(Color.rgb(0, 180, 255));

            canvas.drawCircle(playerX, playerY, 11 + pulse * 0.3f, paint);

            // Continue animation until player reaches target
            if (isMoving) {
                postInvalidateOnAnimation();
            }
        }

        // =========================
        // CYCLE WARNING
        // =========================

        // =========================
        // CYCLE WARNING
        // =========================

        void drawCycleWarning(Canvas canvas) {

            cycleWarningTime += 0.08f;

            float pulse = (float) Math.sin(cycleWarningTime * 8f);

            // Red screen flash
            int alpha = (int) (35 + Math.abs(pulse) * 65);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(alpha, 255, 0, 0));

            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

            float width = getWidth();
            float height = getHeight();

            // Warning box
            float boxWidth = width - 100;
            float boxHeight = 120;

            float boxLeft = (width - boxWidth) / 2f;
            float boxTop = height / 2f - boxHeight / 2f;
            float boxRight = boxLeft + boxWidth;
            float boxBottom = boxTop + boxHeight;

            // Box
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(35, 10, 15));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 25, 25, paint);

            // Border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.RED);

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 25, 25, paint);

            // Warning title
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            paint.setTextSize(30);
            paint.setColor(Color.RED);

            String warning = "CYCLE DETECTED!";

            float warningWidth = paint.measureText(warning);

            canvas.drawText(warning, (width - warningWidth) / 2f, boxTop + 48, paint);

            // Message
            paint.setTypeface(android.graphics.Typeface.DEFAULT);

            paint.setTextSize(17);
            paint.setColor(Color.WHITE);

            String message = "This connection creates a cycle";

            float messageWidth = paint.measureText(message);

            canvas.drawText(message, (width - messageWidth) / 2f, boxTop + 85, paint);

            // =========================
            // WARNING TIMER
            // =========================

            if (cycleWarningTime >= 0.7f) {

                cycleWarning = false;

                gameOver = true;

                animationTime = 0f;
                overlayAlpha = 0f;

                Toast.makeText(MainActivity.this, "Cycle detected!", Toast.LENGTH_SHORT).show();
            }

            postInvalidateOnAnimation();
        }

        // =========================
        // GAME OVER SCREEN
        // =========================

        void drawGameOver(Canvas canvas) {

            // Dark overlay fade
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int) overlayAlpha, 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

            float width = getWidth();
            float height = getHeight();

            // Animation progress
            float progress = Math.min(1f, overlayAlpha / 210f);

            // Smooth scale animation
            float scale = 0.85f + (0.15f * progress);

            float boxWidth = width - 70;
            float boxHeight = 270;

            float centerX = width / 2f;
            float centerY = height / 2f;

            float boxLeft = centerX - (boxWidth * scale) / 2f;
            float boxRight = centerX + (boxWidth * scale) / 2f;
            float boxTop = centerY - (boxHeight * scale) / 2f;
            float boxBottom = centerY + (boxHeight * scale) / 2f;

            // Box background
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(12, 24, 35));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // Red outer glow
            paint.setColor(Color.argb(35, 255, 0, 0));

            canvas.drawRoundRect(
                    boxLeft - 8, boxTop - 8, boxRight + 8, boxBottom + 8, 32, 32, paint);

            // Red border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.RED);

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // Text
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

            // GAME OVER
            paint.setColor(Color.RED);
            paint.setTextSize(42);

            String text = "GAME OVER";
            float textWidth = paint.measureText(text);

            canvas.drawText(text, centerX - textWidth / 2f, boxTop + 70, paint);

            // Score
            paint.setColor(Color.WHITE);
            paint.setTextSize(25);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);

            String scoreText = "Score: " + score;
            float scoreWidth = paint.measureText(scoreText);

            canvas.drawText(scoreText, centerX - scoreWidth / 2f, boxTop + 125, paint);

            // Restart message
            paint.setColor(Color.LTGRAY);
            paint.setTextSize(18);

            String restart = "Tap anywhere to restart";
            float restartWidth = paint.measureText(restart);

            canvas.drawText(restart, centerX - restartWidth / 2f, boxTop + 185, paint);
        }

        void drawWin(Canvas canvas) {

            // Dark overlay fade
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int) overlayAlpha, 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

            float width = getWidth();
            float height = getHeight();

            // Animation progress
            float progress = Math.min(1f, overlayAlpha / 210f);

            // Smooth scale animation
            float scale = 0.85f + (0.15f * progress);

            float boxWidth = width - 70;
            float boxHeight = 300;

            float centerX = width / 2f;
            float centerY = height / 2f;

            float boxLeft = centerX - (boxWidth * scale) / 2f;
            float boxRight = centerX + (boxWidth * scale) / 2f;
            float boxTop = centerY - (boxHeight * scale) / 2f;
            float boxBottom = centerY + (boxHeight * scale) / 2f;

            // Box background
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(12, 24, 35));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // Green outer glow
            paint.setColor(Color.argb(35, 0, 255, 150));

            canvas.drawRoundRect(
                    boxLeft - 8, boxTop - 8, boxRight + 8, boxBottom + 8, 32, 32, paint);

            // Green border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.rgb(0, 230, 150));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // YOU WIN !
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(48);
            paint.setColor(Color.rgb(0, 230, 150));

            String winText = "YOU WIN !";
            float winWidth = paint.measureText(winText);

            canvas.drawText(winText, centerX - winWidth / 2f, boxTop + 75, paint);

            // All nodes collected
            paint.setColor(Color.WHITE);
            paint.setTextSize(23);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);

            String message = "All nodes collected !";
            float messageWidth = paint.measureText(message);

            canvas.drawText(message, centerX - messageWidth / 2f, boxTop + 130, paint);

            // Final score
            paint.setTextSize(24);

            String finalScore = "Score: " + score;
            float finalScoreWidth = paint.measureText(finalScore);

            canvas.drawText(finalScore, centerX - finalScoreWidth / 2f, boxTop + 175, paint);

            // Restart message
            // =========================
            // NEXT LEVEL BUTTON
            // =========================

            float buttonWidth = 220;
            float buttonHeight = 55;

            float buttonLeft = centerX - buttonWidth / 2f;
            float buttonTop = boxTop + 205;
            float buttonRight = buttonLeft + buttonWidth;
            float buttonBottom = buttonTop + buttonHeight;

            // Button glow
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(55, 0, 230, 150));

            canvas.drawRoundRect(
                    buttonLeft - 6,
                    buttonTop - 6,
                    buttonRight + 6,
                    buttonBottom + 6,
                    22,
                    22,
                    paint);

            // Button
            paint.setColor(Color.rgb(0, 180, 140));

            canvas.drawRoundRect(buttonLeft, buttonTop, buttonRight, buttonBottom, 18, 18, paint);

            // Button text
            paint.setColor(Color.WHITE);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(22);

            String nextText = "NEXT LEVEL";

            float nextWidth = paint.measureText(nextText);

            Paint.FontMetrics fm = paint.getFontMetrics();

            float buttonCenterY = buttonTop + buttonHeight / 2f;

            float textY = buttonCenterY - (fm.ascent + fm.descent) / 2f;

            canvas.drawText(nextText, centerX - nextWidth / 2f, textY, paint);
        }

        void drawPauseScreen(Canvas canvas) {

            // Dark overlay
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(210, 0, 0, 0));

            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

            float width = getWidth();
            float height = getHeight();

            // =========================
            // PAUSE BOX
            // =========================

            float boxWidth = width - 70;
            float boxHeight = 300;

            float boxLeft = 35;
            float boxTop = (height - boxHeight) / 2;
            float boxRight = boxLeft + boxWidth;
            float boxBottom = boxTop + boxHeight;

            // Box background
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(12, 24, 35));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // Cyan border
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.rgb(0, 210, 255));

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 28, 28, paint);

            // =========================
            // PAUSED TEXT
            // =========================

            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(44);
            paint.setColor(Color.rgb(0, 210, 255));

            String pausedText = "PAUSED";

            float pausedWidth = paint.measureText(pausedText);

            canvas.drawText(pausedText, (width - pausedWidth) / 2, boxTop + 70, paint);

            // =========================
            // RESUME BUTTON
            // =========================

            float buttonWidth = 220;
            float buttonHeight = 55;

            float buttonLeft = (width - buttonWidth) / 2;
            float resumeTop = boxTop + 105;
            float resumeRight = buttonLeft + buttonWidth;
            float resumeBottom = resumeTop + buttonHeight;

            paint.setColor(Color.rgb(0, 170, 220));

            canvas.drawRoundRect(buttonLeft, resumeTop, resumeRight, resumeBottom, 18, 18, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(22);

            String resumeText = "RESUME";

            float resumeWidth = paint.measureText(resumeText);

            Paint.FontMetrics fm = paint.getFontMetrics();

            float resumeY = resumeTop + buttonHeight / 2 - (fm.ascent + fm.descent) / 2;

            canvas.drawText(resumeText, (width - resumeWidth) / 2, resumeY, paint);

            // =========================
            // RESTART BUTTON
            // =========================

            float restartTop = boxTop + 175;
            float restartBottom = restartTop + buttonHeight;

            paint.setColor(Color.rgb(35, 45, 55));

            canvas.drawRoundRect(buttonLeft, restartTop, resumeRight, restartBottom, 18, 18, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(Color.rgb(0, 210, 255));

            canvas.drawRoundRect(buttonLeft, restartTop, resumeRight, restartBottom, 18, 18, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);

            paint.setTextSize(22);

            String restartText = "RESTART";

            float restartWidth = paint.measureText(restartText);

            Paint.FontMetrics fm2 = paint.getFontMetrics();

            float restartY = restartTop + buttonHeight / 2 - (fm2.ascent + fm2.descent) / 2;

            canvas.drawText(restartText, (width - restartWidth) / 2, restartY, paint);
        }

        // =========================
        // COORDINATES
        // =========================

        float getX(int col) {

            return startX + col * cellSize;
        }

        float getY(int row) {

            return startY + row * cellSize;
        }

        int getNodeId(int row, int col) {
            return row * cols + col;
        }

        long getEdgeKey(int nodeA, int nodeB) {

            int smaller = Math.min(nodeA, nodeB);
            int larger = Math.max(nodeA, nodeB);

            return (((long) smaller) << 32) | (larger & 0xffffffffL);
        }

        boolean edgeExists(int nodeA, int nodeB) {
            return graphEdges.contains(getEdgeKey(nodeA, nodeB));
        }

        // =========================
        // CHECK GRID BOUNDARY
        // =========================

        boolean isInsideGrid(int row, int col) {

            return row >= 0 && row < rows && col >= 0 && col < cols;
        }

        // =========================
        // CHECK VISITED NODE
        // =========================

        boolean isVisited(int row, int col) {

            for (Node node : path) {

                if (node.row == row && node.col == col) {
                    return true;
                }
            }

            return false;
        }

        // =========================
        // MOVE PLAYER
        // =========================

        void movePlayer(int newRow, int newCol) {

            // Game end hone ke baad movement band
            if (gameOver || gameWon) {
                return;
            }

            // Grid ke bahar movement allowed nahi
            if (!isInsideGrid(newRow, newCol)) {
                return;
            }

            // Level ke unavailable node par movement allowed nahi
            if (!isNodeAvailable(newRow, newCol)) {
                return;
            }

            // Sirf adjacent node par move
            int rowDifference = Math.abs(newRow - currentRow);
            int colDifference = Math.abs(newCol - currentCol);

            if (rowDifference + colDifference != 1) {
                return;
            }

            // ==========================================
            // CURRENT NODE → NEW NODE
            // ==========================================

            int currentId = getNodeId(currentRow, currentCol);
            int nextId = getNodeId(newRow, newCol);

            // ==========================================
            // BACKTRACK
            // ==========================================

            if (path.size() >= 2) {

                Node previousNode = path.get(path.size() - 2);

                if (previousNode.row == newRow && previousNode.col == newCol) {

                    // Current node = last node in path
                    Node currentNode = path.get(path.size() - 1);

                    currentId = getNodeId(currentNode.row, currentNode.col);

                    int previousId = getNodeId(previousNode.row, previousNode.col);

                    // Remove the connection we are backing out of.
                    graphEdges.remove(getEdgeKey(currentId, previousId));

                    // Remove current node from movement path.
                    path.remove(path.size() - 1);

                    // Move player back.
                    currentRow = newRow;
                    currentCol = newCol;

                    // Score follows the current path.
                    if (score > 0) {
                        score--;
                    }

                    invalidate();

                    return;
                }
            }

            // ==========================================
            // GRAPH CYCLE CHECK
            // ==========================================

            if (wouldCreateCycle(currentId, nextId)) {

                cycleWarning = true;
                cycleWarningTime = 0f;

                invalidate();

                return;
            }

            // Add the new vertex.
            graphNodes.add(nextId);

            // Add the new connection / edge.
            graphEdges.add(getEdgeKey(currentId, nextId));

            // Move player.
            currentRow = newRow;
            currentCol = newCol;

            path.add(new Node(currentRow, currentCol));

            score++;

            // ==========================================
            // WIN CHECK
            // ==========================================

            int totalAvailableNodes = 0;

            for (int r = 0; r < rows; r++) {

                for (int c = 0; c < cols; c++) {

                    if (isNodeAvailable(r, c)) {
                        totalAvailableNodes++;
                    }
                }
            }

            if (graphNodes.size() == totalAvailableNodes) {

                gameWon = true;

                animationTime = 0f;
                overlayAlpha = 0f;

                Toast.makeText(MainActivity.this, "You Win!", Toast.LENGTH_SHORT).show();
            }

            invalidate();
        }

        boolean canReachNode(int startId, int targetId) {

            if (startId == targetId) {
                return true;
            }

            Set<Integer> visited = new HashSet<>();
            List<Integer> stack = new ArrayList<>();

            stack.add(startId);

            while (!stack.isEmpty()) {

                int current = stack.remove(stack.size() - 1);

                if (current == targetId) {
                    return true;
                }

                if (visited.contains(current)) {
                    continue;
                }

                visited.add(current);

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

        boolean wouldCreateCycle(int currentId, int nextId) {

            // Existing edge ko dobara traverse karna
            // naya cycle create nahi karta.
            if (edgeExists(currentId, nextId)) {
                return false;
            }

            // Agar current aur next node already existing
            // graph ke through connected hain, to naya edge
            // unke beech closed loop bana dega.
            return canReachNode(currentId, nextId);
        }

        // =========================
        // TOUCH / SWIPE
        // =========================

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            // =========================
            // TOUCH START
            // =========================

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                touchStartX = event.getX();
                touchStartY = event.getY();

                // Start screen
                if (!gameStarted) {

                    float width = getWidth();
                    float height = getHeight();

                    float boxTop = height * 0.15f;

                    float buttonWidth = 220;
                    float buttonHeight = 65;

                    float buttonLeft = (width - buttonWidth) / 2;

                    float buttonTop = boxTop + 275;

                    float buttonRight = buttonLeft + buttonWidth;

                    float buttonBottom = buttonTop + buttonHeight;

                    // Check if PLAY button was pressed
                    if (event.getX() >= buttonLeft
                            && event.getX() <= buttonRight
                            && event.getY() >= buttonTop
                            && event.getY() <= buttonBottom) {

                        playPressed = true;

                        invalidate();
                    }

                    return true;
                }

                return true;
            }

            // =========================
            // TOUCH END
            // =========================

            if (event.getAction() == MotionEvent.ACTION_UP) {

                float touchEndX = event.getX();
                float touchEndY = event.getY();

                float dx = touchEndX - touchStartX;

                float dy = touchEndY - touchStartY;

                // =========================
                // PAUSE BUTTON
                // =========================

                if (gameStarted && !gameOver && !gameWon && !gamePaused) {

                    float pauseSize = 55;

                    float pauseRight = getWidth() - 40;
                    float pauseLeft = pauseRight - pauseSize;

                    float pauseTop = 45;
                    float pauseBottom = pauseTop + pauseSize;

                    if (touchEndX >= pauseLeft
                            && touchEndX <= pauseRight
                            && touchEndY >= pauseTop
                            && touchEndY <= pauseBottom) {

                        gamePaused = true;

                        invalidate();

                        return true;
                    }
                }

                // =========================
                // START SCREEN
                // =========================

                if (!gameStarted) {

                    float width = getWidth();
                    float height = getHeight();

                    float boxTop = height * 0.15f;

                    float buttonWidth = 220;
                    float buttonHeight = 65;

                    float buttonLeft = (width - buttonWidth) / 2;

                    float buttonTop = boxTop + 275;

                    float buttonRight = buttonLeft + buttonWidth;

                    float buttonBottom = buttonTop + buttonHeight;

                    // PLAY button released
                    if (event.getX() >= buttonLeft
                            && event.getX() <= buttonRight
                            && event.getY() >= buttonTop
                            && event.getY() <= buttonBottom) {

                        playPressed = false;

                        startNewGame();

                        return true;
                    }

                    // Tapped outside PLAY
                    playPressed = false;

                    invalidate();

                    return true;
                }

                // =========================
                // PAUSE SCREEN BUTTONS
                // =========================

                if (gamePaused) {

                    float width = getWidth();
                    float height = getHeight();

                    float boxHeight = 300;

                    float boxTop = (height - boxHeight) / 2;

                    float buttonWidth = 220;
                    float buttonHeight = 55;

                    float buttonLeft = (width - buttonWidth) / 2;

                    // RESUME button
                    float resumeTop = boxTop + 105;
                    float resumeBottom = resumeTop + buttonHeight;

                    if (touchEndX >= buttonLeft
                            && touchEndX <= buttonLeft + buttonWidth
                            && touchEndY >= resumeTop
                            && touchEndY <= resumeBottom) {

                        gamePaused = false;

                        invalidate();

                        return true;
                    }

                    // RESTART button
                    float restartTop = boxTop + 175;
                    float restartBottom = restartTop + buttonHeight;

                    if (touchEndX >= buttonLeft
                            && touchEndX <= buttonLeft + buttonWidth
                            && touchEndY >= restartTop
                            && touchEndY <= restartBottom) {

                        startNewGame();

                        return true;
                    }

                    return true;
                }

                // =========================
                // GAME OVER / WIN
                // =========================

                if (gameOver || gameWon) {

                    // =========================
                    // WIN SCREEN
                    // =========================

                    if (gameWon) {

                        float width = getWidth();
                        float height = getHeight();

                        float boxHeight = 300;
                        float boxTop = (height - boxHeight) / 2f;

                        float buttonWidth = 220;
                        float buttonHeight = 55;

                        float buttonLeft = (width - buttonWidth) / 2f;

                        float buttonTop = boxTop + 205;

                        float buttonRight = buttonLeft + buttonWidth;
                        float buttonBottom = buttonTop + buttonHeight;

                        // NEXT LEVEL button
                        if (touchEndX >= buttonLeft
                                && touchEndX <= buttonRight
                                && touchEndY >= buttonTop
                                && touchEndY <= buttonBottom) {

                            currentLevel++;

                            startNewGame();

                            return true;
                        }

                        return true;
                    }

                    // =========================
                    // GAME OVER
                    // =========================

                    if (gameOver) {

                        startNewGame();

                        return true;
                    }
                }

                // =========================
                // IGNORE SMALL MOVEMENT
                // =========================

                if (Math.abs(dx) < 50 && Math.abs(dy) < 50) {

                    return true;
                }

                // =========================
                // HORIZONTAL SWIPE
                // =========================

                if (Math.abs(dx) > Math.abs(dy)) {

                    if (dx > 0) {

                        movePlayer(currentRow, currentCol + 1);

                    } else {

                        movePlayer(currentRow, currentCol - 1);
                    }

                }

                // =========================
                // VERTICAL SWIPE
                // =========================

                else {

                    if (dy > 0) {

                        movePlayer(currentRow + 1, currentCol);

                    } else {

                        movePlayer(currentRow - 1, currentCol);
                    }
                }

                return true;
            }

            return true;
        }
    }
}
