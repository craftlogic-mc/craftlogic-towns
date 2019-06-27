package ru.craftlogic.towns.network.message;

import com.mojang.authlib.GameProfile;
import ru.craftlogic.api.network.AdvancedBuffer;
import ru.craftlogic.api.network.AdvancedMessage;
import ru.craftlogic.api.world.Location;

import java.util.UUID;

public class MessageTown extends AdvancedMessage {
    private UUID id;
    private String name;
    private GameProfile mayor;
    private int residents, plots;
    private boolean open, pvp;
    private Location spawn;

    public MessageTown() {}

    public MessageTown(UUID id, String name, GameProfile mayor, int residents, int plots, boolean open, boolean pvp, Location spawn){
        this.id = id;
        this.name = name;
        this.mayor = mayor;
        this.residents = residents;
        this.plots = plots;
        this.open = open;
        this.pvp = pvp;
        this.spawn = spawn;
    }

    @Override
    protected void read(AdvancedBuffer input) {
        this.id = input.readUniqueId();
        this.name = input.readString(Short.MAX_VALUE);
        this.mayor = input.readProfile();
        this.residents = input.readInt();
        this.plots = input.readInt();
        this.open = input.readBoolean();
        this.pvp = input.readBoolean();
        if (input.readBoolean()) {
            this.spawn = input.readEntityLocation();
        }
    }

    @Override
    protected void write(AdvancedBuffer output) {
        output.writeUniqueId(this.id);
        output.writeString(this.name);
        output.writeProfile(this.mayor);
        output.writeInt(this.residents);
        output.writeInt(this.plots);
        output.writeBoolean(this.open);
        output.writeBoolean(this.pvp);
        output.writeBoolean(this.spawn != null);
        if (this.spawn != null) {
            output.writeEntityLocation(this.spawn);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GameProfile getMayor() {
        return mayor;
    }

    public int getResidents() {
        return residents;
    }

    public int getPlots() {
        return plots;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isPvP() {
        return pvp;
    }

    public Location getSpawn() {
        return spawn;
    }
}
