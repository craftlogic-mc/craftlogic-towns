package ru.craftlogic.towns.data.plot.options;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.towns.data.Plot;

import java.util.Arrays;
import java.util.List;

public class PlotOptionAnimals extends PlotOption {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "animals",
            "animal-spawn",
            "animals-spawn",
            "animal-spawning",
            "animals-spawning"
        );
    }

    @Override
    public void set(CommandContext.Argument rawValue, Plot plot) throws CommandException {
        boolean newValue = rawValue.asBoolean();
        plot.setAnimalsSpawningAllowed(newValue);
    }

    @Override
    public String get(Plot plot) {
        return String.valueOf(plot.isAnimalsSpawningAllowed());
    }
}
