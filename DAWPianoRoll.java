package com.beatmaker.game.daw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class DAWPianoRoll {
    
    public static final int STEPS = 32;
    
    // CHANGED: C Major scale only (no sharps/flats)
    // C, D, E, F, G, A, B across 2 octaves = 14 notes
    private static final int[] C_MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11}; // Intervals in one octave
    public static final int TOTAL_SCALE_NOTES = 14; // 2 octaves of C major
    public static final int VISIBLE_ROWS = 7; // One octave visible
    
    private final FitViewport viewport;
    private final DAWTrackManager trackManager;
    
    private Texture cellTex;
    private Texture whiteTex;
    private BitmapFont font;
    
    public Rectangle[][] cellRects = new Rectangle[TOTAL_SCALE_NOTES][STEPS];
    
    private int basePitch = 48; // C3
    private int scrollOffset = 0;
    private float cellH;
    
    // For resizing/moving
    private DAWInstrumentTrack.Note grabbedNote = null;
    private float grabOffsetSteps = 0f;
    private boolean resizing = false;
    private final float RESIZE_HANDLE_WORLD = 0.25f;
    
    private static final String[] NOTE_NAMES = {
        "C", "D", "E", "F", "G", "A", "B"
    };
    
    public DAWPianoRoll(FitViewport viewport, DAWTrackManager trackManager) {
        this.viewport = viewport;
        this.trackManager = trackManager;
        
        Pixmap cellPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        cellPix.setColor(0.2f, 0.2f, 0.25f, 1f);
        cellPix.fill();
        cellTex = new Texture(cellPix);
        cellPix.dispose();
        
        Pixmap whitePix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePix.setColor(Color.WHITE);
        whitePix.fill();
        whiteTex = new Texture(whitePix);
        whitePix.dispose();
        
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.02f);
        
        buildRects();
    }
    
    // ADDED: Convert scale row index to MIDI pitch
    private int scaleRowToPitch(int scaleRow) {
        int octave = scaleRow / 7;
        int noteInScale = scaleRow % 7;
        return basePitch + (octave * 12) + C_MAJOR_SCALE[noteInScale];
    }
    
    // ADDED: Convert MIDI pitch to scale row (if in scale, -1 otherwise)
    private int pitchToScaleRow(int pitch) {
        int relPitch = pitch - basePitch;
        if (relPitch < 0) return -1;
        
        int octave = relPitch / 12;
        int noteInOctave = relPitch % 12;
        
        // Find if this note is in C major scale
        for (int i = 0; i < C_MAJOR_SCALE.length; i++) {
            if (C_MAJOR_SCALE[i] == noteInOctave) {
                return octave * 7 + i;
            }
        }
        return -1; // Not in scale
    }
    
    private void buildRects() {
        final float cellW = 0.8f;
        cellH = 0.7f;
        final float spacing = 0.05f;

        float startX = 3.0f;
        float startY = 7.5f;

        for (int scaleRow = 0; scaleRow < TOTAL_SCALE_NOTES; scaleRow++) {
            for (int step = 0; step < STEPS; step++) {
                float x = startX + step * (cellW + spacing);
                float y = startY - (scaleRow * (cellH + spacing));
                cellRects[scaleRow][step] = new Rectangle(x, y, cellW, cellH);
            }
        }
    }
    
    private float getStepStrideX(int scaleRow) {
        Rectangle a = cellRects[scaleRow][0];
        Rectangle b = cellRects[scaleRow][1];
        return b.x - a.x;
    }

    private float getCellSpacingX(int scaleRow) {
        Rectangle a = cellRects[scaleRow][0];
        return getStepStrideX(scaleRow) - a.width;
    }
    
    private String getNoteName(int pitch) {
        int relPitch = pitch - basePitch;
        int octave = (pitch / 12) - 1;
        int noteInOctave = relPitch % 12;
        
        // Map to C major scale note names
        for (int i = 0; i < C_MAJOR_SCALE.length; i++) {
            if (C_MAJOR_SCALE[i] == noteInOctave) {
                return NOTE_NAMES[i] + octave;
            }
        }
        return "?";
    }
    
    public void handleScroll(int amount) {
        scrollOffset -= amount;
        scrollOffset = Math.max(0, Math.min(scrollOffset, TOTAL_SCALE_NOTES - VISIBLE_ROWS));
    }
    
    public void draw(SpriteBatch batch, int currentStep, boolean playing) {
        int startRow = scrollOffset;
        int endRow = Math.min(scrollOffset + VISIBLE_ROWS, TOTAL_SCALE_NOTES);
        
        final float spacing = 0.05f;
        
        // Draw piano keys
        drawPianoKeys(batch, startRow, endRow);
        
        // Draw note labels
        for (int scaleRow = startRow; scaleRow < endRow; scaleRow++) {
            int midiPitch = scaleRowToPitch(scaleRow);
            Rectangle firstCell = cellRects[scaleRow][0];
            
            batch.setColor(0.1f, 0.1f, 0.12f, 0.9f);
            batch.draw(cellTex, 1.5f, firstCell.y, 1.2f, firstCell.height);
            
            String noteName = getNoteName(midiPitch);
            float textX = 1.65f;
            float textY = firstCell.y + firstCell.height - 0.08f;
            
            font.setColor(Color.WHITE);
            font.draw(batch, noteName, textX, textY);
        }
        
        // Draw background grid
        drawBackgroundGrid(batch, startRow, endRow);
        
        // Draw notes
        drawCurrentTrackNotes(batch, startRow, endRow, spacing);
        
        // Draw bar lines
        batch.setColor(0.6f, 0.6f, 0.7f, 0.5f);
        for (int step = 0; step < STEPS; step += 4) {
            Rectangle cell = cellRects[scrollOffset][step];
            float lineX = cell.x - 0.02f;
            float lineY = cellRects[Math.min(scrollOffset + VISIBLE_ROWS - 1, TOTAL_SCALE_NOTES - 1)][0].y;
            float lineHeight = (cellRects[scrollOffset][0].y + cellRects[scrollOffset][0].height) - lineY;
            
            batch.draw(whiteTex, lineX, lineY, 0.04f, lineHeight);
        }
        
        batch.setColor(Color.WHITE);
        
        // Draw playback line
        if (playing && currentStep >= 0 && currentStep < STEPS) {
            Rectangle currentCell = cellRects[scrollOffset][currentStep];
            
            batch.setColor(0f, 1f, 0f, 0.7f);
            
            float lineX = currentCell.x + currentCell.width / 2f - 0.05f;
            float lineY = cellRects[Math.min(scrollOffset + VISIBLE_ROWS - 1, TOTAL_SCALE_NOTES - 1)][0].y;
            float lineHeight = (cellRects[scrollOffset][0].y + cellRects[scrollOffset][0].height) - lineY;
            
            batch.draw(whiteTex, lineX, lineY, 0.1f, lineHeight);
            
            batch.setColor(Color.WHITE);
        }
    }
    
    private void drawBackgroundGrid(SpriteBatch batch, int startRow, int endRow) {
        for (int scaleRow = startRow; scaleRow < endRow; scaleRow++) {
            for (int step = 0; step < STEPS; step++) {
                Rectangle cell = cellRects[scaleRow][step];
                
                // All white keys in C major, so uniform color
                batch.setColor(0.25f, 0.25f, 0.28f, 1.0f);
                batch.draw(cellTex, cell.x, cell.y, cell.width, cell.height);
                
                batch.setColor(0.4f, 0.4f, 0.4f, 0.3f);
                batch.draw(whiteTex, cell.x, cell.y, cell.width, 0.01f);
                batch.draw(whiteTex, cell.x, cell.y, 0.01f, cell.height);
            }
        }
        
        batch.setColor(Color.WHITE);
    }

    private void drawCurrentTrackNotes(SpriteBatch batch, int startRow, int endRow, float spacing) {
        DAWInstrumentTrack currentTrack = trackManager.getCurrentTrack();

        for (DAWInstrumentTrack.Note note : currentTrack.getAllNotes()) {
            int scaleRow = pitchToScaleRow(note.pitch);
            if (scaleRow == -1 || scaleRow < startRow || scaleRow >= endRow) continue;

            Rectangle cell0 = cellRects[scaleRow][0];
            float strideX = getStepStrideX(scaleRow);
            float spacingX = getCellSpacingX(scaleRow);

            float pad = 0.04f;

            float noteX = cell0.x + (note.startStep * strideX) + pad;
            float noteY = cell0.y + pad;

            float noteWidth = (note.duration * strideX) - spacingX - (pad * 2f);
            float noteHeight = cell0.height - (pad * 2f);

            float v = note.velocity;

            float r = Math.max(0.10f, v * 0.25f);
            float g = Math.max(0.35f, v * 0.65f);
            float b = Math.max(0.70f, v * 1.00f);

            // Draw note body
            batch.setColor(r, g, b, 1.0f);
            batch.draw(cellTex, noteX, noteY, noteWidth, noteHeight);

            // Draw note borders
            float border = 0.01f;
            batch.setColor(0.5f, 0.85f, 1.0f, 1.0f);
            batch.draw(whiteTex, noteX, noteY, noteWidth, border);
            batch.draw(whiteTex, noteX, noteY + noteHeight - border, noteWidth, border);
            batch.draw(whiteTex, noteX, noteY, border, noteHeight);
            batch.draw(whiteTex, noteX + noteWidth - border, noteY, border, noteHeight);
            
            // Draw resize handle
            float handleWidth = 0.15f;
            float handleX = noteX + noteWidth - handleWidth;
            
            batch.setColor(0.2f, 0.4f, 0.6f, 0.8f);
            batch.draw(cellTex, handleX, noteY, handleWidth, noteHeight);
            
            float arrowY1 = noteY + noteHeight * 0.35f;
            float arrowY2 = noteY + noteHeight * 0.65f;
            float arrowX = noteX + noteWidth - 0.08f;
            
            batch.setColor(0.8f, 0.9f, 1.0f, 1.0f);
            batch.draw(whiteTex, arrowX - 0.04f, arrowY1, 0.04f, 0.01f);
            batch.draw(whiteTex, arrowX - 0.04f, arrowY2, 0.04f, 0.01f);
            batch.draw(whiteTex, arrowX, arrowY1, 0.04f, 0.01f);
            batch.draw(whiteTex, arrowX, arrowY2, 0.04f, 0.01f);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawPianoKeys(SpriteBatch batch, int startRow, int endRow) {
        float keyWidth = 1.2f;
        float keyX = 1.3f;
        
        for (int scaleRow = startRow; scaleRow < endRow; scaleRow++) {
            Rectangle firstCell = cellRects[scaleRow][0];
            
            // All white keys (C major scale)
            batch.setColor(0.85f, 0.85f, 0.85f, 1.0f);
            batch.draw(cellTex, keyX, firstCell.y, keyWidth, firstCell.height);
            
            batch.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            batch.draw(whiteTex, keyX, firstCell.y, keyWidth, 0.02f);
            
            // Draw note letter
            int midiPitch = scaleRowToPitch(scaleRow);
            String noteName = getNoteName(midiPitch);
            font.setColor(0.2f, 0.2f, 0.2f, 1f);
            font.draw(batch, noteName, keyX + 0.15f, firstCell.y + (firstCell.height * 0.6f));
        }
        
        batch.setColor(Color.WHITE);
    }

    // Hit detection
    public int hitPitch(float wx, float wy) {
        int startRow = scrollOffset;
        int endRow = Math.min(scrollOffset + VISIBLE_ROWS, TOTAL_SCALE_NOTES);
        
        for (int scaleRow = startRow; scaleRow < endRow; scaleRow++) {
            for (int step = 0; step < STEPS; step++) {
                if (cellRects[scaleRow][step].contains(wx, wy)) {
                    return scaleRowToPitch(scaleRow);
                }
            }
        }
        return -1;
    }
    
    public int hitStep(float wx, float wy) {
        int startRow = scrollOffset;
        int endRow = Math.min(scrollOffset + VISIBLE_ROWS, TOTAL_SCALE_NOTES);
        
        for (int scaleRow = startRow; scaleRow < endRow; scaleRow++) {
            for (int step = 0; step < STEPS; step++) {
                if (cellRects[scaleRow][step].contains(wx, wy)) {
                    return step;
                }
            }
        }
        return -1;
    }
    
    public float hitStepFloat(float wx, float wy) {
        int startRow = scrollOffset;
        int endRow = Math.min(scrollOffset + VISIBLE_ROWS, TOTAL_SCALE_NOTES);
        
        for (int scaleRow = startRow; scaleRow < endRow; scaleRow++) {
            Rectangle firstCell = cellRects[scaleRow][0];
            if (wy >= firstCell.y && wy <= firstCell.y + firstCell.height) {
                Rectangle startCell = cellRects[scaleRow][0];
                float cellWidth = 0.8f;
                float spacing = 0.05f;
                
                float relativeX = wx - startCell.x;
                float stepFloat = relativeX / (cellWidth + spacing);
                
                stepFloat = Math.max(0, Math.min(STEPS - 0.1f, stepFloat));
                
                return stepFloat;
            }
        }
        return -1;
    }
    
    public DAWInstrumentTrack.Note pickNoteAt(float wx, float wy) {
        DAWInstrumentTrack currentTrack = trackManager.getCurrentTrack();
        if (currentTrack == null) return null;

        int pitch = hitPitch(wx, wy);
        if (pitch == -1) return null;

        int scaleRow = pitchToScaleRow(pitch);
        if (scaleRow < 0 || scaleRow >= TOTAL_SCALE_NOTES) return null;
        
        Rectangle cell0 = cellRects[scaleRow][0];
        if (cell0 == null) return null;

        float strideX = cellRects[scaleRow][1].x - cellRects[scaleRow][0].x;
        float stepFloat = (wx - cell0.x) / strideX;
        if (stepFloat < 0f || stepFloat >= STEPS) return null;

        for (DAWInstrumentTrack.Note n : currentTrack.getAllNotes()) {
            if (n.pitch != pitch) continue;

            float start = n.startStep;
            float end = n.startStep + n.duration;

            if (stepFloat >= start && stepFloat < end) {
                return n;
            }
        }

        return null;
    }
    
    public boolean beginRightClickAction(float wx, float wy) {
        DAWInstrumentTrack.Note hit = pickNoteAt(wx, wy);

        if (hit != null) {
            grabbedNote = hit;

            int scaleRow = pitchToScaleRow(hit.pitch);
            if (scaleRow == -1) return false;

            float strideX = cellRects[scaleRow][1].x - cellRects[scaleRow][0].x;
            float stepFloat = (wx - cellRects[scaleRow][0].x) / strideX;

            float noteRightX = cellRects[scaleRow][0].x + ((hit.startStep + hit.duration) * strideX);
            resizing = Math.abs(wx - noteRightX) <= RESIZE_HANDLE_WORLD;

            grabOffsetSteps = stepFloat - hit.startStep;

            return true;
        }

        return false;
    }

    public void dragRightClick(float wx, float wy, boolean snapToGrid) {
        if (grabbedNote == null) return;

        int pitch = hitPitch(wx, wy);
        if (pitch == -1) pitch = grabbedNote.pitch;

        int scaleRow = pitchToScaleRow(pitch);
        if (scaleRow == -1) return;
        
        scaleRow = Math.max(0, Math.min(TOTAL_SCALE_NOTES - 1, scaleRow));

        float strideX = cellRects[scaleRow][1].x - cellRects[scaleRow][0].x;
        float stepFloat = (wx - cellRects[scaleRow][0].x) / strideX;

        if (resizing) {
            float newEnd = stepFloat;
            if (snapToGrid) newEnd = (float)Math.round(newEnd);

            float newDur = newEnd - grabbedNote.startStep;
            newDur = Math.max(0.25f, newDur);
            newDur = Math.min(newDur, STEPS - grabbedNote.startStep);

            grabbedNote.duration = newDur;

        } else {
            float newStart = stepFloat - grabOffsetSteps;
            if (snapToGrid) newStart = (float)Math.round(newStart);

            newStart = Math.max(0f, Math.min(STEPS - grabbedNote.duration, newStart));

            grabbedNote.startStep = newStart;
            grabbedNote.pitch = pitch;
        }
    }

    public void endRightClick() {
        grabbedNote = null;
        resizing = false;
        grabOffsetSteps = 0f;
    }

    public void dispose() {
        if (cellTex != null) cellTex.dispose();
        if (whiteTex != null) whiteTex.dispose();
        if (font != null) font.dispose();
    }

    public int getBasePitch() {
        return basePitch; 
    }
    
    public float getStepWidth() {
        return cellRects[0][0].width;
    }
}