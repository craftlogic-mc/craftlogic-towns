package ru.craftlogic.towns.event;

import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;

public class TownInviteResidentEvent extends TownEvent {
    private final Resident sender;
    private final Resident target;

    public TownInviteResidentEvent(Town town, Resident sender, Resident target) {
        super(town);
        this.sender = sender;
        this.target = target;
    }

    public Resident getSender() {
        return sender;
    }

    public Resident getTarget() {
        return target;
    }
}
