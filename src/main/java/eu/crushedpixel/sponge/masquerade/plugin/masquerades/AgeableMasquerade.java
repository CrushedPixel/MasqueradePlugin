package eu.crushedpixel.sponge.masquerade.plugin.masquerades;

import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;

public class AgeableMasquerade extends LivingMobMasquerade {

    public AgeableMasquerade(Player player, EntityType entityType) {
        super(player, entityType);
    }

    @Override
    protected void registerKeys() {
        super.registerKeys();
        // TODO: register age key once it's added
    }
}
