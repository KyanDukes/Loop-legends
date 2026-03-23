package com.beatmaker.game.daw;

import com.badlogic.gdx.math.MathUtils;

public class DAWPatternGenerator {
    
    private final DAWGrid grid;
    
    public DAWPatternGenerator(DAWGrid grid) {
        this.grid = grid;
    }
    
    // Generate basic 4-on-the-floor kick pattern
    public void generateFourOnFloor() {
        int kickRow = 0;
        
        for (int step = 0; step < DAWGrid.STEPS; step++) { // FIXED
            grid.setPad(kickRow, step, false);
        }
        
        for (int step = 0; step < DAWGrid.STEPS; step += 4) { // FIXED
            grid.setPad(kickRow, step, true);
            grid.setVelocity(kickRow, step, 1.0f);
        }
    }
    
    // Generate trap hi-hat pattern
    public void generateTrapHats() {
        int hatRow = 2;
        
        for (int s = 0; s < DAWGrid.STEPS; s++) { // FIXED
            grid.setPad(hatRow, s, false);
        }
        
        int[] pattern = {1, 0, 1, 1, 1, 0, 1, 1};
        
        for (int i = 0; i < pattern.length && i < DAWGrid.STEPS; i++) { // FIXED
            if (pattern[i] == 1) {
                grid.setPad(hatRow, i, true);
                float vel = (i % 2 == 0) ? 0.8f : 0.5f;
                grid.setVelocity(hatRow, i, vel);
            }
        }
    }
    
    // Generate random melody in C minor scale
    public void generateMelody() {
        int[] scale = {0, 2, 3, 5, 7, 8, 10};
        int baseNote = 60;
        
        grid.clearAll();
        
        for (int step = 0; step < Math.min(8, DAWGrid.STEPS); step++) { // FIXED
            int scaleIndex = MathUtils.random(scale.length - 1);
            int pitch = baseNote + scale[scaleIndex];
            
            if (MathUtils.randomBoolean(0.3f)) {
                pitch += 12;
            }
            
            float velocity = MathUtils.random(0.7f, 1.0f);
            
            int row = step % DAWGrid.ROWS;
            grid.setPad(row, step * 2, true);
            grid.setVelocity(row, step * 2, velocity);
        }
    }
    
    // Generate boom-bap style drums
    public void generateBoomBap() {
        int kickRow = 0;
        for (int s = 0; s < DAWGrid.STEPS; s++) { // FIXED
            grid.setPad(kickRow, s, false);
        }
        grid.setPad(kickRow, 0, true);
        grid.setVelocity(kickRow, 0, 1.0f);
        if (DAWGrid.STEPS > 8) { // FIXED
            grid.setPad(kickRow, 8, true);
            grid.setVelocity(kickRow, 8, 1.0f);
        }
        
        int snareRow = 1;
        for (int s = 0; s < DAWGrid.STEPS; s++) { // FIXED
            grid.setPad(snareRow, s, false);
        }
        if (DAWGrid.STEPS > 4) { // FIXED
            grid.setPad(snareRow, 4, true);
            grid.setVelocity(snareRow, 4, 1.0f);
        }
        if (DAWGrid.STEPS > 12) { // FIXED
            grid.setPad(snareRow, 12, true);
            grid.setVelocity(snareRow, 12, 1.0f);
        }
        
        int hatRow = 2;
        for (int s = 0; s < DAWGrid.STEPS; s++) { // FIXED
            grid.setPad(hatRow, s, false);
        }
        for (int s = 0; s < DAWGrid.STEPS; s += 2) { // FIXED
            grid.setPad(hatRow, s, true);
            grid.setVelocity(hatRow, s, 0.7f);
        }
    }
    
    // Apply swing to current pattern
    public void applySwing(float amount) {
        for (int row = 0; row < DAWGrid.ROWS; row++) {
            for (int step = 0; step < DAWGrid.STEPS; step++) { // FIXED
                if (step % 2 == 1 && grid.isSet(row, step)) {
                    float currentVel = grid.getVelocity(row, step);
                    grid.setVelocity(row, step, currentVel * (1.0f - amount * 0.3f));
                }
            }
        }
    }
    
    // Humanize: add slight velocity variations
    public void humanize(float amount) {
        for (int row = 0; row < DAWGrid.ROWS; row++) {
            for (int step = 0; step < DAWGrid.STEPS; step++) { // FIXED
                if (grid.isSet(row, step)) {
                    float currentVel = grid.getVelocity(row, step);
                    float variation = MathUtils.random(-amount, amount);
                    float newVel = MathUtils.clamp(currentVel + variation, 0.3f, 1.0f);
                    grid.setVelocity(row, step, newVel);
                }
            }
        }
    }
    
    // Euclidean rhythm generator
    public void generateEuclidean(int row, int hits, int steps) {
        for (int s = 0; s < DAWGrid.STEPS; s++) { // FIXED
            grid.setPad(row, s, false);
        }
        
        if (hits <= 0 || steps <= 0) return;
        
        float bucket = 0f;
        for (int i = 0; i < steps && i < DAWGrid.STEPS; i++) { // FIXED
            bucket += hits;
            if (bucket >= steps) {
                bucket -= steps;
                grid.setPad(row, i, true);
                grid.setVelocity(row, i, 0.8f);
            }
        }
    }
}