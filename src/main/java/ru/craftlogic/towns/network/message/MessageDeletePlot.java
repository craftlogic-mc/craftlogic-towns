package ru.craftlogic.towns.network.message;

import ru.craftlogic.api.network.AdvancedBuffer;
import ru.craftlogic.api.network.AdvancedMessage;
import ru.craftlogic.api.world.ChunkLocation;

import java.io.IOException;

public class MessageDeletePlot extends AdvancedMessage {
    private ChunkLocation location;

    public MessageDeletePlot() {}

    public MessageDeletePlot(ChunkLocation location) {
        this.location = location;
    }

    @Override
    protected void read(AdvancedBuffer input) throws IOException {
        this.location = input.readChunkLocation();
    }

    @Override
    protected void write(AdvancedBuffer output) throws IOException {
        output.writeChunkLocation(this.location);
    }

    public ChunkLocation getLocation() {
        return location;
    }
}
