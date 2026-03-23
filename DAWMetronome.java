package com.beatmaker.game.daw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

// ============================================
// METRONOME CLASS
// ============================================
public class DAWMetronome {
    
    private Sound clickHigh;
    private Sound clickLow;
    private boolean enabled = false;
    private float volume = 0.5f;
    
    public DAWMetronome() {
        // Generate click sounds programmatically or load them
        try {
            // You can generate simple click sounds or load from files
            // For now, we'll use the same sound but at different pitches
            clickHigh = Gdx.audio.newSound(Gdx.files.internal("Sounds/Tick.wav"));
            clickLow = clickHigh; // Use same sound, different pitch
        } catch (Exception e) {
            System.out.println("Metronome sounds not found - metronome disabled");
        }
    }
    
    public void playClick(int step) {
        if (!enabled || clickHigh == null) return;
        
        // First beat of bar (every 16 steps) = high click
        // Other quarter notes (every 4 steps) = low click
        if (step % 16 == 0) {
            clickHigh.play(volume, 1.5f, 0f); // Higher pitch
        } else if (step % 4 == 0) {
            clickLow.play(volume, 1.0f, 0f); // Normal pitch
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setVolume(float vol) {
        this.volume = MathUtils.clamp(vol, 0f, 1f);
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void dispose() {
        if (clickHigh != null && clickHigh != clickLow) {
            clickHigh.dispose();
        }
        if (clickLow != null) {
            clickLow.dispose();
        }
    }
}
