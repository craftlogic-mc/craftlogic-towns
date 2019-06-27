package ru.craftlogic.towns.data;

import com.google.gson.JsonObject;
import net.minecraft.util.JsonUtils;
import ru.craftlogic.api.world.Location;
import ru.craftlogic.api.world.OfflinePlayer;
import ru.craftlogic.towns.TownManager;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static ru.craftlogic.towns.CraftTowns.GSON;

public class Warp {
    private final TownManager townManager;
    private final TownWorld world;
    private final UUID uuid;
    private UUID owner;
    private UUID town;
    private String name;
    private String greeting;
    private Location location;
    private AccessLevel accessLevel = AccessLevel.OWNER;
    private boolean dirty;

    public Warp(TownManager townManager, UUID uuid, TownWorld world, Location location) {
        this.townManager = townManager;
        this.world = world;
        this.uuid = uuid;
        this.location = location;
    }

    public UUID getId() {
        return uuid;
    }

    public boolean hasTown() {
        return this.town != null;
    }

    public Town getTown() {
        return this.townManager.getTown(this.town);
    }

    public void setTown(Town town) {
        Town oldTown = this.townManager.getTown(this.town);
        if (oldTown != null && oldTown.hasWarp(this.uuid)) {
            oldTown.removeWarp(this);
        }
        this.town = town == null ? null : town.getId();
        if (town != null && !town.hasWarp(this.uuid)) {
            town.addWarp(this);
        }
        if (town != oldTown) {
            this.dirty = true;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!this.name.equals(name)) {
            this.name = name;
            this.dirty = true;
        }
    }

    public UUID getOwnerId() {
        return this.owner;
    }

    public Resident getOwner() {
        return this.townManager.getResident(this.owner);
    }

    public void setOwner(UUID id) {
        if (this.owner != id) {
            this.owner = id;
            this.dirty = true;
        }
    }

    public void setOwner(OfflinePlayer player) {
        this.setOwner(player.getId());
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        if (!Objects.equals(this.greeting, greeting)) {
            this.greeting = greeting;
            this.dirty = true;
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (this.location != location) {
            this.location = location;
            this.dirty = true;
        }
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        if (this.accessLevel != accessLevel) {
            this.accessLevel = accessLevel;
            this.dirty = true;
        }
    }

    public static Warp load(TownManager townManager, TownWorld world, UUID warpId, JsonObject w) {
        JsonObject sp = JsonUtils.getJsonObject(w, "location");
        double x = JsonUtils.getFloat(sp, "x");
        double y = JsonUtils.getFloat(sp, "y");
        double z = JsonUtils.getFloat(sp, "z");
        float yaw = JsonUtils.getFloat(sp, "yaw");
        float pitch = JsonUtils.getFloat(sp, "pitch");
        Location location = world.unwrap().getLocation(x, y, z, yaw, pitch);
        Warp warp = new Warp(townManager, warpId, world, location);
        warp.setName(JsonUtils.getString(w, "name"));
        if (w.has("greeting")) {
            warp.setGreeting(JsonUtils.getString(w, "greeting"));
        }
        if (w.has("town")) {
            warp.setTown(townManager.getTown(UUID.fromString(JsonUtils.getString(w, "town"))));
        }
        if (w.has("owner")) {
            warp.setOwner(UUID.fromString(JsonUtils.getString(w, "owner")));
        }
        if (w.has("accessLevel")) {
            warp.setAccessLevel(AccessLevel.valueOf(JsonUtils.getString(w, "accessLevel")));
        }
        return warp;
    }

    public void save() throws IOException {
        Path worldDir = this.world.getDir();
        if (!Files.exists(worldDir)) {
            Files.createDirectories(worldDir);
        }
        Path townsDir = worldDir.resolve("warps/");
        if (!Files.exists(townsDir)) {
            Files.createDirectories(townsDir);
        }
        Path warpFile = townsDir.resolve(uuid.toString() + ".json");
        JsonObject w;
        if (!Files.exists(warpFile)) {
            Files.createFile(warpFile);
            w = new JsonObject();
        } else {
            if (!this.dirty) {
                return;
            }
            try (Reader reader = Files.newBufferedReader(warpFile)) {
                w = GSON.fromJson(reader, JsonObject.class);
            }
        }
        w.addProperty("name", this.name);
        if (this.greeting != null) {
            w.addProperty("greeting", this.greeting);
        }
        if (this.owner != null) {
            w.addProperty("owner", this.owner.toString());
        }
        if (this.town != null) {
            w.addProperty("town", this.town.toString());
        }
        if (this.accessLevel != AccessLevel.OWNER) {
            w.addProperty("accessLevel", this.accessLevel.toString());
        }
        JsonObject loc = new JsonObject();
        loc.addProperty("x", this.location.getX());
        loc.addProperty("y", this.location.getY());
        loc.addProperty("z", this.location.getZ());
        loc.addProperty("yaw", this.location.getYaw());
        loc.addProperty("pitch", this.location.getPitch());
        w.add("location", loc);
        Files.write(warpFile, GSON.toJson(w).getBytes(StandardCharsets.UTF_8));
    }

    public enum AccessLevel {
        OWNER,
        FRIEND,
        RESIDENT,
        ALLY,
        EVERYONE
    }
}
