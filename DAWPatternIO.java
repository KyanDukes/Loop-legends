package com.beatmaker.game.daw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class DAWPatternIO {

    private final Json json = new Json();

    public static class PatternData {
        public boolean[][] pads;
        public float[][] velocities;

        public int[] sampleIndex;
        public float[] volume;
        public float[] pan;
        public float[] pitch;

        public float[] attack;
        public float[] decay;
        public float[] sustain;
        public float[] release;

        public float bpm;
        public float swing;

        public int patternIndex; // optional
    }

    public void save(String filename,
                     DAWGrid grid,
                     DAWMixer mixer,
                     DAWTransport transport,
                     int patternIndex) {

        PatternData data = new PatternData();

        // Pads / Velocities
        data.pads       = grid.exportPads();
        data.velocities = grid.exportVelocities();

        // Mixer values
        data.sampleIndex = mixer.exportSampleIndexes();
        data.volume      = mixer.exportVolumes();
        data.pan         = mixer.exportPans();
        data.pitch       = mixer.exportPitches();

        data.attack  = mixer.exportAttack();
        data.decay   = mixer.exportDecay();
        data.sustain = mixer.exportSustain();
        data.release = mixer.exportRelease();

        // Global transport
        data.bpm   = transport.getBpm();
        data.swing = transport.getSwing();

        data.patternIndex = patternIndex;

        FileHandle file = Gdx.files.local("saved_patterns/" + filename);
        file.writeString(json.prettyPrint(data), false);
    }

    public PatternData load(String filename) {
        FileHandle file = Gdx.files.local("saved_patterns/" + filename);

        if (!file.exists()) return null;

        return json.fromJson(PatternData.class, file.readString());
    }
}
