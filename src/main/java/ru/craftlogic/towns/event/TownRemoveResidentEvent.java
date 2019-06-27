package ru.craftlogic.towns.event;

import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;

public class TownRemoveResidentEvent extends TownEvent {
    private final Resident resident;
    private final Reason reason;

    public TownRemoveResidentEvent(Town town, Resident resident, Reason reason) {
        super(town);
        this.resident = resident;
        this.reason = reason;
    }

    public Resident getResident() {
        return resident;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        JOINED_OTHER_TOWN,
        BECAME_NOMAD,
        KICKED_BY_ASSISTANT,
        KICKED_FOR_DEBTS
    }
}
