package com.badlogic.HouseScreen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class DayNightCycle {

    // 0f → 24f (full day)
    private float timeOfDay = 6f; // start at MORNING

    public float dayLength = 60f;  
    // 60 seconds = full day
    // You can change this to 120 or 300 if you want slower cycles

    private Color overlayColor = new Color(0, 0, 0, 0);

    public float getTime() {
        return timeOfDay;
    }

    public void update(float delta) {
        timeOfDay += (24f / dayLength) * delta;

        if (timeOfDay >= 24f) timeOfDay -= 24f;
    }

    public Color getLightTint() {

        // MORNING (5 → 10)
        if (timeOfDay >= 5 && timeOfDay < 10) {
            float t = (timeOfDay - 5f) / 5f;
            overlayColor.set(0f, 0f, 0f, 0.4f - 0.4f * t); // fade out darkness
        }

        // DAY (10 → 17)
        else if (timeOfDay >= 10 && timeOfDay < 17) {
            overlayColor.set(0f, 0f, 0f, 0f); // full brightness
        }

        // EVENING (17 → 20)
        else if (timeOfDay >= 17 && timeOfDay < 20) {
            float t = (timeOfDay - 17f) / 3f;
            overlayColor.set(0.2f, 0.05f, 0f, 0f + 0.3f * t); // warm tint
        }

        // NIGHT (20 → 5)
        else {
            overlayColor.set(0f, 0f, 0.1f, 0.4f); // dark blue tint
        }

        return overlayColor;
    }
}