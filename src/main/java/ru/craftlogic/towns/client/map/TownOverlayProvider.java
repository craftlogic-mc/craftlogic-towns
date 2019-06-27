package ru.craftlogic.towns.client.map;

import mapwriter.api.MapChunkOverlay;
import mapwriter.api.MapMode;
import mapwriter.api.MapOverlayProvider;
import mapwriter.api.MapView;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DimensionType;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.Dimension;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.client.ProxyClient;
import ru.craftlogic.towns.client.VisualPlot;
import ru.craftlogic.towns.client.VisualTown;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TownOverlayProvider implements MapOverlayProvider {
    @Override
    public List<MapChunkOverlay> getChunksOverlay(DimensionType dim, double centerX, double centerZ, double minX, double minZ, double maxX, double maxZ) {
        int minChunkX = (MathHelper.ceil(minX) >> 4) - 1;
        int minChunkZ = (MathHelper.ceil(minZ) >> 4) - 1;
        int maxChunkX = (MathHelper.ceil(maxX) >> 4) + 1;
        int maxChunkZ = (MathHelper.ceil(maxZ) >> 4) + 1;
        int cX = (MathHelper.ceil(centerX) >> 4) + 1;
        int cZ = (MathHelper.ceil(centerZ) >> 4) + 1;
        int limitMinX = Math.max(minChunkX, cX - 100);
        int limitMaxX = Math.min(maxChunkX, cX + 100);
        int limitMinZ = Math.max(minChunkZ, cZ - 100);
        int limitMaxZ = Math.min(maxChunkZ, cZ + 100);
        List<MapChunkOverlay> chunks = new ArrayList<>();

        ProxyClient proxy = (ProxyClient) CraftTowns.PROXY;

        for(int x = limitMinX; x <= limitMaxX; ++x) {
            for(int z = limitMinZ; z <= limitMaxZ; ++z) {
                VisualPlot plot = proxy.plots.get(new ChunkLocation(Dimension.fromVanilla(dim), x, z));
                if (plot != null) {
                    chunks.add(new TownChunkOverlay(plot));
                }
            }
        }

        return chunks;
    }

    @Override
    public ITextComponent getMouseInfo(int mouseX, int mouseY, MapView mapview, MapMode mapmode) {
        Minecraft client = Minecraft.getMinecraft();
        ProxyClient proxy = (ProxyClient) CraftTowns.PROXY;
        final Point p = mapmode.screenXYtoBlockXZ(mapview, mouseX, mouseY);
        VisualPlot plot = proxy.plots.get(new ChunkLocation(
            Dimension.fromVanilla(mapview.getDimension()), p.x >> 4, p.y >> 4
        ));
        if (plot != null) {
            Text<?, ?> tooltip = Text.translation("tooltip.plot.info");
            VisualTown town = plot.town != null ? proxy.towns.get(plot.town) : null;
            if (town != null) {
                tooltip.appendText("\n");
                tooltip.append(
                    Text.translation("tooltip.plot.town").arg(town.name)
                );
            }
            if (plot.owner != null) {
                tooltip.appendText("\n");
                tooltip.append(
                    Text.translation("tooltip.plot.owner").arg(
                        client.player != null && plot.owner.getId().equals(client.player.getUniqueID())
                            ? Text.translation("resident.you")
                            : Text.string(plot.owner.getName())
                    )
                );
            }
            if (plot.forSale) {
                tooltip.appendText("\n");
                if (plot.price > 0) {
                    tooltip.append(
                        Text.translation("tooltip.plot.price").yellow()
                            .arg(plot.price, Text::gold)
                    );
                } else {
                    tooltip.append(
                        Text.translation("tooltip.plot.free").green()
                    );
                }
            }
            Text<?, ?> description = plot.type.getDescription();
            if (description != null) {
                tooltip.appendText("\n\n");
                tooltip.append(description);
            }
            return tooltip.build();
        }
        return null;
    }

    @Override
    public ITextComponent getStatusInfo(DimensionType dim, int bX, int bY, int bZ) {
        return null;
    }

    @Override
    public void onDimensionChanged(DimensionType dimension, MapView mapview) {}

    @Override
    public void onDraw(MapView mapview, MapMode mapmode) {}

    @Override
    public void onMapCenterChanged(double vX, double vZ, MapView mapview) {}

    @Override
    public void onMiddleClick(DimensionType dim, int bX, int bZ, MapView mapview) {}

    @Override
    public boolean onMouseInput(MapView mapview, MapMode mapmode) {
        return false;
    }

    @Override
    public void onOverlayActivated(MapView mapview) {}

    @Override
    public void onOverlayDeactivated(MapView mapview) {}

    @Override
    public void onZoomChanged(int level, MapView mapview) {}
}
