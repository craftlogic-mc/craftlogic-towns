package ru.craftlogic.towns.client;

import com.mojang.authlib.GameProfile;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VisualTown {
    public final UUID id;
    public final String name;
    public final GameProfile mayor;
    public final int residentCount, plotCount;
    public final boolean open, pvp;
    public final Location spawn;
    public final Set<ChunkLocation> plots = new HashSet<>();

    public VisualTown(UUID id, String name, GameProfile mayor, int residents, int plots, boolean open, boolean pvp, Location spawn) {
        this.id = id;
        this.name = name;
        this.mayor = mayor;
        this.residentCount = residents;
        this.plotCount = plots;
        this.open = open;
        this.pvp = pvp;
        this.spawn = spawn;
    }
}
