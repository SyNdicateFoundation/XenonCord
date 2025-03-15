package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

final class EntityMap_1_14 extends EntityMap {

    static final EntityMap_1_14 INSTANCE = new EntityMap_1_14();

    EntityMap_1_14() {
        addRewrite(0x00, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x01, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x03, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x04, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x05, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x06, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x08, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x1B, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x28, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x29, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x38, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3E, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x43, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x44, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x45, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x46, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x55, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x56, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x58, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x59, ProtocolConstants.Direction.TO_CLIENT, true);

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
            case 0x44:
                rewriteInt(packet, oldId, newId, readerIndexAfterPID + 4);
                break;
            case 0x55:
                DefinedPacket.skipVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x4A:
                DefinedPacket.skipVarInt(packet);
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x37:
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, readerIndexAfterPID);
                break;
            case 0x00:
                rewriteSpawnObject(packet, oldId, newId, 2, 101, 71);
                break;
            case 0x05:
                EntityMap_1_8.rewriteSpawnPlayerUuid(packet, readerIndex);
                break;
            case 0x32:
                EntityMap_1_8.rewriteCombatEvent(packet, oldId, newId);
                break;
            case 0x43:
                DefinedPacket.skipVarInt(packet);
                rewriteMetaVarInt(packet, oldId + 1, newId + 1, 7, protocolVersion);
                rewriteMetaVarInt(packet, oldId, newId, 8, protocolVersion);
                rewriteMetaVarInt(packet, oldId, newId, 15, protocolVersion);
                break;
            case 0x50:
                DefinedPacket.skipVarInt(packet);
                DefinedPacket.skipVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
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

    public static void rewriteSpawnObject(ByteBuf packet, int oldId, int newId, int arrowId, int fishingBobberId, int spectralArrowId) {
        DefinedPacket.skipVarInt(packet);
        DefinedPacket.skipUUID(packet);
        final int type = DefinedPacket.readVarInt(packet);
        if (type == arrowId || type == fishingBobberId || type == spectralArrowId) {
            final int modOldId = (type == arrowId || type == spectralArrowId) ? oldId + 1 : oldId;
            final int modNewId = (type == arrowId || type == spectralArrowId) ? newId + 1 : newId;
            packet.skipBytes(26);
            final int position = packet.readerIndex();
            final int readId = packet.readInt();
            if (readId == modOldId) {
                packet.setInt(position, modNewId);
            } else if (readId == modNewId) {
                packet.setInt(position, modOldId);
            }
        }
    }
}
