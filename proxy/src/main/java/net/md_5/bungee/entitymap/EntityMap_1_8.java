package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import java.util.UUID;

class EntityMap_1_8 extends EntityMap {

    static final EntityMap_1_8 INSTANCE = new EntityMap_1_8();

    EntityMap_1_8() {
        addRewrite(0x04, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0A, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0B, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0D, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0E, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x0F, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x10, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x11, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x12, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x14, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x15, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x16, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x17, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x18, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x19, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x1A, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x1B, ProtocolConstants.Direction.TO_CLIENT, false);
        addRewrite(0x1C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x1D, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x1E, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x20, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x25, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x2C, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x43, ProtocolConstants.Direction.TO_CLIENT, true);
        addRewrite(0x49, ProtocolConstants.Direction.TO_CLIENT, true);

        addRewrite(0x02, ProtocolConstants.Direction.TO_SERVER, true);
        addRewrite(0x0B, ProtocolConstants.Direction.TO_SERVER, true);
    }

    public static void rewriteCombatEvent(ByteBuf packet, int oldId, int newId) {
        if (oldId == newId) return;
        final int event = packet.readUnsignedByte();
        if (event == 1) {
            DefinedPacket.skipVarInt(packet);
            rewriteInt(packet, oldId, newId, packet.readerIndex());
        } else if (event == 2) {
            final int pos = packet.readerIndex();
            rewriteVarInt(packet, oldId, newId, packet.readerIndex());
            packet.readerIndex(pos);
            DefinedPacket.skipVarInt(packet);
            rewriteInt(packet, oldId, newId, packet.readerIndex());
        }
    }

    public static void rewriteSpawnPlayerUuid(ByteBuf packet, int startReader) {
        DefinedPacket.skipVarInt(packet);
        final int afterEid = packet.readerIndex();
        final UUID uuid = DefinedPacket.readUUID(packet);
        final ProxiedPlayer player = BungeeCord.getInstance().getPlayerByOfflineUUID(uuid);
        if (player != null) {
            final int prevWriter = packet.writerIndex();
            packet.readerIndex(startReader);
            packet.writerIndex(afterEid);
            DefinedPacket.writeUUID(player.getUniqueId(), packet);
            packet.writerIndex(prevWriter);
        }
    }

    private static void rewriteSpawnObject(ByteBuf packet, int oldId, int newId) {
        if (oldId == newId) return;
        DefinedPacket.skipVarInt(packet);
        final int type = packet.readUnsignedByte();
        if (type == 60 || type == 90) {
            packet.skipBytes(14);
            final int pos = packet.readerIndex();
            final int readId = packet.readInt();
            int changedId = readId;
            if (readId == oldId) {
                packet.setInt(pos, changedId = newId);
            } else if (readId == newId) {
                packet.setInt(pos, changedId = oldId);
            }
            if (readId > 0 && changedId <= 0) {
                packet.writerIndex(packet.writerIndex() - 6);
            } else if (changedId > 0 && readId <= 0) {
                packet.ensureWritable(6);
                packet.writerIndex(packet.writerIndex() + 6);
            }
        }
    }

    public static void rewriteEntityIdArray(ByteBuf packet, int oldId, int newId, int afterPid) {
        if (oldId == newId) return;
        final int count = DefinedPacket.readVarInt(packet);
        final int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = DefinedPacket.readVarInt(packet);
        }
        packet.readerIndex(afterPid);
        packet.writerIndex(afterPid);
        DefinedPacket.writeVarInt(count, packet);
        for (int id : ids) {
            if (id == oldId) id = newId;
            else if (id == newId) id = oldId;
            DefinedPacket.writeVarInt(id, packet);
        }
    }

    public static void rewriteSpectateUuid(ByteBuf packet, int startReader, int afterPid) {
        final UUID uuid = DefinedPacket.readUUID(packet);
        final ProxiedPlayer player = BungeeCord.getInstance().getPlayer(uuid);
        if (player != null) {
            final int prevWriter = packet.writerIndex();
            packet.readerIndex(startReader);
            packet.writerIndex(afterPid);
            DefinedPacket.writeUUID(((UserConnection) player).getPendingConnection().getOfflineId(), packet);
            packet.writerIndex(prevWriter);
        }
    }

    @Override
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void rewriteClientbound(ByteBuf packet, int oldId, int newId) {
        super.rewriteClientbound(packet, oldId, newId);
        final int startReader = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int afterPid = packet.readerIndex();
        switch (packetId) {
            case 0x1B:
                rewriteInt(packet, oldId, newId, afterPid + 4);
                break;
            case 0x0D:
                DefinedPacket.skipVarInt(packet);
                rewriteVarInt(packet, oldId, newId, packet.readerIndex());
                break;
            case 0x13:
                rewriteEntityIdArray(packet, oldId, newId, afterPid);
                break;
            case 0x0E:
                rewriteSpawnObject(packet, oldId, newId);
                break;
            case 0x0C:
                rewriteSpawnPlayerUuid(packet, startReader);
                break;
            case 0x42:
                rewriteCombatEvent(packet, oldId, newId);
                break;
        }
        packet.readerIndex(startReader);
    }

    @Override
    public void rewriteServerbound(ByteBuf packet, int oldId, int newId) {
        super.rewriteServerbound(packet, oldId, newId);
        final int startReader = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int afterPid = packet.readerIndex();
        if (packetId == 0x18 && !BungeeCord.getInstance().getConfig().isIpForward()) {
            rewriteSpectateUuid(packet, startReader, afterPid);
        }
        packet.readerIndex(startReader);
    }
}
