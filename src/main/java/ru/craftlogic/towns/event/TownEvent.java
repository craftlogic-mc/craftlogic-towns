package ru.craftlogic.towns.event;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import ru.craftlogic.towns.data.Town;

@Cancelable
public abstract class TownEvent extends Event {
    private final Town town;

    public TownEvent(Town town) {
        this.town = town;
    }

    public Town getTown() {
        return town;
    }
}
