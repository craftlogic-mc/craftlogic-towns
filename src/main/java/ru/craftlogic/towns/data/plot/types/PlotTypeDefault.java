package ru.craftlogic.towns.data.plot.types;

import ru.craftlogic.api.text.Text;

import java.util.Arrays;
import java.util.List;

public class PlotTypeDefault extends PlotType {
    @Override
    public List<String> getNames() {
        return Arrays.asList(
            "default",
            "usual",
            "normal",
            "def",
            "n"
        );
    }

    @Override
    public Text<?, ?> getDescription() {
        return null;
    }
}
