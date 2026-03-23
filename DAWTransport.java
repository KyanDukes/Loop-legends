package com.beatmaker.game.daw;

/**
 * Handles BPM timing, swing, step-advancement, and callbacks for the DAW.
 *
 * IMPROVED VERSION with better timing precision and no drift.
 *
 * - Supports 16-step sequencer (expandable)
 * - Swing offsets for odd steps
 * - Visual + audio callbacks
 * - High-precision timing with accumulator rollover
 */
public class DAWTransport {
	 private int loopLength = 32; // ADDED: Default loop length
	    
	    // ADDED: Setter for loop length
	    public void setLoopLength(int length) {
	        this.loopLength = length;
	    }
	    
	    // MODIFIED: Step increment wraps at loop length
	    private void nextStep() {
	        currentStep++;
	        if (currentStep >= loopLength) { // CHANGED: Was >= 32 or >= STEPS
	            currentStep = 0;
	        }
	    }

    private float bpm = 120f;
    private float swing = 0f;   // 0–1 range
    private boolean playing = false;

    private float stepInterval;   // seconds per 16th note
    private float accumulator = 0f;

    private int currentStep = 0;
    private final int totalSteps = 32;  // CHANGED: 32 steps for 2 bars

    // Callback interface
    public interface StepListener {
        void onStep(int step);
    }

    public DAWTransport() {
        updateInterval();
    }

    private void updateInterval() {
        // 16th note timing: quarter-note = 60/bpm → * 0.25 for 16th
        stepInterval = (60f / bpm) * 0.25f;
    }

    // -----------------------------------------------
    // Playback Controls
    // -----------------------------------------------

    public void setBpm(float bpm) {
        this.bpm = Math.max(60f, Math.min(300f, bpm));
        updateInterval();
    }

    public float getBpm() {
        return bpm;
    }

    public void setSwing(float swing) {
        this.swing = Math.max(0f, Math.min(1f, swing));
    }

    public float getSwing() {
        return swing;
    }

    public void setPlaying(boolean play) {
        if (play && !this.playing) {
            // Starting fresh - reset timing
            resetCycle();
        }
        this.playing = play;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    private void resetCycle() {
        accumulator = 0f;
        currentStep = 0;
    }

    // -----------------------------------------------
    // Timing Update
    // -----------------------------------------------

    /**
     * Called every frame by DAWScreen.
     */
    public void update(float delta) {
        if (!playing) return;

        accumulator += delta;
    }

    /**
     * Called by DAWScreen to check if it's time to advance a step.
     * 
     * IMPROVED: Uses a while loop to handle multiple steps per frame
     * at very high BPMs or low framerates - prevents drift!
     *
     * @param delta raw frame delta (not used, we use accumulator)
     * @param listener step callback
     */
    public void tickIfNeeded(float delta, StepListener listener) {
        if (!playing || listener == null) return;

        // Process all steps that should have happened
        while (accumulator >= getCurrentStepDuration()) {
            float interval = getCurrentStepDuration();
            accumulator -= interval;

            // DEBUG: Print timing info
            System.out.println("Step " + currentStep + " fired at accumulator: " + accumulator);

            // Fire callback BEFORE advancing step (current step plays)
            listener.onStep(currentStep);

            // Advance to next step
            currentStep++;
            if (currentStep >= totalSteps) {
                currentStep = 0;
            }
        }
    }

    /**
     * Calculate duration for the current step with swing applied.
     */
    private float getCurrentStepDuration() {
        float interval = stepInterval;

        // Swing: delay ODD-numbered steps (makes them come later)
        // This creates the "swing" feel by making pairs uneven
        if (currentStep % 2 == 1) {
            interval += stepInterval * swing * 0.5f;
        }

        return interval;
    }

    public void reset() {
        resetCycle();
    }
}
