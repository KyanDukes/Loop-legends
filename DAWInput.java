package com.beatmaker.game.daw;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class DAWInput extends InputAdapter {

    // Listener used by DAWScreen – optional
    public interface PadListener {
        void onPadOn(int row, int step, float velocity);
        void onVelocityChanged(int row, int step, float velocity);
    }

    private PadListener padListener;

    private final Stage uiStage;
    private final OrthographicCamera camera;

    private final DAWGrid grid;
    private final DAWMixer mixer;
    private final DAWPatternBank patterns;

    private boolean clickStartedOnUI = false;
    private boolean dragging = false;
    
    // ADDED: Right-click deletion tracking
    private boolean rightMouseDragging = false;

    // painting logic
    private boolean paintState = true;   // ON or OFF
    private float paintVelocity = 1f;    // 0.5, 0.8, 1.0

    public DAWInput(Stage uiStage,
                    OrthographicCamera camera,
                    DAWGrid grid,
                    DAWMixer mixer,
                    DAWPatternBank patterns) {

        this.uiStage = uiStage;
        this.camera = camera;
        this.grid = grid;
        this.mixer = mixer;
        this.patterns = patterns;
    }

    public void setPadListener(PadListener listener) {
        this.padListener = listener;
    }

    public void setPaintVelocity(float v) {
        this.paintVelocity = v;
    }

    public DAWInput getGridProcessor() { return this; }

    public void update() {
        // nothing right now
    }

    // --------------------------------------------------------------------
    // TOUCH DOWN
    // --------------------------------------------------------------------
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        // If click is on UI, we ignore grid
        Actor uiHit = uiStage.hit(screenX, screenY, true);
        clickStartedOnUI = (uiHit != null);
        if (clickStartedOnUI) {
            dragging = false;
            rightMouseDragging = false; // ADDED
            return false;
        }

        Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
        int row = grid.hitRow(world.x, world.y);
        int step = grid.hitStep(world.x, world.y);

        if (row < 0 || step < 0) return false;

        // ----------------------------------------------------------
        // Right click = INSTANTLY DELETE pad
        // ----------------------------------------------------------
        if (button == Input.Buttons.RIGHT) {
            // Turn off the pad completely
            grid.setPad(row, step, false);
            grid.setVelocity(row, step, 0f);

            if (padListener != null)
                padListener.onVelocityChanged(row, step, 0f);

            rightMouseDragging = true; // ADDED: Enable hold-to-delete
            return true;
        }

        // ----------------------------------------------------------
        // Left click = toggle + NO SOUND (removed preview)
        // ----------------------------------------------------------
        boolean on = grid.isSet(row, step);
        
        if (on) {
            // Cycle velocity
            float v = grid.getVelocity(row, step);

            if (v >= 0.95f)      v = 0.5f;
            else if (v >= 0.75f) v = 1.0f;
            else if (v >= 0.45f) v = 0.8f;
            else                 v = 0.5f;

            grid.setVelocity(row, step, v);

            if (padListener != null)
                padListener.onVelocityChanged(row, step, v);
            
        } else {
            paintState = true;
            grid.setPad(row, step, true);
            grid.setVelocity(row, step, paintVelocity);

            if (padListener != null)
                padListener.onPadOn(row, step, paintVelocity);
        }

        dragging = true;
        return true;
    }

    // --------------------------------------------------------------------
    // DRAGGING
    // --------------------------------------------------------------------
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (clickStartedOnUI) return false;

        Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
        int row = grid.hitRow(world.x, world.y);
        int step = grid.hitStep(world.x, world.y);

        if (row < 0 || step < 0) return false;

        // ADDED: Handle right-click dragging (hold to delete)
        if (rightMouseDragging) {
            grid.setPad(row, step, false);
            grid.setVelocity(row, step, 0f);
            
            if (padListener != null)
                padListener.onVelocityChanged(row, step, 0f);
            
            return true;
        }

        // LEFT-CLICK DRAGGING (paint mode)
        if (!dragging) return false;

        grid.setPad(row, step, paintState);

        if (paintState) {
            grid.setVelocity(row, step, paintVelocity);
        }

        return true;
    }
    
    // --------------------------------------------------------------------
    // TOUCH UP
    // --------------------------------------------------------------------
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        dragging = false;
        rightMouseDragging = false; // ADDED: Reset right-click drag state
        clickStartedOnUI = false;
        return false;
    }
}