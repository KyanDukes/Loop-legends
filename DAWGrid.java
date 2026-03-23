package com.beatmaker.game.daw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.Arrays;

public class DAWGrid {

    public static final int STEPS = 32;  // REVERTED: Fixed 32 steps for demo
    public static final int ROWS  = 8;

    private final FitViewport viewport;

    private boolean[][] pads       = new boolean[ROWS][STEPS];
    private float[][]   velocities = new float[ROWS][STEPS];
    private Rectangle[][] padRects = new Rectangle[ROWS][STEPS];

    private Texture padTex;
    private Texture whiteTex;
    private Texture labelBoxTex;
    private final BitmapFont font;

    public enum HiHatPattern {
        TWO_STEP, FOUR_STEP, OFFBEAT, RANDOM, TRIPLET, TRAP_ROLL, GHOST_NOTES
    }

    public DAWGrid(FitViewport viewport) {
        this.viewport = viewport;

        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(velocities[r], 1f);
        }

        padTex = new Texture("pad.png");

        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        whiteTex = new Texture(p);
        p.dispose();

        font = new BitmapFont();
        font.setColor(Color.WHITE);

        float worldH = viewport.getWorldHeight();
        float screenH = Gdx.graphics.getHeight();
        float baseScale = (worldH / screenH) * .1f;

        font.getData().setScale(baseScale);
        
        Pixmap labelBg = new Pixmap(60, 20, Pixmap.Format.RGBA8888);
        labelBg.setColor(0, 0, 0, 0.35f);
        labelBg.fill();

        labelBoxTex = new Texture(labelBg);
        labelBg.dispose();
      
        font.setColor(Color.WHITE);

        buildRects();
    }

    // -----------------------------------------------------
    // Build Pad Rectangles
    // -----------------------------------------------------
    private void buildRects() {
        final float cellW = 0.8f;
        final float cellH = 0.5f;
        final float spacing = 0.05f;
        
        float startX = 3.0f;
        float startY = 11.0f;
        
        for (int row = 0; row < ROWS; row++) {
            for (int step = 0; step < STEPS; step++) {
                float x = startX + step * (cellW + spacing);
                float y = startY + (ROWS - 1 - row) * (cellH + spacing);
                padRects[row][step] = new Rectangle(x, y, cellW, cellH);
            }
        }
    }
    
    // -----------------------------------------------------
    // Pad State
    // -----------------------------------------------------
    public boolean isSet(int row, int step) {
        if (step >= STEPS || step < 0) return false;
        return pads[row][step];
    }

    public void setPad(int row, int step, boolean on) {
        if (step >= 0 && step < STEPS && row >= 0 && row < ROWS) {
            pads[row][step] = on;
        }
    }

    public void togglePad(int row, int step) {
        if (step >= 0 && step < STEPS && row >= 0 && row < ROWS) {
            pads[row][step] = !pads[row][step];
        }
    }

    public void clearAll() {
        for (int r = 0; r < ROWS; r++) {
            Arrays.fill(pads[r], false);
        }
    }

    public void toggle(int row, int step, boolean on) {
        if (step >= 0 && step < STEPS && row >= 0 && row < ROWS) {
            pads[row][step] = on;
        }
    }

    // -----------------------------------------------------
    // Velocity
    // -----------------------------------------------------
    public float getVelocity(int row, int step) {
        if (step >= STEPS || step < 0) return 1f;
        return velocities[row][step];
    }

    public void setVelocity(int row, int step, float v) {
        if (step >= 0 && step < STEPS && row >= 0 && row < ROWS) {
            velocities[row][step] = MathUtils.clamp(v, 0f, 1f);
        }
    }

    // -----------------------------------------------------
    // Hit Detection
    // -----------------------------------------------------
    public int hitRow(float wx, float wy) {
        for (int r = 0; r < ROWS; r++) {
            for (int s = 0; s < STEPS; s++) {
                if (padRects[r][s].contains(wx, wy)) return r;
            }
        }
        return -1;
    }

    public int hitStep(float wx, float wy) {
        for (int r = 0; r < ROWS; r++) {
            for (int s = 0; s < STEPS; s++) {
                if (padRects[r][s].contains(wx, wy)) return s;
            }
        }
        return -1;
    }

    // -----------------------------------------------------
    // Draw Grid - SIMPLE VERSION
    // -----------------------------------------------------
    public void draw(SpriteBatch batch, int currentStep, boolean isPlaying) {
        // Draw all pads
        for (int row = 0; row < ROWS; row++) {
            for (int step = 0; step < STEPS; step++) {
                Rectangle pad = padRects[row][step];
                
                if (pads[row][step]) {
                    // Pad is ON - velocity affects brightness
                    float vel = velocities[row][step];
                    batch.setColor(vel * 0.5f, vel * 0.9f, vel * 1.0f, 1f);
                } else {
                    // Pad is OFF - gray
                    batch.setColor(0.35f, 0.35f, 0.35f, 1f);
                }
                
                batch.draw(padTex, pad.x, pad.y, pad.width, pad.height);
            }
        }
        
        // Draw grid lines
        for (int row = 0; row < ROWS; row++) {
            for (int step = 0; step < STEPS; step++) {
                Rectangle pad = padRects[row][step];
                
                // Beat markers (every 4 steps) - brighter
                if (step % 4 == 0) {
                    batch.setColor(0.6f, 0.6f, 0.6f, 0.4f);
                } else {
                    batch.setColor(0.4f, 0.4f, 0.4f, 0.3f);
                }
                batch.draw(whiteTex, pad.x, pad.y, 0.02f, pad.height);
            }
        }
        
        // PLAYBACK LINE
        if (isPlaying && currentStep >= 0 && currentStep < STEPS) {
            Rectangle currentCell = padRects[0][currentStep];
            
            batch.setColor(0f, 1f, 0f, 0.7f); // Green
            
            float lineX = currentCell.x + currentCell.width / 2f - 0.05f;
            float lineY = padRects[ROWS - 1][0].y;
            float lineHeight = (padRects[0][0].y + padRects[0][0].height) - lineY;
            
            batch.draw(whiteTex, lineX, lineY, 0.1f, lineHeight);
            
            batch.setColor(Color.WHITE);
        }
    }

    // -----------------------------------------------------
    // HiHat Patterns
    // -----------------------------------------------------
    public void applyHiHatPattern(HiHatPattern type) {
        int row = 2; // hi-hat row
        Arrays.fill(pads[row], false);

        switch (type) {
            case TWO_STEP:
                for (int i = 0; i < STEPS; i += 2) pads[row][i] = true;
                break;
            case FOUR_STEP:
                for (int i = 0; i < STEPS; i += 4) pads[row][i] = true;
                break;
            case OFFBEAT:
                for (int i = 1; i < STEPS; i += 4) pads[row][i] = true;
                break;
            case RANDOM:
                for (int i = 0; i < STEPS; i++) {
                    float chance = (i % 2 == 0) ? 0.7f : 0.3f;
                    pads[row][i] = MathUtils.randomBoolean(chance);
                }
                break;
            case TRIPLET:
                for (int i = 0; i < STEPS; i += 3) pads[row][i] = true;
                break;
            case TRAP_ROLL:
                // Fast roll in last 8 steps of each bar
                for (int i = 8; i < 16; i++) pads[row][i] = true;
                for (int i = 24; i < 32; i++) pads[row][i] = true;
                break;
            case GHOST_NOTES:
                for (int i = 0; i < STEPS; i += 2) pads[row][i] = true;
                for (int i = 1; i < STEPS; i += 4) {
                    pads[row][i] = true;
                    velocities[row][i] = 0.4f; // quieter ghost notes
                }
                break;
        }

        // Set default velocity for hats
        for (int i = 0; i < STEPS; i++) {
            if (pads[row][i] && velocities[row][i] > 0.5f) {
                velocities[row][i] = 0.8f;
            }
        }
    }

    public void applyHiHatEuclidean(int hits, int stepsParam, int offset) {
        int row = 2;
        Arrays.fill(pads[row], false);
        if (hits <= 0) return;

        float bucket = 0f;
        for (int i = 0; i < stepsParam && i < STEPS; i++) {
            bucket += hits;
            if (bucket >= stepsParam) {
                bucket -= stepsParam;
                int idx = (i + offset) % stepsParam;
                if (idx < STEPS) {
                    pads[row][idx] = true;
                    velocities[row][idx] = 0.8f;
                }
            }
        }
    }

    public void resize() {
        buildRects();
    }

    public void dispose() {
        if (padTex   != null) padTex.dispose();
        if (whiteTex != null) whiteTex.dispose();
        if (labelBoxTex != null) labelBoxTex.dispose();
        font.dispose();
    }

    public boolean[][] exportPads() {
        boolean[][] out = new boolean[ROWS][STEPS];
        for (int r = 0; r < ROWS; r++)
            for (int s = 0; s < STEPS; s++)
                out[r][s] = pads[r][s];
        return out;
    }

    public float[][] exportVelocities() {
        float[][] out = new float[ROWS][STEPS];
        for (int r = 0; r < ROWS; r++)
            for (int s = 0; s < STEPS; s++)
                out[r][s] = velocities[r][s];
        return out;
    }

    public void importPads(boolean[][] data) {
        for (int r = 0; r < ROWS && r < data.length; r++)
            for (int s = 0; s < STEPS && s < data[r].length; s++)
                pads[r][s] = data[r][s];
    }

    public void importVelocities(float[][] data) {
        for (int r = 0; r < ROWS && r < data.length; r++)
            for (int s = 0; s < STEPS && s < data[r].length; s++)
                velocities[r][s] = data[r][s];
    }
}