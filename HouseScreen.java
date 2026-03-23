package com.badlogic.HouseScreen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;


public class HouseScreen implements Screen {
	private enum Direction { UP, DOWN, LEFT, RIGHT }

    private static final float TILE_SIZE = 32f;
    private static final float UNIT_SCALE = 1f / TILE_SIZE;
    private static final float FOOT_OFFSET = 0.12f;  // tune 0.08–0.18

    private final Main game;

    // --- Map / renderer ---
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private int mapW, mapH; // tiles

    // --- World camera ---
    private OrthographicCamera worldCamera;
    private ExtendViewport worldViewport;

    // --- UI camera for day/night overlay ---
    private OrthographicCamera uiCamera;
    private ScreenViewport uiViewport;

    // --- Player ---
    private Texture playerSheet;
    private Animation<TextureRegion> walkUp, walkDown, walkLeft, walkRight;
    private TextureRegion idleUp, idleDown, idleLeft, idleRight;
    private float animTimer = 0f;

    private Direction currentDir = Direction.DOWN;
    private boolean isMoving = false;

    private final Vector2 playerPos = new Vector2();      // tile units
    private final Rectangle playerRect = new Rectangle(); // tile units (reused)

    private float playerSpeed = 3f; // tiles/sec

    // --- Door / interaction ---
    private final Rectangle dawZone = new Rectangle(); // tile units
    private boolean doorIsOpen = false;
    private boolean hasEnteredInterior = false;

    // --- Day/Night ---
    private DayNightCycle dayNight;
    private Texture whitePixel;

    public HouseScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        SpriteBatch batch = game.batch;

        // --- DAY/NIGHT ---
        dayNight = new DayNightCycle();
        whitePixel = new Texture("white.png"); // 1x1 white pixel

        // --- MAP ---
        map = new TmxMapLoader().load("WorldMap.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, UNIT_SCALE);

        MapProperties props = map.getProperties();
        mapW = props.get("width", Integer.class);
        mapH = props.get("height", Integer.class);

        // --- CAMERAS ---
        worldCamera = new OrthographicCamera();
        // Zoomed out: tune these two numbers to taste
        worldViewport = new ExtendViewport(24f, 14f, worldCamera);
        worldViewport.apply(true);

        uiCamera = new OrthographicCamera();
        uiViewport = new ScreenViewport(uiCamera);
        uiViewport.apply(true);

        // --- PLAYER SHEET ---
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

        // --- SPAWN ---
        setSpawnFromLayer();

        // --- DOOR / INTERACTION BOUNDS ---
        setDoorBoundsFromLayer();

        // Door layer visibility at start
        MapLayer closedLayer = map.getLayers().get("Closed Door");
        MapLayer openLayer   = map.getLayers().get("Open Door");
        if (closedLayer != null) closedLayer.setVisible(true);
        if (openLayer != null) openLayer.setVisible(false);

        updateCameraToPlayer();
    }

    private void setSpawnFromLayer() {
        // Fallback: center of map
        playerPos.set(mapW / 2f, mapH / 2f);

        MapLayer spawnLayer = map.getLayers().get("PlayerSpawn");
        if (spawnLayer == null) return;

        Rectangle spawnRect = null;

        // Prefer named "spawn"
        MapObject named = spawnLayer.getObjects().get("spawn");
        if (named instanceof RectangleMapObject) {
            spawnRect = ((RectangleMapObject) named).getRectangle();
        } else {
            // Otherwise first rectangle
            for (MapObject obj : spawnLayer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    spawnRect = ((RectangleMapObject) obj).getRectangle();
                    break;
                }
            }
        }

        if (spawnRect != null) {
            playerPos.set(spawnRect.x / TILE_SIZE, spawnRect.y / TILE_SIZE);
        }
    }

    private void setDoorBoundsFromLayer() {
        MapLayer doorLayer = map.getLayers().get("Interaction");
        if (doorLayer == null) return;

        for (MapObject obj : doorLayer.getObjects()) {
            if (obj instanceof RectangleMapObject) {
                Rectangle r = ((RectangleMapObject) obj).getRectangle();
                dawZone.set(
                        r.x / TILE_SIZE,
                        r.y / TILE_SIZE,
                        r.width / TILE_SIZE,
                        r.height / TILE_SIZE
                );
                return;
            }
        }

        // If no rectangle present, keep it zeroed
        dawZone.set(0, 0, 0, 0);
    }

    private void handleInput(float delta) {
        dayNight.update(delta);

        float move = playerSpeed * delta;
        isMoving = false;

        float dx = 0f, dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            dy += move; currentDir = Direction.UP; isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            dy -= move; currentDir = Direction.DOWN; isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= move; currentDir = Direction.LEFT; isMoving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += move; currentDir = Direction.RIGHT; isMoving = true;
        }

        // Axis-separated movement (Stardew feel)
        tryMove(dx, 0f);
        tryMove(0f, dy);

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            playerRect.set(playerPos.x, playerPos.y, 1f, 1f);
            if (playerRect.overlaps(dawZone)) {
                toggleDoorAndEnter();
            }
        }
    }

    private void tryMove(float dx, float dy) {
        if (dx == 0f && dy == 0f) return;

        float newX = playerPos.x + dx;
        float newY = playerPos.y + dy;

        Rectangle nextRect = playerRect.set(newX, newY, 1f, 1f);

        if (!isRectBlocked(nextRect)) {
            playerPos.set(newX, newY);
        }
    }

    private boolean isRectBlocked(Rectangle r) {
        // out of bounds
        if (r.x < 0 || r.y < 0 || r.x + r.width > mapW || r.y + r.height > mapH) {
            return true;
        }

        // check 4 corners against tile props
        int left   = MathUtils.floor(r.x);
        int right  = MathUtils.floor(r.x + r.width  - 0.001f);
        int bottom = MathUtils.floor(r.y);
        int top    = MathUtils.floor(r.y + r.height - 0.001f);

        if (isCellBlocked(left, bottom)) return true;
        if (isCellBlocked(right, bottom)) return true;
        if (isCellBlocked(left, top)) return true;
        if (isCellBlocked(right, top)) return true;

        // optional object collision layer
        MapLayer collisionLayer = map.getLayers().get("Collision");
        if (collisionLayer != null) {
            for (MapObject obj : collisionLayer.getObjects()) {
                if (obj instanceof RectangleMapObject) {
                    Rectangle cr = ((RectangleMapObject) obj).getRectangle();
                    Rectangle tileRect = new Rectangle(
                            cr.x / TILE_SIZE,
                            cr.y / TILE_SIZE,
                            cr.width / TILE_SIZE,
                            cr.height / TILE_SIZE
                    );
                    if (r.overlaps(tileRect)) return true;
                }
            }
        }

        return false;
    }

    private boolean isCellBlocked(int x, int y) {
        if (x < 0 || y < 0 || x >= mapW || y >= mapH) return true;

        for (MapLayer layer : map.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer)) continue;

            TiledMapTileLayer tLayer = (TiledMapTileLayer) layer;
            TiledMapTileLayer.Cell cell = tLayer.getCell(x, y);
            if (cell == null || cell.getTile() == null) continue;

            MapProperties p = cell.getTile().getProperties();
            if (p.containsKey("blocked") || p.containsKey("collides") || p.containsKey("solid")) {

                Object v =
                        p.containsKey("blocked")  ? p.get("blocked") :
                        p.containsKey("collides") ? p.get("collides") :
                                                    p.get("solid");

                if (v == null) return true;
                if (v instanceof Boolean && (Boolean) v) return true;
                if (v instanceof String && ((String) v).equalsIgnoreCase("true")) return true;
            }
        }

        return false;
    }

    private void toggleDoorAndEnter() {
        MapLayer openLayer   = map.getLayers().get("Open Door");
        MapLayer closedLayer = map.getLayers().get("Closed Door");

        if (doorIsOpen) {
            if (openLayer != null) openLayer.setVisible(false);
            if (closedLayer != null) closedLayer.setVisible(true);
            doorIsOpen = false;
            return;
        }

        if (openLayer != null) openLayer.setVisible(true);
        if (closedLayer != null) closedLayer.setVisible(false);
        doorIsOpen = true;

        if (!hasEnteredInterior) {
            hasEnteredInterior = true;
            game.setScreen(new InteriorScreen(game));
        }
    }

    private void updateCameraToPlayer() {
        float halfW = worldCamera.viewportWidth / 2f;
        float halfH = worldCamera.viewportHeight / 2f;

        worldCamera.position.set(
                MathUtils.clamp(playerPos.x, halfW, mapW - halfW),
                MathUtils.clamp(playerPos.y, halfH, mapH - halfH),
                0f
        );
        worldCamera.update();
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        updateCameraToPlayer();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --- WORLD ---
        worldViewport.apply();
        mapRenderer.setView(worldCamera);
        mapRenderer.render();

        // Player frame
        animTimer += delta;
        TextureRegion frame;

        if (isMoving) {
            switch (currentDir) {
                case UP:    frame = walkUp.getKeyFrame(animTimer, true); break;
                case DOWN:  frame = walkDown.getKeyFrame(animTimer, true); break;
                case LEFT:  frame = walkLeft.getKeyFrame(animTimer, true); break;
                case RIGHT: frame = walkRight.getKeyFrame(animTimer, true); break;
                default:    frame = idleDown; break;
            }
        } else {
            switch (currentDir) {
                case UP:    frame = idleUp; break;
                case DOWN:  frame = idleDown; break;
                case LEFT:  frame = idleLeft; break;
                case RIGHT: frame = idleRight; break;
                default:    frame = idleDown; break;
            }
        }

     // Draw player in world space
        SpriteBatch batch = game.batch;
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // 1) soft ground shadow (use whitePixel, not the sprite)
        batch.setColor(0f, 0f, 0f, 0.25f);
        batch.draw(
                whitePixel,
                playerPos.x + 0.2f,                      // center shadow under feet
                playerPos.y - FOOT_OFFSET - 0.05f,
                0.6f,                                    // shadow width
                0.18f                                    // shadow height
        );

        // 2) actual player, grounded with FOOT_OFFSET
        batch.setColor(Color.WHITE);
        batch.draw(frame, playerPos.x, playerPos.y - FOOT_OFFSET, 1f, 1f);

        batch.end();
    }
    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (playerSheet != null) playerSheet.dispose();
        if (whitePixel != null) whitePixel.dispose();
    }
}