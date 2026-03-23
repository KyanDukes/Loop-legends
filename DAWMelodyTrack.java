package com.beatmaker.game.daw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;

public class DAWMelodyTrack {

    // Simple note structure
    public static class Note {
        public int step;        // When the note plays (0-31)
        public int pitch;       // MIDI note number (60 = C4)
        public float velocity;  // 0.0 to 1.0
        public int duration;    // How many steps it lasts

        public Note(int step, int pitch, float velocity, int duration) {
            this.step = step;
            this.pitch = pitch;
            this.velocity = velocity;
            this.duration = duration;
        }
    }

    private Sound sample;
    private Array<Note> notes;  // CHANGED: Now dynamic array instead of fixed
    private Array<Long> playingIds; // Track which notes are currently playing

    public DAWMelodyTrack(String samplePath) {
        // Load a single sample (we'll pitch-shift it)
        sample = Gdx.audio.newSound(Gdx.files.internal(samplePath));
        notes = new Array<>();
        playingIds = new Array<>();
        
        System.out.println("Melody track created - ready for notes!");
    }

    // Add a note
    public void addNote(int step, int pitch, float velocity, int duration) {
        // Check if note already exists at this step/pitch
        for (Note n : notes) {
            if (n.step == step && n.pitch == pitch) {
                // Update existing note
                n.velocity = velocity;
                n.duration = duration;
                return;
            }
        }
        
        // Add new note
        notes.add(new Note(step, pitch, velocity, duration));
        System.out.println("Added note: step=" + step + " pitch=" + pitch);
    }

    // Remove a note
    public void removeNote(int step, int pitch) {
        for (int i = 0; i < notes.size; i++) {
            Note n = notes.get(i);
            if (n.step == step && n.pitch == pitch) {
                notes.removeIndex(i);
                System.out.println("Removed note: step=" + step + " pitch=" + pitch);
                return;
            }
        }
    }

    // Check if a note exists
    public boolean hasNote(int step, int pitch) {
        for (Note n : notes) {
            if (n.step == step && n.pitch == pitch) {
                return true;
            }
        }
        return false;
    }

    // Get a note at a specific step/pitch
    public Note getNote(int step, int pitch) {
        for (Note n : notes) {
            if (n.step == step && n.pitch == pitch) {
                return n;
            }
        }
        return null;
    }

    // Clear all notes
    public void clearAll() {
        notes.clear();
        System.out.println("All melody notes cleared");
    }

    // Convert MIDI note to pitch multiplier
    private float midiToPitch(int midiNote) {
        // A4 (MIDI 69) = 440 Hz
        // Formula: pitch = 2^((midiNote - 60) / 12)
        // 60 is C4, our reference
        return (float) Math.pow(2.0, (midiNote - 60) / 12.0);
    }

    // Called every step by transport
    public void playStep(int step) {
        // Check if any notes start on this step
        for (int i = 0; i < notes.size; i++) {
            Note note = notes.get(i);

            if (note.step == step) {
                // Play the note at the correct pitch
                float pitch = midiToPitch(note.pitch);
                long id = sample.play(note.velocity, pitch, 0f);
                
                // Store the playing ID
                while (playingIds.size <= i) playingIds.add(0L);
                playingIds.set(i, id);
            }

            // Check if any notes should stop
            if (note.step + note.duration == step) {
                if (i < playingIds.size && playingIds.get(i) != 0) {
                    sample.stop(playingIds.get(i));
                    playingIds.set(i, 0L);
                }
            }
        }
    }

    // Get all notes (for rendering)
    public Array<Note> getNotes() {
        return notes;
    }

    public void dispose() {
        if (sample != null) sample.dispose();
    }

    public void previewNote(int pitch, float velocity) {
        float pitchMultiplier = midiToPitch(pitch);
        sample.play(velocity, pitchMultiplier, 0f);
    }

    // Make sure midiToPitch is accessible (change from private to public or add this if it's private)
    public float midiToPitch1(int midiNote) {
        return (float) Math.pow(2.0, (midiNote - 60) / 12.0);
    }
}