package ru.craftlogic.towns.common.command;

import net.minecraft.command.CommandException;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import ru.craftlogic.api.command.CommandBase;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.api.server.PlayerManager;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.CommandSender;
import ru.craftlogic.api.world.Location;
import ru.craftlogic.api.world.Player;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;
import ru.craftlogic.towns.data.TownWorld;
import ru.craftlogic.towns.data.plot.types.PlotType;
import ru.craftlogic.towns.event.*;
import ru.craftlogic.towns.network.message.MessageDeleteTown;
import ru.craftlogic.towns.utils.RandomSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommandTown extends CommandBase {
    public CommandTown() {
        super("town", 0, "",
            "here|autoplot|assistants|reload|delete|remove|abandon|ruin|list|leave",
            "claim",
            "claim <type:PlotType>",
            "spawn",
            "spawn <town:TownWithSpawn>",
            "delete|remove|abandon|ruin <town:Town>",
            "join",
            "join <town:TownOpen>",
            "create <name>",
            "invite <target:Player>",
            "bank",
            "bank <town:Town>",
            "bank deposit|withdraw <amount>",
            "bank deposit|withdraw <amount> <town:Town>",
            "assistants add|remove <target:Player>",
            "set public|private|spawn",
            "set pvp",
            "set pvp true|false|on|off",
            "set name <name>",
            "<town:Town>"
        );
        Collections.addAll(aliases, "t");
    }

    @Override
    protected void execute(CommandContext ctx) throws Throwable {
        TownManager townManager = ctx.server().getManager(TownManager.class);
        PlayerManager playerManager = ctx.server().getPlayerManager();
        if (ctx.hasAction(0)) {
            switch (ctx.action(0)) {
                case "here": {
                    Town town = townManager.getTown(ctx.senderAsLocatable().getLocation());
                    if (town != null) {
                        townManager.sendTownInfo(town, ctx.sender());
                    } else {
                        throw new CommandException("town.error.no-town-here");
                    }
                    break;
                }
                case "spawn": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    if (ctx.has("town")) {
                        String targetTownName = ctx.get("town").asString();
                        Town town = townManager.getTown(targetTownName);
                        if (town != null) {
                            if (town.hasSpawnpoint()) {
                                if (town.isOpen() || town.hasResident(resident) || ctx.checkPermission(false, "town.private-bypass", 2)) {
                                    Location targetSpawn = town.getSpawnpoint();
                                    player.teleport(targetSpawn);
                                    ctx.sendMessage(
                                        Text.translation("town.spawn.welcome-town").yellow()
                                            .arg(town.getName(), Text::gold)
                                    );
                                } else {
                                    throw new CommandException("town.error.not-open", town.getName());
                                }
                            } else {
                                throw new CommandException("town.error.no-spawn");
                            }
                        } else {
                            throw new CommandException("town.error.no-town", targetTownName);
                        }
                    } else {
                        if (resident.hasTown() && resident.getTown().hasSpawnpoint()) {
                            Location targetSpawn = resident.getTown().getSpawnpoint();
                            player.teleport(targetSpawn);
                            ctx.sendMessage(
                                Text.translation("town.spawn.welcome-town").yellow()
                                    .arg(resident.getTown().getName(), Text::gold)
                            );
                        } else {
                            List<Town> spawns = new ArrayList<>();
                            for (TownWorld tw : townManager.getAllWorlds()) {
                                for (Town town : tw.getAllTowns()) {
                                    if (town.hasSpawnpoint() && (town.isOpen() || player.hasPermission("town.private-bypass"))) {
                                        spawns.add(town);
                                    }
                                }
                            }
                            if (!spawns.isEmpty()) {
                                Town t = spawns.get(new Random().nextInt(spawns.size()));
                                player.teleport(t.getSpawnpoint());
                                ctx.sendMessage(
                                    Text.translation("town.spawn.welcome-town").yellow()
                                        .arg(t.getName(), Text::gold)
                                );
                            } else {
                                throw new CommandException("town.spawn.not-found");
                            }
                        }
                    }
                    break;
                }
                case "join": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    if (resident.hasTown()) {
                        throw new CommandException("town.error.already-resident");
                    } else {
                        Town town;
                        if (ctx.has("town")) {
                            String townName = ctx.get("town").asString();
                            if ((town = townManager.getTown(townName)) == null) {
                                throw new CommandException("town.error.no-town", townName);
                            }
                        } else if ((town = townManager.getTown(player.getLocation())) == null) {
                            throw new CommandException("town.error.no-town-here");
                        }
                        if (!town.isOpen()) {
                            throw new CommandException("town.error.not-open", town.getName());
                        } else if (town.addResident(resident)) {
                            ctx.sendMessage(
                                Text.translation("town.join.success").green()
                                    .arg(town.getName(), Text::darkGreen)
                            );
                            town.broadcast(resident,
                                Text.translation("town.join.success-broadcast").green()
                                    .arg(player.getName(), Text::darkGreen)
                                    .arg(town.getName(), Text::darkGreen)
                            );
                        } else {
                            throw new CommandException("commands.generic.unknownFailure");
                        }
                    }
                    break;
                }
                case "leave": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    Town town = resident.getTown();
                    if (town != null) {
                        int total = town.getTotalPlots(p -> p.isOwner(resident));
                        if (total == 0) {
                            town.removeResident(resident, TownRemoveResidentEvent.Reason.BECAME_NOMAD);
                            ctx.sendMessage(
                                Text.translation("town.leave.success").yellow()
                                    .arg(town.getName(), Text::gold)
                            );
                            town.broadcast(resident,
                                Text.translation("town.leave.success-broadcast").yellow()
                                    .arg(resident.getName(), Text::gold)
                                    .arg(town.getName(), Text::gold)
                            );
                        } else {
                            throw new CommandException("town.leave.has-plots", total);
                        }
                    } else {
                        throw new CommandException("town.error.not-a-resident");
                    }
                    break;
                }
                case "invite": {
                    Player player = ctx.senderAsPlayer();
                    Resident target = townManager.getResident(ctx.get("target").asPlayer());
                    Resident resident = townManager.getResident(player);
                    if (!resident.hasTown()) {
                        throw new CommandException("town.error.not-a-resident");
                    } else if (resident.isMayor() || resident.isAssistant()) {
                        Town town = resident.getTown();
                        String targetPlayerName = target.getName();
                        TownInviteResidentEvent event = new TownInviteResidentEvent(town, resident, target);
                        if (!MinecraftForge.EVENT_BUS.post(event)) {
                            if (townManager.sendInvitation(resident, target, town)) {
                                ctx.sendMessage(
                                    Text.translation("town.invite.success").green()
                                        .arg(targetPlayerName, Text::darkGreen)
                                        .arg(town.getName(), Text::darkGreen)
                                );
                                town.broadcast(resident,
                                    Text.translation("town.invite.success-broadcast").green()
                                        .arg(player.getName(), Text::darkGreen)
                                        .arg(targetPlayerName, Text::darkGreen)
                                        .arg(town.getName(), Text::darkGreen)
                                );
                            } else {
                                throw new CommandException("town.invite.already", targetPlayerName);
                            }
                        }
                    } else {
                        throw new CommandException("town.invite.no-permission");
                    }
                    break;
                }
                case "create": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    if (!player.hasPermission("town.create")) {
                        throw new CommandException("town.create.no-permission");
                    }
                    if (resident.hasTown()) {
                        throw new CommandException("town.error.already-resident");
                    }
                    Location playerLocation = player.getLocation();
                    TownWorld tw = townManager.getWorld(playerLocation.getWorld());
                    if (tw == null) {
                        throw new CommandException("town.disabled-in-world");
                    }

                    String name = ctx.get("name").asString();
                    for (TownWorld world : townManager.getAllWorlds()) {
                        if (world.hasTown(name)) {
                            throw new CommandException("town.create.already-exists");
                        }
                    }
                    Plot plot = townManager.getPlot(playerLocation);
                    if (plot != null) {
                        throw new CommandException("town.create.clashes-with-plot");
                    }
                    float price = townManager.townCreationPrice;
                    if (price > 0) {
                        if (!resident.withdrawMoney(price, (amt, success, fmt) -> {
                            if (!success) {
                                throw new CommandException("town.create.insufficient-money", fmt.apply(amt).build());
                            }
                        })) {
                            return;
                        }
                    }
                    UUID townId = null;
                    while (townId == null || townManager.getTown(townId) != null) {
                        townId = UUID.nameUUIDFromBytes((UUID.randomUUID().toString() + name).getBytes(StandardCharsets.UTF_8));
                    }
                    Town town = new Town(townManager, townId, tw, name);
                    town.setMayor(player);
                    town.addResident(resident);
                    town.setSpawnpoint(playerLocation);
                    Plot homePlot = new Plot(townManager, tw, new ChunkLocation(
                        playerLocation.getWorld(),
                        playerLocation.getChunkX(),
                        playerLocation.getChunkZ()
                    ));
                    homePlot.setType(TownManager.findPlotType("centre"));
                    town.addPlot(homePlot);
                    TownCreationEvent event = new TownCreationEvent(town, resident, resident);
                    if (!MinecraftForge.EVENT_BUS.post(event)) {
                        if (tw.addTown(town)) {
                            try {
                                townManager.saveData();
                                ctx.sendMessage(
                                    Text.translation("town.create.success").green()
                                        .arg(name, Text::darkGreen)
                                );
                                town.broadcast(resident,
                                    Text.translation("town.create.success-broadcast").green()
                                        .arg(player.getName(), Text::darkGreen)
                                        .arg(name, Text::darkGreen)
                                );
                                CraftTowns.NETWORK.broadcast(town.serialize());
                                for (Player p : playerManager.getAllOnline()) {
                                    Resident r = townManager.getResident(p);
                                    if (r != null) {
                                        CraftTowns.NETWORK.sendTo(p.getEntity(), homePlot.serialize(r));
                                    }
                                }
                                return;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        throw new CommandException("commands.generic.unknownFailure");
                    }
                    break;
                }
                case "claim": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    TownWorld tw = townManager.getWorld(player.getWorld());
                    if (tw == null) {
                        throw new CommandException("town.disabled-in-world");
                    }
                    if (resident.isMayor() || resident.isAssistant()) {
                        Town town = resident.getTown();
                        ChunkLocation location = new ChunkLocation(player.getLocation());
                        if (townManager.getPlot(location) != null) {
                            throw new CommandException("town.claim.already");
                        }
                        PlotType type = TownManager.findPlotType(ctx.getIfPresent("type", CommandContext.Argument::asString)
                            .orElse("default"));
                        if (type == null) {
                            throw new CommandException("town.error.unknown-plot-type");
                        }
                        boolean isConnected = false;
                        for (EnumFacing side : EnumFacing.HORIZONTALS) {
                            Plot p = townManager.getPlot(location.offset(side));
                            if (p != null) {
                                if (p.getTown() == town) {
                                    if (type.isOutpost()) {
                                        throw new CommandException("town.create.outpost-too-close");
                                    }
                                    isConnected = true;
                                } else {
                                    throw new CommandException("town.claim.other-too-close");
                                }
                            }
                        }
                        if (isConnected) {
                            Plot plot = new Plot(townManager, tw, location);
                            plot.setType(type);
                            TownClaimEvent event = new TownClaimEvent(town, location, plot);
                            if (!MinecraftForge.EVENT_BUS.post(event)) {
                                if (town.addPlot(plot)) {
                                    CraftTowns.NETWORK.broadcast(town.serialize());
                                    for (Player p : playerManager.getAllOnline()) {
                                        Resident r = townManager.getResident(p);
                                        if (r != null) {
                                            CraftTowns.NETWORK.sendTo(p.getEntity(), plot.serialize(r));
                                        }
                                    }
                                    ctx.sendMessage(
                                        Text.translation("town.claim.success.default").green()
                                            .arg(location.getChunkX(), Text::darkGreen)
                                            .arg(location.getChunkZ(), Text::darkGreen)
                                    );
                                } else {
                                    throw new CommandException("commands.generic.unknownFailure");
                                }
                            }
                        } else {
                            throw new CommandException("town.error.too-far-from-town");
                        }
                    } else {
                        throw new CommandException("town.claim.no-permission");
                    }
                    break;
                }
                case "list": {
                    Resident resident = null;
                    if (ctx.sender() instanceof Player) {
                        resident = townManager.getResident(ctx.senderAsPlayer());
                    }
                    Text<?, ?> separator = Text.translation("town.list.separator");
                    Text<?, ?> openCities = Text.string();
                    Text<?, ?> closedCities = Text.string();
                    boolean first = true;
                    for (TownWorld tw : townManager.getAllWorlds()) {
                        for (Town town : tw.getAllTowns()) {
                            if (town.isOpen()) {
                                if (first) {
                                    first = false;
                                } else {
                                    openCities.append(separator);
                                }
                                if (resident == null || !town.hasResident(resident)) {
                                    openCities.append(Text.string(town.getName()).green());
                                } else {
                                    openCities.append(Text.string(town.getName()).gold().underlined());
                                }
                            } else {
                                if (first) {
                                    first = false;
                                } else {
                                    closedCities.append(separator);
                                }
                                if (resident == null || !town.hasResident(resident)) {
                                    closedCities.append(Text.string(town.getName()).red());
                                } else {
                                    closedCities.append(Text.string(town.getName()).gold().underlined());
                                }
                            }
                        }
                    }
                    ctx.sendMessage(
                        Text.translation("town.list.open").yellow()
                            .arg(openCities.build())
                    );
                    ctx.sendMessage(
                        Text.translation("town.list.closed").yellow()
                            .arg(closedCities.build())
                    );
                    break;
                }
                case "assistants": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    if (ctx.hasAction(1)) {
                        switch (ctx.action(1)) {
                            case "add": {
                                Town town = resident.getTown();
                                if (town == null) {
                                    throw new CommandException("town.error.not-a-resident");
                                }
                                if (!town.isMayor(resident)) {
                                    throw new CommandException("town.error.not-a-mayor");
                                } else {
                                    Resident target = townManager.getResident(ctx.get("target").asPlayer());
                                    String targetPlayerName = target.getName();
                                    if (town.addAssistant(target.getId())) {
                                        player.sendMessage(
                                            Text.translation("town.assistants.new").green()
                                                .arg(targetPlayerName, Text::darkGreen)
                                        );
                                    } else {
                                        throw new CommandException("town.assistants.already", targetPlayerName);
                                    }
                                }
                                break;
                            }
                            case "remove": {
                                Town town = resident.getTown();
                                if (town == null) {
                                    throw new CommandException("town.error.not-a-resident");
                                }
                                if (!town.isMayor(resident)) {
                                    throw new CommandException("town.error.not-a-mayor");
                                } else {
                                    Resident target = townManager.getResident(ctx.get("target").asPlayer());
                                    String targetPlayerName = target.getName();
                                    if (town.removeAssistant(target.getId())) {
                                        ctx.sendMessage(
                                            Text.translation("town.assistants.removed").red()
                                                .arg(targetPlayerName, Text::darkRed)
                                        );
                                    } else {
                                        throw new CommandException("town.assistants.not", targetPlayerName);
                                    }
                                }
                                break;
                            }
                            default: {
                                String townName = ctx.get("town").asString();
                                Town town = townManager.getTown(townName);
                                if (town != null) {
                                    sendAssistants(town, townManager, player);
                                } else {
                                    throw new CommandException("town.error.no-town", townName);
                                }
                                break;
                            }
                        }
                    } else {
                        Town town = resident.getTown();
                        if (town == null) {
                            throw new CommandException("town.error.not-a-resident");
                        } else {
                            sendAssistants(town, townManager, player);
                        }
                    }
                    break;
                }
                case "autoplot": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    if (!resident.hasTown()) {
                        throw new CommandException("town.error.not-a-resident");
                    } else {
                        Town town = resident.getTown();
                        if (town.getTotalPlots(p -> p.isOwner(resident)) > 0 && !ctx.checkPermission(false, "town.admin", 2)) {
                            throw new CommandException("town.autoplot.newbies-only");
                        }
                        RandomSet<Plot> availablePlots = new RandomSet<>(
                            town.getAllPlots(p -> !p.hasOwner() && p.isForSale() && !p.isLocked() && resident.getBalance() >= p.getPrice())
                        );

                        if (!availablePlots.isEmpty()) {
                            Plot p = availablePlots.pollRandom(new Random());

                            if (player.teleport(p.getSafeTeleportLocation())) {
                                ctx.sendMessage(
                                    Text.translation("town.autoplot.success").yellow()
                                );
                            }
                        } else {
                            throw new CommandException("town.autoplot.no-available");
                        }
                    }
                    break;
                }
                case "remove":
                case "abandon":
                case "ruin":
                case "delete": {
                    if (!ctx.checkPermission(false, "town.delete", 2)) {
                        throw new CommandException("town.delete.no-permission");
                    }
                    TownWorld tw = null;
                    Town town = null;
                    if (ctx.has("town")) {
                        String name = ctx.get("town").asString();
                        for (TownWorld world : townManager.getAllWorlds()) {
                            town = world.getTown(name);
                            if (town != null) {
                                tw = world;
                                break;
                            }
                        }
                        if (town == null) {
                            throw new CommandException("town.error.no-town", name);
                        }
                    } else {
                        Player player = ctx.senderAsPlayer();
                        tw = townManager.getWorld(player.getWorld().getDimension());
                        if (tw == null) {
                            throw new CommandException("town.disabled-in-world");
                        }
                        town = townManager.getTown(player.getLocation());
                        if (town == null) {
                            throw new CommandException("town.error.no-town-here");
                        }
                    }
                    if (ctx.checkPermission(false, "town.admin", 2)) {
                        TownDeletionEvent event = new TownDeletionEvent(town);
                        if (!MinecraftForge.EVENT_BUS.post(event)) {
                            if (tw.removeTown(town)) {
                                try {
                                    town.delete();
                                    ctx.sendMessage(
                                        Text.translation("town.delete.success").yellow()
                                            .arg(town.getName(), Text::gold)
                                    );
                                    town.broadcast(ctx.sender(),
                                        Text.translation("town.delete.success-broadcast").yellow()
                                            .arg(town.getName(), Text::gold)
                                    );
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                CraftTowns.NETWORK.broadcast(new MessageDeleteTown(town.getId()));
                                return;
                            }
                            throw new CommandException("commands.generic.unknownFailure");
                        }
                        return;
                    } else {
                        Player player = ctx.senderAsPlayer();
                        if (town.isMayor(player) || player.hasPermission("town.admin")) {
                            TownDeletionEvent event = new TownDeletionEvent(town);
                            if (!MinecraftForge.EVENT_BUS.post(event)) {
                                if (tw.removeTown(town)) {
                                    try {
                                        townManager.saveData();
                                        ctx.sendMessage(
                                            Text.translation("town.delete.success").yellow()
                                                .arg(town.getName(), Text::gold)
                                        );
                                        town.broadcast(player,
                                            Text.translation("town.delete.success-broadcast").yellow()
                                                .arg(town.getName(), Text::gold)
                                        );
                                        return;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    CraftTowns.NETWORK.broadcast(new MessageDeleteTown(town.getId()));
                                }
                                throw new CommandException("commands.generic.unknownFailure");
                            }
                            return;
                        } else {
                            throw new CommandException("town.error.not-a-mayor");
                        }
                    }
                }
                case "set": {
                    Player player = ctx.senderAsPlayer();
                    Resident resident = townManager.getResident(player);
                    Town town = resident.getTown();
                    if (town == null) {
                        throw new CommandException("town.error.not-a-resident");
                    }
                    if (!town.isMayor(resident) || !player.hasPermission("town.admin")) {
                        throw new CommandException("town.error.not-a-mayor");
                    }
                    switch (ctx.action(1)) {
                        case "spawn": {
                            town.setSpawnpoint(player.getLocation());
                            ctx.sendMessage(
                                Text.translation("town.set.spawn").yellow()
                            );
                            break;
                        }
                        case "tax": {
                            throw new CommandException("Not implemented yet!");
                        }
                        case "mayor": {
                            throw new CommandException("Not implemented yet!");
                        }
                        case "name": {
                            String oldName = town.getName();
                            String newName = ctx.get("name").asString();
                            TownRenameEvent event = new TownRenameEvent(town, oldName, newName);
                            if (!MinecraftForge.EVENT_BUS.post(event)) {
                                town.setName(newName);
                                player.sendMessage("Renamed town '" + oldName + "' to '" + newName + "'");
                                CraftTowns.NETWORK.broadcast(town.serialize());
                            }
                            break;
                        }
                        case "public": {
                            if (!town.isOpen()) {
                                town.setOpen(true);
                                player.sendMessage("Now " + town.getName() + " is public!");
                            } else {
                                player.sendMessage("Already public!");
                            }
                            break;
                        }
                        case "private": {
                            if (town.isOpen()) {
                                town.setOpen(false);
                                player.sendMessage("Now " + town.getName() + " is private!");
                            } else {
                                player.sendMessage("Already private!");
                            }
                            break;
                        }
                        case "pvp": {
                            boolean value = ctx.hasAction(2)
                                ? ctx.action(2).equalsIgnoreCase("on") || ctx.action(2).equalsIgnoreCase("true")
                                : !town.isPvP();

                            town.setPvP(value);
                            player.sendMessage("PvP " + (value ? "enabled" : "disabled"));
                            break;
                        }
                    }
                    break;
                }
                case "reload": {
                    if (ctx.checkPermission(true, "town.admin", 2)) {
                        ctx.sendMessage(
                            Text.translation("town.reload.initiated").yellow()
                        );
                        boolean result = false;
                        try {
                            townManager.load();
                            result = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (result) {
                            ctx.sendMessage(
                                Text.translation("town.reload.success").green()
                            );
                        } else {
                            throw new CommandException("town.reload.failed");
                        }
                    }
                    break;
                }
            }
        } else if (ctx.has("town")) {
            String townName = ctx.get("town").asString();
            Town t = townManager.getTown(townName);
            if (t != null) {
                townManager.sendTownInfo(t, ctx.sender());
            } else {
                throw new CommandException("town.error.no-town", townName);
            }
        } else {
            Player player = ctx.senderAsPlayer();
            Resident resident = townManager.getResident(player);
            if (!resident.hasTown()) {
                throw new CommandException("town.error.not-a-resident");
            } else {
                townManager.sendTownInfo(resident.getTown(), player);
            }
        }
    }

    private static void sendAssistants(Town town, TownManager townManager, CommandSender receiver) {
        Set<UUID> assistants = town.getAssistants();
        Text<?, ?> list = Text.string();
        Text<?, ?> separator = Text.translation("town.assistants.separator");
        boolean first = true;
        for (UUID a : assistants) {
            if (first) {
                first = false;
            } else {
                list.append(separator);
            }
            Resident assistant = townManager.getResident(a);
            list.append(Text.string(assistant.getName()).color(assistant.isOnline() ? TextFormatting.GREEN : TextFormatting.GRAY));
        }
        receiver.sendMessage(
            Text.translation("town.assistants.caption").yellow()
                .arg(town.getMayor().getName(), Text::gold)
                .arg(assistants.isEmpty() ? Text.translation("resident.no-one") : list)
        );
    }
}
