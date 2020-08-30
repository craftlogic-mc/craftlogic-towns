package ru.craftlogic.towns.listeners;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ru.craftlogic.api.event.block.DispenserShootEvent;
import ru.craftlogic.api.event.block.FarmlandTrampleEvent;
import ru.craftlogic.api.event.block.FluidFlowEvent;
import ru.craftlogic.api.event.block.PistonCheckCanMoveEvent;
import ru.craftlogic.api.event.player.PlayerCheckCanEditEvent;
import ru.craftlogic.api.inventory.InventoryHolder;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.world.Location;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;

import java.util.Random;

public class PlotListener {
    private final TownManager townManager;

    public PlotListener(TownManager townManager) {
        this.townManager = townManager;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        checkBlocks(event, event.getPlayer());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        checkBlocks(event, event.getPlayer());
    }

    private void checkBlocks(BlockEvent event, EntityPlayer player) {
        Location location = new Location(event.getWorld(), event.getPos());
        Plot plot = this.townManager.getPlot(location);
        Resident resident = this.townManager.getResident(player.getUniqueID());
        if (plot != null) {
            boolean locked = plot.isLocked();
            if (!plot.hasPermission(resident, Plot.Permission.BUILD) || locked) {
                if (locked) {
                    player.sendStatusMessage(Text.translation("plot.error.locked").darkRed().build(), true);
                } else {
                    player.sendStatusMessage(Text.translation("plot.error.no-build").darkRed().build(), true);
                }
                event.setCanceled(true);
            }
        } else if (this.townManager.disableWildInteract) {
            player.sendStatusMessage(Text.translation("resident.error.no-wild-build").darkRed().build(), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCheckBlockModify(PlayerCheckCanEditEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Location location = new Location(player.getEntityWorld(), event.pos);
        Plot plot = this.townManager.getPlot(location);
        Resident resident = this.townManager.getResident(player.getUniqueID());
        if (plot != null && !plot.hasPermission(resident, Plot.Permission.BUILD)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity target = event.getTarget();
        if (checkAttack(player, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTrample(FarmlandTrampleEvent event) {
        Entity entity = event.getEntity();
        net.minecraft.world.World world = event.getWorld();
        BlockPos pos = event.getPos();
        Location location = new Location(world, pos);
        Plot plot = this.townManager.getPlot(location);
        if (plot != null) {
            if (entity instanceof EntityPlayerMP) {
                Resident resident = this.townManager.getResident(entity.getUniqueID());
                if (!plot.hasPermission(resident, Plot.Permission.BUILD)) {
                    EntityPlayerMP player = (EntityPlayerMP) entity;
                    player.sendStatusMessage(Text.translation("plot.error.no-build").darkRed().build(), true);
                    player.connection.sendPacket(new SPacketBlockChange(world, pos));
                    event.setCanceled(true);
                }
            } else if (entity instanceof EntityLivingBase) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onArrowHit(ProjectileImpactEvent.Arrow event) {
        EntityArrow arrow = event.getArrow();
        RayTraceResult target = event.getRayTraceResult();
        if (target.entityHit != null) {
            if (arrow.shootingEntity instanceof EntityPlayer) {
                if (checkAttack(((EntityPlayer) arrow.shootingEntity), target.entityHit)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onThrowableImpact(ProjectileImpactEvent.Throwable event) {
        EntityThrowable throwable = event.getThrowable();
        RayTraceResult target = event.getRayTraceResult();
        EntityLivingBase thrower = throwable.getThrower();
        if (target.entityHit != null) {
            if (thrower instanceof EntityPlayer) {
                Plot plot = this.townManager.getPlot(new Location(target.entityHit));
                Resident resident = this.townManager.getResident(thrower.getUniqueID());
                if (plot != null) {
                    if (!plot.hasPermission(resident, Plot.Permission.INTERACT)) {
                        event.setCanceled(true);
                        ((EntityPlayer) thrower).sendStatusMessage(Text.translation("chat.region.interact.entities").darkRed().build(), true);
                        if (throwable instanceof EntityPotion) {
                            throwable.entityDropItem(((EntityPotion) throwable).getPotion(), 0F);
                        }
                    } else if (!plot.hasPermission(resident, Plot.Permission.LAUNCH_PROJECTILE)) {
                        event.setCanceled(true);
                        if (throwable instanceof EntityPotion) {
                            throwable.entityDropItem(((EntityPotion) throwable).getPotion(), 0F);
                            ((EntityPlayer) thrower).sendStatusMessage(Text.translation("chat.region.interact.potions").darkRed().build(), true);
                        } else {
                            ((EntityPlayer) thrower).sendStatusMessage(Text.translation("chat.region.interact.projectiles").darkRed().build(), true);
                        }
                    }
                }
            }
        } else if (throwable instanceof EntityPotion) {
            if (thrower instanceof EntityPlayer) {
                Plot plot = this.townManager.getPlot(new Location(throwable));
                Resident resident = this.townManager.getResident(thrower.getUniqueID());
                if (plot != null && !plot.hasPermission(resident, Plot.Permission.LAUNCH_PROJECTILE)) {
                    event.setCanceled(true);
                    throwable.entityDropItem(((EntityPotion) throwable).getPotion(), 0F);
                    throwable.setDead();
                    ((EntityPlayer) thrower).sendStatusMessage(Text.translation("chat.region.interact.potions").darkRed().build(), true);
                }
            }
        }
    }

    private boolean checkAttack(EntityPlayer player, Entity target) {
        Resident resident = this.townManager.getResident(player.getUniqueID());
        Plot targetPlot = this.townManager.getPlot(new Location(target));
        if (target instanceof EntityPlayer) {
            Plot fromPlot = this.townManager.getPlot(new Location(player));
            if (fromPlot != null && fromPlot.hasTown()) {
                Town fromTown = fromPlot.getTown();
                if (!fromTown.isPvP() || targetPlot != null && (!targetPlot.hasTown() || !targetPlot.getTown().isPvP())) {
                    player.sendStatusMessage(Text.translation("chat.region.attack.players").darkRed().build(), true);
                    return true;
                }
            }
        } else if (target instanceof IMob) {
            if (targetPlot != null && false /*&& !targetRegion.canAttackHostiles(player.getUniqueID())*/) {
                player.sendStatusMessage(Text.translation("chat.region.attack.monsters").darkRed().build(), true);
                return true;
            }
        } else if (target instanceof IAnimals) {
            if (targetPlot != null && !targetPlot.hasPermission(resident, Plot.Permission.INTERACT)) { // FIXME Interact -> DamageAnimals
                player.sendStatusMessage(Text.translation("chat.region.attack.animals").darkRed().build(), true);
                return true;
            }
        } else {
            if (targetPlot != null && !targetPlot.hasPermission(resident, Plot.Permission.BUILD)) {
                player.sendStatusMessage(Text.translation("plot.error.no-build").darkRed().build(), true);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        Resident resident = this.townManager.getResident(player.getUniqueID());
        Location location = new Location(event.getWorld(), event.getPos());
        if (!location.isAir()) {
            if (resident.hasPermission("town.interaction-bypass")) {
                return;
            }
            InventoryHolder inventory = location.getTileEntity(InventoryHolder.class);
            if (inventory != null) {
                Plot plot = this.townManager.getPlot(location);
                if (plot != null && !plot.hasPermission(resident, Plot.Permission.OPEN_CONTAINER)) {
                    event.setUseBlock(Event.Result.DENY);
                    player.sendStatusMessage(Text.translation("plot.error.no-container").darkRed().build(), true);
                }
            } else {
                Plot plot = this.townManager.getPlot(location);
                if (plot != null && !plot.hasPermission(resident, Plot.Permission.INTERACT)) {
                    event.setUseBlock(Event.Result.DENY);
                    player.sendStatusMessage(Text.translation("chat.region.interact.blocks").darkRed().build(), true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        Resident resident = this.townManager.getResident(player.getUniqueID());
        Location location = new Location(event.getWorld(), event.getPos());
        if (!location.isAir()) {
            if (resident.hasPermission("town.interaction-bypass")) {
                return;
            }
            Plot plot = this.townManager.getPlot(location);
            if (plot != null && !plot.hasPermission(resident, Plot.Permission.INTERACT)) {
                event.setUseBlock(Event.Result.DENY);
                player.sendStatusMessage(Text.translation("chat.region.interact.blocks").darkRed().build(), true);
            }
        }
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        checkEntityInteract(event.getEntityPlayer(), event.getTarget(), event);
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        checkEntityInteract(event.getEntityPlayer(), event.getTarget(), event);
    }

    private void checkEntityInteract(EntityPlayer player, Entity target, Event event) {
        Resident resident = this.townManager.getResident(player.getUniqueID());
        Plot plot = this.townManager.getPlot(new Location(target));
        if (plot != null && !plot.hasPermission(resident, Plot.Permission.INTERACT)) {
            event.setCanceled(true);
            player.sendStatusMessage(Text.translation("chat.region.interact.entities").darkRed().build(), true);
        }
    }

    @SubscribeEvent
    public void onBucketFill(FillBucketEvent event) {
        RayTraceResult target = event.getTarget();
        if (target != null) {
            Location location = new Location(event.getWorld(), target.getBlockPos());
            EntityPlayer player = event.getEntityPlayer();
            Plot plot = this.townManager.getPlot(location);
            Resident resident = this.townManager.getResident(player.getUniqueID());
            if (plot != null && !plot.hasPermission(resident, Plot.Permission.INTERACT)) {
                event.setResult(Event.Result.DENY);
                event.setCanceled(true);
                player.sendStatusMessage(Text.translation("chat.region.interact.blocks").darkRed().build(), true);
            }
        }
    }

    /*@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
        if (!event.isCancelled()) {
            Player player = event.getPlayer();
            Entity entity = event.getRightClicked();
            Resident resident = this.townManager.getResident(player);
            TownWorld tw = this.townManager.getWorld(entity.getWorld());
            if (tw != null) {
                if (entity instanceof Monster || player.hasPermission("town.entity-bypass")) {
                    return;
                }
                Plot plot = tw.getPlot(entity.getLocation());
                if (plot != null) {
                    if (this.townManager.plotInteractionBypass.contains(entity.getType().toString())) {
                        return;
                    }
                    Town town = plot.getTown();
                    if (town != null && (town.isMayor(player) || !plot.hasOwner() && town.isAssistant(player))) {
                        return;
                    }
                    boolean locked = plot.isLocked();
                    if (entity instanceof InventoryHolder && !(entity instanceof Player)) {
                        if (!plot.hasPermission(resident, Plot.Permission.INTERACT) || locked) {
                            if (locked) {
                                resident.sendTranslatedMessage("plot.error.locked");
                            } else {
                                resident.sendTranslatedMessage("plot.error.no-container");
                            }
                            event.setCancelled(true);
                        }
                    } else if (!plot.hasPermission(resident, Plot.Permission.INTERACT) || locked) {
                        if (locked) {
                            resident.sendTranslatedMessage("plot.error.locked");
                        } else {
                            player.sendMessage(this.townManager.translate("plot.error.no-interact"));
                        }
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(ExplosionPrimeEvent event) {
        Entity entity = event.getEntity();
        TownWorld tw = this.townManager.getWorld(entity.getWorld());
        if (tw != null) {
            Plot plot = tw.getPlot(entity.getLocation());
            if (plot != null) {
                if (!plot.areExplosionsAllowed() || plot.isLocked()) {
                    event.setRadius(0);
                    event.setFire(false);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (shooter != null && shooter instanceof LivingEntity) {
            LivingEntity shooterEntity = (LivingEntity) shooter;
            Plot plot = this.townManager.getPlot(shooterEntity.getLocation());
            if (plot != null) {
                if (shooterEntity instanceof HumanEntity) {
                    HumanEntity humanEntity = (HumanEntity) shooterEntity;
                    if (humanEntity.hasPermission("town.projectile-bypass")) {
                        return;
                    }
                    Resident resident = this.townManager.getResident(humanEntity.getUniqueId());
                    if (resident != null) {
                        Town town = plot.getTown();
                        if (town != null && town.isAuthority(resident)) {
                            return;
                        }
                        if (!plot.hasPermission(resident, Plot.Permission.LAUNCH_PROJECTILE)) {
                            if (resident.isOnline()) {
                                resident.sendTranslatedMessage("plot.error.no-projectile");
                            }
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            LivingEntity entity = event.getEntity();
            Plot plot = this.townManager.getPlot(entity.getLocation());
            if (plot != null) {
                if (entity instanceof Monster && !plot.isMonstersSpawningAllowed()) {
                    event.setCancelled(true);
                } else if (entity instanceof Animals && !plot.isAnimalsSpawningAllowed()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamaged(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (player.hasPermission("town.entity-bypass")) {
                return;
            }
            Entity victim = event.getEntity();
            Resident resident = this.townManager.getResident(player);
            Plot plot = this.townManager.getPlot(victim.getLocation());
            if (plot != null) {
                if (victim instanceof Player) {
                    Town town = plot.getTown();
                    if (town != null && !town.isPvP() && !town.isAuthority(resident)) {
                        resident.sendTranslatedMessage("plot.error.no-pvp");
                        event.setCancelled(true);
                    }
                } else if (!(victim instanceof Monster)) {
                    Town town = plot.getTown();
                    if (town != null && town.isAuthority(resident)) {
                        return;
                    }
                    if (!plot.hasPermission(resident, Plot.Permission.INTERACT)) {
                        resident.sendTranslatedMessage("plot.error.no-" + (victim instanceof Animals ? "damage"
                            : "interact"));
                        event.setCancelled(true);
                    }
                }
            }
        }
    }*/

    /*@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Block oldBlock = event.getBlock();
        Block newBlock = event.getNewState().getBlock();
        if (newBlock.getType() == Material.SNOW) {
            Plot plot = this.townManager.getPlot(newBlock.getLocation());
            if (plot != null && !plot.isSnowFormingAllowed()) {
                event.setCancelled(true);
            }
        }
    }*/

    @SubscribeEvent
    public void onDispenserShoot(DispenserShootEvent event) {
        net.minecraft.world.WorldServer world = (WorldServer) event.getWorld();
        onBlockFromTo(event, world, event.getPos(), event.getFacing(), true, event.getPlayer(world));
    }

    @SubscribeEvent
    public void onPistonMove(PistonCheckCanMoveEvent event) {
        onBlockFromTo(event, event.getWorld(), event.getBlockToMove(), event.getMoveDirection(), true, null);
    }

    @SubscribeEvent
    public void onFluidFlow(FluidFlowEvent event) {
        onBlockFromTo(event, event.getWorld(), event.getPos(), event.getFacing(), false, null);
    }

    private void onBlockFromTo(Event event, net.minecraft.world.World world, BlockPos pos, EnumFacing facing, boolean multiParticles, EntityPlayer player) {
        Location from = new Location(world, pos);
        Location to = from.offset(facing);
        Plot targetPlot = this.townManager.getPlot(to);
        Resident resident = player == null ? null : this.townManager.getResident(player.getUniqueID());
        if (targetPlot != null && targetPlot != this.townManager.getPlot(from) && (resident == null || !targetPlot.hasPermission(resident, Plot.Permission.BUILD))) {
            Random rand = world.rand;
            int max = multiParticles ? 2 + rand.nextInt(3) : 1;
            for (int i = 0; i < max; i++) {
                double x = (rand.nextDouble() - 0.5D) * 0.2D;
                double y = 0.2D + (rand.nextDouble() - 0.5D) * 0.2D;
                double z = (rand.nextDouble() - 0.5D) * 0.2D;
                to.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0, 0, 0);
            }
            event.setCanceled(true);
        }
    }
}
