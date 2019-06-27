package ru.craftlogic.towns.event;

import ru.craftlogic.towns.data.Town;

public class TownRenameEvent extends TownEvent {
    private final String oldName;
    private String newName;

    public TownRenameEvent(Town town, String oldName, String newName) {
        super(town);
        this.oldName = oldName;
        this.newName = newName;
    }

    public String getOldName() {
        return oldName;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }
}
