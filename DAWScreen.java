package com.badlogic.HouseScreen;

import com.badlogic.gdx.Screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class DAWScreen implements Screen {

	private Main game;
	private OrthographicCamera camera;
	private FitViewport viewport;
	private SpriteBatch batch;

	private Texture playButtonTexture;
	private boolean[][] grid = new boolean[4][4]; // 4x4 pad grid
	private Rectangle[][] padRects = new Rectangle[4][4];
	private Sound[][] padSounds = new Sound[4][4];
	private Slider[] volumeSliders = new Slider[4];
	private float timer = 0;
	private float stepInterval = 0.5f; // seconds
	private int currentStep = 0;
	private boolean isPlaying = false;
	private Texture padTex;
	private Texture highlightTex;
	private Stage uiStage;
	private Skin skin;
	private Slider tempoSlider;
	private Label bpmLabel;
	private Table uiTable;
	private Array<String>[] soundOptions = new Array[4]; // 4 rows
	private int[] selectedSoundIndex = { 0, 0, 0, 0 };
	private Label[] stepLabels = new Label[4];

	public DAWScreen(Main game) {
		this.game = game;
	}

	@Override
	public void show() {
		// Camera & Viewport
	    camera = new OrthographicCamera();
	    viewport = new FitViewport(20, 15, camera);
	    batch = game.batch;

	    // UI Stage + Input
	    uiStage = new Stage(new ScreenViewport(), batch);
	    InputMultiplexer multiplexer = new InputMultiplexer(uiStage, new InputAdapter());
	    Gdx.input.setInputProcessor(multiplexer);

	    // Skin & Core Widgets
	    skin = new Skin(Gdx.files.internal("uiskin.json"));

	    for (int i = 0; i < 4; i++) {
	        stepLabels[i] = new Label("Step " + (i + 1), skin);
	        stepLabels[i].setFontScale(1.2f);
	        stepLabels[i].setColor(Color.GRAY);
	    }

	    tempoSlider = new Slider(60, 180, 1, false, skin);
	    tempoSlider.setValue(120);
	    tempoSlider.setSize(600, 40);
	    tempoSlider.getStyle().knob.setMinHeight(30);
	    tempoSlider.getStyle().knob.setMinWidth(30);

	    bpmLabel = new Label("BPM: 120", skin);
	    bpmLabel.setFontScale(3f);

	    // Volume Sliders
	    for (int i = 0; i < 4; i++) {
	        volumeSliders[i] = new Slider(0f, 1f, 0.01f, true, skin);
	        volumeSliders[i].setValue(1f);
	        volumeSliders[i].setSize(50, 200);
	        volumeSliders[i].getStyle().knob.setMinHeight(30);
	        volumeSliders[i].getStyle().knob.setMinWidth(30);
	    }

	    // Load Sounds Automatically by Type
	    FileHandle dir = Gdx.files.internal("sounds/");
	    FileHandle[] files = dir.list();
	    for (int i = 0; i < 4; i++) soundOptions[i] = new Array<>();

	    for (FileHandle file : files) {
	        String name = file.name().toLowerCase();
	        if (name.contains("kick")) soundOptions[0].add(file.path());
	        else if (name.contains("snare")) soundOptions[1].add(file.path());
	        else if (name.contains("hihat")) soundOptions[2].add(file.path());
	        else if (name.contains("clap")) soundOptions[3].add(file.path());
	    }

	    // Mixer Table: Volume + Sound Buttons
	    Table mixerTable = new Table().left().padLeft(10);
	    for (int i = 0; i < 4; i++) {
	        Table col = new Table();
	        final int rowIndex = i;

	        col.add(volumeSliders[i]).width(40).height(200).padBottom(10).row();

	        TextButton changeBtn = new TextButton("Change", skin);
	        changeBtn.getLabel().setFontScale(0.8f);
	        changeBtn.addListener(new ClickListener() {
	            @Override
	            public void clicked(InputEvent event, float x, float y) {
	                selectedSoundIndex[rowIndex] = (selectedSoundIndex[rowIndex] + 1) % soundOptions[rowIndex].size;
	                String newSound = soundOptions[rowIndex].get(selectedSoundIndex[rowIndex]);
	                for (int xPad = 0; xPad < 4; xPad++) {
	                    padSounds[rowIndex][xPad].dispose();
	                    padSounds[rowIndex][xPad] = Gdx.audio.newSound(Gdx.files.internal(newSound));
	                }
	            }
	        });

	        col.add(changeBtn).width(60).height(30).padBottom(10);
	        mixerTable.add(col).pad(5);
	    }

	    // Create UI Table
	    uiTable = new Table();
	    uiTable.top();
	    uiTable.left();
	    uiTable.setFillParent(true);
	    uiTable.add(tempoSlider).width(600).height(40).padBottom(10).row();
	    uiTable.add(bpmLabel).center().padBottom(10).row();

	    for (int i = 0; i < 4; i++) uiTable.add(stepLabels[i]).padTop(5).padRight(40);
	    uiTable.row();
	    uiTable.add().expand(); // spacer
	    uiTable.add(mixerTable).padLeft(50);
	    uiStage.addActor(uiTable);

	    // Control Buttons (Play, Save, Load, Clear)
	    TextButton saveButton = new TextButton("Save", skin);
	    saveButton.getLabel().setFontScale(1.5f);
	    saveButton.addListener(new ClickListener() {
	        public void clicked(InputEvent e, float x, float y) {
	            saveBeat();
	        }
	    });

	    TextButton loadButton = new TextButton("Load", skin);
	    loadButton.getLabel().setFontScale(1.5f);
	    loadButton.addListener(new ClickListener() {
	        public void clicked(InputEvent e, float x, float y) {
	            loadBeat();
	        }
	    });

	    TextButton clearButton = new TextButton("Clear", skin);
	    clearButton.addListener(new ClickListener() {
	        public void clicked(InputEvent e, float x, float y) {
	            for (int y1 = 0; y1 < 4; y1++) {
	                for (int x1 = 0; x1 < 4; x1++) {
	                    grid[y1][x1] = false;
	                }
	            }
	        }
	    });

	    TextButton playPauseButton = new TextButton("Play", skin);
	    playPauseButton.getLabel().setFontScale(2f);
	    playPauseButton.addListener(new ClickListener() {
	    	public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
	            playPauseButton.getLabel().setColor(Color.YELLOW); // or change background
	        }

	        @Override
	        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
	            playPauseButton.getLabel().setColor(Color.WHITE);
	        }
	    });

	    // Add buttons to the bottom row
	 // Group buttons horizontally
	    HorizontalGroup buttonGroup = new HorizontalGroup();
	    buttonGroup.space(15f); // space between buttons
	    buttonGroup.addActor(saveButton);
	    buttonGroup.addActor(loadButton);
	    buttonGroup.addActor(clearButton);
	    buttonGroup.addActor(playPauseButton);

	    // Add to table with padding
	    uiTable.row();
	    uiTable.add().expandX(); // push to right
	    uiTable.add(buttonGroup).right().padTop(30).padRight(20);

	    // Pad Textures
	    padTex = new Texture("pad.png");
	    Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
	    p.setColor(Color.WHITE);
	    p.fill();
	    highlightTex = new Texture(p);
	    p.dispose();

	    // Pad Positions
	    float padSize = 2.5f, padSpacing = 0.5f;
	    float totalWidth = 4 * padSize + 3 * padSpacing;
	    float totalHeight = 4 * padSize + 3 * padSpacing;
	    float startX = (totalHeight - totalWidth) / 2f;
	    float startY = (totalHeight - totalHeight) / 2f;

	    for (int i = 0; i < 4; i++)
	        if (soundOptions[i].size == 0)
	            throw new GdxRuntimeException("Missing sounds for row " + i);

	    for (int y = 0; y < 4; y++) {
	        for (int x = 0; x < 4; x++) {
	            float xPos = startX + x * (padSize + padSpacing);
	            float yPos = startY + (3 - y) * (padSize + padSpacing);
	            padRects[y][x] = new Rectangle(xPos, yPos, padSize, padSize);
	            String soundFile = soundOptions[y].get(selectedSoundIndex[y]);
	            padSounds[y][x] = Gdx.audio.newSound(Gdx.files.internal(soundFile));
	        }
	    }
	}

	public void render(float delta) {
		stepInterval = 60f / tempoSlider.getValue() / 4f;
		bpmLabel.setText("BPM: " + (int) tempoSlider.getValue());

		// Sync viewport with screen size
		uiStage.getViewport().apply(true);
		((ScreenViewport) uiStage.getViewport()).setUnitsPerPixel(1f);

		uiStage.act(delta);
		uiStage.getViewport().setScreenSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		uiStage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

		if (isPlaying) {
			timer += delta;
			if (timer >= stepInterval) {
				timer = 0;
				playCurrentStep();
				currentStep = (currentStep + 1) % 4;
				for (int i = 0; i < 4; i++) {
					if (i == currentStep)
						stepLabels[i].setColor(Color.WHITE);
					else
						stepLabels[i].setColor(Color.GRAY);
				}
			}
		}

		handleInput();

		Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		viewport.apply();
		camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
		camera.update();
		batch.setProjectionMatrix(camera.combined);

		batch.begin();
		drawPadsAndPlayButton();
		batch.end();

		uiStage.draw();
	}

	private void drawPadsAndPlayButton() {
		Vector3 mousePos = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
		// Draw pads
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				Rectangle rect = padRects[y][x];
				Color rowColor;
				switch (y) {
				case 0:
					rowColor = Color.RED;
					break; // Kicks
				case 1:
					rowColor = Color.BLUE;
					break; // Snares
				case 2:
					rowColor = Color.GREEN;
					break; // Hi-hats
				case 3:
					rowColor = Color.YELLOW;
					break; // Percs
				default:
					rowColor = Color.GRAY;
				}
				batch.setColor(grid[y][x] ? rowColor : Color.DARK_GRAY);
				batch.draw(padTex, rect.x, rect.y, rect.width, rect.height);

				if (x == currentStep) {
					batch.setColor(1, 1, 1, 0.25f);
					batch.draw(highlightTex, rect.x, rect.y, rect.width, rect.height);
					if (rect.contains(mousePos.x, mousePos.y)) {
					    batch.setColor(1, 1, 1, 0.15f); // subtle hover white
					    batch.draw(highlightTex, rect.x, rect.y, rect.width, rect.height);
				}
			}
		}
		}
	
	}

	private void handleInput() {
	    if (Gdx.input.justTouched()) {
	        // Let UI handle its widgets first
	        if (uiStage.hit(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), true) != null)
	            return;

	        // Convert screen coords to world coords
	        Vector3 worldClick = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

	        for (int y = 0; y < 4; y++) {
	            for (int x = 0; x < 4; x++) {
	                Rectangle rect = padRects[y][x];

	                // Expand hitbox slightly (optional)
	                Rectangle expanded = new Rectangle(rect.x - 0.1f, rect.y - 0.1f, rect.width + 0.2f, rect.height + 0.2f);

	                if (expanded.contains(worldClick.x, worldClick.y)) {
	                    grid[y][x] = !grid[y][x];
	                    if (grid[y][x]) padSounds[y][x].play();
	                    return;
	                }
	            }
	        }
	    }
	}
	// Play button

	// private void handleWorldClick(float x, float y) {
	// TODO Auto-generated method stub

	private void playCurrentStep() {
		for (int y = 0; y < 4; y++) {
			if (grid[y][currentStep]) {
				float volume = volumeSliders[y].getValue(); // get row volume
				padSounds[y][currentStep].play(volume);
			}
		}
	}

	public void resize(int width, int height) {
		viewport.update(width, height, true);
		uiStage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {

		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				padSounds[y][x].dispose();
		padTex.dispose();
		highlightTex.dispose();
		uiStage.dispose();
		skin.dispose();
	}
	private void saveBeat() {
	    SaveData data = new SaveData();
	    data.grid = grid;
	    data.selectedSoundIndex = selectedSoundIndex;
	    data.tempo = tempoSlider.getValue();

	    Json json = new Json();
	    FileHandle file = Gdx.files.local("beat.json");
	    file.writeString(json.prettyPrint(data), false);
	    System.out.println("Beat saved.");
	}

	private void loadBeat() {
	    FileHandle file = Gdx.files.local("beat.json");
	    if (!file.exists()) {
	        System.out.println("No saved beat found.");
	        return;
	    }

	    Json json = new Json();
	    SaveData data = json.fromJson(SaveData.class, file);

	    this.grid = data.grid;
	    this.selectedSoundIndex = data.selectedSoundIndex;
	    tempoSlider.setValue(data.tempo);

	    // Reload sounds
	    for (int y = 0; y < 4; y++) {
	        String soundPath = soundOptions[y].get(selectedSoundIndex[y]);
	        for (int x = 0; x < 4; x++) {
	            padSounds[y][x].dispose();
	            padSounds[y][x] = Gdx.audio.newSound(Gdx.files.internal(soundPath));
	        }
	    }

	    System.out.println("Beat loaded.");
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
	}
}