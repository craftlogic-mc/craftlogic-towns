package ru.craftlogic.towns.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.World;
import ru.craftlogic.towns.TownManager;

import java.io.IOException;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static ru.craftlogic.towns.CraftTowns.GSON;

public class TownWorld {
    private final TownManager townManager;
    private final WeakReference<World> world;
    private final Map<UUID, Town> towns = new HashMap<>();
    private final Map<UUID, Warp> warps = new HashMap<>();
    private final Map<ChunkLocation, Plot> plots = new HashMap<>();
    private boolean dirty;

    public TownWorld(TownManager townManager, World world) {
        this.townManager = townManager;
        this.world = new WeakReference<>(world);
    }

    public static TownWorld load(TownManager townManager, World world) {
        TownWorld tw = new TownWorld(townManager, world);
        try {
            tw.load();
        } catch (Throwable thr) {
            thr.printStackTrace();
        }
        return tw;
    }

    public Path getDir() {
        return this.unwrap().getDir();
    }

    public boolean hasTown(String name) {
        return getTown(name) != null;
    }

    public boolean hasTown(UUID uuid) {
        return this.towns.containsKey(uuid);
    }

    public Town getTown(UUID uuid) {
        return this.towns.get(uuid);
    }

    public Town getTown(String name) {
        for (Town town : this.towns.values()) {
            if (town.getName().equalsIgnoreCase(name)) {
                return town;
            }
        }
        return null;
    }

    public boolean addTown(Town town) {
        if (!this.towns.containsKey(town.getId())) {
            this.towns.put(town.getId(), town);
            for (Plot plot : town.getAllPlots(p -> !this.hasPlot(p.getLocation()))) {
                this.addPlot(plot);
            }
            this.dirty = true;
            return true;
        }
        return false;
    }

    public boolean removeTown(Town town) {
        if (this.towns.remove(town.getId()) == town) {
            for (Plot plot : town.getAllPlots(p -> this.hasPlot(p.getLocation()))) {
                this.removePlot(plot.getLocation());
            }
            this.dirty = true;
        }
        return true;
    }

    public boolean removeTown(String name) {
        Town town = this.getTown(name);
        if (town != null) {
            this.towns.remove(town.getId());
            for (Plot plot : town.getAllPlots(p -> this.hasPlot(p.getLocation()))) {
                this.removePlot(plot.getLocation());
            }
            this.dirty = true;
            return true;
        }
        return false;
    }

    public Set<Town> getAllTowns() {
        return new HashSet<>(this.towns.values());
    }

    public boolean hasWarp(String name) {
        return getWarp(name) != null;
    }

    public boolean hasWarp(UUID uuid) {
        return this.warps.containsKey(uuid);
    }

    public Warp getWarp(UUID uuid) {
        return this.warps.get(uuid);
    }

    public Warp getWarp(String name) {
        for (Warp warp : this.warps.values()) {
            if (warp.getName().equalsIgnoreCase(name)) {
                return warp;
            }
        }
        return null;
    }

    public boolean addWarp(Warp warp) {
        if (!this.warps.containsKey(warp.getId())) {
            this.warps.put(warp.getId(), warp);
            this.dirty = true;
            return true;
        }
        return false;
    }

    public boolean removeWarp(Warp warp) {
        if (this.warps.remove(warp.getId()) == warp) {
            this.dirty = true;
        }
        return true;
    }

    public boolean removeWarp(String name) {
        Warp warp = this.getWarp(name);
        if (warp != null) {
            this.warps.remove(warp.getId());
            this.dirty = true;
            return true;
        }
        return false;
    }

    public Set<Warp> getAllWarps() {
        return new HashSet<>(this.warps.values());
    }

    public boolean hasPlot(ChunkLocation location) {
        return this.plots.containsKey(new ChunkLocation(location));
    }

    public Plot getPlot(ChunkLocation location) {
        return this.plots.get(new ChunkLocation(location));
    }

    public boolean addPlot(Plot plot) {
        ChunkLocation location = plot.getLocation();
        if (this.plots.putIfAbsent(new ChunkLocation(location), plot) == null) {
            Town t = plot.getTown();
            if (t != null) {
                t.addPlot(plot);
            }
            this.dirty = true;
            return true;
        }
        return false;
    }

    public boolean removePlot(ChunkLocation location) {
        Plot plot = this.plots.remove(new ChunkLocation(location));
        if (plot != null) {
            Town t = plot.getTown();
            if (t != null) {
                t.removePlot(plot);
            }
            this.dirty = true;
            return true;
        }
        return false;
    }

    public Set<Plot> getAllPlots() {
        return new HashSet<>(this.plots.values());
    }

    public World unwrap() {
        return this.world.get();
    }

    public String getName() {
        return this.unwrap().getName();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void save() throws IOException {
        if (!this.dirty) return;
        this.dirty = false;
        World world = this.world.get();
        Objects.requireNonNull(world);
        Path worldDir = this.getDir();
        if (!Files.exists(worldDir)) {
            Files.createDirectories(worldDir);
        }
        JsonObject plots = new JsonObject();
        Path plotsFile = worldDir.resolve("plots.yml");
        for (Plot plot : this.getAllPlots()) {
            ChunkLocation location = plot.getLocation();
            JsonObject p = new JsonObject();
            plot.save(p);
            plots.add(location.getChunkX() + ":" + location.getChunkZ(), p);
        }
        Files.write(plotsFile, GSON.toJson(plots).getBytes(StandardCharsets.UTF_8));
    }

    public void load() throws IOException {
        World world = this.world.get();
        Objects.requireNonNull(world);
        String worldName = world.getName();
        Path worldDir = this.getDir();
        Path plotsFile = worldDir.resolve("plots.yml");
        Map<UUID, Set<Plot>> townPlots = new HashMap<>();
        if (Files.exists(plotsFile)) {
            try (Reader reader = Files.newBufferedReader(plotsFile)) {
                JsonObject data = GSON.fromJson(reader, JsonObject.class);
                for (Map.Entry<String, JsonElement> e : data.entrySet()) {
                    String[] coords = e.getKey().split(":");
                    if (coords.length == 2) {
                        ChunkLocation location = new ChunkLocation(world.unwrap(),
                            Integer.parseInt(coords[0]),
                            Integer.parseInt(coords[1])
                        );
                        JsonObject p = e.getValue().getAsJsonObject();
                        Plot plot = Plot.load(this.townManager, this, location, p);
                        UUID townId = plot.getTownId();
                        if (townId != null) {
                            townPlots.computeIfAbsent(townId, l -> new HashSet<>()).add(plot);
                        }
                        this.addPlot(plot);
                    }
                }
            }
        }
        Path townsFolder = worldDir.resolve("towns/");
        if (Files.exists(townsFolder)) {
            Files.list(townsFolder).filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                String fileName = file.getFileName().toString();
                UUID townId = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonObject t = GSON.fromJson(reader, JsonObject.class);
                    Town town = Town.load(this.townManager, this, townId, t);
                    if (townPlots.containsKey(townId)) {
                        for (Plot plot : townPlots.get(townId)) {
                            town.addPlot(plot);
                        }
                    }
                    System.out.println("Loaded town " + townId + " (" + town.getName() + ")");
                    this.addTown(town);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Path warpsFolder = worldDir.resolve("warps/");
        if (Files.exists(warpsFolder)) {
            Files.list(townsFolder).filter(p -> p.toString().endsWith(".json")).forEach(file -> {
                String fileName = file.getFileName().toString();
                UUID warpId = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonObject w = GSON.fromJson(reader, JsonObject.class);
                    Warp warp = Warp.load(this.townManager, this, warpId, w);
                    Town t = warp.getTown();
                    if (t != null) {
                        System.out.println("Loaded warp " + warpId + " (" + warp.getName() + ")");
                        t.addWarp(warp);
                    } else {
                        System.out.println("Warp loading failed: " + warpId + " (" + warp.getName() + "). Unknown host-town!");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
