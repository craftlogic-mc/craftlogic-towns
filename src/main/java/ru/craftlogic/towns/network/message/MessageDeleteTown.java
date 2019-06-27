package ru.craftlogic.towns.network.message;

import ru.craftlogic.api.network.AdvancedBuffer;
import ru.craftlogic.api.network.AdvancedMessage;

import java.util.UUID;

public class MessageDeleteTown extends AdvancedMessage {
    private UUID id;

    public MessageDeleteTown() {}

    public MessageDeleteTown(UUID id) {
        this.id = id;
    }

    @Override
    protected void read(AdvancedBuffer input) {
        this.id = input.readUniqueId();
    }

    @Override
    protected void write(AdvancedBuffer output) {
        output.writeUniqueId(this.id);
    }

    public UUID getId() {
        return id;
    }
}
