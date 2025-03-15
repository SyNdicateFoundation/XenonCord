package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

final class EntityMap_1_15 extends EntityMap {

    static final EntityMap_1_15 INSTANCE = new EntityMap_1_15();

    private EntityMap_1_15() {
        addRewrite(0x00, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x01, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x03, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x04, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x05, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x06, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x09, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x1C, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x29, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x39, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3F, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x44, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x45, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x46, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x47, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x56, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x57, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x59, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x5A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0E, ProtocolConstants.Direction.TO_SERVER, true);
        addRewrite(0x1B, ProtocolConstants.Direction.TO_SERVER, true);
    }

    @Override
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void rewriteClientbound(ByteBuf packet, int oldId, int newId, int protocolVersion) {
        super.rewriteClientbound(packet, oldId, newId);
        final int readerIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int readerIndexAfterPID = packet.readerIndex();
        switch (packetId) {
            case 0x45:
                rewriteInt(packet, oldId, newId, readerIndexAfterPID + 4);
                break;
            case 0x56:
                DefinedPacket.skipVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x4B:
                DefinedPacket.skipVarInt(packet);
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x38:
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, readerIndexAfterPID);
                break;
            case 0x00:
                EntityMap_1_14.rewriteSpawnObject(packet, oldId, newId, 2, 102, 72);
                break;
            case 0x05:
                EntityMap_1_8.rewriteSpawnPlayerUuid(packet, readerIndex);
                break;
            case 0x33:
                EntityMap_1_8.rewriteCombatEvent(packet, oldId, newId);
                break;
            case 0x44:
                DefinedPacket.skipVarInt(packet);
                rewriteMetaVarInt(packet, oldId + 1, newId + 1, 7, protocolVersion);
                rewriteMetaVarInt(packet, oldId, newId, 8, protocolVersion);
                rewriteMetaVarInt(packet, oldId, newId, 16, protocolVersion);
                break;
            case 0x51:
                DefinedPacket.skipVarInt(packet);
                DefinedPacket.skipVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                break;
            default:
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
        if (packetId == 0x2B && !BungeeCord.getInstance().getConfig().isIpForward()) {
            EntityMap_1_8.rewriteSpectateUuid(packet, readerIndex, readerIndexAfterPID);
        }
        packet.readerIndex(readerIndex);
    }
}
