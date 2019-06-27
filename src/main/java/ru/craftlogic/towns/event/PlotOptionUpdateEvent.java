package ru.craftlogic.towns.event;

import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.plot.options.PlotOption;

public class PlotOptionUpdateEvent extends PlotUpdateEvent {
    private final ChunkLocation location;
    private final PlotOption option;
    private final String oldValue;
    private final String newValue;

    public PlotOptionUpdateEvent(Plot plot, PlotOption option, String oldValue, String newValue) {
        super(plot);
        this.location = plot.getLocation();
        this.option = option;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public ChunkLocation getLocation() {
        return location;
    }

    public PlotOption getOption() {
        return option;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
