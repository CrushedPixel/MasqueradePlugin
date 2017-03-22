package eu.crushedpixel.sponge.masquerade.plugin;

import eu.crushedpixel.sponge.masquerade.api.Masquerades;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = MasqueradePlugin.ID, version = MasqueradePlugin.VERSION,
        name = MasqueradePlugin.NAME,
        dependencies = { @Dependency(id = "packetgate") })
public class MasqueradePlugin {

    static final String ID = "masquerade";
    static final String VERSION = "0.1";
    static final String NAME = "Masquerade";

    @Listener(order = Order.EARLY)
    public void onPreInit(GamePreInitializationEvent event) {
        Masquerades masquerades = new MasqueradesImpl();
        Sponge.getServiceManager().setProvider(this, Masquerades.class, masquerades);
    }

}
