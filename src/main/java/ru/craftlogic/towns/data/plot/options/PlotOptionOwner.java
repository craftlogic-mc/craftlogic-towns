package ru.craftlogic.towns.data.plot.options;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;

import java.util.Collections;
import java.util.List;

public class PlotOptionOwner extends PlotOption {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("owner");
    }

    @Override
    public void set(CommandContext.Argument rawValue, Plot plot) throws CommandException {
        plot.setOwner(rawValue.asUUID());
    }

    @Override
    public void set(TownManager townManager, Resident resident, CommandContext.Argument rawValue, Plot plot) throws CommandException {
        Resident pl = townManager.getResident(rawValue.asUUID());
        if (pl != null && pl.getFirstPlayed() > 0) {
            plot.setOwner(pl);
            if (pl == resident) {
                resident.sendMessage(
                    Text.translation("plot.owner.new.self").yellow()
                        .arg(plot.getLocation().getChunkX(), Text::gold)
                        .arg(plot.getLocation().getChunkZ(), Text::gold)
                );
            } else {
                resident.sendMessage(
                    Text.translation("plot.owner.new.other").yellow()
                        .arg(pl.getName(), Text::gold)
                );
                if (pl.isOnline()) {
                    pl.sendMessage(
                        Text.translation("plot.owner.new.self").yellow()
                            .arg(plot.getLocation().getChunkX(), Text::gold)
                            .arg(plot.getLocation().getChunkZ(), Text::gold)
                    );
                }
            }
        } else {
            throw new CommandException("commands.generic.player.notFound", rawValue);
        }
    }

    @Override
    public String get(Plot plot) {
        Resident owner = plot.getOwner();
        return owner != null ? owner.getId().toString() : "-";
    }

    @Override
    public Text<?, ?> getFormatted(TownManager townManager, Plot plot) {
        Resident owner = plot.getOwner();
        return Text.string(owner != null ? owner.getName() : "-");
    }
}
