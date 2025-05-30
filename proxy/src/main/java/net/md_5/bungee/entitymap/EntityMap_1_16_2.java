package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.DefinedPacket;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class EntityMap_1_16_2 extends EntityMap {
    static final EntityMap_1_16_2 INSTANCE_1_16_2 = new EntityMap_1_16_2(0x04, 0x2D);
    static final EntityMap_1_16_2 INSTANCE_1_17 = new EntityMap_1_16_2(0x04, 0x2D);
    static final EntityMap_1_16_2 INSTANCE_1_18 = new EntityMap_1_16_2(0x04, 0x2D);
    static final EntityMap_1_16_2 INSTANCE_1_19 = new EntityMap_1_16_2(0x02, 0x2F);
    static final EntityMap_1_16_2 INSTANCE_1_19_1 = new EntityMap_1_16_2(0x02, 0x30);
    static final EntityMap_1_16_2 INSTANCE_1_19_4 = new EntityMap_1_16_2(0x03, 0x30);

    private final int spawnPlayerId;
    private final int spectateId;

    @Override
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void rewriteClientbound(ByteBuf packet, int oldId, int newId, int protocolVersion) {
        final int originalReaderIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int packetIdLength = packet.readerIndex() - originalReaderIndex;
        if (packetId == spawnPlayerId) {
            EntityMap_1_8.rewriteSpawnPlayerUuid(packet, originalReaderIndex);
        }
        packet.readerIndex(originalReaderIndex);
    }

    @Override
    public void rewriteServerbound(ByteBuf packet, int oldId, int newId) {
        final int originalReaderIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int packetIdLength = packet.readerIndex() - originalReaderIndex;
        if (packetId == spectateId && !BungeeCord.getInstance().getConfig().isIpForward()) {
            EntityMap_1_8.rewriteSpectateUuid(packet, originalReaderIndex, packetIdLength);
        }
        packet.readerIndex(originalReaderIndex);
    }
}
