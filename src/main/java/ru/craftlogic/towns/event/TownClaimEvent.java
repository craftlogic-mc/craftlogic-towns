package ru.craftlogic.towns.event;

import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Town;

public class TownClaimEvent extends TownEvent {
    private final ChunkLocation location;
    private final Plot plot;

    public TownClaimEvent(Town town, ChunkLocation location, Plot plot) {
        super(town);
        this.location = location;
        this.plot = plot;
    }

    public ChunkLocation getLocation() {
        return location;
    }

    public Plot getPlot() {
        return plot;
    }
}
