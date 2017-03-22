package eu.crushedpixel.sponge.masquerade.plugin.data.flag;

import eu.crushedpixel.sponge.masquerade.plugin.AbstractMasquerade;
import net.minecraft.network.datasync.EntityDataManager;

public class BooleanFlagEntityMetadata extends FlagEntityMetadata<Boolean> {

    public BooleanFlagEntityMetadata(AbstractMasquerade masquerade, EntityDataManager.DataEntry<Byte> flagDataEntry, int position) {
        super(masquerade, flagDataEntry, position);
    }

    @Override
    protected Boolean convertToExternal(Byte value) {
        return getFlag(value, position);
    }

    @Override
    protected Byte convertToInternal(Boolean value) {
        return setFlag(this.dataEntry.getValue(), position, value);
    }

}
