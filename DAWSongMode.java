package com.beatmaker.game.daw;

import com.badlogic.gdx.utils.Array;

public class DAWSongMode {
    
    public static class ChainEntry {
        public int patternIndex;
        public int repeatCount;
        
        public ChainEntry(int patternIndex, int repeatCount) {
            this.patternIndex = patternIndex;
            this.repeatCount = repeatCount;
        }
    }
    
    private Array<ChainEntry> chain = new Array<>();
    private boolean enabled = false;
    private int currentChainIndex = 0;
    private int currentRepeat = 0;
    private DAWPatternBank patternBank;
    
    public DAWSongMode(DAWPatternBank patternBank) {
        this.patternBank = patternBank;
    }
    
    // Add pattern to chain
    public void addToChain(int patternIndex, int repeatCount) {
        chain.add(new ChainEntry(patternIndex, repeatCount));
        System.out.println("Added pattern " + patternIndex + " x" + repeatCount + " to chain");
    }
    
    // Remove from chain
    public void removeFromChain(int chainIndex) {
        if (chainIndex >= 0 && chainIndex < chain.size) {
            chain.removeIndex(chainIndex);
        }
    }
    
    // Clear entire chain
    public void clearChain() {
        chain.clear();
        currentChainIndex = 0;
        currentRepeat = 0;
        System.out.println("Chain cleared");
    }
    
    // Called when pattern finishes playing
    public void onPatternComplete() {
        if (!enabled || chain.size == 0) return;
        
        ChainEntry current = chain.get(currentChainIndex);
        currentRepeat++;
        
        if (currentRepeat >= current.repeatCount) {
            // Move to next pattern
            currentRepeat = 0;
            currentChainIndex++;
            
            if (currentChainIndex >= chain.size) {
                // Loop back to start
                currentChainIndex = 0;
            }
            
            // Switch to next pattern
            ChainEntry next = chain.get(currentChainIndex);
            patternBank.setIndex(next.patternIndex);
            System.out.println("Chain: Switched to pattern " + (next.patternIndex + 1));
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            currentChainIndex = 0;
            currentRepeat = 0;
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public Array<ChainEntry> getChain() {
        return chain;
    }
    
    public int getCurrentChainIndex() {
        return currentChainIndex;
    }
    
    public int getCurrentRepeat() {
        return currentRepeat;
    }
    
    public String getChainLabel() {
        if (chain.size == 0) return "Empty Chain";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chain.size; i++) {
            ChainEntry entry = chain.get(i);
            sb.append("P").append(entry.patternIndex + 1)
              .append("x").append(entry.repeatCount);
            
            if (i < chain.size - 1) sb.append(" → ");
        }
        return sb.toString();
    }
}