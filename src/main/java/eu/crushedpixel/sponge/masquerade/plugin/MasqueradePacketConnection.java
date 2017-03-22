package eu.crushedpixel.sponge.masquerade.plugin;

import eu.crushedpixel.sponge.masquerade.plugin.utils.PacketUtils;
import eu.crushedpixel.sponge.packetgate.api.event.PacketEvent;
import eu.crushedpixel.sponge.packetgate.api.listener.PacketListenerAdapter;
import eu.crushedpixel.sponge.packetgate.api.registry.PacketConnection;
import eu.crushedpixel.sponge.packetgate.api.registry.PacketGate;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.network.play.server.SPacketEntityProperties;
import net.minecraft.network.play.server.SPacketSpawnPlayer;
import org.spongepowered.api.Sponge;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static eu.crushedpixel.sponge.masquerade.plugin.utils.PacketUtils.rotationToByte;

public class MasqueradePacketConnection extends PacketListenerAdapter {

    private static final String UNREGISTER_CHANNEL = "Msqrd|unregister";

    private final AbstractMasquerade masquerade;
    private final PacketConnection connection;
    private final UUID uuid;

    public MasqueradePacketConnection(AbstractMasquerade masquerade, PacketConnection connection) {
        this.masquerade = masquerade;
        this.connection = connection;
        this.uuid = UUID.randomUUID();
    }

    /**
     * List of Packets that were sent out manually and should therefore not be handled
     * by the packet listener.
     */
    private final Set<Packet> toIgnore = new HashSet<>();

    public void register() {
        Sponge.getServiceManager().provide(PacketGate.class).get()
                .registerListener(this,
                        ListenerPriority.LAST,
                        connection,
                        SPacketSpawnPlayer.class,
                        SPacketEntityMetadata.class,
                        SPacketEntityProperties.class,
                        SPacketAnimation.class,
                        SPacketCustomPayload.class);
    }

    /**
     * Sends a packet through the netty pipeline that, once received by this listener,
     * unregisters this listener so no further packets are modified.
     * This ensures that all packets that are queued up are properly handled before
     * the listener is unregistered.
     */
    public void unregister() {
        PacketBuffer packetBuffer = new PacketBuffer(Unpooled.buffer(16));
        packetBuffer.writeUniqueId(uuid);

        SPacketCustomPayload packetCustomPayload = new SPacketCustomPayload(UNREGISTER_CHANNEL, packetBuffer);

        connection.sendPacket(packetCustomPayload);
    }

    public void sendPackets(List<Packet> packets) {
        packets.forEach(this::sendPacket);
    }

    public void sendPacket(Packet packet) {
        toIgnore.add(packet);
        connection.sendPacket(packet);
    }

    @Override
    public void onPacketWrite(PacketEvent packetEvent, PacketConnection connection) {
        Packet packet = packetEvent.getPacket();

        if (toIgnore.contains(packet)) {
            toIgnore.remove(packet);
            return;
        }

        if (packet instanceof SPacketSpawnPlayer) {
            handlePacketSpawnPlayer((SPacketSpawnPlayer) packet, packetEvent);
        } else if (packet instanceof SPacketEntityMetadata) {
            handlePacketEntityMetadata((SPacketEntityMetadata) packet, packetEvent);
        } else if (packet instanceof SPacketEntityProperties) {
            handlePacketEntityProperties((SPacketEntityProperties) packet, packetEvent);
        } else if (packet instanceof SPacketAnimation) {
            handlePacketAnimation((SPacketAnimation) packet, packetEvent);
        } else if (packet instanceof SPacketCustomPayload) {
            handlePacketCustomPayload((SPacketCustomPayload) packet, packetEvent);
        }
    }

    private void handlePacketSpawnPlayer(SPacketSpawnPlayer packetSpawnPlayer, PacketEvent packetEvent) {
        // when a new SPacketSpawnPlayer is sent for the masked player,
        // update the entityID as it has most likely changed
        if (packetSpawnPlayer.uniqueId.equals(masquerade.getPlayerUUID())) {
            masquerade.setEntityID(packetSpawnPlayer.entityId);
        }

        if (packetSpawnPlayer.entityId != masquerade.getEntityID()) return;

        packetEvent.setCancelled(true);

        // despawn the entity first in case the client still knows the fake entity
        masquerade.despawnEntity(this);

        // replace spawn player packets with the fake entity spawn packets
        sendPackets(masquerade.createSpawnPackets(
                packetSpawnPlayer.x, packetSpawnPlayer.y, packetSpawnPlayer.z,
                packetSpawnPlayer.yaw, packetSpawnPlayer.pitch, rotationToByte(0),
                (short) 0, (short) 0, (short) 0
        ));

        // once the new entity spawns, send all metadata
        masquerade.sendEntityData(this);
    }

    private void handlePacketEntityMetadata(SPacketEntityMetadata packetEntityMetadata, PacketEvent packetEvent) {
        if (packetEntityMetadata.entityId != masquerade.getEntityID()) return;

        // as Minecraft shares this packet with other connections,
        // clone the packet so any manipulations only affect the packet sent to this connection
        SPacketEntityMetadata packet = PacketUtils.clonePacketEntityMetadata(packetEntityMetadata);
        packetEvent.setPacket(packet);

        masquerade.handlePacketEntityMetadata(packet);

        if (packet.dataManagerEntries.isEmpty()) {
            packetEvent.setCancelled(true);
        }
    }

    private void handlePacketEntityProperties(SPacketEntityProperties packetEntityProperties, PacketEvent packetEvent) {
        if (packetEntityProperties.entityId != masquerade.getEntityID()) return;

        // property packets are only accepted by the client for living entities
        if (!EntityLivingBase.class.isAssignableFrom(masquerade.getEntityClass())) {
            packetEvent.setCancelled(true);
            return;
        }

        // player entities have attributes that other entity types may not have, for example luck.
        // remove all attributes that are not registered for the masquerade's entity type.

        Iterator<SPacketEntityProperties.Snapshot> it = packetEntityProperties.snapshots.iterator();

        while (it.hasNext()) {
            SPacketEntityProperties.Snapshot snapshot = it.next();

            boolean valid = false;
            for (IAttribute attribute : masquerade.getValidAttributes()) {
                if (snapshot.getName().equals(attribute.getName())) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                it.remove();
            }
        }

        if (packetEntityProperties.snapshots.isEmpty()) {
            packetEvent.setCancelled(true);
        }
    }

    private void handlePacketAnimation(SPacketAnimation packetAnimation, PacketEvent packetEvent) {
        if (packetAnimation.entityId != masquerade.getEntityID()) return;

        // the clients can't handle animation packets when the entity is not a subclass of EntityLivingBase.
        if (!EntityLivingBase.class.isAssignableFrom(masquerade.getEntityClass())) {
            packetEvent.setCancelled(true);
        }
    }

    private void handlePacketCustomPayload(SPacketCustomPayload packetCustomPayload, PacketEvent packetEvent) {
        if (!UNREGISTER_CHANNEL.equals(packetCustomPayload.channel)) return;

        packetCustomPayload.data.markReaderIndex();
        UUID uuid = packetCustomPayload.data.readUniqueId();
        packetCustomPayload.data.resetReaderIndex();
        if (!this.uuid.equals(uuid)) return;

        packetEvent.setCancelled(true);

        // unregister listener
        Sponge.getServiceManager().provide(PacketGate.class).get().unregisterListener(this);
    }
}
