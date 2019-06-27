package ru.craftlogic.towns.event;

import net.minecraftforge.fml.common.eventhandler.Event;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.towns.data.Plot;

public class PlotUpdateEvent extends Event {
    private final ChunkLocation location;
    private final Plot plot;

    public PlotUpdateEvent(Plot plot) {
        this.location = plot.getLocation();
        this.plot = plot;
    }

    public ChunkLocation getLocation() {
        return location;
    }

    public Plot getPlot() {
        return plot;
    }
}
