package com.beatmaker.game.daw;

public class DAWPatternBank {

    private static final int MAX_PATTERNS = 16;
    private static final int STEPS = 32; // REVERTED: Fixed 32 steps for demo
    private final DAWGrid grid;
    private final DAWMixer mixer;

    // Store velocities for each pattern (0 = pad off, >0 = pad on with velocity)
    private final float[][][] patterns = new float[MAX_PATTERNS][DAWGrid.ROWS][STEPS];
    
    private int current = 0;

    public DAWPatternBank(DAWGrid grid, DAWMixer mixer) {
        this.grid = grid;
        this.mixer = mixer;
        
        System.out.println("Pattern bank initialized - 16 patterns, 32 steps each");
    }

    public void next() {
        store();
        current = (current + 1) % MAX_PATTERNS;
        recall();
    }

    public void prev() {
        store();
        current = (current - 1 + MAX_PATTERNS) % MAX_PATTERNS;
        recall();
    }

    public String labelText() {
        return "Pattern " + (current + 1);
    }

    public int getIndex() {
        return current;
    }

    public int getMaxPatterns() {
        return MAX_PATTERNS;
    }

    public void setIndex(int index) {
        if (index < 0 || index >= MAX_PATTERNS) {
            System.err.println("Invalid pattern index: " + index);
            return;
        }
        
        store();
        current = index;
        recall();
        
        System.out.println("Switched to pattern " + (index + 1));
    }

    public void store() {
        // Save current grid state into pattern slot
        for (int r = 0; r < DAWGrid.ROWS; r++) {
            for (int s = 0; s < STEPS; s++) {
                if (grid.isSet(r, s)) {
                    patterns[current][r][s] = grid.getVelocity(r, s);
                } else {
                    patterns[current][r][s] = 0f;
                }
            }
        }
    }

    public void recall() {
        // Load pattern from current slot into grid
        for (int r = 0; r < DAWGrid.ROWS; r++) {
            for (int s = 0; s < STEPS; s++) {
                float vel = patterns[current][r][s];
                
                if (vel > 0f) {
                    grid.setPad(r, s, true);
                    grid.setVelocity(r, s, vel);
                } else {
                    grid.setPad(r, s, false);
                }
            }
        }
    }

    public void duplicate() {
        int nextIndex = (current + 1) % MAX_PATTERNS;
        
        // Copy current pattern to next slot
        for (int r = 0; r < DAWGrid.ROWS; r++) {
            for (int s = 0; s < STEPS; s++) {
                patterns[nextIndex][r][s] = patterns[current][r][s];
            }
        }
        
        current = nextIndex;
        recall();
        
        System.out.println("Pattern duplicated to slot " + (current + 1));
    }

    public float[][][] exportAllPatterns() {
        store();
        
        float[][][] copy = new float[MAX_PATTERNS][DAWGrid.ROWS][STEPS];
        for (int p = 0; p < MAX_PATTERNS; p++) {
            for (int r = 0; r < DAWGrid.ROWS; r++) {
                System.arraycopy(patterns[p][r], 0, copy[p][r], 0, STEPS);
            }
        }
        return copy;
    }

    public void importAllPatterns(float[][][] data) {
        if (data == null || data.length != MAX_PATTERNS) return;
        
        for (int p = 0; p < MAX_PATTERNS; p++) {
            for (int r = 0; r < DAWGrid.ROWS; r++) {
                System.arraycopy(data[p][r], 0, patterns[p][r], 0, STEPS);
            }
        }
        
        recall();
    }
}