package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

class EntityMap_1_9 extends EntityMap {
    static final EntityMap_1_9 INSTANCE = new EntityMap_1_9();

    EntityMap_1_9() {
        addRewrite(0x00, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x01, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x03, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x04, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x05, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x06, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x08, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x1B, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x25, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x26, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x27, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x28, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2F, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x31, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x34, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x36, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x39, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3A, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x3B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x40, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x49, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0A, ProtocolConstants.Direction.TO_SERVER, true);
        addRewrite(0x14, ProtocolConstants.Direction.TO_SERVER, true);
    }

    public static void rewriteSpawnObject(ByteBuf packet, int oldId, int newId) {
        if (oldId == newId) return;
        DefinedPacket.skipVarInt(packet);
        DefinedPacket.skipUUID(packet);
        int type = packet.readUnsignedByte();
        if (type == 60 || type == 90 || type == 91) {
            if (type == 60 || type == 91) {
                oldId++;
                newId++;
            }
            packet.skipBytes(26);
            int pos = packet.readerIndex();
            int read = packet.readInt();
            if (read == oldId) packet.setInt(pos, newId);
            else if (read == newId) packet.setInt(pos, oldId);
        }
    }

    @Override
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void rewriteClientbound(ByteBuf packet, int oldId, int newId) {
        super.rewriteClientbound(packet, oldId, newId);
        int initIndex = packet.readerIndex();
        int packetId = DefinedPacket.readVarInt(packet);
        int afterPID = packet.readerIndex();
        switch (packetId) {
            case 0x3A:
                rewriteInt(packet, oldId, newId, afterPID + 4);
                break;
            case 0x49:
                DefinedPacket.skipVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x40:
                DefinedPacket.skipVarInt(packet);
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x30:
                EntityMap_1_8.rewriteEntityIdArray(packet, oldId, newId, afterPID);
                break;
            case 0x00:
                rewriteSpawnObject(packet, oldId, newId);
                break;
            case 0x05:
                EntityMap_1_8.rewriteSpawnPlayerUuid(packet, initIndex);
                break;
            case 0x2C:
                EntityMap_1_8.rewriteCombatEvent(packet, oldId, newId);
                break;
            case 0x39:
                DefinedPacket.skipVarInt(packet);
                rewriteMetaVarInt(packet, oldId + 1, newId + 1, 5);
                rewriteMetaVarInt(packet, oldId, newId, 12);
                break;
        }
        packet.readerIndex(initIndex);
    }

    @Override
    public void rewriteServerbound(ByteBuf packet, int oldId, int newId) {
        super.rewriteServerbound(packet, oldId, newId);
        int initIndex = packet.readerIndex();
        int packetId = DefinedPacket.readVarInt(packet);
        int afterPID = packet.readerIndex();
        if (packetId == 0x1B && !BungeeCord.getInstance().getConfig().isIpForward()) {
            EntityMap_1_8.rewriteSpectateUuid(packet, initIndex, afterPID);
        }
        packet.readerIndex(initIndex);
    }
}
