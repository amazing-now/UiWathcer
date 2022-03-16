package com.tinypace.uiwathcer;

public final class DisplayInfo {
    private int width;
    private int height;
    private int rotation;

    public DisplayInfo(int width, int height, int rotation) {
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRotation() {
        return rotation;
    }
}

