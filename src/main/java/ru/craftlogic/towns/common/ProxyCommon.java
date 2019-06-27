package ru.craftlogic.towns.common;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import ru.craftlogic.api.event.server.ServerAddManagersEvent;
import ru.craftlogic.api.network.AdvancedMessage;
import ru.craftlogic.api.network.AdvancedMessageHandler;
import ru.craftlogic.towns.CraftTowns;
import ru.craftlogic.towns.TownManager;
import ru.craftlogic.towns.network.message.MessageDeletePlot;
import ru.craftlogic.towns.network.message.MessageDeleteTown;
import ru.craftlogic.towns.network.message.MessagePlot;
import ru.craftlogic.towns.network.message.MessageTown;
import ru.craftlogic.util.ReflectiveUsage;

@ReflectiveUsage
public class ProxyCommon extends AdvancedMessageHandler {
    public void preInit() {

    }

    public void init() {
        CraftTowns.NETWORK.registerMessage(this::handleTown, MessageTown.class, Side.CLIENT);
        CraftTowns.NETWORK.registerMessage(this::handlePlot, MessagePlot.class, Side.CLIENT);
        CraftTowns.NETWORK.registerMessage(this::handleDeleteTown, MessageDeleteTown.class, Side.CLIENT);
        CraftTowns.NETWORK.registerMessage(this::handleDeletePlot, MessageDeletePlot.class, Side.CLIENT);
    }

    public void postInit() {

    }

    protected AdvancedMessage handleTown(MessageTown message, MessageContext context) {
        return null;
    }

    protected AdvancedMessage handlePlot(MessagePlot message, MessageContext context) {
        return null;
    }

    protected AdvancedMessage handleDeleteTown(MessageDeleteTown message, MessageContext context) {
        return null;
    }

    protected AdvancedMessage handleDeletePlot(MessageDeletePlot message, MessageContext context) {
        return null;
    }

    @SubscribeEvent
    public void onServerAddManagers(ServerAddManagersEvent event) {
        event.addManager(TownManager.class, TownManager::new);
    }
}
