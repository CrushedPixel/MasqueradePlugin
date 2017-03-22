package eu.crushedpixel.sponge.masquerade.plugin.data;

import eu.crushedpixel.sponge.masquerade.plugin.AbstractMasquerade;
import net.minecraft.network.datasync.DataParameter;

/**
 * Boolean Metadata that has an inverted plugin meaning than the Key suggests
 */
public class InvertedBooleanMetadata extends BasicEntityMetadata<Boolean> {

    public InvertedBooleanMetadata(AbstractMasquerade masquerade, DataParameter<Boolean> parameter, Boolean initialValue) {
        super(masquerade, parameter, !initialValue);
    }

    @Override
    protected Boolean convertToExternal(Boolean value) {
        return !super.convertToExternal(value);
    }

    @Override
    protected Boolean convertToInternal(Boolean value) {
        return !super.convertToInternal(value);
    }
}
