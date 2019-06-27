package ru.craftlogic.towns.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.JsonUtils;
import net.minecraft.world.World;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.Location;
import ru.craftlogic.api.world.OfflinePlayer;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.plot.types.PlotType;
import ru.craftlogic.towns.network.message.MessagePlot;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

public class Plot {
    private final TownManager townManager;
    private final ChunkLocation location;
    private final TownWorld world;
    private PlotType type = TownManager.findPlotType("default");
    private String tooltip = "";
    private UUID town;
    private UUID owner;
    private boolean forSale;
    private boolean spawningMonsters;
    private boolean spawningAnimals;
    private boolean explosions;
    private boolean snowForming;
    private boolean locked;
    private float price;
    private EnumMap<Permission, EnumSet<AccessLevel>> permissions = new EnumMap<>(Permission.class);

    public Plot(TownManager townManager, TownWorld world, ChunkLocation location) {
        this.townManager = townManager;
        this.world = world;
        this.location = new ChunkLocation(location);
    }

    public static Plot load(TownManager plugin, TownWorld world, ChunkLocation location, JsonObject p) {
        String t = JsonUtils.getString(p, "type", "default");
        PlotType type = TownManager.findPlotType(t);
        UUID town = UUID.fromString(JsonUtils.getString(p, "town"));
        UUID owner = null;
        if (p.has("owner")) {
            owner = UUID.fromString(JsonUtils.getString(p, "owner"));
        }
        float price = JsonUtils.getFloat(p, "price", 0);
        boolean snow = JsonUtils.getBoolean(p, "snow", false);
        boolean forSale = JsonUtils.getBoolean(p, "selling", false);
        boolean animals = JsonUtils.getBoolean(p, "animals", false);
        boolean monsters = JsonUtils.getBoolean(p, "monsters", false);
        boolean explosions = JsonUtils.getBoolean(p, "explosions", false);
        boolean locked = JsonUtils.getBoolean(p, "locked", false);
        String tooltip = JsonUtils.getString(p, "tooltip", "");

        EnumMap<Permission, EnumSet<AccessLevel>> permissions = new EnumMap<>(Permission.class);
        JsonObject perm = p.getAsJsonObject("permissions");
        for (Permission permission : Permission.values()) {
            EnumSet<AccessLevel> set = permissions.computeIfAbsent(permission, k -> EnumSet.noneOf(AccessLevel.class));
            if (perm != null && perm.has(permission.getName())) {
                set.clear();
                for (JsonElement s : perm.getAsJsonArray(permission.getName())) {
                    set.add(AccessLevel.valueOf(s.getAsString().toUpperCase()));
                }
            }
            permissions.put(permission, set);
        }

        Plot plot = new Plot(plugin, world, location);
        plot.setTown(town);
        plot.setType(type);
        plot.setOwner(owner);
        plot.setTooltip(tooltip);
        plot.setForSale(forSale);
        plot.setPrice(price);
        plot.setMonstersSpawningAllowed(monsters);
        plot.setAnimalsSpawningAllowed(animals);
        plot.setSnowFormingAllowed(snow);
        plot.setExplosionsAllowed(explosions);
        plot.setLocked(locked);
        plot.permissions = permissions;
        return plot;
    }

    public void save(JsonObject p) {
        p.addProperty("town", this.town.toString());
        PlotType type = this.getType();
        if (!type.isDefault()) {
            p.addProperty("type", type.getNames().get(0));
        }
        if (this.hasOwner()) {
            p.addProperty("owner", this.getOwnerId().toString());
        }
        if (this.getTooltip() != null && !this.getTooltip().isEmpty()) {
            p.addProperty("tooltip", this.getTooltip());
        }
        if (this.isLocked()) {
            p.addProperty("locked", true);
        }
        if (this.isForSale()) {
            p.addProperty("selling", true);
            p.addProperty("price", this.getPrice());
        }
        if (this.isMonstersSpawningAllowed()) {
            p.addProperty("monsters", true);
        }
        if (this.isAnimalsSpawningAllowed()) {
            p.addProperty("animals", true);
        }
        if (this.isSnowFormingAllowed()) {
            p.addProperty("snow", true);
        }
        if (this.areExplosionsAllowed()) {
            p.addProperty("explosions", true);
        }
        if (!this.permissions.isEmpty()) {
            JsonObject permissions = null;
            for (Permission permission : Permission.values()) {
                EnumSet<AccessLevel> set = this.permissions.get(permission);
                if (set != null && !set.isEmpty()) {
                    JsonArray val = new JsonArray();
                    for (AccessLevel accessLevel : set) {
                        val.add(accessLevel.toString());
                    }
                    if (permissions == null) {
                        permissions = new JsonObject();
                    }
                    permissions.add(permission.getName(), val);
                }
            }
            if (permissions != null) {
                p.add("permissions", permissions);
            }
        }
    }

    /*public boolean hasBackup() {
        Path worldDir = this.world.getDir();
        return new File(worldDir, "plots/" + this.getLocation().toLong() + ".dat").exists();
    }

    public boolean createBackup(boolean overwrite) {
        Path worldDir = this.world.getDir();
        File backupDir = new File(worldDir, "plots/");
        if (!backupDir.exists()) {
            backupDir.mkdir();
        }
        ChunkLocation location = this.getLocation();
        File file = new File(backupDir, location.toLong() + ".dat");
        if (!file.exists() || overwrite) {
            NBTContainerChunk chunk = new NBTContainerChunk(location.getChunk());
            new NBTContainerFile(file).writeTag(chunk.readTag());
            return true;
        }
        return false;
    }

    public boolean loadBackup() {
        Path worldDir = this.world.getDir();
        ChunkLocation location = this.getLocation();
        File file = new File(worldDir, "plots/" + location.toLong() + ".dat");
        if (file.exists()) {
            NBTContainerChunk chunk = new NBTContainerChunk(location.getChunk());
            chunk.writeTag(new NBTContainerFile(file).readTag());
            return true;
        }
        return false;
    }*/

    public ChunkLocation getLocation() {
        return location;
    }

    public PlotType getType() {
        return type;
    }

    public void setType(@Nonnull PlotType type) {
        this.type = type;
    }

    public boolean hasTown() {
        return this.town != null && this.getTown() != null;
    }

    public UUID getTownId() {
        return this.town;
    }

    public Town getTown() {
        return this.townManager.getTown(this.town);
    }

    public void setTown(UUID id) {
        this.town = id;
    }

    public boolean hasOwner() {
        return this.owner != null;
    }

    public Resident getOwner() {
        return this.owner == null ? null : this.townManager.getResident(this.owner);
    }

    public UUID getOwnerId() {
        return this.owner;
    }

    public boolean isOwner(OfflinePlayer player) {
        return this.owner != null && this.owner.equals(player.getId());
    }

    public void setOwner(OfflinePlayer player) {
        this.owner = player.getId();
    }

    public void setOwner(UUID uuid) {
        this.owner = uuid;
    }

    public boolean hasTooltip() {
        return tooltip != null && !tooltip.isEmpty();
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(@Nonnull String name) {
        this.tooltip = name;
    }

    public TownWorld getWorld() {
        return world;
    }

    public boolean isForSale() {
        return forSale;
    }

    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public boolean isAnimalsSpawningAllowed() {
        return spawningAnimals;
    }

    public boolean isMonstersSpawningAllowed() {
        return spawningMonsters;
    }

    public boolean isSnowFormingAllowed() {
        return snowForming;
    }

    public boolean areExplosionsAllowed() {
        return explosions;
    }

    public void setExplosionsAllowed(boolean explosions) {
        this.explosions = explosions;
    }

    public void setAnimalsSpawningAllowed(boolean spawningAnimals) {
        this.spawningAnimals = spawningAnimals;
    }

    public void setMonstersSpawningAllowed(boolean spawningMonsters) {
        this.spawningMonsters = spawningMonsters;
    }

    public void setSnowFormingAllowed(boolean snowForming) {
        this.snowForming = snowForming;
    }

    public boolean isWarZone() {
        return false;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean hasPermission(Resident resident, Permission permission) {
        if (this.isOwner(resident)) {
            return true;
        }
        if (this.hasTown()) {
            Town town = this.getTown();
            if (town.isMayor(resident) || !this.hasOwner() && town.isAssistant(resident)) {
                return true;
            }
        }
        AccessLevel accessLevel = getAccessLevel(resident);
        return this.permissions.containsKey(permission) && this.permissions.get(permission).contains(accessLevel);
    }

    public void setPermission(Permission permission, EnumSet<AccessLevel> accessLevels) {
        this.permissions.put(permission, accessLevels);
    }

    public EnumSet<AccessLevel> getPermission(Permission permission) {
        return EnumSet.copyOf(this.permissions.getOrDefault(permission, EnumSet.noneOf(AccessLevel.class)));
    }

    public EnumMap<Permission, EnumSet<AccessLevel>> getPermissions() {
        return this.permissions;
    }

    public AccessLevel getAccessLevel(Resident resident) {
        Resident owner = this.getOwner();
        if (resident == owner) {
            return AccessLevel.OWNER;
        }
        if (owner == null) {
            Town t = resident.getTown();
            if (t != null && this.town != null) {
                if (t.getId().equals(this.town)) {
                    return AccessLevel.RESIDENT;
                } else if (t.getRelationshipWith(this.town) == Town.Relationship.ALLY) {
                    return AccessLevel.ALLY;
                }
            }
            return AccessLevel.STRANGER;
        }
        switch (owner.getStatusFor(resident)) {
            case FRIEND:
                return AccessLevel.FRIEND;
            case RESIDENT:
                return AccessLevel.RESIDENT;
            case ALLY:
                return AccessLevel.ALLY;
            default:
                return AccessLevel.STRANGER;
        }
    }

    public Set<Plot> findNearbyPlots(Predicate<Plot> filter) {
        Set<Plot> lockedPlots = new HashSet<>();
        this.findNearbyPlots(lockedPlots, this, filter);
        return lockedPlots;
    }

    private void findNearbyPlots(Set<Plot> lockedPlots, Plot plot, Predicate<Plot> filter) {
        ChunkLocation l = plot.getLocation();
        lockedPlots.add(plot);
        for (EnumFacing face : EnumFacing.values()) {
            ChunkLocation lo = new ChunkLocation(l.getWorld(),
                l.getChunkX() + face.getFrontOffsetX(),
                l.getChunkZ() + face.getFrontOffsetZ()
            );
            Plot po = this.townManager.getPlot(lo);
            if (po != null && !lockedPlots.contains(po) && filter.test(po)) {
                this.findNearbyPlots(lockedPlots, po, filter);
            }
        }
    }

    public Location getSafeTeleportLocation() {
        int x = (this.location.getChunkX() << 4) + 8;
        int z = (this.location.getChunkZ() << 4) + 8;
        World world = this.world.unwrap().unwrap();
        int y = world.getHeight(x, z);
        Location location = new Location(world, x, y, z);
        while (!location.isAir()) {
            location = location.offset(EnumFacing.UP);
        }
        return location;
    }

    public boolean contains(ChunkLocation location) {
        return this.getLocation().equals(location);
    }

    public MessagePlot serialize(Resident resident) {
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        for (Permission permission : Permission.values()) {
            if (this.hasPermission(resident, permission)) {
                permissions.add(permission);
            }
        }
        return new MessagePlot(
            getLocation(), getType(), getTownId(), hasOwner() ? getOwner().getProfile(): null, isForSale(),
            isLocked(), getPrice(), permissions, getAccessLevel(resident)
        );
    }

    public enum Permission {
        INTERACT("interact"),
        OPEN_CONTAINER("container"),
        MANAGE("manage"),
        BUILD("build"),
        LAUNCH_PROJECTILE("projectile");

        private final String name;

        Permission(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Permission parse(String key) {
            for (Permission permission : values()) {
                if (permission.name.equalsIgnoreCase(key)) {
                    return permission;
                }
            }
            return null;
        }
    }

    public enum AccessLevel {
        OWNER,
        FRIEND,
        RESIDENT,
        ALLY,
        STRANGER
    }
}
