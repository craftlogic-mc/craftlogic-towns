package ru.craftlogic.towns.event;

import ru.craftlogic.towns.data.Town;

public class TownDeletionEvent extends TownEvent {
    public TownDeletionEvent(Town town) {
        super(town);
    }
}
