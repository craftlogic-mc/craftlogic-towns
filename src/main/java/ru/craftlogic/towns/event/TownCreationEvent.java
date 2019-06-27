package ru.craftlogic.towns.event;

import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;

public class TownCreationEvent extends TownEvent {
    private final Resident creator;
    private Resident mayor;

    public TownCreationEvent(Town town, Resident creator, Resident mayor) {
        super(town);
        this.creator = creator;
        this.mayor = mayor;
    }

    public Resident getCreator() {
        return creator;
    }

    public Resident getMayor() {
        return mayor;
    }

    public void setMayor(Resident mayor) {
        this.mayor = mayor;
    }
}
