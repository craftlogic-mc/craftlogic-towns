package ru.craftlogic.towns.commands;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.Command;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.api.command.CommandRegistrar;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.world.Player;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;

public class ResidentCommands implements CommandRegistrar {
    @Command(name = "resident", aliases = "res", syntax = {
        "",
        "set warp <name>",
        "set title <player:Player> <value>...",
        "set title <value>...",
        "remove warp|title"
    })
    public static void commandResident(CommandContext ctx) throws CommandException {
        TownManager townManager = ctx.server().getManager(TownManager.class);
        Player player = ctx.senderAsPlayer();
        Resident resident = townManager.getResident(player);

        if (ctx.hasAction(0)) {
            switch (ctx.action(0)) {
                case "set": {
                    switch (ctx.action(1)) {
                        case "warp": {
                            String name = ctx.get("name").asString();
                            Plot plot = townManager.getPlot(player.getLocation());
                            if (plot != null && plot.isOwner(resident)) {

                                resident.sendMessage(
                                    Text.translation("resident.warp.set.success").green()
                                );
                            } else {
                                throw new CommandException("resident.warp.set.invalid-claim");
                            }
                            break;
                        }
                        case "title": {
                            Resident target = resident;
                            String title = ctx.get("value").asString();
                            if (ctx.has("player")) {
                                target = townManager.getResident(ctx.get("player").asPlayer());
                                if (target == null || target.getFirstPlayed() <= 0) {
                                    throw new CommandException("commands.generic.player.notFound", ctx.get("player").asString());
                                }
                            }
                            if (target.getTown() == resident.getTown() && (resident.isAuthority() || player.hasPermission("town.admin"))) {
                                target.setTitle(title);
                                if (target == resident) {
                                    resident.sendMessage(
                                        Text.translation("resident.title.set.self").green()
                                            .arg(title, Text::darkGreen)
                                    );
                                } else {
                                    resident.sendMessage(
                                        Text.translation("resident.title.set.other").yellow()
                                            .arg(target.getName(), Text::gold)
                                            .arg(title, Text::gold)
                                    );
                                    if (target.isOnline()) {
                                        target.sendMessage(
                                            Text.translation("resident.title.set.self").green()
                                                .arg(title, Text::darkGreen)
                                        );
                                    }
                                }
                            } else {
                                throw new CommandException("town.error.not-a-mayor");
                            }
                            break;
                        }
                    }
                    break;
                }
                case "remove": {
                    switch (ctx.action(1)) {
                        case "warp": {
                            String name = ctx.get("name").asString();
                        }
                    }
                    break;
                }
            }
        } else {
            townManager.sendResidentInfo(resident, player);
        }
    }
}
