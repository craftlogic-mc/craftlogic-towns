package ru.craftlogic.towns.data;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandException;
import net.minecraft.util.JsonUtils;
import ru.craftlogic.api.economy.EconomyManager;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.Dimension;
import ru.craftlogic.api.world.PhantomPlayer;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.event.TownRemoveResidentEvent;
import ru.craftlogic.towns.utils.FloatFunction;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static ru.craftlogic.towns.CraftTowns.GSON;

public class Resident extends PhantomPlayer implements PlotOwner {
    private final TownManager townManager;
    private UUID town;
    private String title;
    private Map<ChunkLocation, Plot> plots = new HashMap<>();
    private Set<UUID> friends = new HashSet<>();

    public Resident(TownManager townManager, GameProfile profile) {
        super(townManager.getServer().getWorldManager().get(Dimension.OVERWORLD), profile); //FIXME Overworld
        this.townManager = townManager;
    }

    public boolean isMayor() {
        return this.hasTown() && this.getTown().isMayor(this);
    }

    public boolean isAssistant() {
        return this.hasTown() && this.getTown().isAssistant(this);
    }

    public boolean isAuthority() {
        return this.hasTown() && this.getTown().isAuthority(this);
    }

    public boolean hasTown() {
        return this.town != null && this.getTown() != null;
    }

    public Town getTown() {
        return this.townManager.getTown(town);
    }

    public void setTown(Town town) {
        Town oldTown = this.townManager.getTown(this.town);
        if (oldTown != null && oldTown.hasResident(this)) {
            oldTown.removeResident(this, town == null ? TownRemoveResidentEvent.Reason.BECAME_NOMAD
                                                               : TownRemoveResidentEvent.Reason.JOINED_OTHER_TOWN);
        }
        this.town = town == null ? null : town.getId();
        if (town != null && !town.hasResident(this)) {
            town.addResident(this);
        }
    }

    @Override
    public boolean isOwning(Plot plot) {
        return plot.isOwner(this);
    }

    @Override
    public boolean removePlot(Plot plot) {
        if (this.plots.remove(plot.getLocation()) != null && plot.isOwner(this)) {
            plot.setOwner((UUID) null);
            return true;
        }
        return false;
    }

    @Override
    public boolean addPlot(Plot plot) {
        if (this.plots.putIfAbsent(plot.getLocation(), plot) == null && !plot.isOwner(this)) {
            plot.setOwner(this);
            return true;
        }
        return false;
    }

    public boolean hasTitle() {
        return this.title != null;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public Set<UUID> getFriendsIds() {
        return new HashSet<>(this.friends);
    }

    public Set<Resident> getFriends() {
        return this.getFriends(r -> true);
    }

    public Set<Resident> getFriends(Predicate<Resident> filter) {
        Set<Resident> result = new HashSet<>();
        for (UUID id : this.friends) {
            Resident resident = this.townManager.getResident(id);
            if (filter.test(resident)) {
                result.add(resident);
            }
        }
        return result;
    }

    @Override
    public long getTimePlayed() {
        return getLastPlayed() - getFirstPlayed();
    }

    public float getBalance() {
        EconomyManager economyManager = this.townManager.getServer().getEconomyManager();
        return economyManager.isEnabled() ? economyManager.getBalance(this) : 0;
    }

    public boolean withdrawMoney(float amount, PaymentCallback callback) throws CommandException {
        EconomyManager economyManager = this.townManager.getServer().getEconomyManager();
        if (economyManager.isEnabled()) {
            float taken = economyManager.take(this, amount);
            if (taken == amount) {
                if (callback != null) {
                    callback.accept(taken, true, economyManager::format);
                }
                return true;
            } else {
                if (callback != null) {
                    callback.accept(amount - taken, false, economyManager::format);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean depositMoney(float amount) {
        EconomyManager economyManager = this.townManager.getServer().getEconomyManager();
        if (economyManager.isEnabled()) {
            economyManager.give(this, amount);
            return true;
        } else {
            return false;
        }
    }

    public Town.Relationship getRelationshipWith(Town town) {
        if (this.hasTown()) {
            this.getTown().getRelationshipWith(town);
        }
        return Town.Relationship.NEUTRAL;
    }

    public ResidentStatus getStatusFor(Resident other) {
        /*if (this.hasFriend(other)) {
            return ResidentStatus.FRIEND;
        } else */if (other.hasTown()) {
            Town b = other.getTown();
            if (this.hasTown()) {
                Town a = this.getTown();
                if (a.getId().equals(b.getId())) {
                    return ResidentStatus.RESIDENT;
                } else {
                    switch (a.getRelationshipWith(b)) {
                        case ALLY:
                            return ResidentStatus.ALLY;
                        case ENEMY:
                            return ResidentStatus.ENEMY;
                    }
                }
                return ResidentStatus.STRANGER;
            }
        }
        return ResidentStatus.NOMAD;
    }

    public static Resident load(TownManager townManager, GameProfile profile) {
        Resident r = new Resident(townManager, profile);
        r.load();
        return r;
    }

    public void load() {
        Path residentFile = this.townManager.getDataFolder().resolve("residents/" + getId().toString() + ".json");
        if (Files.exists(residentFile)) {
            try (Reader reader = Files.newBufferedReader(residentFile)) {
                JsonObject data = GSON.fromJson(reader, JsonObject.class);
                if (data.has("title")) {
                    this.setTitle(JsonUtils.getString(data, "title"));
                }
                if (data.has("town")) {
                    this.town = UUID.fromString(JsonUtils.getString(data, "town"));
                }
                for (TownWorld world : this.townManager.getAllWorlds()) {
                    for (Plot plot : world.getAllPlots()) {
                        if (plot.isOwner(this)) {
                            this.addPlot(plot);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void save() throws IOException {
        Path residentFile = this.townManager.getDataFolder().resolve("residents/" + this.getId().toString() + ".json");
        JsonObject r;
        if (!Files.exists(residentFile)) {
            Files.createDirectories(residentFile.getParent());
            Files.createFile(residentFile);
            r = new JsonObject();
        } else {
            try (Reader reader = Files.newBufferedReader(residentFile)) {
                r = GSON.fromJson(reader, JsonObject.class);
            }
        }
        if (this.hasTitle()) {
            r.addProperty("title", this.getTitle());
        }
        if (this.hasTown()) {
            r.addProperty("town", this.getTown().getId().toString());
        }
        Files.write(residentFile, GSON.toJson(r).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resident)) return false;

        Resident resident = (Resident) o;

        return getId().equals(resident.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public enum ResidentStatus {
        FRIEND,
        RESIDENT,
        ALLY,
        NOMAD,
        STRANGER,
        ENEMY
    }

    @FunctionalInterface
    public interface PaymentCallback {
        void accept(float amount, boolean success, FloatFunction<Text<?, ?>> formatter) throws CommandException;
    }
}
