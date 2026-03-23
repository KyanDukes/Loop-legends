package com.beatmaker.game.daw;

import com.badlogic.gdx.audio.Sound;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DAWInstrumentTrack {

	public static class Note {
	    public float startStep;  // CHANGED from int to float
	    public int pitch;
	    public float duration;   // CHANGED from int to float
	    public float velocity;

	    // Playback state
	    public long voiceId = -1L;
	    public boolean isOn = false;
	    
	    // Sample-based playback tracking
	    public float playbackStartTime = -1f;

	    public Note(float startStep, int pitch, float velocity, float duration) {
	        this.startStep = startStep;
	        this.pitch = pitch;
	        this.velocity = velocity;
	        this.duration = duration;
	    }

	    public float endStepExclusive() {
	        return startStep + duration;
	    }
	}
    private final String name;
    private final Sound sound;
    private final List<Note> notes = new ArrayList<>();

    private float volume = 0.8f;
    private boolean muted = false;
    private boolean solo = false;

    // Track the loop length so note-off works when patterns loop
    private int patternSteps = 32;

    // Optional preview voice tracking so clicks don't stack forever
    private long previewVoiceId = -1L;
    
    // ✅ NEW: Sample-based mode (Keyscape behavior)
    private boolean isSampleBased = false;
    private float bpm = 120f;
    private float stepDuration = 0.125f; // calculated from BPM (16th note at 120 BPM)
    
    // Track playing samples for duration-based auto-release
    private Map<Long, SampleInstance> activeSamples = new HashMap<>();
    
    private static class SampleInstance {
        Note note;
        float startTime;
        float duration;
        
        SampleInstance(Note note, float startTime, float duration) {
            this.note = note;
            this.startTime = startTime;
            this.duration = duration;
        }
    }

    public DAWInstrumentTrack(String name, Sound sound) {
        this.name = name;
        this.sound = sound;
        this.isSampleBased = false; // default to normal behavior
    }
    
    /**
     * Constructor for sample-based instruments (piano, realistic sounds).
     */
    public DAWInstrumentTrack(String name, Sound sound, boolean isSampleBased) {
        this.name = name;
        this.sound = sound;
        this.isSampleBased = isSampleBased;
        calculateStepDuration();
    }
    
    private void calculateStepDuration() {
        // 16th note duration at current BPM
        float beatsPerSecond = bpm / 60f;
        float sixteenthNoteDuration = 1f / (beatsPerSecond * 4f);
        this.stepDuration = sixteenthNoteDuration;
    }
    
    public void setBPM(float bpm) {
        this.bpm = bpm;
        calculateStepDuration();
    }

    public void setPatternSteps(int steps) {
        this.patternSteps = Math.max(1, steps);
    }

    public void clearAll() {
        stopAllVoices();
        notes.clear();
    }

    public Note addNote(float startStep, int pitch, float velocity, float duration) {
        Note n = new Note(startStep, pitch, velocity, duration);
        notes.add(n);
        return n;
    }

    // Get note at a specific float position
    public Note getNote(float step, int pitch) {
        for (Note n : notes) {
            if (n.pitch == pitch && step >= n.startStep && step < n.startStep + n.duration) {
                return n;
            }
        }
        return null;
    }

    // Remove note at float position
    public void removeNote(float step, int pitch) {
        for (int i = notes.size() - 1; i >= 0; i--) {
            Note n = notes.get(i);
            if (n.pitch == pitch && Math.abs(n.startStep - step) < 0.1f) {
                if (n.isOn && sound != null) {
                    sound.stop(n.voiceId);
                    activeSamples.remove(n.voiceId);
                }
                notes.remove(i);
                return;
            }
        }
    }
    /**
     * Preview note sound (for piano roll clicking/dragging).
     * 
     * KEYSCAPE MODE: Plays sample from beginning, short duration
     * NORMAL MODE: Simple sound playback
     */
    public void previewNote(int pitch, float velocity) {
        if (sound == null || muted) return;

        // Stop previous preview so rapid clicks don't stack
        if (previewVoiceId != -1L) {
            sound.stop(previewVoiceId);
            activeSamples.remove(previewVoiceId);
        }

        float pitchMultiplier = (float) Math.pow(2, (pitch - 60) / 12.0);
        previewVoiceId = sound.play(volume * velocity, pitchMultiplier, 0);
        
        if (isSampleBased) {
            // Track this preview for auto-release (short 250ms preview)
            Note tempNote = new Note(0, pitch, velocity, 2); // ~2 steps
            tempNote.voiceId = previewVoiceId;
            tempNote.isOn = true;
            activeSamples.put(previewVoiceId, new SampleInstance(tempNote, getCurrentTime(), 0.25f));
            
            
        }
    }
    public void playNotesAtStep(int currentStep) {
        if (muted || sound == null) return;

        // If loop wrapped to 0, kill any voices still on (prevents hanging notes)
        if (currentStep == 0) {
            stopAllVoices();
        }

        if (isSampleBased) {
            // ✅ SAMPLE-BASED MODE (Keyscape behavior)
            playNotesAtStepSampleBased(currentStep);
        } else {
            // ✅ NORMAL MODE (your existing behavior)
            playNotesAtStepNormal(currentStep);
        }
    }

    /**
     * ✅ Playback with note-off based on duration.
     * 
     * KEYSCAPE MODE: Each note plays sample from beginning, holds for duration, auto-releases
     * NORMAL MODE: Your existing behavior
     */
    private void playNotesAtStepNormal(int currentStep) {
        // 1) NOTE OFF
        for (Note n : notes) {
            if (!n.isOn) continue;

            float endStep = n.startStep + n.duration;

            // FIXED: Direct comparison
            if (currentStep > n.startStep && currentStep >= endStep) {
                sound.stop(n.voiceId);
                n.isOn = false;
                n.voiceId = -1L;
                System.out.println("Note OFF at step " + currentStep + 
                                 " (started at " + n.startStep + 
                                 ", duration " + n.duration + 
                                 ", ended at " + endStep + ")");
            }
        }

        // 2) NOTE ON
        for (Note n : notes) {
            int noteStartStep = (int)Math.floor(n.startStep);
            
            if (currentStep == noteStartStep && !n.isOn) {
                if (n.voiceId != -1L) {
                    sound.stop(n.voiceId);
                }

                float pitchMultiplier = (float) Math.pow(2, (n.pitch - 60) / 12.0);
                n.voiceId = sound.play(volume * n.velocity, pitchMultiplier, 0);
                n.isOn = true;
                
                System.out.println("Note ON at step " + currentStep + 
                                 " pitch=" + n.pitch + 
                                 " duration=" + n.duration +
                                 " (will end at step " + (n.startStep + n.duration) + ")");
            }
        }
    }

    private void playNotesAtStepSampleBased(int currentStep) {
        // 1) NOTE OFF: Stop notes whose duration has ended
        for (Note n : notes) {
            if (!n.isOn) continue;
            
            float endStep = n.startStep + n.duration;
            
            // FIXED: Direct comparison instead of Math.ceil
            // Note should stop when we reach or pass its end point
            if (currentStep > n.startStep && currentStep >= endStep) {
                if (n.voiceId != -1L) {
                    sound.stop(n.voiceId);
                    activeSamples.remove(n.voiceId);
                    System.out.println("Sample Note OFF at step " + currentStep + 
                                     " (started at " + n.startStep + 
                                     ", duration " + n.duration + 
                                     ", ended at " + endStep + ")");
                }
                n.isOn = false;
                n.voiceId = -1L;
            }
        }
        
        // 2) NOTE ON: Trigger notes that start at this step
        for (Note n : notes) {
            // Check if this step is within the note's start range
            // Handle float precision: note at 15.9 should trigger at step 15
            int noteStartStep = (int)Math.floor(n.startStep);
            
            if (currentStep == noteStartStep && !n.isOn) {
                // Stop if somehow still playing
                if (n.voiceId != -1L) {
                    sound.stop(n.voiceId);
                    activeSamples.remove(n.voiceId);
                }

                float pitchMultiplier = (float) Math.pow(2, (n.pitch - 60) / 12.0);
                n.voiceId = sound.play(volume * n.velocity, pitchMultiplier, 0);
                n.isOn = true;
                n.playbackStartTime = getCurrentTime();
                
                float noteDurationSeconds = n.duration * stepDuration;
                activeSamples.put(n.voiceId, new SampleInstance(n, n.playbackStartTime, noteDurationSeconds));
                
                System.out.println("Sample Note ON at step " + currentStep + 
                                 " pitch=" + n.pitch + 
                                 " duration=" + n.duration + " steps" +
                                 " (will end at step " + (n.startStep + n.duration) + ")");
            }
        }
    }
    public void update(float delta) {
        if (!isSampleBased || sound == null) return;
        
        float currentTime = getCurrentTime();
        List<Long> toRemove = new ArrayList<>();
        
        for (Map.Entry<Long, SampleInstance> entry : activeSamples.entrySet()) {
            long voiceId = entry.getKey();
            SampleInstance instance = entry.getValue();
            
            float elapsed = currentTime - instance.startTime;
            
            // ✅ Auto-release when duration elapsed
            if (elapsed >= instance.duration) {
                // Natural fade/release
                sound.stop(voiceId);
                
                if (instance.note != null) {
                    instance.note.isOn = false;
                    instance.note.voiceId = -1L;
                }
                
                toRemove.add(voiceId);
            }
        }
        
        // Clean up finished samples
        for (Long id : toRemove) {
            activeSamples.remove(id);
        }
    }

    private void stopAllVoices() {
        if (sound == null) return;

        // Stop preview
        if (previewVoiceId != -1L) {
            sound.stop(previewVoiceId);
            previewVoiceId = -1L;
        }

        // Stop notes
        for (Note n : notes) {
            if (n.isOn) {
                sound.stop(n.voiceId);
                n.isOn = false;
                n.voiceId = -1L;
            }
        }
        
        // Clear sample tracking
        activeSamples.clear();
    }
    
    private float getCurrentTime() {
        return System.nanoTime() / 1_000_000_000f;
    }

    // Getters/setters
    public String getName() { return name; }

    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }

    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { 
        this.muted = muted;
        if (muted) stopAllVoices();
    }

    public boolean isSolo() { return solo; }
    public void setSolo(boolean solo) { this.solo = solo; }
    
    public boolean isSampleBased() { return isSampleBased; }
    public void setIsSampleBased(boolean isSampleBased) { 
        this.isSampleBased = isSampleBased;
        if (isSampleBased) calculateStepDuration();
    }

    public List<Note> getAllNotes() {
        return notes;
    }
}