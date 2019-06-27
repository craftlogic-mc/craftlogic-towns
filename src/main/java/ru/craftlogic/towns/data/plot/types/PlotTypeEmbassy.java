package ru.craftlogic.towns.data.plot.types;

import java.util.Arrays;
import java.util.List;

public class PlotTypeEmbassy extends PlotType {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "embassy",
            "emb"
        );
    }
}
