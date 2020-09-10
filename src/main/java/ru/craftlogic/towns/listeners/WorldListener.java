package ru.craftlogic.towns.listeners;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ru.craftlogic.api.server.Server;
import ru.craftlogic.api.world.Dimension;
import ru.craftlogic.api.world.World;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.TownWorld;

import java.io.IOException;

public class WorldListener {
    private final TownManager townManager;

    public WorldListener(TownManager townManager) {
        this.townManager = townManager;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        Server server = Server.from(FMLCommonHandler.instance().getMinecraftServerInstance());
        Dimension dimension = Dimension.fromVanilla(event.getWorld().provider.getDimensionType());
        if (this.townManager.enabledWorlds.contains(dimension.getName())) {
            TownWorld tw = TownWorld.load(this.townManager, World.fromVanilla(server, event.getWorld()));
            this.townManager.worlds.put(dimension, tw);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        TownWorld tw = this.townManager.worlds.remove(Dimension.fromVanilla(event.getWorld().provider.getDimensionType()));
        if (tw != null) {
            try {
                this.townManager.saveData();
            } catch (IOException e) {
                CraftTowns.LOGGER.error("Error saving world data", e);
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        TownWorld tw = this.townManager.worlds.get(Dimension.fromVanilla(event.getWorld().provider.getDimensionType()));
        if (tw != null) {
            try {
                this.townManager.saveData();
            } catch (IOException e) {
                CraftTowns.LOGGER.error("Error saving world data", e);
            }
        }
    }
}
