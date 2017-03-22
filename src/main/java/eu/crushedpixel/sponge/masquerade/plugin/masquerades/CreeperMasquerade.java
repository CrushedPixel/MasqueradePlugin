package eu.crushedpixel.sponge.masquerade.plugin.masquerades;

import eu.crushedpixel.sponge.masquerade.plugin.data.BasicEntityMetadata;
import net.minecraft.entity.monster.EntityCreeper;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;

public class CreeperMasquerade extends LivingMobMasquerade {

    public CreeperMasquerade(Player player) {
        super(player, EntityTypes.CREEPER);
    }

    @Override
    protected void registerKeys() {
        super.registerKeys();
        registerKey(Keys.CREEPER_CHARGED, new BasicEntityMetadata<>(this, EntityCreeper.POWERED, false));
    }
}
