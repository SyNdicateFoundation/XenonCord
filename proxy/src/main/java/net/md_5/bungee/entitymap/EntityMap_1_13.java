package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

final class EntityMap_1_13 extends EntityMap {

    static final EntityMap_1_13 INSTANCE = new EntityMap_1_13();

    EntityMap_1_13() {
        addRewrite(0x00, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x01, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x03, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x04, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x05, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x06, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x08, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x1C, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x27, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x28, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x29, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x33, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x36, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x39, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3F, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x40, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x41, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x42, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x46, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4F, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x50, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x52, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x53, ProtocolConstants.Direction.TO_CLIENT, true);

        addRewrite(0x0D, ProtocolConstants.Direction.TO_SERVER, true);
        addRewrite(0x19, ProtocolConstants.Direction.TO_SERVER, true);
    }

    @Override
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void rewriteClientbound(ByteBuf packet, int oldId, int newId, int protocolVersion) {
        super.rewriteClientbound(packet, oldId, newId);
        final int readerIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int readerIndexAfterPID = packet.readerIndex();
        switch (packetId) {
            case 0x40:
                rewriteInt(packet, oldId, newId, readerIndexAfterPID + 4);
                break;
            case 0x4F:
                DefinedPacket.skipVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x46:
                DefinedPacket.skipVarInt(packet);
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x35:
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, readerIndexAfterPID);
                break;
            case 0x00:
                EntityMap_1_9.rewriteSpawnObject(packet, oldId, newId);
                break;
            case 0x05:
                EntityMap_1_8.rewriteSpawnPlayerUuid(packet, readerIndex);
                break;
            case 0x2F:
                EntityMap_1_8.rewriteCombatEvent(packet, oldId, newId);
                break;
            case 0x3F:
                DefinedPacket.skipVarInt(packet);
                rewriteMetaVarInt(packet, oldId + 1, newId + 1, 6, protocolVersion);
                rewriteMetaVarInt(packet, oldId, newId, 7, protocolVersion);
                rewriteMetaVarInt(packet, oldId, newId, 13, protocolVersion);
                break;
        }
        packet.readerIndex(readerIndex);
    }

    @Override
    public void rewriteServerbound(ByteBuf packet, int oldId, int newId) {
        super.rewriteServerbound(packet, oldId, newId);
        final int readerIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int readerIndexAfterPID = packet.readerIndex();
        if (packetId == 0x28 && !BungeeCord.getInstance().getConfig().isIpForward()) {
            EntityMap_1_8.rewriteSpectateUuid(packet, readerIndex, readerIndexAfterPID);
        }
        packet.readerIndex(readerIndex);
    }
}
