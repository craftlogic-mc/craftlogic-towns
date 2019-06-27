package ru.craftlogic.towns.data.plot.options;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.towns.data.Plot;

import java.util.Arrays;
import java.util.List;

public class PlotOptionSnowForming extends PlotOption {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "snow",
            "snow-forming",
            "snow-form"
        );
    }

    @Override
    public void set(CommandContext.Argument rawValue, Plot plot) throws CommandException {
        boolean newValue = rawValue.asBoolean();
        plot.setSnowFormingAllowed(newValue);
    }

    @Override
    public String get(Plot plot) {
        return String.valueOf(plot.isSnowFormingAllowed());
    }
}
