package com.beatmaker.game.daw;

import java.util.Stack;

public class DAWHistory {
    
    private static final int MAX_HISTORY = 50;
    
    private Stack<DAWState> undoStack = new Stack<>();
    private Stack<DAWState> redoStack = new Stack<>();
    
    private DAWGrid grid;
    private DAWMixer mixer;
    private DAWTransport transport;
    
    public DAWHistory(DAWGrid grid, DAWMixer mixer, DAWTransport transport) {
        this.grid = grid;
        this.mixer = mixer;
        this.transport = transport;
    }
    
    // Capture current state before making changes
    public void captureState() {
        DAWState state = new DAWState(grid, mixer, transport);
        undoStack.push(state);
        
        // Limit history size
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.remove(0);
        }
        
        // Clear redo stack when new action is performed
        redoStack.clear();
        
        System.out.println("State captured. Undo stack size: " + undoStack.size());
    }
    
    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo");
            return;
        }
        
        // Save current state to redo stack
        DAWState currentState = new DAWState(grid, mixer, transport);
        redoStack.push(currentState);
        
        // Restore previous state
        DAWState previousState = undoStack.pop();
        previousState.restore(grid, mixer, transport);
        
        System.out.println("Undo performed. Undo stack: " + undoStack.size() + ", Redo stack: " + redoStack.size());
    }
    
    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo");
            return;
        }
        
        // Save current state to undo stack
        DAWState currentState = new DAWState(grid, mixer, transport);
        undoStack.push(currentState);
        
        // Restore next state
        DAWState nextState = redoStack.pop();
        nextState.restore(grid, mixer, transport);
        
        System.out.println("Redo performed. Undo stack: " + undoStack.size() + ", Redo stack: " + redoStack.size());
    }
    
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
    
    // Inner class to store complete DAW state
    private static class DAWState {
        private boolean[][] pads;
        private float[][] velocities;
        private int[] sampleIndexes;
        private float[] volumes;
        private float[] pans;
        private float[] pitches;
        private float[] attack;
        private float[] decay;
        private float[] sustain;
        private float[] release;
        private float bpm;
        private float swing;
        
        public DAWState(DAWGrid grid, DAWMixer mixer, DAWTransport transport) {
            // Clone grid data
            this.pads = grid.exportPads();
            this.velocities = grid.exportVelocities();
            
            // Clone mixer data
            this.sampleIndexes = mixer.exportSampleIndexes();
            this.volumes = mixer.exportVolumes();
            this.pans = mixer.exportPans();
            this.pitches = mixer.exportPitches();
            this.attack = mixer.exportAttack();
            this.decay = mixer.exportDecay();
            this.sustain = mixer.exportSustain();
            this.release = mixer.exportRelease();
            
            // Clone transport data
            this.bpm = transport.getBpm();
            this.swing = transport.getSwing();
        }
        
        public void restore(DAWGrid grid, DAWMixer mixer, DAWTransport transport) {
            // Restore grid
            grid.importPads(pads);
            grid.importVelocities(velocities);
            
            // Restore mixer
            mixer.importSampleIndexes(sampleIndexes);
            mixer.importMixerFloats(volumes, mixer.getVolumeArray());
            mixer.importMixerFloats(pans, mixer.getPanArray());
            mixer.importMixerFloats(pitches, mixer.getPitchArray());
            mixer.importMixerFloats(attack, mixer.getAttackArray());
            mixer.importMixerFloats(decay, mixer.getDecayArray());
            mixer.importMixerFloats(sustain, mixer.getSustainArray());
            mixer.importMixerFloats(release, mixer.getReleaseArray());
            
            // Restore transport
            transport.setBpm(bpm);
            transport.setSwing(swing);
        }
    }
}