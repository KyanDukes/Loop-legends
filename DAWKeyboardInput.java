package com.beatmaker.game.daw;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class DAWKeyboardInput extends InputAdapter {
    
    private final DAWScreen screen;
    
    public DAWKeyboardInput(DAWScreen screen) {
        this.screen = screen;
    }
    
    @Override
    public boolean keyDown(int keycode) {
        // Check for Ctrl modifier
        boolean ctrl = com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || 
                      com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        
        
        // Undo/Redo
        if (ctrl && keycode == Input.Keys.Z) {
            screen.undo();
            return true;
        }
        if (ctrl && keycode == Input.Keys.Y) {
            screen.redo();
            return true;
        }
        
        // Copy/Paste
        if (ctrl && keycode == Input.Keys.C) {
            screen.copy();
            return true;
        }
        if (ctrl && keycode == Input.Keys.V) {
            screen.paste();
            return true;
        }
        
        // Delete
        if (keycode == Input.Keys.FORWARD_DEL || keycode == Input.Keys.BACKSPACE) {
            screen.clearSelected();
            return true;
        }
        
        // Pattern switching (1-9)
        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            int patternIndex = keycode - Input.Keys.NUM_1;
            screen.switchToPattern(patternIndex);
            return true;
        }
        
        // Space is already handled in DAWScreen.render()
        
        return false;
    }
}