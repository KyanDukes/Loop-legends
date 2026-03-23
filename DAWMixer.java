package com.beatmaker.game.daw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

public class DAWMixer {

    private static final int ROWS = DAWGrid.ROWS;

    private final Skin skin;

    private final Sound[] sounds = new Sound[ROWS];
    private final float[] volume = new float[ROWS];
    private final float[] pan = new float[ROWS];
    private final float[] pitch = new float[ROWS];
    private final boolean[] mute = new boolean[ROWS];
    private final boolean[] solo = new boolean[ROWS];
    private final float[] attack = new float[ROWS];
    private final float[] decay = new float[ROWS];
    private final float[] sustain = new float[ROWS];
    private final float[] release = new float[ROWS];

    // Volume / Pan / Pitch
    public float[] getVolumeArray() { return volume; }
    public float[] getPanArray() { return pan; }
    public float[] getPitchArray() { return pitch; }

    // ADSR
    public float[] getAttackArray() { return attack; }
    public float[] getDecayArray() { return decay; }
    public float[] getSustainArray() { return sustain; }
    public float[] getReleaseArray() { return release; }

    // Sample indexes
    public int[] getSelectedIndexes() { return selectedIndex; }
    
    private float masterVolume = 1f;

    // sample options per row
    private final Array<String> allSamples = new Array<String>();
    private final Array<String>[] samplesPerRow = new Array[ROWS];
    private final int[] selectedIndex = new int[ROWS];

    public DAWMixer(Skin skin) {
        this.skin = skin;

        for (int r = 0; r < ROWS; r++) {
            volume[r] = 0.9f;
            pan[r] = 0f;
            pitch[r] = 1f;
            mute[r] = false;
            solo[r] = false;

            attack[r] = 0f;
            decay[r] = 0f;
            sustain[r] = 1f; // IMPORTANT — drums must have sustain > 0
            release[r] = 0f;

            samplesPerRow[r] = new Array<>();
            selectedIndex[r] = 0;
        }
        
        // Initialize samples
        indexSamples();
    }

    // ---------------------------------------------------------
    // Sample indexing / assignment
    // ---------------------------------------------------------
    private void indexSamples() {
        FileHandle dir = Gdx.files.internal("Sounds/");
        if (!dir.exists()) {
            System.out.println("SOUND FOLDER NOT FOUND");
            return;
        }

        allSamples.clear();
        for (FileHandle f : dir.list()) {
            System.out.println("FOUND SAMPLE: " + f.name());

            String name = f.name().toLowerCase();

            if (name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".ogg")) {
                allSamples.add("Sounds/" + f.name());
            }
        }

        System.out.println("TOTAL SAMPLES LOADED: " + allSamples.size);
        
        defaultAssignSamples();
        reloadAllSounds();
    }

    // Assign by keyword if possible, else fallback sequential
    private void defaultAssignSamples() {
        // Clear existing assignments
        for (int r = 0; r < ROWS; r++) {
            samplesPerRow[r].clear();
        }
        
        for (String path : allSamples) {
            String n = path.toLowerCase();
            if (n.contains("kick")) samplesPerRow[0].add(path);
            else if (n.contains("snare")) samplesPerRow[1].add(path);
            else if (n.contains("hat") || n.contains("hihat")) samplesPerRow[2].add(path);
            else if (n.contains("clap") || n.contains("perc")) samplesPerRow[3].add(path);
        }

        // fill remaining rows with all samples if empty
        for (int r = 0; r < ROWS; r++) {
            if (samplesPerRow[r].size == 0) {
                samplesPerRow[r].addAll(allSamples);
            }
        }
    }

    public void reloadAllSounds() {
        for (int r = 0; r < ROWS; r++) {
            disposeRow(r);

            if (samplesPerRow[r].size == 0) continue;
            int idx = clampInt(selectedIndex[r], 0, samplesPerRow[r].size - 1);
            String path = samplesPerRow[r].get(idx);

            sounds[r] = Gdx.audio.newSound(Gdx.files.internal(path));
            
            // CRITICAL TIMING FIX: Prime the sound to reduce first-play latency
            // Play at zero volume then immediately stop to "warm up" the audio pipeline
            if (sounds[r] != null) {
                try {
                    long id = sounds[r].play(0f);
                    sounds[r].stop(id);
                } catch (Exception e) {
                    System.out.println("Warning: Could not prime sound for row " + r);
                }
            }
        }
        
        System.out.println("All sounds loaded and primed!");
    }

    private void disposeRow(int r) {
        if (sounds[r] != null) {
            try { 
                sounds[r].stop(); 
                sounds[r].dispose(); 
            } catch(Exception ignored) {}
            sounds[r] = null;
        }
    }

    private int clampInt(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    // ---------------------------------------------------------
    // Playback - OPTIMIZED FOR TIMING
    // ---------------------------------------------------------
    public void playPadHit(int row, float velocity) {
        if (!rowAudible(row) || sounds[row] == null) return;

        float shaped = applyADSR(row, velocity);

        // Play immediately - no buffering
        sounds[row].play(
            masterVolume * volume[row] * shaped,
            pitch[row],
            pan[row]
        );
    }

    private boolean rowAudible(int row) {
        // If muted, not audible
        if (mute[row]) return false;

        // If ANY solo is active, only solo tracks can play
        boolean anySolo = false;
        for (boolean s : solo) if (s) anySolo = true;

        if (anySolo) return solo[row];

        // Normal condition
        return true;
    }

    public void playStep(DAWGrid grid, int step, DAWTransport transport) {
        for (int r = 0; r < ROWS; r++) {
            if (!rowAudible(r) || sounds[r] == null) continue;

            if (grid.isSet(r, step)) {
                float vel = grid.getVelocity(r, step);
                float shaped = applyADSR(r, vel);

                // Play immediately with no delay
                sounds[r].play(
                    masterVolume * volume[r] * shaped,
                    pitch[r],
                    pan[r]
                );
            }
        }
    }

    // ---------------------------------------------------------
    // Controls
    // ---------------------------------------------------------
    public void setMasterVolume(float v) { masterVolume = clamp(v, 0f, 1f); }
    public float getMasterVolume() { return masterVolume; }

    public void setVolume(int row, float v) { volume[row] = clamp(v, 0f, 1f); }
    public void setPan(int row, float p) { pan[row] = clamp(p, -1f, 1f); }
    public void setPitch(int row, float p) { pitch[row] = clamp(p, 0.5f, 2f); }

    public float getVolume(int row) { return volume[row]; }
    public float getPan(int row) { return pan[row]; }
    public float getPitch(int row) { return pitch[row]; }

    public void toggleMute(int row) { mute[row] = !mute[row]; }
    public void toggleSolo(int row) { solo[row] = !solo[row]; }

    private float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    // ---------------------------------------------------------
    // UI builder (8 channels + master)
    // ---------------------------------------------------------
    public Table buildMixerTable() {
        Table root = new Table();
        root.pad(25);
        root.defaults().pad(8);

        // ----------------------------------------------------
        // Title
        // ----------------------------------------------------
        Label title = new Label("MIXER", skin);
        title.setFontScale(1.2f);
        root.add(title).colspan(8).padBottom(20);
        root.row();

        // ----------------------------------------------------
        // MASTER VOLUME
        // ----------------------------------------------------
        root.add(new Label("Master Volume", skin)).left();

        final Slider masterSlider = new Slider(0f, 1f, 0.01f, false, skin);
        masterSlider.setValue(masterVolume);
        masterSlider.addListener(new ChangeListener() {
            @Override 
            public void changed(ChangeEvent event, Actor actor) {
                setMasterVolume(masterSlider.getValue());
            }
        });
        root.add(masterSlider).width(300).colspan(7).padBottom(20);
        root.row();

        // ----------------------------------------------------
        // HEADER BAR
        // ----------------------------------------------------
        root.add(new Label("Row", skin)).padRight(10);
        root.add(new Label("ADSR", skin)).padRight(20);
        root.add(new Label("Sample", skin)).padRight(20);
        root.add(new Label("Vol", skin)).padRight(20);
        root.add(new Label("Pan", skin)).padRight(20);
        root.add(new Label("Pitch", skin)).padRight(20);
        root.add(new Label("M", skin)).padRight(10);
        root.add(new Label("S", skin));
        root.row();

        // ----------------------------------------------------
        // CHANNEL STRIP ROWS
        // ----------------------------------------------------
        String[] names = {
            "Kick", "Snare", "HiHat", "Clap",
            "Row5", "Row6", "Row7", "Row8"
        };

        for (int r = 0; r < ROWS; r++) {
            final int row = r;

            // 1. NAME
            root.add(new Label(names[r], skin)).left().padRight(10);

            // 2. ADSR CONTROLS
            Table adsrTable = new Table();

            // A
            final Slider atkSlider = new Slider(0f, 0.5f, 0.001f, false, skin);
            atkSlider.setValue(attack[row]);
            atkSlider.addListener(e -> { attack[row] = atkSlider.getValue(); return false; });
            adsrTable.add(new Label("A", skin)).padRight(4);
            adsrTable.add(atkSlider).width(60).padRight(8);

            // D
            final Slider decSlider = new Slider(0f, 1.0f, 0.01f, false, skin);
            decSlider.setValue(decay[row]);
            decSlider.addListener(e -> { decay[row] = decSlider.getValue(); return false; });
            adsrTable.add(new Label("D", skin)).padRight(4);
            adsrTable.add(decSlider).width(60).padRight(8);

            // S
            final Slider susSlider = new Slider(0f, 1.0f, 0.01f, false, skin);
            susSlider.setValue(sustain[row]);
            susSlider.addListener(e -> { sustain[row] = susSlider.getValue(); return false; });
            adsrTable.add(new Label("S", skin)).padRight(4);
            adsrTable.add(susSlider).width(60).padRight(8);

            // R
            final Slider relSlider = new Slider(0f, 1.0f, 0.01f, false, skin);
            relSlider.setValue(release[row]);
            relSlider.addListener(e -> { release[row] = relSlider.getValue(); return false; });
            adsrTable.add(new Label("R", skin)).padRight(4);
            adsrTable.add(relSlider).width(60);

            root.add(adsrTable).padRight(20);

            // 3. SAMPLE SELECTBOX
            SelectBox<String> sampleBox = new SelectBox<>(skin);
            if (samplesPerRow[r].size > 0) {
                sampleBox.setItems(samplesPerRow[r]);
                sampleBox.setSelectedIndex(selectedIndex[r]);
            } else {
                Array<String> noSamples = new Array<>();
                noSamples.add("NO SAMPLES FOUND");
                sampleBox.setItems(noSamples);
            }

            sampleBox.addListener(new ChangeListener() {
                @Override 
                public void changed(ChangeEvent event, Actor actor) {
                    selectedIndex[row] = sampleBox.getSelectedIndex();
                    reloadAllSounds();
                }
            });
            root.add(sampleBox).width(160).padRight(20);

            // 4. VOLUME
            Slider volSlider = new Slider(0f, 1f, 0.01f, false, skin);
            volSlider.setValue(volume[row]);
            volSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    volume[row] = volSlider.getValue();
                }
            });
            root.add(volSlider).width(120).padRight(20);

            // 5. PAN
            Slider panSlider = new Slider(-1f, 1f, 0.01f, false, skin);
            panSlider.setValue(pan[row]);
            panSlider.addListener(new ChangeListener() {
                @Override 
                public void changed(ChangeEvent event, Actor actor) {
                    pan[row] = panSlider.getValue();
                }
            });
            root.add(panSlider).width(120).padRight(20);

            // 6. PITCH
            Slider pitchSlider = new Slider(0.5f, 2f, 0.01f, false, skin);
            pitchSlider.setValue(pitch[row]);
            pitchSlider.addListener(new ChangeListener() {
                @Override 
                public void changed(ChangeEvent event, Actor actor) {
                    pitch[row] = pitchSlider.getValue();
                }
            });
            root.add(pitchSlider).width(120).padRight(20);

            // 7. MUTE
            TextButton muteBtn = new TextButton("M", skin);
            muteBtn.setColor(mute[row] ? Color.RED : Color.WHITE);

            muteBtn.addListener(new ClickListener() {
                @Override 
                public void clicked(InputEvent event, float x, float y) {
                    toggleMute(row);
                    muteBtn.setColor(mute[row] ? Color.RED : Color.WHITE);
                }
            });

            root.add(muteBtn).width(40).padRight(10);

            // 8. SOLO
            TextButton soloBtn = new TextButton("S", skin);
            soloBtn.setColor(solo[row] ? Color.GREEN : Color.WHITE);

            soloBtn.addListener(new ClickListener() {
                @Override 
                public void clicked(InputEvent event, float x, float y) {
                    toggleSolo(row);
                    soloBtn.setColor(solo[row] ? Color.GREEN : Color.WHITE);
                }
            });

            root.add(soloBtn).width(40);

            root.row();
        }

        return root;
    }

    private float applyADSR(int row, float velocity) {
        float A = attack[row];
        float D = decay[row];
        float S = sustain[row];

        // Simplified ADSR for drums - instant response
        // For drums, we mostly just use sustain level
        float amp = S;

        // If you have attack/decay set, apply a simple multiplier
        if (A > 0f || D > 0f) {
            // Reduce amplitude slightly if attack/decay are used
            amp *= 0.9f;
        }

        // Final amplitude scaled by hit velocity
        return amp * velocity;
    }

    public void dispose() {
        for (int r = 0; r < ROWS; r++) disposeRow(r);
    }

    public int[] exportSampleIndexes() { return selectedIndex.clone(); }
    public float[] exportVolumes() { return volume.clone(); }
    public float[] exportPans() { return pan.clone(); }
    public float[] exportPitches() { return pitch.clone(); }

    public float[] exportAttack() { return attack.clone(); }
    public float[] exportDecay() { return decay.clone(); }
    public float[] exportSustain() { return sustain.clone(); }
    public float[] exportRelease() { return release.clone(); }

    public void importSampleIndexes(int[] arr) {
        System.arraycopy(arr, 0, selectedIndex, 0, ROWS);
        reloadAllSounds();
    }

    public void importMixerFloats(float[] src, float[] dst) {
        System.arraycopy(src, 0, dst, 0, ROWS);
    }
}