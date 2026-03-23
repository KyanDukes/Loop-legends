package com.badlogic.HouseScreen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.beatmaker.game.daw.DAWScreen;

public class Main extends Game {
    
    public SpriteBatch batch;
    
    // ADDED: Store screen references
    public InteriorScreen interiorScreen;
    public DAWScreen dawScreen; // ADDED
    
    @Override
    public void create() {
        batch = new SpriteBatch();
        
        // Create screens once
        interiorScreen = new InteriorScreen(this);
        dawScreen = new DAWScreen(this); // ADDED
        
        // Start with HouseScreen (outside)
        setScreen(new HouseScreen(this));
    }
    
    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (dawScreen != null) dawScreen.dispose(); // ADDED
    }
}