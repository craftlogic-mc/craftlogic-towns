package ru.craftlogic.towns.data.plot.options;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.towns.data.Plot;

import java.util.Arrays;
import java.util.List;

public class PlotOptionMonsters extends PlotOption {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "monsters",
            "mobs",
            "mob-spawn",
            "mobs-spawn",
            "monster-spawn",
            "monsters-spawn",
            "mob-spawning",
            "mobs-spawning",
            "monster-spawning",
            "monsters-spawning"
        );
    }

    @Override
    public void set(CommandContext.Argument rawValue, Plot plot) throws CommandException {
        boolean newValue = rawValue.asBoolean();
        plot.setMonstersSpawningAllowed(newValue);
    }

    @Override
    public String get(Plot plot) {
        return String.valueOf(plot.isMonstersSpawningAllowed());
    }
}
