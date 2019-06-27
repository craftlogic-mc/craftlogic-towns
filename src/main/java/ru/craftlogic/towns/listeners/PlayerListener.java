package ru.craftlogic.towns.listeners;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import ru.craftlogic.api.world.Location;
import ru.craftlogic.api.world.Player;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;
import ru.craftlogic.towns.data.TownWorld;
import ru.craftlogic.towns.utils.RandomSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerListener {
    private final TownManager townManager;

    public PlayerListener(TownManager townManager) {
        this.townManager = townManager;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = Player.from((EntityPlayerMP) event.player);
        if (player.getLastPlayed() - player.getLastPlayed() > 100L) {
            List<Location> spawns = new ArrayList<>();
            for (TownWorld tw : this.townManager.getAllWorlds()) {
                for (Town town : tw.getAllTowns()) {
                    if (town.hasSpawnpoint() && (town.isOpen() || player.hasPermission("town.private-bypass"))) {
                        spawns.add(town.getSpawnpoint());
                    }
                }
            }
            if (!spawns.isEmpty()) {
                Location spawn = spawns.get(new Random().nextInt(spawns.size()));
                player.teleport(spawn);
            }
        }
        for (TownWorld world : townManager.getAllWorlds()) {
            for (Town town : world.getAllTowns()) {
                CraftTowns.NETWORK.sendTo(event.player, town.serialize());
            }
            Resident r = this.townManager.getResident(player);
            if (r != null) {
                for (Plot plot : world.getAllPlots()) {
                    CraftTowns.NETWORK.sendTo(event.player, plot.serialize(r));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = Player.from((EntityPlayerMP) event.player);
        if (this.townManager.isOverridesRespawn()) {
            RandomSet<Location> spawns = new RandomSet<>();
            for (TownWorld tw : this.townManager.getAllWorlds()) {
                for (Town town : tw.getAllTowns()) {
                    if (town.hasSpawnpoint() && (town.isOpen() || player.hasPermission("town.private-bypass"))) {
                        spawns.add(town.getSpawnpoint());
                    }
                }
            }
            if (!spawns.isEmpty()) {
                player.teleport(spawns.pollRandom(new Random()));
            }
        }
    }
}
