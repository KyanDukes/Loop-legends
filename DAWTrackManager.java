package com.beatmaker.game.daw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DAWTrackManager {
    
    private final List<DAWInstrumentTrack> tracks = new ArrayList<>();
    private int currentTrackIndex = 0;
    
    public void addTrack(DAWInstrumentTrack track) {
        if (track == null) return;
        tracks.add(track);
        if (tracks.size() == 1) currentTrackIndex = 0;
    }
    
    public void removeTrack(int index) {
        if (index >= 0 && index < tracks.size()) {
            tracks.remove(index);
            if (tracks.isEmpty()) {
                currentTrackIndex = 0;
            } else if (currentTrackIndex >= tracks.size()) {
                currentTrackIndex = tracks.size() - 1;
            }
        }
    }
    
    public DAWInstrumentTrack getCurrentTrack() {
        if (tracks.isEmpty()) return null;
        return tracks.get(currentTrackIndex);
    }
    
    public void setCurrentTrack(int index) {
        if (index >= 0 && index < tracks.size()) currentTrackIndex = index;
    }
    
    public int getCurrentTrackIndex() {
        return currentTrackIndex;
    }
    
    public List<DAWInstrumentTrack> getTracks() {
        return tracks;
    }
    
    public List<DAWInstrumentTrack> getAllTracks() {
        return tracks;
    }
    
    public int getTrackCount() {
        return tracks.size();
    }
    
    public void nextTrack() {
        if (tracks.isEmpty()) return;
        currentTrackIndex = (currentTrackIndex + 1) % tracks.size();
    }
    
    public void prevTrack() {
        if (tracks.isEmpty()) return;
        currentTrackIndex = (currentTrackIndex - 1 + tracks.size()) % tracks.size();
    }
    
    public void playAllTracks(int step) {
        for (DAWInstrumentTrack track : tracks) {
            track.playNotesAtStep(step);
        }
    }
    
    public void playAllTracksAtStep(int step) {
        playAllTracks(step);
    }
    
    public void update(float delta) {
        for (DAWInstrumentTrack track : tracks) {
            track.update(delta);
        }
    }
    
    // ADDED: Stop all playing notes (called when ESC exits DAW)
    public void stopAllNotes() {
        for (DAWInstrumentTrack track : tracks) {
            // Stop all voices and clear playback state
            for (DAWInstrumentTrack.Note note : track.getAllNotes()) {
                if (note.isOn && note.voiceId != -1L) {
                    // Sound is already stopped in DAWInstrumentTrack, just reset state
                    note.isOn = false;
                    note.voiceId = -1L;
                }
            }
        }
    }
}