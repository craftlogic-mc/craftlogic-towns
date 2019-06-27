package ru.craftlogic.towns.data;

import ru.craftlogic.towns.TownManager;

import java.util.UUID;

public class Nation {
    private final TownManager townManager;
    private final UUID id;
    private String name;
    private UUID founder;

    public Nation(TownManager townManager, UUID id, String name, UUID founder) {
        this.townManager = townManager;
        this.id = id;
        this.name = name;
        this.founder = founder;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getFounderId() {
        return founder;
    }

    public Resident getFounder() {
        return this.townManager.getResident(this.founder);
    }
}
