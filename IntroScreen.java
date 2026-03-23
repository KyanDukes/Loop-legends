package com.badlogic.HouseScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class IntroScreen implements Screen {
	private final Main game;

	private OrthographicCamera camera;
	private FitViewport viewport;
	private SpriteBatch batch;

	private BitmapFont titleFont;
	private BitmapFont uiFont;
	private GlyphLayout layout = new GlyphLayout();

	private Texture logo; // assets/logo.png (optional)

	private float blinkTimer = 0f;
	private float fade = 0f; // 0 visible -> 1 black
	private boolean startRequested = false;

	// Button bounds (computed each frame since font scale may change with viewport)
	private float optionsX, optionsY, optionsW, optionsH;
	private float quitX, quitY, quitW, quitH;

	private static final float VIRTUAL_WIDTH = 1920f / 64f; // match world units vibe
	private static final float VIRTUAL_HEIGHT = 1080f / 64f;

	private enum State {
		TITLE, OPTIONS, FADING
	}

	private State state = State.TITLE;

	public IntroScreen(Main game) {
		this.game = game;
	}

	@Override
	public void dispose() {
		if (logo != null)
			logo.dispose();
		if (titleFont != null)
			titleFont.dispose();
		if (uiFont != null)
			uiFont.dispose();
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(float delta) {
		   handleInput(delta);
		    update(delta);

		    Gdx.gl.glClearColor(0.06f, 0.07f, 0.09f, 1f);
		    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		    camera.update();
		    batch.setProjectionMatrix(camera.combined);
		    batch.begin();

		    float worldW = viewport.getWorldWidth();
		    float worldH = viewport.getWorldHeight();
		    float centerX = worldW / 2f;

		    // --- Draw Logo (if any) ---
		    if (logo != null) {
		        float desiredHeight = worldH * 0.65f;
		        float aspect = (float) logo.getWidth() / (float) logo.getHeight();
		        float w = desiredHeight * aspect;
		        float h = desiredHeight;
		        float y = worldH * 0.62f - h / 2f;
		        batch.draw(logo, centerX - w / 2f, y, w, h);
		    } else {
		        String title = "Loop Legends";
		        titleFont.getData().setScale(0.04f);
		        layout.setText(titleFont, title);
		        titleFont.draw(batch, title, centerX - layout.width / 2f, worldH * 0.68f);
		    }

		    // --- Prompt ---
		    String prompt = (state == State.TITLE) ? "Click anywhere to begin"
		                     : (state == State.OPTIONS) ? "Click to close Options" : "Loading...";
		    float alpha = 0.5f + 0.5f * (float)Math.sin(blinkTimer * 4.0f);
		    uiFont.getData().setScale(0.02f);
		    uiFont.setColor(1f, 1f, 1f, alpha);
		    layout.setText(uiFont, prompt);
		    uiFont.draw(batch, prompt, centerX - layout.width / 2f, worldH * 0.28f);
		    uiFont.setColor(Color.WHITE);

		    // --- Menu items: Options & Quit (bottom area) ---
		    float menuY = worldH * 0.18f;
		    float gap = worldW * 0.04f;

		    // OPTIONS
		    String options = "Options";
		    uiFont.getData().setScale(0.02f);
		    layout.setText(uiFont, options);
		    optionsW = layout.width;
		    optionsH = layout.height;
		    optionsX = centerX - optionsW - gap * 0.5f;
		    optionsY = menuY;

		    // QUIT
		    String quit = "Quit";
		    layout.setText(uiFont, quit);
		    quitW = layout.width;
		    quitH = layout.height;
		    quitX = centerX + gap * 0.5f;
		    quitY = menuY;

		    // Hover effect
		    Vector3 m = getMouseInWorld();
		    boolean overOptions = isOver(optionsX, optionsY, optionsW, optionsH, m.x, m.y);
		    boolean overQuit    = isOver(quitX,    quitY,    quitW,    quitH,    m.x, m.y);

		    uiFont.setColor(overOptions ? Color.SKY : Color.WHITE);
		    uiFont.draw(batch, options, optionsX, optionsY);

		    uiFont.setColor(overQuit ? Color.SKY : Color.WHITE);
		    uiFont.draw(batch, quit,    quitX,    quitY);

		    uiFont.setColor(Color.WHITE);

		    // --- Options overlay (placeholder) ---
		    if (state == State.OPTIONS) {
		        Texture pixel = Pixel.get();
		        batch.setColor(0f, 0f, 0f, 0.7f);
		        batch.draw(pixel, worldW * 0.2f, worldH * 0.25f, worldW * 0.6f, worldH * 0.4f);
		        batch.setColor(1f, 1f, 1f, 1f);

		        String optTitle = "Options";
		        uiFont.getData().setScale(0.025f);
		        layout.setText(uiFont, optTitle);
		        uiFont.draw(batch, optTitle, centerX - layout.width / 2f, worldH * 0.60f);

		        uiFont.getData().setScale(0.018f);
		        String msg1 = "(Placeholder) Press ESC to Quit game. Click anywhere to close.";
		        String msg2 = "Future: Music/SFX volume, fullscreen, keybinds.";
		        layout.setText(uiFont, msg1); uiFont.draw(batch, msg1, centerX - layout.width / 2f, worldH * 0.52f);
		        layout.setText(uiFont, msg2); uiFont.draw(batch, msg2, centerX - layout.width / 2f, worldH * 0.47f);
		    }

		    // --- Fade overlay ---
		    if (fade > 0f) {
		        Texture pixel = Pixel.get();
		        batch.setColor(0f, 0f, 0f, Math.min(1f, fade));
		        batch.draw(pixel, 0, 0, worldW, worldH);
		        batch.setColor(1f, 1f, 1f, 1f);
		    }

		    batch.end(); // <<< IMPORTANT: close the batch!
		}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void show() {
		batch = game.batch;

		camera = new OrthographicCamera();
		viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
		camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
		camera.update();

		titleFont = new BitmapFont();
		uiFont = new BitmapFont();

		try {
			logo = new Texture("logo.png");
		} catch (Exception ignored) {
			logo = null;
		}

		Gdx.input.setInputProcessor(null); // we poll input
	}

	private void requestStart() {
		if (!startRequested) {
			startRequested = true;
			state = State.FADING;
		}
	}

	private boolean isOver(float x, float y, float w, float h, float mx, float my) {
		return mx >= x && mx <= x + w && my >= y - h && my <= y; // y is baseline top; h is ascent+descent
	}

	private Vector3 getMouseInWorld() {
		Vector3 v = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		viewport.unproject(v);
		return v;
	}

	private void handleInput(float delta) {
		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
			if (state == State.OPTIONS) {
				state = State.TITLE;
				return;
			}
			Gdx.app.exit();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.O))
			state = State.OPTIONS;
		if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
			requestStart();

		// Mouse / touch
		if (Gdx.input.justTouched()) {
			Vector3 m = getMouseInWorld();

			if (state == State.OPTIONS) {
				// Click anywhere outside the options panel goes back to TITLE
				state = State.TITLE;
				return;
			}

			// If click hits Options or Quit, trigger those; else start
			if (isOver(optionsX, optionsY, optionsW, optionsH, m.x, m.y)) {
				state = State.OPTIONS;
				return;
			}
			if (isOver(quitX, quitY, quitW, quitH, m.x, m.y)) {
				Gdx.app.exit();
				return;
			}
			requestStart();
		}
	}

	private void update(float delta) {
		blinkTimer += delta;
		if (startRequested) {
			fade += delta * 2.0f; // ~0.5s fade
			if (fade >= 1f) {
				fade = 1f;
				game.setScreen(new HouseScreen(game));
			}
		}
	}

	public static class Pixel {
		private static Texture instance;

		public static Texture get() {
			if (instance == null) {
				Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
				pm.setColor(1, 1, 1, 1);
				pm.fill();
				instance = new Texture(pm);
				pm.dispose();
			}
			return instance;
		}
	}
}