package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.UUID;

class EntityMap_1_12 extends EntityMap {

    static final EntityMap_1_12 INSTANCE = new EntityMap_1_12();

    EntityMap_1_12() {
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
        addRewrite(0x32, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x35, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x38, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3C, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x3D, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x3E, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x42, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4D, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x4E, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0B, ProtocolConstants.Direction.TO_SERVER, true);
        addRewrite(0x15, ProtocolConstants.Direction.TO_SERVER, true);
    }

    @Override
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void rewriteClientbound(ByteBuf packet, int oldId, int newId) {
        super.rewriteClientbound(packet, oldId, newId);
        final int readerIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int packetIdLength = packet.readerIndex() - readerIndex;
        int jumpIndex = packet.readerIndex();
        switch (packetId) {
            case 0x3C:
                rewriteInt(packet, oldId, newId, readerIndex + packetIdLength + 4);
                break;
            case 0x4A:
                DefinedPacket.readVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x42:
                DefinedPacket.readVarInt(packet);
                jumpIndex = packet.readerIndex();
            case 0x31: {
                final int count = DefinedPacket.readVarInt(packet);
                final int[] ids = new int[count];
                for (int i = 0; i < count; i++) {
                    ids[i] = DefinedPacket.readVarInt(packet);
                }
                packet.readerIndex(jumpIndex);
                packet.writerIndex(jumpIndex);
                DefinedPacket.writeVarInt(count, packet);
                for (int id : ids) {
                    if (id == oldId) {
                        id = newId;
                    } else if (id == newId) {
                        id = oldId;
                    }
                    DefinedPacket.writeVarInt(id, packet);
                }
                break;
            }
            case 0x00: {
                DefinedPacket.readVarInt(packet);
                DefinedPacket.readUUID(packet);
                final int type = packet.readUnsignedByte();
                if (type == 60 || type == 90 || type == 91) {
                    if (type == 60 || type == 91) {
                        oldId = oldId + 1;
                        newId = newId + 1;
                    }
                    packet.skipBytes(26);
                    final int pos = packet.readerIndex();
                    final int readId = packet.readInt();
                    if (readId == oldId) {
                        packet.setInt(pos, newId);
                    } else if (readId == newId) {
                        packet.setInt(pos, oldId);
                    }
                }
                break;
            }
            case 0x05: {
                DefinedPacket.readVarInt(packet);
                final int idLength = packet.readerIndex() - readerIndex - packetIdLength;
                final UUID uuid = DefinedPacket.readUUID(packet);
                final ProxiedPlayer player = BungeeCord.getInstance().getPlayerByOfflineUUID(uuid);
                if (player != null) {
                    final int previous = packet.writerIndex();
                    packet.readerIndex(readerIndex);
                    packet.writerIndex(readerIndex + packetIdLength + idLength);
                    DefinedPacket.writeUUID(((UserConnection) player).getRewriteId(), packet);
                    packet.writerIndex(previous);
                }
                break;
            }
            case 0x2C: {
                final int event = packet.readUnsignedByte();
                if (event == 1) {
                    DefinedPacket.readVarInt(packet);
                    rewriteInt(packet, oldId, newId, packet.readerIndex());
                } else if (event == 2) {
                    final int pos = packet.readerIndex();
                    rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                    packet.readerIndex(pos);
                    DefinedPacket.readVarInt(packet);
                    rewriteInt(packet, oldId, newId, packet.readerIndex());
                }
                break;
            }
            case 0x3B:
                DefinedPacket.readVarInt(packet);
                rewriteMetaVarInt(packet, oldId + 1, newId + 1, 6);
                rewriteMetaVarInt(packet, oldId, newId, 7);
                rewriteMetaVarInt(packet, oldId, newId, 13);
                break;
        }
        packet.readerIndex(readerIndex);
    }

    @Override
    public void rewriteServerbound(ByteBuf packet, int oldId, int newId) {
        super.rewriteServerbound(packet, oldId, newId);
        final int readerIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int packetIdLength = packet.readerIndex() - readerIndex;
        if (packetId == 0x1E) {
            final UUID uuid = DefinedPacket.readUUID(packet);
            final ProxiedPlayer player = BungeeCord.getInstance().getPlayer(uuid);
            if (player != null) {
                final int previous = packet.writerIndex();
                packet.readerIndex(readerIndex);
                packet.writerIndex(readerIndex + packetIdLength);
                DefinedPacket.writeUUID(((UserConnection) player).getRewriteId(), packet);
                packet.writerIndex(previous);
            }
        }
        packet.readerIndex(readerIndex);
    }
}
