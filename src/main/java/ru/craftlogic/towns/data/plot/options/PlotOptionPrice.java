package ru.craftlogic.towns.data.plot.options;

import net.minecraft.command.CommandException;
import ru.craftlogic.api.command.CommandContext;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;

import java.util.Collections;
import java.util.List;

public class PlotOptionPrice extends PlotOption {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("price");
    }

    @Override
    public void set(CommandContext.Argument rawValue, Plot plot) throws CommandException {
        float price = rawValue.asFloat(0, Float.MAX_VALUE);
        plot.setPrice(price);
    }

    @Override
    public String get(Plot plot) {
        return String.valueOf(plot.getPrice());
    }

    @Override
    public Text<?, ?> getFormatted(TownManager townManager, Plot plot) {
        return townManager.price(plot.getPrice());
    }
}
