package ru.craftlogic.towns.event;

import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;

public class TownResidentAcceptedInvitationEvent extends TownEvent {
    private final Resident resident;

    public TownResidentAcceptedInvitationEvent(Town town, Resident resident) {
        super(town);
        this.resident = resident;
    }

    public Resident getResident() {
        return resident;
    }
}
