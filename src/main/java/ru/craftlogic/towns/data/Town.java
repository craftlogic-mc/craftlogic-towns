package ru.craftlogic.towns.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.MinecraftForge;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.world.*;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.event.TownAddResidentEvent;
import ru.craftlogic.towns.event.TownRemoveResidentEvent;
import ru.craftlogic.towns.network.message.MessageTown;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static ru.craftlogic.towns.CraftTowns.GSON;

public class Town implements PlotOwner {
    private final TownManager townManager;
    private final UUID uuid;
    private final TownWorld world;
    private Bank bank;
    private String name;
    private UUID mayor;
    private UUID nation;
    private Set<UUID> warps = new HashSet<>();
    private Set<UUID> residents = new HashSet<>();
    private Set<UUID> assistants = new HashSet<>();
    private Map<UUID, Relationship> relationships = new HashMap<>();
    private Set<ChunkLocation> plots = new HashSet<>();
    private Location spawnpoint;
    private boolean open, pvp;
    private boolean dirty;

    public Town(TownManager townManager, UUID uuid, TownWorld world, String name) {
        this.townManager = townManager;
        this.uuid = uuid;
        this.world = world;
        this.name = name;
        if (townManager.getServer().getEconomyManager().isEnabled()) {
            this.bank = new Bank(townManager, this);
        }
    }

    public static Town load(TownManager plugin, TownWorld world, UUID uuid, JsonObject t) {
        String name = JsonUtils.getString(t, "name");
        Town town = new Town(plugin, uuid, world, name);
        UUID mayor = UUID.fromString(JsonUtils.getString(t, "mayor"));
        boolean open = JsonUtils.getBoolean(t, "open");
        boolean pvp = JsonUtils.getBoolean(t, "pvp");
        Location spawnpoint = null;
        if (t.has("spawnpoint")) {
            JsonObject sp = t.getAsJsonObject("spawnpoint");
            double x = JsonUtils.getFloat(sp, "x");
            double y = JsonUtils.getFloat(sp, "y");
            double z = JsonUtils.getFloat(sp, "z");
            float yaw = JsonUtils.getFloat(sp, "yaw");
            float pitch = JsonUtils.getFloat(sp, "pitch");
            spawnpoint = world.unwrap().getLocation(x, y, z, yaw, pitch);
        }
        JsonArray warps = t.getAsJsonArray("warps");
        JsonArray residents = t.getAsJsonArray("residents");
        JsonArray assistants = t.getAsJsonArray("assistants");
        JsonObject relationships = t.getAsJsonObject("relationships");
        JsonObject bank = t.getAsJsonObject("bank");

        town.setMayor(mayor);
        town.setOpen(open);
        town.setPvP(pvp);

        if (spawnpoint != null) {
            town.setSpawnpoint(spawnpoint);
        }
        for (JsonElement w : warps) {
            town.addWarp(UUID.fromString(w.getAsString()));
        }
        for (JsonElement r : residents) {
            town.addResident(UUID.fromString(r.getAsString()));
        }
        for (JsonElement a : assistants) {
            town.addAssistant(UUID.fromString(a.getAsString()));
        }
        if (relationships != null) {
            for (Map.Entry<String, JsonElement> e : relationships.entrySet()) {
                UUID id = UUID.fromString(e.getKey());
                Relationship relationship = Relationship.valueOf(e.getValue().getAsString());
                town.setRelationshipWith(id, relationship);
            }
        }
        if (bank != null) {
            town.bank.load(bank);
        }
        return town;
    }

    public void save() throws IOException {
        Path worldDir = this.world.getDir();
        if (!Files.exists(worldDir)) {
            Files.createDirectories(worldDir);
        }
        Path townsDir = worldDir.resolve("towns/");
        if (!Files.exists(townsDir)) {
            Files.createDirectories(townsDir);
        }
        Path townFile = townsDir.resolve(uuid.toString() + ".json");
        JsonObject t;
        if (!Files.exists(townFile)) {
            Files.createFile(townFile);
            t = new JsonObject();
        } else {
            if (!this.dirty) {
                return;
            }
            try (Reader reader = Files.newBufferedReader(townFile)) {
                t = GSON.fromJson(reader, JsonObject.class);
            }
        }
        t.addProperty("name", this.getName());
        t.addProperty("mayor", this.getMayorId().toString());
        t.addProperty("open", this.isOpen());
        t.addProperty("pvp", this.isPvP());
        if (this.hasSpawnpoint()) {
            JsonObject sp = new JsonObject();
            Location spawnpoint = this.getSpawnpoint();
            sp.addProperty("x", spawnpoint.getX());
            sp.addProperty("y", spawnpoint.getY());
            sp.addProperty("z", spawnpoint.getZ());
            sp.addProperty("yaw", spawnpoint.getYaw());
            sp.addProperty("pitch", spawnpoint.getPitch());
            t.add("spawnpoint", sp);
        }
        JsonArray warps = new JsonArray();
        for (UUID p : this.getWarps()) {
            warps.add(p.toString());
        }
        t.add("warps", warps);
        JsonArray residents = new JsonArray();
        for (UUID p : this.getResidents()) {
            residents.add(p.toString());
        }
        t.add("residents", residents);
        JsonArray assistants = new JsonArray();
        for (UUID p : this.getAssistants()) {
            assistants.add(p.toString());
        }
        JsonObject relationships = new JsonObject();
        for (Map.Entry<UUID, Relationship> entry : this.relationships.entrySet()) {
            relationships.addProperty(entry.getKey().toString(), entry.getValue().toString());
        }
        t.add("relationships", relationships);
        t.add("assistants", assistants);
        if (this.bank != null) {
            JsonObject b = new JsonObject();
            this.bank.save(b);
            t.add("bank", b);
        }
        Files.write(townFile, GSON.toJson(t).getBytes(StandardCharsets.UTF_8));
        this.dirty = false;
    }

    public void delete() throws IOException {
        Path worldDir = this.world.getDir();
        if (!Files.exists(worldDir)) {
            return;
        }
        Path townsDir = worldDir.resolve("towns/");
        if (!Files.exists(townsDir)) {
            return;
        }
        Path townFile = townsDir.resolve(uuid.toString() + ".json");
        Files.deleteIfExists(townFile);
    }

    public UUID getId() {
        return this.uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasMayor() {
        return this.mayor != null;
    }

    public Resident getMayor() {
        return this.mayor == null ? null : this.townManager.getResident(this.mayor);
    }

    public UUID getMayorId() {
        return mayor;
    }

    public void setMayor(UUID uuid) {
        this.mayor = uuid;
        this.dirty = true;
    }

    public void setMayor(OfflinePlayer mayor) {
        this.setMayor(mayor.getId());
    }

    public boolean isMayor(UUID uuid) {
        return this.mayor.equals(uuid);
    }

    public boolean isMayor(OfflinePlayer player) {
        return this.isMayor(player.getId());
    }

    public Set<UUID> getAssistants() {
        return this.assistants;
    }

    public boolean addAssistant(UUID uuid) {
        if (this.assistants.add(uuid)) {
            this.dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean addAssistant(OfflinePlayer player) {
        return this.addAssistant(player.getId());
    }

    public boolean removeAssistant(UUID uuid) {
        if (this.assistants.remove(uuid)) {
            this.dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAssistant(OfflinePlayer player) {
        return this.removeAssistant(player.getId());
    }

    public boolean isAssistant(UUID uuid) {
        return this.assistants.contains(uuid);
    }

    public boolean isAssistant(OfflinePlayer player) {
        return this.isAssistant(player.getId());
    }

    public boolean isAuthority(UUID uuid) {
        return this.isMayor(uuid) || this.isAssistant(uuid);
    }

    public boolean isAuthority(OfflinePlayer player) {
        return this.isMayor(player) || this.isAssistant(player);
    }

    public boolean hasBank() {
        return bank != null;
    }

    public Bank getBank() {
        return bank;
    }

    public boolean hasNation() {
        return this.nation != null && this.getNation() != null;
    }

    public Nation getNation() {
        return this.townManager.getNation(this.nation);
    }

    public void setNation(Nation nation) {
        this.dirty = true;
        throw new UnsupportedOperationException();
    }

    public boolean hasSpawnpoint() {
        return spawnpoint != null;
    }

    public Location getSpawnpoint() {
        return spawnpoint;
    }

    public void setSpawnpoint(Location spawnpoint) {
        this.dirty = true;
        this.spawnpoint = spawnpoint;
    }

    @Override
    public boolean isOwning(Plot plot) {
        if (plot.getTown() == this) {
            if (!this.plots.contains(plot.getLocation())) {
                this.plots.add(plot.getLocation());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removePlot(Plot plot) {
        if (this.plots.remove(plot.getLocation())) {
            this.dirty = true;
            plot.setTown(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean addPlot(Plot plot) {
        if (this.plots.add(plot.getLocation())) {
            this.dirty = true;
            plot.setTown(this.getId());
            if (!this.world.hasPlot(plot.getLocation())) {
                this.world.addPlot(plot);
            }
            return true;
        }
        return false;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.dirty = true;
        this.open = open;
    }

    public boolean isPvP() {
        return pvp;
    }

    public void setPvP(boolean pvp) {
        this.dirty = true;
        this.pvp = pvp;
    }

    public void broadcast(@Nullable CommandSender exception, Text<?, ?> message) {
        if (townManager.broadcastEvents) {
            for (Resident r : getOnlineResidents()) {
                if (!(exception instanceof Player) || !((Player) exception).getId().equals(r.getId())) {
                    r.sendMessage(message);
                }
            }
        }
    }

    public boolean hasResident(OfflinePlayer player) {
        return this.residents.contains(player.getId());
    }

    public boolean addResident(UUID uuid) {
        if (this.residents.add(uuid)) {
            this.dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean addResident(Resident resident) {
        if (resident.getTown() == null && this.residents.add(resident.getId())) {
            TownAddResidentEvent event = new TownAddResidentEvent(this, resident);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.dirty = true;
                resident.setTown(this);
                CraftTowns.NETWORK.broadcast(serialize());
                return true;
            }
        }
        return false;
    }

    public boolean removeResident(Resident resident, TownRemoveResidentEvent.Reason reason) {
        if (resident.getTown() != null && this.residents.remove(resident.getId())) {
            TownRemoveResidentEvent event = new TownRemoveResidentEvent(this, resident, reason);
            if (!MinecraftForge.EVENT_BUS.post(event)) {
                this.dirty = true;
                resident.setTown(null);
                CraftTowns.NETWORK.broadcast(serialize());
                return true;
            }
        }
        return false;
    }

    public Set<UUID> getResidents() {
        return new HashSet<>(this.residents);
    }

    public Set<Resident> getOnlineResidents() {
        Set<Resident> result = new HashSet<>();
        for (UUID r : this.residents) {
            Resident resident = this.townManager.getResident(r);
            if (resident.isOnline()) {
                result.add(resident);
            }
        }
        return result;
    }

    public int getTotalResidents() {
        return this.residents.size();
    }

    public boolean hasWarp(UUID warp) {
        return this.warps.contains(warp);
    }

    public boolean addWarp(UUID uuid) {
        if (this.warps.add(uuid)) {
            this.dirty = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean addWarp(Warp warp) {
        if (warp.getTown() == null && this.warps.add(warp.getId())) {
            this.dirty = true;
            warp.setTown(this);
            return true;
        }
        return false;
    }

    public boolean removeWarp(Warp warp) {
        if (warp.getTown() != null && this.residents.remove(warp.getId())) {
            this.dirty = true;
            warp.setTown(null);
            return true;
        }
        return false;
    }

    public Set<UUID> getWarps() {
        return new HashSet<>(this.warps);
    }

    public TownWorld getWorld() {
        return world;
    }

    public Relationship getRelationshipWith(Town other) {
        return this.getRelationshipWith(other.getId());
    }

    public Relationship getRelationshipWith(UUID other) {
        if (this.relationships.containsKey(other)) {
            return this.relationships.get(other);
        }
        return Relationship.NEUTRAL;
    }

    public void setRelationshipWith(UUID uuid, Relationship relationship) {
        this.dirty = true;
        this.relationships.put(uuid, relationship);
    }

    public void setRelationshipWith(Town other, Relationship relationship) {
        this.setRelationshipWith(other.getId(), relationship);
    }

    public Set<ChunkLocation> getAllPlotLocations() {
        return new HashSet<>(this.plots);
    }

    public Set<Plot> getAllPlots(Predicate<Plot> predicate) {
        Set<Plot> plots = new HashSet<>();
        for (ChunkLocation loc : this.plots) {
            Plot plot = this.world.getPlot(loc);
            if (plot != null && predicate.test(plot)) {
                plots.add(plot);
            }
        }
        return plots;
    }

    public Set<Plot> getAllPlots() {
        return this.getAllPlots(p -> true);
    }

    public int getTotalPlots(Predicate<Plot> predicate) {
        int count = 0;
        for (ChunkLocation loc : this.plots) {
            Plot plot = this.world.getPlot(loc);
            if (plot != null && predicate.test(plot)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalPlots() {
        return this.getTotalPlots(p -> true);
    }

    public MessageTown serialize() {
        GameProfile mayor = hasMayor() ? getMayor().getProfile() : null;
        return new MessageTown(
            getId(), getName(), mayor, getTotalResidents(), getTotalPlots(), isOpen(), isPvP(), getSpawnpoint()
        );
    }

    public enum Relationship {
        ALLY,
        ENEMY,
        NEUTRAL
    }
}
