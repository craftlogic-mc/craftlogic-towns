package ru.craftlogic.towns.network.message;

import com.mojang.authlib.GameProfile;
import ru.craftlogic.api.network.AdvancedBuffer;
import ru.craftlogic.api.network.AdvancedMessage;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot.AccessLevel;
import ru.craftlogic.towns.data.Plot.Permission;
import ru.craftlogic.towns.data.plot.types.PlotType;

import java.util.EnumSet;
import java.util.UUID;

public class MessagePlot extends AdvancedMessage {
    private ChunkLocation location;
    private PlotType type;
    private UUID town;
    private GameProfile owner;
    private boolean forSale, locked;
    private float price;
    private EnumSet<Permission> permissions;
    private AccessLevel access;

    public MessagePlot() {}

    public MessagePlot(ChunkLocation location, PlotType type, UUID town, GameProfile owner, boolean forSale,
                       boolean locked, float price, EnumSet<Permission> permissions, AccessLevel access) {

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

    @Override
    protected void read(AdvancedBuffer input) {
        this.location = input.readChunkLocation();
        this.type = TownManager.findPlotType(input.readString(Short.MAX_VALUE));
        if (input.readBoolean()) {
            this.town = input.readUniqueId();
        }
        if (input.readBoolean()) {
            this.owner = input.readProfile();
        }
        this.forSale = input.readBoolean();
        this.locked = input.readBoolean();
        this.price = input.readFloat();
        int permissions = input.readInt();
        this.permissions = EnumSet.noneOf(Permission.class);
        for (int i = 0; i < permissions; i++) {
            Permission p = input.readEnumValue(Permission.class);
            this.permissions.add(p);
        }
        this.access = input.readEnumValue(AccessLevel.class);
    }

    @Override
    protected void write(AdvancedBuffer output) {
        output.writeChunkLocation(this.location);
        output.writeString(this.type.getNames().get(0));
        boolean hasTown = this.town != null;
        output.writeBoolean(hasTown);
        if (hasTown) {
            output.writeUniqueId(this.town);
        }
        boolean hasOwner = this.owner != null;
        output.writeBoolean(hasOwner);
        if (hasOwner) {
            output.writeProfile(this.owner);
        }
        output.writeBoolean(this.forSale);
        output.writeBoolean(this.locked);
        output.writeFloat(this.price);
        output.writeInt(this.permissions.size());
        for (Permission p : this.permissions) {
            output.writeEnumValue(p);
        }
        output.writeEnumValue(this.access);
    }

    public ChunkLocation getLocation() {
        return location;
    }

    public PlotType getType() {
        return type;
    }

    public UUID getTown() {
        return town;
    }

    public GameProfile getOwner() {
        return owner;
    }

    public boolean isForSale() {
        return forSale;
    }

    public boolean isLocked() {
        return locked;
    }

    public float getPrice() {
        return price;
    }

    public EnumSet<Permission> getPermissions() {
        return permissions;
    }

    public AccessLevel getAccess() {
        return access;
    }
}
