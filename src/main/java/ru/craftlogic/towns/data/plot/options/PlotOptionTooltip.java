package ru.craftlogic.towns.data.plot.options;

import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;

import java.util.Collections;
import java.util.List;

public class PlotOptionTooltip extends PlotOption {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("tooltip");
    }

    @Override
    public boolean hasPermission(Resident resident, Plot plot) {
        return super.hasPermission(resident, plot) && resident.hasPermission("town.cosmetic");
    }

    @Override
    public void set(CommandContext.Argument rawValue, Plot plot) {
        plot.setTooltip(rawValue.asString());
    }

    @Override
    public String get(Plot plot) {
        return plot.getTooltip();
    }
}
