package ru.craftlogic.towns;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.craftlogic.api.server.Server;
import ru.craftlogic.api.util.ConfigurableManager;
import ru.craftlogic.api.world.Dimension;
import ru.craftlogic.api.world.World;

public class WorldTownManager extends ConfigurableManager {
    private static final Logger LOGGER = LogManager.getLogger("WorldTownManager");

    //private final Map<UUID, Region> regions = new HashMap<>();
    private final Dimension dimension;

    public WorldTownManager(Server server, World world, Logger logger) {
        super(server, world.getDir().resolve("towns.json"), logger);
        this.dimension = world.getDimension();
    }

    @Override
    protected String getDefaultConfig() {
        return null;
    }

    @Override
    protected void load(JsonObject regions) {
        /*for (Map.Entry<String, JsonElement> entry : regions.entrySet()) {
            UUID id = UUID.fromString(entry.getKey());
            this.regions.put(id, new Region(this.dimension, id, entry.getValue().getAsJsonObject()));
        }
        LOGGER.info("Loaded {} towns for world {}", this.regions.size(), this.dimension.getName());*/
    }

    @Override
    protected void save(JsonObject regions) {
        /*for (Map.Entry<UUID, Region> entry : this.regions.entrySet()) {
            regions.add(entry.getKey().toString(), entry.getValue().toJson());
        }
        LOGGER.info("Saved {} towns for world {}", regions.size(), this.dimension.getName());*/
    }
}
