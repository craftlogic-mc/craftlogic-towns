package ru.craftlogic.towns.common.command;

import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import ru.craftlogic.api.command.CommandBase;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.api.server.PlayerManager;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.Player;
import ru.craftlogic.api.world.World;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Bank;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;
import ru.craftlogic.towns.data.plot.options.PlotOption;
import ru.craftlogic.towns.event.PlotOptionUpdateEvent;
import ru.craftlogic.towns.event.PlotUpdateEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

public class CommandPlot extends CommandBase {
    public CommandPlot() {
        super("plot", 0, "",
            "option <option:PlotOption> <value>...",
            "backup|restore|abandon|claim|unlock|stopselling|ss",
            "expel <target:Player>",
            "border",
            "border <block:Block>",
            "lock",
            "lock <price>",
            "sell",
            "sell <price>"
        );
    }

    @Override
    protected void execute(CommandContext ctx) throws Throwable {
        TownManager townManager = ctx.server().getManager(TownManager.class);
        PlayerManager playerManager = ctx.server().getPlayerManager();
        Player player = ctx.senderAsPlayer();
        Resident resident = townManager.getResident(player);
        Plot plot = townManager.getPlot(player.getLocation());
        if (plot != null) {
            if (ctx.hasAction(0)) {
                switch (ctx.action(0)) {
                    case "claim": {
                        if (plot.isOwner(resident)) {
                            throw new CommandException("plot.claim.yours");
                        } else if (plot.isForSale()) {
                            if (plot.isLocked()) {
                                throw new CommandException("plot.error.locked");
                            }
                            if (plot.hasTown()) {
                                if (resident.getTown() != plot.getTown()) {
                                    throw new CommandException("town.claim.not-a-resident");
                                }
                            }
                            Resident oldOwner = plot.getOwner();
                            float price = plot.getPrice();
                            if (price > 0) {
                                if (!resident.withdrawMoney(price, (amt, success, fmt) -> {
                                    if (success) {
                                        plot.setLocked(false);
                                        plot.setForSale(false);
                                        plot.setOwner(resident);
                                        if (oldOwner != null) {
                                            oldOwner.depositMoney(amt);
                                            oldOwner.sendMessage(
                                                Text.translation("plot.claim.success-owner").green()
                                                    .arg(resident.getName(), Text::darkGreen)
                                                    .arg(plot.getLocation().toString(), Text::darkGreen)
                                                    .arg(fmt.apply(amt).darkGreen())
                                            );
                                        } else if (plot.hasTown()) {
                                            Town town = plot.getTown();
                                            Bank bank = town.getBank();
                                            if (bank != null) {
                                                bank.deposit(amt);
                                            }
                                            //...
                                        }
                                        player.sendMessage(
                                            Text.translation("plot.unlock.successful").green()
                                        );
                                    } else {
                                        throw new CommandException("plot.claim.insufficient-money", fmt.apply(amt));
                                    }
                                })) {
                                    return;
                                }
                            } else if (price < 0) {
                                throw new CommandException("plot.claim.invalid-price");
                            } else {
                                plot.setForSale(false);
                                plot.setOwner(resident);
                            }
                            Set<Plot> plots = plot.findNearbyPlots(po -> !po.isLocked() && !po.hasOwner() && po.isForSale());
                            for (Plot po : plots) {
                                if (po != plot) {
                                    po.setLocked(true);
                                }
                                po.setOwner(resident);
                                MinecraftForge.EVENT_BUS.post(new PlotUpdateEvent(po));

                                for (Player p : playerManager.getAllOnline()) {
                                    Resident r = townManager.getResident(p);
                                    if (r != null) {
                                        CraftTowns.NETWORK.sendTo(p.getEntity(), po.serialize(r));
                                    }
                                }
                            }
                            if (plots.size() > 1) {
                                player.sendMessage(
                                    Text.translation("plot.claim.success.nearby-locked").green()
                                        .arg(plots.size() - 1, Text::darkGreen)
                                );
                            } else {
                                player.sendMessage(
                                    Text.translation("plot.claim.success.alone").green()
                                );
                            }
                            plot.getWorld().markDirty();
                        } else {
                            throw new CommandException("plot.claim.not-for-sale");
                        }
                        break;
                    }
                    case "sell": {
                        if (plot.isLocked()) {
                            throw new CommandException("plot.error.locked");
                        } else if (plot.isOwner(resident) || !plot.hasOwner() && plot.hasTown() && plot.getTown().isAuthority(resident)) {
                            if (!plot.isForSale()) {
                                float price = plot.getPrice();
                                if (ctx.has("price")) {
                                    price = ctx.get("price").asFloat(0, Float.MAX_VALUE);
                                } else if (price < 0) {
                                    throw new CommandException("plot.sell.invalid-price");
                                }
                                plot.setForSale(true);
                                plot.setPrice(price);
                                plot.getWorld().markDirty();
                                if (price > 0) {
                                    player.sendMessage(
                                        Text.translation("plot.sell.ok.price").yellow()
                                            .arg(townManager.price(price).gold())
                                    );
                                } else {
                                    player.sendMessage(
                                        Text.translation("plot.sell.ok.free").yellow()
                                    );
                                }
                                MinecraftForge.EVENT_BUS.post(new PlotUpdateEvent(plot));
                                for (Player p : playerManager.getAllOnline()) {
                                    Resident r = townManager.getResident(p);
                                    if (r != null) {
                                        CraftTowns.NETWORK.sendTo(p.getEntity(), plot.serialize(r));
                                    }
                                }
                            } else {
                                throw new CommandException("plot.sell.already");
                            }
                        } else {
                            throw new CommandException("commands.generic.noPermission", ""); //FIXME
                        }
                        break;
                    }
                    case "stopselling":
                    case "ss": {
                        if (plot.isLocked()) {
                            throw new CommandException("plot.error.locked");
                        } else if (plot.isOwner(resident) || plot.hasTown() && plot.getTown().isAuthority(resident)) {
                            if (plot.isForSale()) {
                                plot.setForSale(false);
                                player.sendMessage(
                                    Text.translation("plot.stop-selling.ok").yellow()
                                );
                                plot.getWorld().markDirty();
                                MinecraftForge.EVENT_BUS.post(new PlotUpdateEvent(plot));
                                for (Player p : playerManager.getAllOnline()) {
                                    Resident r = townManager.getResident(p);
                                    if (r != null) {
                                        CraftTowns.NETWORK.sendTo(p.getEntity(), plot.serialize(r));
                                    }
                                }
                            } else {
                                throw new CommandException("plot.stop-selling.already");
                            }
                        } else {
                            throw new CommandException("commands.generic.noPermission", ""); //FIXME;
                        }
                        break;
                    }
                    case "border": {
                        Block block = ctx.getIfPresent("block", CommandContext.Argument::asBlock).orElse(Blocks.STONE_SLAB);
                        if (ctx.checkPermission(true, "town.admin", 2)) {
                            World world = plot.getWorld().unwrap();
                            boolean successful = false;
                            Set<Plot> plots = plot.findNearbyPlots(p -> p.hasOwner() || p.isForSale());
                            for (Plot p : plots) {
                                ChunkLocation l = p.getLocation();
                                for (EnumFacing face : EnumFacing.values()) {
                                    ChunkLocation lo = l.offset(face);
                                    Plot po = townManager.getPlot(lo);
                                    if (po == null || po.isForSale() || po.hasOwner()) {
                                        successful = true;
                                        for (int x = 0; x < 16; x++) {
                                            for (int z = 0; z < 16; z++) {
                                                int y = world.getTerrainHeight(lo.getChunkX() << 4 + x, lo.getChunkZ() << 4 + z);
                                                if (face == EnumFacing.NORTH && z == 0
                                                    || face == EnumFacing.SOUTH && z == 15
                                                    || face == EnumFacing.WEST && x == 15
                                                    || face == EnumFacing.EAST && x == 0) {
                                                    world.getLocation(x, y, z).setBlock(block);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (successful) {
                                player.sendMessage("Border generation successful!");
                            } else {
                                player.sendMessage("Border generation failed");
                            }
                        }
                        break;
                    }
                    case "lock": {
                        if (!plot.isLocked()) {
                            boolean owner = plot.isOwner(resident);
                            boolean authority = plot.hasTown() && plot.getTown().isAuthority(resident);
                            if (owner || authority) {
                                if (ctx.has("price") && authority) {
                                    plot.setPrice(ctx.get("price").asFloat(0, Float.MAX_VALUE));
                                    plot.setForSale(true);
                                }
                                plot.setLocked(true);
                                plot.getWorld().markDirty();
                                player.sendMessage(
                                    Text.translation("plot.lock.successful").yellow()
                                );
                                MinecraftForge.EVENT_BUS.post(new PlotUpdateEvent(plot));
                                for (Player p : playerManager.getAllOnline()) {
                                    Resident r = townManager.getResident(p);
                                    if (r != null) {
                                        CraftTowns.NETWORK.sendTo(p.getEntity(), plot.serialize(r));
                                    }
                                }
                            } else {
                                throw new CommandException("commands.generic.noPermission", ""); //FIXME
                            }
                        } else {
                            throw new CommandException("plot.lock.already");
                        }
                        break;
                    }
                    case "unlock": {
                        boolean locked = plot.isLocked();
                        if (locked) {
                            if (!plot.isOwner(resident)) {
                                if (plot.hasTown() && plot.getTown().isAuthority(resident) || player.hasPermission("town.admin")) {
                                    plot.setLocked(false);
                                    player.sendMessage(
                                        Text.translation("plot.unlock.successful").green()
                                    );
                                } else {
                                    throw new CommandException("plot.unlock.owner-only");
                                }
                            } else {
                                float price = plot.getPrice();
                                if (price > 0) {
                                    resident.withdrawMoney(price, (amt, success, fmt) -> {
                                        if (success) {
                                            plot.setLocked(false);
                                            plot.setForSale(false);
                                            plot.setOwner(resident);
                                            if (plot.hasTown()) {
                                                Town town = plot.getTown();
                                                Bank bank = town.getBank();
                                                if (bank != null) {
                                                    bank.deposit(amt);
                                                }
                                                //...
                                            }
                                            player.sendMessage(
                                                Text.translation("plot.unlock.successful").green()
                                            );
                                        } else {
                                            throw new CommandException("plot.claim.insufficient-money", fmt.apply(amt).build());
                                        }
                                    });
                                }
                            }
                        }
                        break;
                    }
                    case "abandon": {
                        if (plot.isOwner(resident) || plot.hasTown() && plot.getTown().isAuthority(resident) || player.hasPermission("town.admin")) {
                            plot.setOwner((UUID) null);
                            plot.setLocked(false);
                            player.sendMessage(
                                Text.translation("plot.abandon.success").gray()
                            );
                            plot.getWorld().markDirty();
                            MinecraftForge.EVENT_BUS.post(new PlotUpdateEvent(plot));
                            for (Player p : playerManager.getAllOnline()) {
                                Resident r = townManager.getResident(p);
                                if (r != null) {
                                    CraftTowns.NETWORK.sendTo(p.getEntity(), plot.serialize(r));
                                }
                            }
                        } else {
                            throw new CommandException("commands.generic.noPermission", ""); //FIXME
                        }
                        break;
                    }
                    case "option": {
                        if (plot.isLocked()) {
                            throw new CommandException("plot.error.locked");
                        } else if (plot.isOwner(resident) || plot.hasTown() && plot.getTown().isMayor(resident) || player.hasPermission("town.admin")) {
                            String optionName = ctx.get("option").asString();
                            PlotOption option = TownManager.findPlotOption(optionName);

                            if (option != null) {
                                if (!option.hasPermission(resident, plot)) {
                                    throw new CommandException("commands.generic.noPermission", ""); //FIXME
                                }
                                optionName = option.getNames().get(0);
                                if (ctx.has("value")) {
                                    String oldValue = option.get(plot);
                                    option.set(townManager, resident, ctx.get("value"), plot);
                                    String newValue = option.get(plot);
                                    player.sendMessage(
                                        Text.translation("plot.option.set").yellow()
                                            .arg(optionName, Text::gold)
                                            .arg(newValue, Text::gold)
                                    );
                                    plot.getWorld().markDirty();
                                    PlotOptionUpdateEvent event = new PlotOptionUpdateEvent(plot, option, oldValue, newValue);
                                    MinecraftForge.EVENT_BUS.post(event);
                                } else {
                                    Text<?, ?> formattedValue = option.getFormatted(townManager, plot);
                                    player.sendMessage(
                                        Text.translation("plot.option.info").yellow()
                                            .arg(formattedValue.build(), Text::gold)
                                    );
                                }
                            } else {
                                for (String p : optionName.split(",")) {
                                    Plot.Permission permission = Plot.Permission.parse(p);
                                    if (permission != null) {
                                        if (ctx.has("value")) {
                                            String value = ctx.get("value").asString();
                                            if (value.trim().equalsIgnoreCase("none")) {
                                                plot.setPermission(permission, EnumSet.noneOf(Plot.AccessLevel.class));
                                                player.sendMessage(
                                                    Text.translation("plot.option.cleared").yellow()
                                                        .arg(permission.getName(), Text::gold)
                                                );
                                            } else {
                                                String[] values = value.trim().split(",");
                                                EnumSet<Plot.AccessLevel> lvls = EnumSet.noneOf(Plot.AccessLevel.class);
                                                for (String v : values) {
                                                    try {
                                                        Plot.AccessLevel lvl = Plot.AccessLevel.valueOf(v.toUpperCase());
                                                        lvls.add(lvl);
                                                    } catch (IllegalArgumentException exc) {
                                                        throw new CommandException("plot.error.unknown-access-level", v);
                                                    }
                                                }

                                                player.sendMessage(
                                                    Text.translation("plot.option.set").yellow()
                                                        .arg(permission.getName(), Text::gold)
                                                        .arg(lvls.toString(), Text::gold)
                                                );
                                                plot.setPermission(permission, lvls);
                                            }
                                            plot.getWorld().markDirty();
                                        } else {
                                            StringJoiner lvls = new StringJoiner(",");
                                            for (Plot.AccessLevel accessLevel : plot.getPermission(permission)) {
                                                lvls.add(accessLevel.name().toLowerCase());
                                            }
                                            player.sendMessage(
                                                Text.translation("plot.option.info").yellow()
                                                    .arg(permission.getName(), Text::gold)
                                                    .arg(lvls.toString(), Text::gold)
                                            );
                                        }
                                    } else {
                                        throw new CommandException("plot.option.unknown", p);
                                    }
                                }
                            }
                        } else {
                            throw new CommandException("town.error.not-a-mayor");
                        }
                        break;
                    }
                    case "expel": {
                        if (plot.isOwner(resident) || plot.hasTown() && plot.getTown().isAuthority(resident)
                            || player.hasPermission("town.admin")) {

                            Player target = ctx.get("target").asPlayer();
                            String targetName = target.getName();
                            Resident tr = townManager.getResident(target);
                            if (!(plot.hasTown() && plot.getTown().isAuthority(tr)) && !tr.hasPermission("town.admin")) {
                                if (plot.getLocation().equals(target.getLocation())) {
                                    if (target.getBedLocation() != null) {
                                        target.teleport(target.getBedLocation());
                                    } else if (tr.hasTown() && tr.getTown().hasSpawnpoint()) {
                                        target.teleport(tr.getTown().getSpawnpoint());
                                    } else {
                                        target.teleport(target.getWorld().getSpawnLocation());
                                    }
                                    player.sendMessage(
                                        Text.translation("plot.expel.success.owner").green()
                                            .arg(targetName, Text::darkGreen)
                                    );
                                    target.sendMessage(
                                        Text.translation("plot.expel.success.self").red()
                                            .arg(resident.getName(), Text::darkRed)
                                    );
                                } else {
                                    throw new CommandException("plot.expel.not-on-your-plot", targetName);
                                }
                            } else {
                                throw new CommandException("plot.expel.no-permission");
                            }
                        } else {
                            throw new CommandException("commands.generic.noPermission", ""); //FIXME
                        }
                        break;
                    }
                    case "backup": {
                        if (!ctx.checkPermission(true, "town.admin", 2)) {
                            /*boolean result = plot.createBackup(true);
                            resident.sendMessage("Backup created (" + (result ? "overwritten" : "new") + ")");*/
                        }
                        break;
                    }
                    case "restore": {
                        if (!ctx.checkPermission(true, "town.admin", 2)) {
                            /*boolean result = plot.loadBackup();
                            resident.sendMessage("Backup loading " + (result ? "successful" : "failed"));*/
                        }
                        break;
                    }
                }
            } else {
                townManager.sendPlotInfo(resident, plot);
            }
        } else {
            throw new CommandException("plot.error.no-plot-here");
        }
    }
}
