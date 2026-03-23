package com.badlogic.HouseScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.beatmaker.game.daw.DAWScreen;
import com.badlogic.gdx.math.Interpolation;

public class InteriorScreen implements Screen {

    private enum Direction { UP, DOWN, LEFT, RIGHT }

    private final Main game;
 // At the top with other fields:
    private boolean hasUsedDAW = false;
    private boolean showedPostDAWDialogue = false;
    private boolean hasSeenIntro = false; // ADDED
    
    // -----------------------------
    // WORLD
    // -----------------------------
    private OrthographicCamera worldCamera;
    private ExtendViewport worldViewport;
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private float mapW, mapH;

    // -----------------------------
    // UI
    // -----------------------------
    private OrthographicCamera uiCamera;
    private ScreenViewport uiViewport;

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private Stage stage;

    // -----------------------------
    // PLAYER + ANIMATIONS
    // -----------------------------
    private Texture playerSheet;
    private Animation<TextureRegion> walkUp, walkDown, walkLeft, walkRight;
    private TextureRegion idleUp, idleDown, idleLeft, idleRight;

    private float animTimer = 0f;
    private Direction currentDir = Direction.DOWN;
    private boolean isMoving = false;

    private float playerX = 10f;
    private float playerY = 7f;
    private float moveSpeed = 3f; // tiles/sec

    // -----------------------------
    // GHOST PORTRAIT (UI ONLY)
    // -----------------------------
    private Texture portraitNeutral;

    // -----------------------------
    // DIALOGUE + TYPEWRITER
    // -----------------------------
    private BitmapFont font;
    private BitmapFont uiFont;
    private GlyphLayout layout;

    private boolean isDialogueActive = true;
    private int currentLine = 0;
    private float typeTimer = 0f;
    private int visibleChars = 0;
    private float typeSpeed = 0.03f;
    private boolean lineFinished = false;

    private final String[] dialogueLines = {
            "Ah... you arrived just on time.",
            "You might not see me, but I can see you clearly.",
            "This room is where every journey begins.",
            "Over there — your workstation.",
            "Walk up to it and press E.",
            "After that... the sound is yours."
    };

    // -----------------------------
    // INTRO CUTSCENE STATE MACHINE
    // -----------------------------
    private enum IntroState {
        DIALOGUE,
        PAN_TO_DAW,
        HOLD_AT_DAW,
        PAN_BACK,
        FREE
    }
    private IntroState introState = IntroState.DIALOGUE;

    private float panTimer = 0f;
    private float panDuration = 1.0f;  // seconds per pan
    private float holdDuration = 1.0f; // seconds to hold on DAW
    private float holdTimer = 0f;

    private float panStartX, panStartY;
    private float panTargetX, panTargetY;

    // DAW desk interaction zone (tile units)
    private final Rectangle dawZone = new Rectangle(19f, 10f, 3f, 3f);

    public InteriorScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = game.batch;
        shape = new ShapeRenderer();
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // CHANGED: Only initialize these on FIRST show
        if (!hasSeenIntro) {
            // World camera/viewport (tile units)
            worldCamera = new OrthographicCamera();
            worldViewport = new ExtendViewport(20f, 15f, worldCamera);

            // UI camera/viewport (pixel space)
            uiCamera = new OrthographicCamera();
            uiViewport = new ScreenViewport(uiCamera);

            // Load map
            map = new TmxMapLoader().load("Interior.tmx");
            mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / 32f);

            TiledMapTileLayer baseLayer = null;
            for (MapLayer l : map.getLayers()) {
                if (l instanceof TiledMapTileLayer) {
                    baseLayer = (TiledMapTileLayer) l;
                    break;
                }
            }
            if (baseLayer == null) throw new GdxRuntimeException("Interior.tmx has no tile layers");

            mapW = baseLayer.getWidth();
            mapH = baseLayer.getHeight();

            // Player animations
            playerSheet = new Texture("blonde_man.png");
            TextureRegion[][] frames = TextureRegion.split(playerSheet, 32, 32);

            idleDown  = frames[0][1];
            idleLeft  = frames[1][1];
            idleRight = frames[2][1];
            idleUp    = frames[3][1];

            walkDown  = new Animation<TextureRegion>(0.15f, frames[0]);
            walkLeft  = new Animation<TextureRegion>(0.15f, frames[1]);
            walkRight = new Animation<TextureRegion>(0.15f, frames[2]);
            walkUp    = new Animation<TextureRegion>(0.15f, frames[3]);

            // Ghost portrait (UI only)
            portraitNeutral = new Texture("Ghost1.png");

            // Fonts
            FreeTypeFontGenerator gen =
                    new FreeTypeFontGenerator(Gdx.files.internal("Kenney Mini Square.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p =
                    new FreeTypeFontGenerator.FreeTypeFontParameter();

            p.size = 32;
            p.borderWidth = 2;
            p.color = Color.WHITE;
            p.borderColor = Color.BLACK;
            p.minFilter = Texture.TextureFilter.Nearest;
            p.magFilter = Texture.TextureFilter.Nearest;
            font = gen.generateFont(p);

            p.size = 20;
            uiFont = gen.generateFont(p);

            gen.dispose();

            layout = new GlyphLayout();

            introState = IntroState.DIALOGUE;
            isDialogueActive = true;
            currentLine = 0;
            visibleChars = 0;
            lineFinished = false;
            typeTimer = 0f;

            hasSeenIntro = true;
        } else {
            introState = IntroState.FREE;
            isDialogueActive = false;
        }
    }
    // -----------------------------
    // CAMERA UPDATE (CUTSCENE-AWARE)
    // -----------------------------
    private void updateWorldCamera(float delta) {

        if (introState == IntroState.PAN_TO_DAW || introState == IntroState.PAN_BACK) {
            panTimer += delta;

            float t = MathUtils.clamp(panTimer / panDuration, 0f, 1f);
            t = Interpolation.smooth2.apply(t);

            float camX = MathUtils.lerp(panStartX, panTargetX, t);
            float camY = MathUtils.lerp(panStartY, panTargetY, t);

            worldCamera.position.set(camX, camY, 0f);
            worldCamera.update();

            if (panTimer >= panDuration) {
                panTimer = 0f;

                if (introState == IntroState.PAN_TO_DAW) {
                    introState = IntroState.HOLD_AT_DAW;
                    holdTimer = 0f;
                } else {
                    introState = IntroState.FREE;
                }
            }
            return;
        }

        if (introState == IntroState.HOLD_AT_DAW) {
            holdTimer += delta;
            if (holdTimer >= holdDuration) {
                startPanBackToPlayer();
            }
            return;
        }

        // Normal follow (dialogue or free)
        worldCamera.position.set(
                MathUtils.clamp(playerX, worldCamera.viewportWidth / 2f,
                        mapW - worldCamera.viewportWidth / 2f),
                MathUtils.clamp(playerY, worldCamera.viewportHeight / 2f,
                        mapH - worldCamera.viewportHeight / 2f),
                0f
        );
        worldCamera.update();
    }

    private void startPanToDaw() {
        introState = IntroState.PAN_TO_DAW;
        panTimer = 0f;

        panStartX = worldCamera.position.x;
        panStartY = worldCamera.position.y;

        panTargetX = dawZone.x + dawZone.width / 2f;
        panTargetY = dawZone.y + dawZone.height / 2f;
    }

    private void startPanBackToPlayer() {
        introState = IntroState.PAN_BACK;
        panTimer = 0f;

        panStartX = worldCamera.position.x;
        panStartY = worldCamera.position.y;

        panTargetX = playerX;
        panTargetY = playerY;
    }

    @Override
    public void render(float delta) {

        handleInput(delta);
        updateWorldCamera(delta);

        // -----------------------------
        // TYPEWRITER (ONLY DURING DIALOGUE)
        // -----------------------------
        if (hasUsedDAW && !showedPostDAWDialogue && introState == IntroState.FREE) {
            showPostDAWDialogue();
        }
        if (introState == IntroState.DIALOGUE && isDialogueActive) {
            typeTimer += delta;

            if (!lineFinished && typeTimer >= typeSpeed) {
                typeTimer = 0f;

                visibleChars++;
                if (visibleChars >= dialogueLines[currentLine].length()) {
                    visibleChars = dialogueLines[currentLine].length();
                    lineFinished = true;
                }
            }
        }

        // SPACE advances dialogue
        if (introState == IntroState.DIALOGUE && isDialogueActive &&
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {

            if (!lineFinished) {
                visibleChars = dialogueLines[currentLine].length();
                lineFinished = true;
            } else {
                currentLine++;

                if (currentLine >= dialogueLines.length) {
                    // Dialogue finished -> start pan
                    isDialogueActive = false;
                    startPanToDaw();
                } else {
                    visibleChars = 0;
                    lineFinished = false;
                    typeTimer = 0f;
                }
            }
        }

        // -----------------------------
        // CLEAR
        // -----------------------------
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // -----------------------------
        // WORLD RENDER
        // -----------------------------
        worldViewport.apply();
        mapRenderer.setView(worldCamera);
        mapRenderer.render();

        // -----------------------------
        // PLAYER RENDER
        // -----------------------------
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        animTimer += delta;
        TextureRegion frame;

        if (isMoving) {
            switch (currentDir) {
                case UP:    frame = walkUp.getKeyFrame(animTimer, true); break;
                case DOWN:  frame = walkDown.getKeyFrame(animTimer, true); break;
                case LEFT:  frame = walkLeft.getKeyFrame(animTimer, true); break;
                case RIGHT: frame = walkRight.getKeyFrame(animTimer, true); break;
                default:    frame = idleDown;
            }
        } else {
            switch (currentDir) {
                case UP:    frame = idleUp; break;
                case DOWN:  frame = idleDown; break;
                case LEFT:  frame = idleLeft; break;
                case RIGHT: frame = idleRight; break;
                default:    frame = idleDown;
            }
        }

        batch.draw(frame, playerX, playerY, 1f, 1f);
        batch.end();

        // -----------------------------
        // E POPUP (ONLY WHEN FREE)
        // -----------------------------
        if (introState == IntroState.FREE) {
            Rectangle playerRect = new Rectangle(playerX, playerY, 1f, 1f);
            boolean nearDaw = playerRect.overlaps(dawZone);

            if (nearDaw) {
                drawInteractionPopup();
            }
        }

        // -----------------------------
        // DIALOGUE UI (ONLY DURING DIALOGUE)
        // -----------------------------
        if (introState == IntroState.DIALOGUE && isDialogueActive) {
            String full = dialogueLines[currentLine];
            String visible = full.substring(0, Math.min(visibleChars, full.length()));
            drawDialogue(visible);
        }
    }

    private void showPostDAWDialogue() {
    	 showedPostDAWDialogue = true;
    	    
    	    // Draw simple dialogue box
    	    uiViewport.apply();
    	    uiCamera.update();
    	    batch.setProjectionMatrix(uiCamera.combined);
    	    shape.setProjectionMatrix(uiCamera.combined);

    	    float boxX = 40f;
    	    float boxY = 40f;
    	    float boxW = Gdx.graphics.getWidth() - 80f;
    	    float boxH = 150f;

    	    shape.begin(ShapeRenderer.ShapeType.Filled);
    	    shape.setColor(0f, 0f, 0f, 0.75f);
    	    shape.rect(boxX, boxY, boxW, boxH);
    	    shape.end();

    	    batch.begin();
    	    float portraitSize = 96f;
    	    batch.draw(portraitNeutral, boxX + 20f, boxY + 20f, portraitSize, portraitSize);
    	    
    	    font.draw(batch, "Not bad. Keep practicing.",
    	            boxX + 20f + portraitSize + 20f,
    	            boxY + boxH - 40f);
    	    batch.end();
		
	}

	private void drawInteractionPopup() {

        // World highlight
        shape.setProjectionMatrix(worldCamera.combined);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.GREEN);
        shape.rect(dawZone.x, dawZone.y, dawZone.width, dawZone.height);
        shape.end();

        // UI bubble
        uiViewport.apply();
        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);
        shape.setProjectionMatrix(uiCamera.combined);

        float bubbleW = 220f;
        float bubbleH = 80f;
        float bubbleX = (Gdx.graphics.getWidth() - bubbleW) / 2f;
        float bubbleY = 120f;

        // background
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.85f);
        shape.rect(bubbleX, bubbleY, bubbleW, bubbleH);
        shape.end();

        // border
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        shape.rect(bubbleX + 2f, bubbleY + 2f, bubbleW - 4f, bubbleH - 4f);
        shape.end();

        batch.begin();
        uiFont.draw(batch, "[E]  Use Computer",
                bubbleX + 20f,
                bubbleY + bubbleH / 2f + 8f);
        batch.end();
    }

    private void drawDialogue(String text) {

        uiViewport.apply();
        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);
        shape.setProjectionMatrix(uiCamera.combined);

        float boxX = 40f;
        float boxY = 40f;
        float boxW = Gdx.graphics.getWidth() - 80f;
        float boxH = 200f;

        // background
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.75f);
        shape.rect(boxX, boxY, boxW, boxH);
        shape.end();

        batch.begin();

        float portraitSize = 96f;
        batch.draw(portraitNeutral, boxX + 20f, boxY + 20f, portraitSize, portraitSize);

        font.draw(batch, text,
                boxX + 20f + portraitSize + 20f,
                boxY + boxH - 60f);

        batch.end();
    }

    private void handleInput(float delta) {

        // If intro not free, lock movement completely
        if (introState != IntroState.FREE) {
            isMoving = false;
            return;
        }

        float move = moveSpeed * delta;
        isMoving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerY += move;
            currentDir = Direction.UP;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            playerY -= move;
            currentDir = Direction.DOWN;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerX -= move;
            currentDir = Direction.LEFT;
            isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerX += move;
            currentDir = Direction.RIGHT;
            isMoving = true;
        }

     // Enter DAW
        Rectangle playerRect = new Rectangle(playerX, playerY, 1f, 1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) && playerRect.overlaps(dawZone)) {
            hasUsedDAW = true;
            game.setScreen(game.dawScreen); // CHANGED: Reuse same screen instead of creating new one
        }
    }

    @Override
    public void resize(int w, int h) {
        worldViewport.update(w, h);
        uiViewport.update(w, h, true);
    }

    @Override
    public void dispose() {
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (playerSheet != null) playerSheet.dispose();
        if (portraitNeutral != null) portraitNeutral.dispose();
        if (shape != null) shape.dispose();
        if (font != null) font.dispose();
        if (uiFont != null) uiFont.dispose();
        if (stage != null) stage.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}