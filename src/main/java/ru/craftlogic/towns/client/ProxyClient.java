package ru.craftlogic.towns.client;

import mapwriter.api.MapWriterAPI;
import mapwriter.util.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ru.craftlogic.api.network.AdvancedMessage;
import ru.craftlogic.api.world.ChunkLocation;
import ru.craftlogic.api.world.Location;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.client.map.TownOverlayProvider;
import ru.craftlogic.towns.common.ProxyCommon;
import ru.craftlogic.towns.network.message.MessageDeletePlot;
import ru.craftlogic.towns.network.message.MessageDeleteTown;
import ru.craftlogic.towns.network.message.MessagePlot;
import ru.craftlogic.towns.network.message.MessageTown;
import ru.craftlogic.util.ReflectiveUsage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ReflectiveUsage
@SideOnly(Side.CLIENT)
public class ProxyClient extends ProxyCommon {
    public final Minecraft client = FMLClientHandler.instance().getClient();
    public final Map<UUID, VisualTown> towns = new HashMap<>();
    public final Map<ChunkLocation, VisualPlot> plots = new HashMap<>();

    @Override
    public void preInit() {
        super.preInit();
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void postInit() {
        super.postInit();
        if (Loader.isModLoaded(Reference.MOD_ID)) {
            MapWriterAPI.registerDataProvider("towns", new TownOverlayProvider());
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world != null && world.isRemote) {
            //this.regions.clear();
        }
    }

    @Override
    protected AdvancedMessage handleTown(MessageTown message, MessageContext context) {
        VisualTown town = new VisualTown(
            message.getId(), message.getName(), message.getMayor(), message.getResidents(), message.getPlots(),
            message.isOpen(), message.isPvP(), message.getSpawn()
        );
        syncTask(context, () -> {
            VisualTown old = this.towns.put(town.id, town);
            if (old != null) {
                town.plots.addAll(old.plots);
            }
        });
        return null;
    }

    @Override
    protected AdvancedMessage handlePlot(MessagePlot message, MessageContext context) {
        VisualPlot plot = new VisualPlot(
            message.getLocation(), message.getType(), message.getTown(), message.getOwner(), message.isForSale(),
            message.isLocked(), message.getPrice(), message.getPermissions(), message.getAccess());
        syncTask(context, () -> {
            this.plots.put(plot.location, plot);
            VisualTown town = this.towns.get(plot.town);
            if (town != null) {
                town.plots.add(plot.location);
            } else {
                CraftTowns.LOGGER.error("Got plot for town " + plot.town + " which cannot be found in local cache");
            }
        });
        return null;
    }

    @Override
    protected AdvancedMessage handleDeleteTown(MessageDeleteTown message, MessageContext context) {
        syncTask(context, () -> {
            VisualTown town = this.towns.remove(message.getId());
            for (ChunkLocation plot : town.plots) {
                this.plots.remove(plot);
            }
        });
        return null;
    }

    @Override
    protected AdvancedMessage handleDeletePlot(MessageDeletePlot message, MessageContext context) {
        syncTask(context, () -> {
            VisualPlot plot = this.plots.remove(message.getLocation());
            for (VisualTown town : this.towns.values()) {
                town.plots.remove(plot.location);
            }
        });
        return null;
    }

    /*@SubscribeEvent
    public void onTextRender(RenderGameOverlayEvent.Text event) {
        EntityPlayer player = this.client.player;
        if (player != null && this.client.currentScreen == null) {
            for (VisualRegion region : this.regions.values()) {
                if (region.isOwning(player.posX, player.posY, player.posZ)) {
                    region.renderTextOverlay(this.client, event);
                }
            }
        }
    }*/

    @SubscribeEvent
    public void onWorldRenderLast(RenderWorldLastEvent event) {
        if (this.client.gameSettings.showDebugInfo) {
            /*this.client.mcProfiler.startSection("regions");
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.pushMatrix();
            GlStateManager.enableAlpha();
            GlStateManager.doPolygonOffset(-3F, -3F);
            GlStateManager.enablePolygonOffset();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.glLineWidth(3F);

            if (this.showRegionsThroughBlocks) {
                GlStateManager.depthMask(false);
                GlStateManager.disableDepth();
            }

            for (VisualRegion region : this.regions.values()) {
                region.renderVolumetric(this.client, event.getPartialTicks());
            }

            if (this.showRegionsThroughBlocks) {
                GlStateManager.enableDepth();
                for (VisualRegion region : this.regions.values()) {
                    region.renderVolumetric(this.client, event.getPartialTicks());
                }
            }

            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.doPolygonOffset(0F, 0F);
            GlStateManager.disablePolygonOffset();
            GlStateManager.enableAlpha();

            if (this.showRegionsThroughBlocks) {
                GlStateManager.depthMask(true);
            }
            GlStateManager.popMatrix();
            this.client.mcProfiler.endSection();*/
        }
    }

    @SubscribeEvent
    public void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        Location location = new Location(event.getWorld(), event.getPos());
        if (location.isWorldRemote() && !location.isAir() && this.client.playerController.getCurrentGameType() != GameType.SPECTATOR) {
            /*VisualRegion region = this.getRegion(location);
            if (region != null && !region.interactBlocks && !region.owner.getId().equals(player.getUniqueID())) {
                event.setUseBlock(Event.Result.DENY);
                player.swingArm(event.getHand());
                player.sendStatusMessage(Text.translation("chat.region.interact.blocks").darkRed().build(), true);

                Block block = location.getBlock();

                if ((block instanceof BlockDoor || block instanceof BlockTrapDoor || block instanceof BlockChest) &&
                        location.getBlockMaterial() == Material.WOOD) {

                    location.playSound(CraftSounds.OPENING_FAILED, SoundCategory.PLAYERS, 1F, 1F);
                }
            }*/
        }
    }

    @SubscribeEvent
    public void onBlockLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        Location location = new Location(event.getWorld(), event.getPos());
        if (location.isWorldRemote() && !location.isAir() && this.client.playerController.getCurrentGameType() != GameType.SPECTATOR) {
            /*VisualRegion region = this.getRegion(location);
            if (region != null && !region.interactBlocks && !region.owner.getId().equals(player.getUniqueID())) {
                event.setUseBlock(Event.Result.DENY);
                player.sendStatusMessage(Text.translation("chat.region.interact.blocks").darkRed().build(), true);
            }*/
        }
    }
}
