package ru.craftlogic.towns.data.plot.options;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;

import java.util.List;

public abstract class PlotOption {
    public abstract List<String> getNames();

    public void set(TownManager townManager, Resident sender, CommandContext.Argument rawValue, Plot plot) throws CommandException {
        this.set(rawValue, plot);
    }

    public abstract void set(CommandContext.Argument rawValue, Plot plot) throws CommandException;

    public abstract String get(Plot plot);

    public Text<?, ?> getFormatted(TownManager townManager, Plot plot) {
        return Text.string(this.get(plot));
    }

    public boolean hasPermission(Resident resident, Plot plot) {
        if (plot.hasTown()) {
            if (plot.getTown().isAuthority(resident)) {
                return true;
            }
        }
        return plot.isOwner(resident) || plot.hasPermission(resident, Plot.Permission.MANAGE);
    }
}
