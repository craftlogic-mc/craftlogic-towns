package ru.craftlogic.towns.client.map;

import mapwriter.api.MapChunkOverlay;
import ru.craftlogic.towns.client.VisualPlot;

import java.awt.*;

public class TownChunkOverlay implements MapChunkOverlay {
    private final VisualPlot plot;
    private final Point coord;

    public TownChunkOverlay(VisualPlot plot) {
        this.plot = plot;
        this.coord = new Point(plot.location.getChunkX(), plot.location.getChunkZ());
    }

    @Override
    public int getBorderColor() {
        return 0xAF000000 + plot.type.getColor(plot.permissions, plot.access);
    }

    @Override
    public float getBorderWidth() {
        return 0.5F;
    }

    @Override
    public int getColor() {
        return 0x7F000000 + plot.type.getColor(plot.permissions, plot.access);
    }

    @Override
    public Point getCoordinates() {
        return coord;
    }

    @Override
    public float getFilling() {
        return 1F;
    }

    @Override
    public byte getBorder() {
        return 0b1111;
    }
}
