package ru.craftlogic.towns.event;

import net.minecraftforge.fml.common.eventhandler.Event;
import ru.craftlogic.towns.TownManager;

public class TownManagerReloadEvent extends Event {
    private TownManager townManager;

    public TownManagerReloadEvent(TownManager townManager) {
        this.townManager = townManager;
    }

    public TownManager getTownManager() {
        return townManager;
    }
}
