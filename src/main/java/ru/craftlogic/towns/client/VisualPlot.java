package ru.craftlogic.towns.client;

import com.mojang.authlib.GameProfile;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.towns.data.Plot.AccessLevel;
import ru.craftlogic.towns.data.Plot.Permission;
import ru.craftlogic.towns.data.plot.types.PlotType;

import java.util.EnumSet;
import java.util.UUID;

public class VisualPlot {
    public final ChunkLocation location;
    public final PlotType type;
    public final UUID town;
    public final GameProfile owner;
    public final boolean forSale, locked;
    public final float price;
    public final EnumSet<Permission> permissions;
    public final AccessLevel access;

    public VisualPlot(ChunkLocation location, PlotType type, UUID town, GameProfile owner, boolean forSale, boolean locked,
                      float price, EnumSet<Permission> permissions, AccessLevel access) {

        this.location = location;
        this.type = type;
        this.town = town;
        this.owner = owner;
        this.forSale = forSale;
        this.locked = locked;
        this.price = price;
        this.permissions = permissions;
        this.access = access;
    }
}
