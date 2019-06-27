package ru.craftlogic.towns.data.plot.types;

import ru.craftlogic.towns.data.Plot.AccessLevel;
import ru.craftlogic.towns.data.Plot.Permission;

import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class PlotTypeTownCentre extends PlotType {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "town-centre",
            "tc",
            "centre",
            "home",
            "h"
        );
    }

    @Override
    public int getColor(EnumSet<Permission> permissions, AccessLevel access) {
        return (access == AccessLevel.STRANGER ? Color.GRAY : Color.ORANGE).getRGB();
    }

}
