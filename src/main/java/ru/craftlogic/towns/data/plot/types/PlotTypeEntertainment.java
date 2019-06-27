package ru.craftlogic.towns.data.plot.types;

import java.util.Arrays;
import java.util.List;

public class PlotTypeEntertainment extends PlotType {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "entertainment",
            "minigames",
            "arena",
            "games",
            "ent",
            "g"
        );
    }
}
