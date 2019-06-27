package ru.craftlogic.towns.event;

import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;

public class TownAddResidentEvent extends TownEvent {
    public final Resident resident;

    public TownAddResidentEvent(Town town, Resident resident) {
        super(town);
        this.resident = resident;
    }
}
