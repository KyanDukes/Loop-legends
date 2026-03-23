package com.beatmaker.game.daw;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

public class DAWPianoRollInput extends InputAdapter {
    
    private final OrthographicCamera camera;
    private final DAWPianoRoll pianoRoll;
    private final DAWTrackManager trackManager;
    private final Vector3 worldCoords = new Vector3();
    private boolean enabled = false;
    
    // States
    private enum Mode { NONE, PLACING, MOVING, RESIZING }
    private Mode currentMode = Mode.NONE;
    
    private float placeStartStep = -1;
    private int placeStartPitch = -1;
    
    public DAWPianoRollInput(OrthographicCamera camera, DAWPianoRoll pianoRoll, DAWTrackManager trackManager) {
        this.camera = camera;
        this.pianoRoll = pianoRoll;
        this.trackManager = trackManager;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
  
    
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!enabled) return false;
        
        worldCoords.set(screenX, screenY, 0);
        camera.unproject(worldCoords);
        
        float wx = worldCoords.x;
        float wy = worldCoords.y;
        
        // RIGHT CLICK = DELETE
        if (button == Input.Buttons.RIGHT) {
            DAWInstrumentTrack.Note hitNote = pianoRoll.pickNoteAt(wx, wy);
            if (hitNote != null) {
                trackManager.getCurrentTrack().removeNote(hitNote.startStep, hitNote.pitch);
                return true;
            }
            return false;
        }
        
        // LEFT CLICK
        int pitch = pianoRoll.hitPitch(wx, wy);
        float stepFloat = pianoRoll.hitStepFloat(wx, wy);
        
        if (pitch != -1 && stepFloat != -1) {
            // ADDED: Preview the note sound when clicking
            trackManager.getCurrentTrack().previewNote(pitch, 0.8f);
            
            DAWInstrumentTrack.Note existingNote = pianoRoll.pickNoteAt(wx, wy);
            
            if (existingNote != null) {
                // Clicking on existing note
                int pitchRow = existingNote.pitch - pianoRoll.getBasePitch();
                if (pitchRow < 0 || pitchRow >= DAWPianoRoll.TOTAL_SCALE_NOTES) {
                    return false;
                }
                
                float strideX = pianoRoll.getStepWidth() + 0.05f;
                float noteRightEdge = pianoRoll.cellRects[pitchRow][0].x + 
                                     ((existingNote.startStep + existingNote.duration) * strideX);
                float distanceToRightEdge = Math.abs(wx - noteRightEdge);
                
                if (distanceToRightEdge < 0.25f) {
                    // RESIZE MODE
                    boolean grabbed = pianoRoll.beginRightClickAction(wx, wy);
                    if (grabbed) {
                        currentMode = Mode.RESIZING;
                        return true;
                    }
                } else {
                    // MOVE MODE
                    boolean grabbed = pianoRoll.beginRightClickAction(wx, wy);
                    if (grabbed) {
                        currentMode = Mode.MOVING;
                        return true;
                    }
                }
                return true;
            } else {
                // PLACE NEW NOTE
                trackManager.getCurrentTrack().addNote(stepFloat, pitch, 0.8f, 0.5f);
                
                currentMode = Mode.PLACING;
                placeStartStep = stepFloat;
                placeStartPitch = pitch;
                
                return true;
            }
        }
        
        return false;
    }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!enabled) return false;
        
        worldCoords.set(screenX, screenY, 0);
        camera.unproject(worldCoords);
        
        float wx = worldCoords.x;
        float wy = worldCoords.y;
        
        switch (currentMode) {
            case MOVING:
                // Drag note to new position (pitch + time)
                pianoRoll.dragRightClick(wx, wy, false);
                return true;
                
            case RESIZING:
                // Resize note duration
                pianoRoll.dragRightClick(wx, wy, false);
                return true;
                
            case PLACING:
                // Extend the new note
                float currentStepFloat = pianoRoll.hitStepFloat(wx, wy);
                
                if (currentStepFloat != -1) {
                    float duration = Math.max(0.25f, currentStepFloat - placeStartStep);
                    
                    trackManager.getCurrentTrack().removeNote(placeStartStep, placeStartPitch);
                    trackManager.getCurrentTrack().addNote(placeStartStep, placeStartPitch, 0.8f, duration);
                }
                return true;
                
            default:
                return false;
        }
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!enabled) return false;
        
        if (currentMode == Mode.MOVING || currentMode == Mode.RESIZING) {
            pianoRoll.endRightClick();
        }
        
        currentMode = Mode.NONE;
        placeStartStep = -1;
        placeStartPitch = -1;
        
        return true;
    }
}