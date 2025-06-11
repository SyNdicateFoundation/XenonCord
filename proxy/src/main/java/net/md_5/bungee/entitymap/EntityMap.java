package net.md_5.bungee.entitymap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.Tag;

import java.io.DataInputStream;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class EntityMap {

    private final boolean[] clientboundInts = new boolean[256];
    private final boolean[] clientboundVarInts = new boolean[256];
    private final boolean[] serverboundInts = new boolean[256];
    private final boolean[] serverboundVarInts = new boolean[256];

    public static EntityMap getEntityMap(int version) {
        if (net.md_5.bungee.api.ProxyServer.getInstance().getConfig().isDisableEntityMetadataRewrite()) {
            return EntityMap_Dummy.INSTANCE;
        }
        switch (version) {
            case ProtocolConstants.MINECRAFT_1_8:
                return EntityMap_1_8.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_9:
            case ProtocolConstants.MINECRAFT_1_9_1:
            case ProtocolConstants.MINECRAFT_1_9_2:
                return EntityMap_1_9.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_9_4:
                return EntityMap_1_9_4.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_10:
                return EntityMap_1_10.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_11:
            case ProtocolConstants.MINECRAFT_1_11_1:
                return EntityMap_1_11.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_12:
                return EntityMap_1_12.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_12_1:
            case ProtocolConstants.MINECRAFT_1_12_2:
                return EntityMap_1_12_1.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_13:
            case ProtocolConstants.MINECRAFT_1_13_1:
            case ProtocolConstants.MINECRAFT_1_13_2:
                return EntityMap_1_13.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_14:
            case ProtocolConstants.MINECRAFT_1_14_1:
            case ProtocolConstants.MINECRAFT_1_14_2:
            case ProtocolConstants.MINECRAFT_1_14_3:
            case ProtocolConstants.MINECRAFT_1_14_4:
                return EntityMap_1_14.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_15:
            case ProtocolConstants.MINECRAFT_1_15_1:
            case ProtocolConstants.MINECRAFT_1_15_2:
                return EntityMap_1_15.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_16:
            case ProtocolConstants.MINECRAFT_1_16_1:
                return EntityMap_1_16.INSTANCE;
            case ProtocolConstants.MINECRAFT_1_16_2:
            case ProtocolConstants.MINECRAFT_1_16_3:
            case ProtocolConstants.MINECRAFT_1_16_4:
                return EntityMap_1_16_2.INSTANCE_1_16_2;
            case ProtocolConstants.MINECRAFT_1_17:
            case ProtocolConstants.MINECRAFT_1_17_1:
                return EntityMap_1_16_2.INSTANCE_1_17;
            case ProtocolConstants.MINECRAFT_1_18:
            case ProtocolConstants.MINECRAFT_1_18_2:
                return EntityMap_1_16_2.INSTANCE_1_18;
            case ProtocolConstants.MINECRAFT_1_19:
                return EntityMap_1_16_2.INSTANCE_1_19;
            case ProtocolConstants.MINECRAFT_1_19_1:
            case ProtocolConstants.MINECRAFT_1_19_3:
                return EntityMap_1_16_2.INSTANCE_1_19_1;
            case ProtocolConstants.MINECRAFT_1_19_4:
            case ProtocolConstants.MINECRAFT_1_20:
                return EntityMap_1_16_2.INSTANCE_1_19_4;
            default:
                return null;

        }
    }

    protected static void rewriteInt(ByteBuf packet, int oldId, int newId, int offset) {
        if (oldId == newId) return;
        final int readId = packet.getInt(offset);
        if (readId == oldId) {
            packet.setInt(offset, newId);
        } else if (readId == newId) {
            packet.setInt(offset, oldId);
        }
    }

    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    protected static void rewriteVarInt(ByteBuf packet, int oldId, int newId, int offset) {
        if (oldId == newId) return;
        final int readId = DefinedPacket.readVarInt(packet);
        if (readId == oldId || readId == newId) {
            ByteBuf data = packet.copy();
            packet.readerIndex(offset);
            packet.writerIndex(offset);
            DefinedPacket.writeVarInt(readId == oldId ? newId : oldId, packet);
            packet.writeBytes(data);
            data.release();
        }
    }

    protected static void rewriteMetaVarInt(ByteBuf packet, int oldId, int newId, int metaIndex) {
        rewriteMetaVarInt(packet, oldId, newId, metaIndex, -1);
    }

    protected static void rewriteMetaVarInt(ByteBuf packet, int oldId, int newId, int metaIndex, int protocolVersion) {
        if (oldId == newId) return;
        final int readerIndex = packet.readerIndex();
        short index;
        while ((index = packet.readUnsignedByte()) != 0xFF) {
            int type = DefinedPacket.readVarInt(packet);
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                switch (type) {
                    case 5:
                        if (packet.readBoolean()) {
                            DefinedPacket.skipVarInt(packet);
                        }
                        continue;
                    case 15:
                        int particleId = DefinedPacket.readVarInt(packet);
                        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_14) {
                            switch (particleId) {
                                case 3:
                                case 23:
                                    DefinedPacket.skipVarInt(packet);
                                    break;
                                case 14:
                                    packet.skipBytes(16);
                                    break;
                                case 32:
                                    readSkipSlot(packet, protocolVersion);
                                    break;
                            }
                        } else {
                            switch (particleId) {
                                case 3:
                                case 20:
                                    DefinedPacket.skipVarInt(packet);
                                    break;
                                case 11:
                                    packet.skipBytes(16);
                                    break;
                                case 27:
                                    readSkipSlot(packet, protocolVersion);
                                    break;
                            }
                        }
                        continue;
                    default:
                        if (type >= 6) {
                            type--;
                        }
                        break;
                }
            }
            switch (type) {
                case 0:
                    packet.skipBytes(1);
                    break;
                case 1:
                    if (index == metaIndex) {
                        final int position = packet.readerIndex();
                        rewriteVarInt(packet, oldId, newId, position);
                        packet.readerIndex(position);
                    }
                    DefinedPacket.skipVarInt(packet);
                    break;
                case 2:
                    packet.skipBytes(4);
                    break;
                case 3:
                case 4:
                    DefinedPacket.skipString(packet);
                    break;
                case 5:
                    readSkipSlot(packet, protocolVersion);
                    break;
                case 6:
                    packet.skipBytes(1);
                    break;
                case 7:
                    packet.skipBytes(12);
                    break;
                case 8:
                    packet.readLong();
                    break;
                case 9:
                    if (packet.readBoolean()) {
                        packet.skipBytes(8);
                    }
                    break;
                case 10:
                    DefinedPacket.skipVarInt(packet);
                    break;
                case 11:
                    if (packet.readBoolean()) {
                        packet.skipBytes(16);
                    }
                    break;
                case 12:
                    DefinedPacket.skipVarInt(packet);
                    break;
                case 13:
                    final Tag tag = NamedTag.read(new DataInputStream(new ByteBufInputStream(packet)));
                    if (tag.isError()) {
                        throw new RuntimeException(tag.error());
                    }
                    break;
                case 15:
                    DefinedPacket.skipVarInt(packet);
                    DefinedPacket.skipVarInt(packet);
                    DefinedPacket.skipVarInt(packet);
                    break;
                case 16:
                    if (index == metaIndex) {
                        final int position = packet.readerIndex();
                        rewriteVarInt(packet, oldId + 1, newId + 1, position);
                        packet.readerIndex(position);
                    }
                    DefinedPacket.skipVarInt(packet);
                    break;
                case 17:
                    DefinedPacket.skipVarInt(packet);
                    break;
                default:
                    if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
                        type++;
                    }
                    throw new IllegalArgumentException("Unknown meta type " + type + ": Using mods? refer to disable_entity_metadata_rewrite in waterfall.yml");
            }
        }
        packet.readerIndex(readerIndex);
    }

    private static void readSkipSlot(ByteBuf packet, int protocolVersion) {
        if ((protocolVersion >= ProtocolConstants.MINECRAFT_1_13_2) ? packet.readBoolean() : packet.readShort() != -1) {
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13_2) {
                DefinedPacket.readVarInt(packet);
            }
            packet.skipBytes((protocolVersion >= ProtocolConstants.MINECRAFT_1_13) ? 1 : 3);
            final int position = packet.readerIndex();
            if (packet.readByte() != 0) {
                packet.readerIndex(position);
                final Tag tag = NamedTag.read(new DataInputStream(new ByteBufInputStream(packet)));
                if (tag.isError()) {
                    throw new RuntimeException(tag.error());
                }
            }
        }
    }

    private static void rewrite(ByteBuf packet, int oldId, int newId, boolean[] ints, boolean[] varints) {
        if (oldId == newId) return;
        final int readerIndex = packet.readerIndex();
        final int packetId = DefinedPacket.readVarInt(packet);
        final int readerIndexAfterPID = packet.readerIndex();
        if (packetId < 0 || packetId > ints.length || packetId > varints.length) {
            packet.readerIndex(readerIndex);
            return;
        }
        if (ints[packetId]) {
            rewriteInt(packet, oldId, newId, readerIndexAfterPID);
        } else if (varints[packetId]) {
            rewriteVarInt(packet, oldId, newId, readerIndexAfterPID);
        }
        packet.readerIndex(readerIndex);
    }

    protected void addRewrite(int id, ProtocolConstants.Direction direction, boolean varint) {
        if (direction == ProtocolConstants.Direction.TO_CLIENT) {
            if (varint) {
                clientboundVarInts[id] = true;
            } else {
                clientboundInts[id] = true;
            }
        } else if (varint) {
            serverboundVarInts[id] = true;
        } else {
            serverboundInts[id] = true;
        }
    }

    public void rewriteServerbound(ByteBuf packet, int oldId, int newId) {
        rewrite(packet, oldId, newId, serverboundInts, serverboundVarInts);
    }

    public void rewriteServerbound(ByteBuf packet, int oldId, int newId, int protocolVersion) {
        rewriteServerbound(packet, oldId, newId);
    }

    public void rewriteClientbound(ByteBuf packet, int oldId, int newId) {
        rewrite(packet, oldId, newId, clientboundInts, clientboundVarInts);
    }

    public void rewriteClientbound(ByteBuf packet, int oldId, int newId, int protocolVersion) {
        rewriteClientbound(packet, oldId, newId);
    }
}
