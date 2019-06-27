package ru.craftlogic.towns.data.plot.types;

import ru.craftlogic.towns.data.Plot.AccessLevel;
import ru.craftlogic.towns.data.Plot.Permission;

import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class PlotTypeOutpost extends PlotType {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "outpost",
            "tower",
            "o"
        );
    }

    @Override
    public int getColor(EnumSet<Permission> permissions, AccessLevel access) {
        return new Color(134, 104, 54).getRGB();
    }

}
