package eu.crushedpixel.sponge.masquerade.plugin.data;

import eu.crushedpixel.sponge.masquerade.plugin.AbstractMasquerade;
import net.minecraft.network.datasync.DataParameter;

public class BasicEntityMetadata<T> extends EntityMetadata<T, T> {

    public BasicEntityMetadata(AbstractMasquerade masquerade, DataParameter<T> parameter, T initialValue) {
        super(masquerade, parameter, initialValue);
    }

    @Override
    protected T convertToExternal(T value) {
        return value;
    }

    @Override
    protected T convertToInternal(T value) {
        return value;
    }
}
