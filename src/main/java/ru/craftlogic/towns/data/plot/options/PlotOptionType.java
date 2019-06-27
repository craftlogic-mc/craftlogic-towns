package ru.craftlogic.towns.data.plot.options;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.plot.types.PlotType;

import java.util.Collections;
import java.util.List;

public class PlotOptionType extends PlotOption {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("type");
    }

    @Override
    public void set(CommandContext.Argument rawValue, Plot plot) throws CommandException {
        PlotType type = TownManager.findPlotType(rawValue.asString());
        if (type != null) {
            plot.setType(type);
        } else {
            throw new CommandException("town.error.unknown-plot-type");
        }
    }

    @Override
    public String get(Plot plot) {
        return String.valueOf(plot.getType());
    }
}
