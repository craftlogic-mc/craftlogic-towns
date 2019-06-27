package ru.craftlogic.towns.data.plot.types;

import ru.craftlogic.api.text.Text;
import ru.craftlogic.towns.data.Plot.AccessLevel;
import ru.craftlogic.towns.data.Plot.Permission;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public abstract class PlotType {
    public abstract List<String> getNames();

    public int getColor(EnumSet<Permission> permissions, AccessLevel access) {
        if (access == AccessLevel.STRANGER) {
            return Color.LIGHT_GRAY.getRGB();
        } else {
            if (permissions.contains(Permission.MANAGE)) {
                return new Color(38, 128, 17).getRGB();
            } else if (permissions.contains(Permission.BUILD)) {
                return new Color(179, 242, 1).getRGB();
            } else {
                return new Color(255, 255, 0).getRGB();
            }
        }
    }

    public Text<?, ?> getDescription() {
        return Text.translation("plot.type." + getNames().get(0));
    }

    public final boolean isDefault() {
        return this instanceof PlotTypeDefault;
    }

    public final boolean isOutpost() {
        return this instanceof PlotTypeOutpost;
    }
}
