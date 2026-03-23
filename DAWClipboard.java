package com.beatmaker.game.daw;

public class DAWClipboard {
    
    private boolean[][] copiedPads = null;
    private float[][] copiedVelocities = null;
    private boolean hasData = false;
    
    public void copy(DAWGrid grid) {
        copiedPads = grid.exportPads();
        copiedVelocities = grid.exportVelocities();
        hasData = true;
        System.out.println("Pattern copied to clipboard");
    }
    
    public void paste(DAWGrid grid) {
        if (!hasData) {
            System.out.println("Clipboard is empty");
            return;
        }
        
        grid.importPads(copiedPads);
        grid.importVelocities(copiedVelocities);
        System.out.println("Pattern pasted from clipboard");
    }
    
    public boolean hasData() {
        return hasData;
    }
    
    public void clear() {
        copiedPads = null;
        copiedVelocities = null;
        hasData = false;
    }
}