package com.beatmaker.game.daw;

import com.badlogic.gdx.graphics.Texture;

public class DAWScreenUtil {

    private static Texture pixel;

    public static Texture pixel() {
        if (pixel == null) pixel = new Texture("white_pixel.png");
        return pixel;
    }
}
