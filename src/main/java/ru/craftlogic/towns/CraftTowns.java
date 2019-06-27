package ru.craftlogic.towns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.craftlogic.api.CraftAPI;
import ru.craftlogic.api.network.AdvancedNetwork;
import ru.craftlogic.api.server.Server;
import ru.craftlogic.api.text.Text;
import ru.craftlogic.chat.ChatManager;
import ru.craftlogic.chat.CraftChat;
import ru.craftlogic.towns.common.ProxyCommon;
import ru.craftlogic.towns.data.Resident;

@Mod(modid = CraftTowns.MOD_ID, version = CraftTowns.VERSION, dependencies = "required-after:" + CraftAPI.MOD_ID)
public class CraftTowns {
    public static final String MOD_ID = CraftAPI.MOD_ID + "-towns";
    public static final String VERSION = "0.2.0-BETA";
    public static final Logger LOGGER = LogManager.getLogger("CraftTowns");

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SidedProxy(clientSide = "ru.craftlogic.towns.client.ProxyClient", serverSide = "ru.craftlogic.towns.common.ProxyCommon")
    public static ProxyCommon PROXY;
    public static final AdvancedNetwork NETWORK = new AdvancedNetwork(MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PROXY);
        PROXY.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NETWORK.openChannel();
        PROXY.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        PROXY.postInit();
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        Server server = Server.from(FMLCommonHandler.instance().getMinecraftServerInstance());
        if (Loader.isModLoaded(CraftChat.MOD_ID)) {
            TownManager townManager = server.getManager(TownManager.class);
            ChatManager chatManager = server.getManager(ChatManager.class);
            chatManager.addArgSupplier("town-name", player -> {
                Resident resident = townManager.getResident(player);
                return resident.hasTown() ? Text.string(resident.getTown().getName()) : Text.string();
            });
            chatManager.addArgSupplier("resident-title", player -> {
                Resident resident = townManager.getResident(player);
                return resident.hasTitle() ? Text.string(resident.getTitle()) : Text.string();
            });
        }
    }
}
