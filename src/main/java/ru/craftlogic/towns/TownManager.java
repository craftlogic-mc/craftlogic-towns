package ru.craftlogic.towns;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import ru.craftlogic.api.economy.EconomyManager;
import ru.craftlogic.api.server.PlayerManager;
import ru.craftlogic.api.server.Server;
import ru.craftlogic.api.server.WorldManager;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.api.util.ConfigurableManager;
import ru.craftlogic.api.world.*;
import ru.craftlogic.common.command.CommandManager;
import ru.craftlogic.towns.common.command.CommandPlot;
import ru.craftlogic.towns.common.command.CommandResident;
import ru.craftlogic.towns.common.command.CommandTown;
import ru.craftlogic.towns.data.Bank;
import ru.craftlogic.towns.data.Nation;
import ru.craftlogic.towns.data.Plot;
import ru.craftlogic.towns.data.Resident;
import ru.craftlogic.towns.data.Town;
import ru.craftlogic.towns.data.TownWorld;
import ru.craftlogic.towns.data.Warp;
import ru.craftlogic.towns.data.plot.options.PlotOption;
import ru.craftlogic.towns.data.plot.options.PlotOptionAnimals;
import ru.craftlogic.towns.data.plot.options.PlotOptionMonsters;
import ru.craftlogic.towns.data.plot.options.PlotOptionOwner;
import ru.craftlogic.towns.data.plot.options.PlotOptionPrice;
import ru.craftlogic.towns.data.plot.options.PlotOptionSnowForming;
import ru.craftlogic.towns.data.plot.options.PlotOptionTooltip;
import ru.craftlogic.towns.data.plot.options.PlotOptionType;
import ru.craftlogic.towns.data.plot.types.PlotType;
import ru.craftlogic.towns.data.plot.types.PlotTypeCommercial;
import ru.craftlogic.towns.data.plot.types.PlotTypeDefault;
import ru.craftlogic.towns.data.plot.types.PlotTypeEmbassy;
import ru.craftlogic.towns.data.plot.types.PlotTypeEntertainment;
import ru.craftlogic.towns.data.plot.types.PlotTypeOutpost;
import ru.craftlogic.towns.data.plot.types.PlotTypeTownCentre;
import ru.craftlogic.towns.event.TownManagerReloadEvent;
import ru.craftlogic.towns.event.TownResidentAcceptedInvitationEvent;
import ru.craftlogic.towns.listeners.PlayerListener;
import ru.craftlogic.towns.listeners.PlotListener;
import ru.craftlogic.towns.listeners.WorldListener;
import ru.craftlogic.towns.utils.SelfExpiringHashMap;
import ru.craftlogic.towns.utils.SelfExpiringMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ru.craftlogic.towns.CraftTowns.LOGGER;

public class TownManager extends ConfigurableManager {
    public static final Set<PlotOption> PLOT_OPTIONS = new HashSet<>(); static {
        PLOT_OPTIONS.add(new PlotOptionAnimals());
        PLOT_OPTIONS.add(new PlotOptionMonsters());
        PLOT_OPTIONS.add(new PlotOptionSnowForming());
        PLOT_OPTIONS.add(new PlotOptionType());
        PLOT_OPTIONS.add(new PlotOptionTooltip());
        PLOT_OPTIONS.add(new PlotOptionPrice());
        PLOT_OPTIONS.add(new PlotOptionOwner());
    }

    public static PlotOption findPlotOption(String name) {
        for (PlotOption option : PLOT_OPTIONS) {
            for (String s : option.getNames()) {
                if (s.equalsIgnoreCase(name)) {
                    return option;
                }
            }
        }
        return null;
    }

    public static final Set<PlotType> PLOT_TYPES = new HashSet<>(); static {
        PLOT_TYPES.add(new PlotTypeDefault());
        PLOT_TYPES.add(new PlotTypeTownCentre());
        PLOT_TYPES.add(new PlotTypeEntertainment());
        PLOT_TYPES.add(new PlotTypeEmbassy());
        PLOT_TYPES.add(new PlotTypeCommercial());
        PLOT_TYPES.add(new PlotTypeOutpost());
    }

    public static PlotType findPlotType(String name) {
        for (PlotType type : PLOT_TYPES) {
            for (String s : type.getNames()) {
                if (s.equalsIgnoreCase(name)) {
                    return type;
                }
            }
        }
        return null;
    }

    private SelfExpiringMap<UUID, Resident> residents = new SelfExpiringHashMap<>(7200000, u -> {
        Resident r = this.residents.get(u);
        if (r.isOnline()) {
            this.residents.renewKey(u);
        }
        try {
            r.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    });
    private ScheduledExecutorService taskScheduler = Executors.newSingleThreadScheduledExecutor();
    private SelfExpiringMap<UUID, UUID> pendingInvitations = new SelfExpiringHashMap<>(120000, this::expireInvitation);

    public Map<Dimension, TownWorld> worlds = new WeakHashMap<>();
    public Map<UUID, Nation> nations = new HashMap<>();

    public float nonpaymentFine = 100;
    public boolean disableWildInteract = false;
    public boolean broadcastEvents = true;
    public float townCreationPrice;
    public Map<PlotType, Float> plotTaxes = new HashMap<>();
    public List<String> plotInteractionBypass = new ArrayList<>();
    public List<String> enabledWorlds = new ArrayList<>();
    public final Map<String, WorldTownManager> managers = new HashMap<>();
    private boolean loaded;

    public TownManager(Server server, Path settingsDirectory) {
        super(server, settingsDirectory.resolve("towns.json"), LOGGER);
        MinecraftForge.EVENT_BUS.register(new WorldListener(this));
        MinecraftForge.EVENT_BUS.register(new PlayerListener(this));
        MinecraftForge.EVENT_BUS.register(new PlotListener(this));
    }

    public Path getDataFolder() {
        return getServer().getSettingsDirectory().resolve("towns/");
    }

    @Override
    public void registerCommands(CommandManager commandManager) {
        commandManager.registerArgumentType("Town", false, ctx -> Collections.emptySet());
        commandManager.registerArgumentType("TownWithSpawn", false, ctx -> {
            TownManager townManager = ctx.server().getManager(TownManager.class);
            Player player = ctx.senderAsPlayer();
            Resident resident = townManager.getResident(player);
            Set<String> result = new HashSet<>();
            String partial = ctx.partialName();
            for (TownWorld world : townManager.getAllWorlds()) {
                for (Town town : world.getAllTowns()) {
                    if (partial.isEmpty() || town.getName().toLowerCase().startsWith(partial)) {
                        if (town.hasSpawnpoint() || town.isOpen() || town.hasResident(resident)
                            || resident.hasPermission("town.private-bypass")) {

                            result.add(town.getName());
                        }
                    }
                }
            }
            return result;
        });
        commandManager.registerArgumentType("TownOpen", false, ctx -> {
            TownManager townManager = ctx.server().getManager(TownManager.class);
            Player player = ctx.senderAsPlayer();
            Resident resident = townManager.getResident(player);
            Set<String> result = new HashSet<>();
            String partial = ctx.partialName();
            for (TownWorld world : townManager.getAllWorlds()) {
                for (Town town : world.getAllTowns()) {
                    if (partial.isEmpty() || town.getName().toLowerCase().startsWith(partial)) {
                        if (town.isOpen() || town.hasResident(resident) || resident.hasPermission("town.private-bypass")) {
                            result.add(town.getName());
                        }
                    }
                }
            }
            return result;
        });
        commandManager.registerArgumentType("PlotType", false, ctx -> {
            Set<String> result = new HashSet<>();
            String partial = ctx.partialName();
            for (PlotType type : TownManager.PLOT_TYPES) {
                for (String a : type.getNames()) {
                    if (partial.isEmpty() || a.startsWith(partial)) {
                        result.add(a);
                    }
                }
            }
            return result;
        });
        commandManager.registerArgumentType("PlotOptions", false, ctx -> {
            TownManager townManager = ctx.server().getManager(TownManager.class);
            Player player = ctx.senderAsPlayer();
            Resident resident = townManager.getResident(player);
            Plot plot = townManager.getPlot(player.getLocation());
            if (plot.isOwner(resident) || plot.hasTown() && plot.getTown().isMayor(resident)
                || player.hasPermission("town.admin")) {

                String partial = ctx.partialName();

                List<String> result = new ArrayList<>();
                for (PlotOption o : TownManager.PLOT_OPTIONS) {
                    for (String a : o.getNames()) {
                        if (partial.isEmpty() || a.startsWith(partial)) {
                            result.add(a);
                        }
                    }
                }
                return result;
            }
            return Collections.emptyList();
        });
        commandManager.registerCommand(new CommandTown());
        commandManager.registerCommand(new CommandResident());
        commandManager.registerCommand(new CommandPlot());

    }

    @Override
    public void load(JsonObject towns) {
        LOGGER.info("Loading configuration...");

        this.loaded = true;

        for (WorldTownManager manager : this.managers.values()) {
            try {
                manager.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.disableWildInteract = JsonUtils.getBoolean(towns, "disable-wild-interact", false);
        this.plotInteractionBypass.clear();
        for (JsonElement pib : JsonUtils.getJsonArray(towns, "plot-interaction-bypass")) {
            this.plotInteractionBypass.add(pib.getAsString());
        }
        this.enabledWorlds.clear();
        for (JsonElement ew : JsonUtils.getJsonArray(towns, "enabled-worlds")) {
            this.enabledWorlds.add(ew.getAsString());
        }

        JsonObject town = JsonUtils.getJsonObject(towns, "town", new JsonObject());

        this.nonpaymentFine = JsonUtils.getFloat(town, "nonpayment-fine", 100);
        this.broadcastEvents = JsonUtils.getBoolean(town, "event-broadcast", true);
        JsonObject taxes = JsonUtils.getJsonObject(town, "taxes", new JsonObject());
        this.plotTaxes.clear();
        if (taxes.size() > 0) {
            for (PlotType type : PLOT_TYPES) {
                float t = JsonUtils.getFloat(taxes, type.getNames().get(0), 0);
                this.plotTaxes.put(type, t);
            }
        }
        this.townCreationPrice = JsonUtils.getFloat(town, "price", 0);

        this.worlds.clear();

        WorldManager worldManager = getServer().getWorldManager();

        for (String w : this.enabledWorlds) {
            World world = worldManager.get(w);
            if (world != null) {
                TownWorld tw = TownWorld.load(this, world);
                this.worlds.put(world.getDimension(), tw);
            }
        }

        this.taskScheduler.shutdownNow();
        this.taskScheduler = Executors.newSingleThreadScheduledExecutor();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 14);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long delay = (c.getTimeInMillis()-System.currentTimeMillis());
        this.taskScheduler.scheduleAtFixedRate(this::collectTaxes, delay, DAYS.toMillis(1), MILLISECONDS);

        TownManagerReloadEvent event = new TownManagerReloadEvent(this);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    protected String getDefaultConfig() {
        return "/assets/" + CraftTowns.MOD_ID + "/config/towns.json";
    }

    @Override
    public void save(JsonObject config) {
        for (WorldTownManager manager : this.managers.values()) {
            try {
                manager.save(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config.addProperty("disable-wild-interact", disableWildInteract);
        JsonArray plotInteractionBypass = new JsonArray();
        for (String pib : this.plotInteractionBypass) {
            plotInteractionBypass.add(pib);
        }
        config.add("plot-interaction-bypass", plotInteractionBypass);
        JsonArray enabledWorlds = new JsonArray();
        for (String pib : this.enabledWorlds) {
            enabledWorlds.add(pib);
        }
        config.add("enabled-worlds", enabledWorlds);

        JsonObject town = new JsonObject();
        town.addProperty("nonpayment-fine", nonpaymentFine);
        town.addProperty("event-broadcast", broadcastEvents);
        town.addProperty("price", townCreationPrice);

        JsonObject taxes = new JsonObject();
        for (PlotType type : PLOT_TYPES) {
            taxes.addProperty(type.getNames().get(0), plotTaxes.getOrDefault(type, 0F));
        }
        town.add("taxes", taxes);
        config.add("town", town);
    }

    @Override
    public void unload() throws Exception {
        super.unload();
        this.taskScheduler.shutdownNow();
    }

    public boolean isOverridesRespawn() {
        return false;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = World.fromVanilla(this.server, event.getWorld());
        if (world != null) {
            WorldTownManager manager = new WorldTownManager(this.server, world, LOGGER);
            this.managers.put(world.getName(), manager);
            if (this.loaded) {
                try {
                    manager.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        String worldName = event.getWorld().provider.getDimensionType().getName();
        WorldTownManager manager = this.managers.remove(worldName);
        if (manager != null) {
            try {
                manager.save(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerMove(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer && !event.getEntityLiving().getEntityWorld().isRemote) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (player.lastTickPosX != player.posX || player.lastTickPosY != player.posY || player.lastTickPosZ != player.posZ) {
                Location oldLocation = new Location(player.world, player.lastTickPosX, player.lastTickPosY, player.lastTickPosZ);
                Location newLocation = new Location(player.world, player.posX, player.posY, player.posZ);
                Town oldTown = this.getTown(oldLocation);
                Town newTown = this.getTown(newLocation);
                if (!Objects.equals(oldTown, newTown) && newTown != null) {
                    Player.from((EntityPlayerMP) player).sendTitle(
                        Text.string(newTown.getName()), Text.translation("tooltip.town.name"), 20, 20, 20
                    );
                }
            }
        }
    }

    //--------------------------------------------------------\\

    private void collectTaxes() {
        EconomyManager economyManager = this.server.getEconomyManager();
        if (!economyManager.isEnabled()) {
            return;
        }

        for (TownWorld world : getAllWorlds()) {
            for (Town town : world.getAllTowns()) {
                if (!town.hasBank()) {
                    continue;
                }
                Bank bank = town.getBank();
                for (Plot plot : town.getAllPlots()) {
                    if (plot.hasOwner() && !plot.isLocked()) {
                        Resident owner = plot.getOwner();
                        try {
                            if (!this.withdrawTax(town, bank, owner, plot)) {
                                plot.setPrice(plot.getPrice() + this.nonpaymentFine); //FIXME
                                plot.setLocked(true);
                            }
                        } catch (CommandException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        getServer().broadcast(
            Text.translation("town.tax-collect").yellow()
        );
    }

    private void expireInvitation(UUID u) {
        Resident resident = getResident(u);
        resident.sendMessage(Text.translation("resident.invitation-expired").red());
    }

    private boolean withdrawTax(Town town, Bank bank, Resident resident, Plot plot) throws CommandException {
        float tax = this.plotTaxes.getOrDefault(plot.getType(), 0F);
        return tax == 0 || resident.withdrawMoney(tax, (amt, success, fmt) -> {
            String msg = "plot.tax.pay." + (success ? "unable" : "success");
            resident.sendMessage(msg, fmt.apply(amt), plot.getLocation());
            if (success) {
                bank.deposit(amt);
            }
        });
    }

    public void saveData() throws IOException {
        for (Resident resident : this.residents.values()) {
            resident.save();
        }
        for (TownWorld tw : this.getAllWorlds()) {
            for (Town town : tw.getAllTowns()) {
                town.save();
            }
            for (Warp warp : tw.getAllWarps()) {
                warp.save();
            }
            tw.save();
        }
    }

    public Text<?, ?> price(float price) {
        EconomyManager economyManager = this.getServer().getEconomyManager();
        if (economyManager.isEnabled()) {
            return economyManager.format(price);
        } else {
            return Text.string(String.format(price % 1.0 != 0 ? "%s" : "%.0f", price));
        }
    }

    public boolean hasWorld(World world) {
        return this.worlds.containsKey(world.getDimension());
    }

    public Town getTown(String name) {
        for (TownWorld world : this.worlds.values()) {
            Town t = world.getTown(name);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public Town getTown(UUID id) {
        for (TownWorld world : this.worlds.values()) {
            Town t = world.getTown(id);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public Town getTown(ChunkLocation location) {
        Plot plot = this.getPlot(location);
        if (plot != null && plot.hasTown()) {
            return plot.getTown();
        }
        return null;
    }

    public Warp getWarp(String name) {
        for (TownWorld world : this.worlds.values()) {
            Warp w = world.getWarp(name);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    public Warp getWarp(UUID id) {
        for (TownWorld world : this.worlds.values()) {
            Warp w = world.getWarp(id);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    public Plot getPlot(ChunkLocation location) {
        TownWorld tw = this.getWorld(location.getDimension());
        if (tw != null) {
            return tw.getPlot(location);
        }
        return null;
    }

    public TownWorld getWorld(net.minecraft.world.World world) {
        return this.worlds.get(Dimension.fromVanilla(world.provider.getDimensionType()));
    }

    public TownWorld getWorld(Dimension dimension) {
        return this.worlds.get(dimension);
    }

    public TownWorld getWorld(World world) {
        return this.worlds.get(world.getDimension());
    }

    public Set<TownWorld> getAllWorlds() {
        return new HashSet<>(this.worlds.values());
    }

    public Resident getResident(String name) {
        PlayerManager playerManager = this.getServer().getPlayerManager();
        OfflinePlayer player = playerManager.getOffline(name);
        if (player != null) {
            return this.getResident(player);
        }
        return null;
    }

    public Resident getResident(OfflinePlayer player) {
        return this.getResident(player.getId());
    }

    public Resident getResident(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        OfflinePlayer opl = this.getServer().getPlayerManager().getOffline(uuid);
        if (opl == null) {
            return null;
        }
        return this.residents.computeIfAbsent(uuid, u -> Resident.load(this, opl.getProfile()));
    }

    public Nation getNation(String name) {
        for (Nation nation : this.nations.values()) {
            if (nation.getName().equalsIgnoreCase(name)) {
                return nation;
            }
        }
        return null;
    }

    public Nation getNation(UUID id) {
        return this.nations.get(id);
    }

    public boolean sendInvitation(Resident inviter, Resident target, Town town) {
        if (!this.pendingInvitations.containsKey(target.getId())) {
            this.pendingInvitations.put(target.getId(), town.getId());
            target.asOnline().sendQuestion("town-invitation:" + town.getName(),
                Text.translation("town.invite.message.header").yellow()
                    .arg(inviter.getName(), Text::gold)
                    .arg(town.getName(), Text::gold),
                    120000,
                    accepted -> {
                        Town t = getInvitation(target);
                        if (accepted) {
                            if (t != null && target.isOnline()) {
                                TownResidentAcceptedInvitationEvent event = new TownResidentAcceptedInvitationEvent(t, target);
                                if (!MinecraftForge.EVENT_BUS.post(event)) {
                                    if (t.addResident(target)) {
                                        target.sendMessage(
                                            Text.translation("town.join.success").green()
                                                .arg(t.getName(), Text::darkGreen)
                                        );
                                        t.broadcast(target,
                                            Text.translation("town.join.success-broadcast").green()
                                                .arg(target.getName(), Text::darkGreen)
                                                .arg(t.getName(), Text::darkGreen)
                                        );
                                        CraftTowns.NETWORK.broadcast(town.serialize());
                                    } else {
                                        target.sendMessage(
                                            Text.translation("commands.generic.unknownFailure").red()
                                        );
                                    }
                                } else {
                                    target.sendMessage(
                                        Text.string("Event canceled").green()
                                    );
                                }
                            } else {
                                target.sendMessage(
                                    Text.translation("town.invite.no-pending").red()
                                );
                            }
                        } else {
                            if (t == null) {
                                target.sendMessage(
                                    Text.translation("town.invite.no-pending").red()
                                );
                            }
                        }
                    }
            );
            return true;
        }
        return false;
    }

    public Town getInvitation(Resident resident) {
        return this.getTown(this.pendingInvitations.remove(resident.getId()));
    }

    public void sendTownInfo(Town town, CommandSender receiver) {
        Text<?,?> message = Text.translation("town.info.header").gray()
                                .arg(town.getName(), Text::white);
        message.appendText("\n");
        Text<?, ?> mayorName = receiver instanceof Resident && town.isMayor((Resident)receiver)
                ? Text.translation("resident.you")
                : Text.string(this.getResident(town.getMayorId()).getName());
        message.append(
            Text.translation("town.info.mayor").gray()
                .arg(mayorName)
        );
        message.appendText("\n");
        message.append(
            Text.translation("town.info.residents").gray()
                .arg(town.getTotalResidents())
        );
        message.appendText("\n");
        int ownedPlots = town.getTotalPlots(Plot::hasOwner);
        int sellingPlots = town.getTotalPlots(Plot::isForSale);
        message.append(
            Text.translation("town.info.plots").gray()
                .arg(town.getTotalPlots())
                .arg(ownedPlots)
                .arg(sellingPlots)
        );
        message.appendText("\n");
        if (receiver instanceof Resident) {
            Resident resident = (Resident)receiver;
            if (town == resident.getTown()) {
                String yourStatus = town.isMayor(resident) ? "mayor" : (town.isAssistant(resident) ? "assistant" : "resident");
                message.appendTranslate("town.info.status", t -> t.argTranslate("town.status." + yourStatus));
            }
        }
        receiver.sendMessage(message);
    }

    public void sendPlotInfo(Resident resident, Plot plot) {
        Text<?, ?> message = Text.translation("plot.info.header").gray()
            .arg(plot.getLocation().getChunkX(), Text::white)
            .arg(plot.getLocation().getChunkZ(), Text::white);
        message.appendText("\n");
        if (plot.hasTown()) {
            message.append(
                Text.translation("plot.info.town").gray()
                    .arg(plot.getTown().getName())
            );
        }
        if (plot.hasOwner()) {
            message.appendText("\n")
                   .append(
                       Text.translation("plot.info.owner").gray()
                           .arg(plot.getOwner().getName())
                   );
        }
        if (plot.isForSale()) {
            message.appendText("\n")
                   .append(
                       Text.translation("plot.info.price").gray()
                           .arg(plot.getPrice())
                   );
        }
        resident.sendMessage(message);
    }

    public void sendResidentInfo(Resident target, CommandSender receiver) {
        Town town = target.getTown();
        Text<?, ?> message = Text.translation("resident.info.header").gray()
                                 .arg(target.getName());
        String status;
        if (town != null) {
            message.appendText("\n");
            message.append(
                Text.translation("resident.info.town").gray()
                    .arg(town.getName())
            );
            message.appendText("\n");
            int ownedPlots = town.getTotalPlots(p -> p.isOwner(target));
            message.append(
                Text.translation("resident.info.plots").gray()
                    .arg(ownedPlots)
            );
            status = town.isMayor(target) ? "mayor" : (town.isAssistant(target) ? "assistant" : "resident");
        } else {
            status = "nomad";
        }
        message.appendText("\n");
        message.append(
            Text.translation("resident.info.friends").gray()
                .arg(target.getFriendsIds().size())
        );
        message.appendText("\n");
        message.append(
            Text.translation("resident.info.status").gray()
                .argTranslate("town.status." + status)
        );
        receiver.sendMessage(message);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.player.world.isRemote) {
            this.residents.computeIfAbsent(event.player.getUniqueID(), u -> Resident.load(this, event.player.getGameProfile()));
        }
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.player.world.isRemote) {
            Resident resident = this.residents.get(event.player.getUniqueID());
            if (resident != null) { //FIXME
                try {
                    resident.save();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isLiquid(IBlockState type) {
        return type.getBlock() instanceof BlockLiquid || type.getBlock() instanceof BlockFluidBase;
    }
}
