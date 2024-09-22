package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Getter
    @Setter
    private Protocol protocol;
    private final boolean server;
    @Setter
    private int protocolVersion;
    @Setter
    private boolean supportsForge = false;
    @Setter
    private boolean slice = true;

    public MinecraftDecoder(Protocol protocol, boolean server, int protocolVersion) {
        this.protocol = protocol;
        this.server = server;
        this.protocolVersion = protocolVersion;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!ctx.channel().isActive())
            return;

        Protocol.DirectionData prot = server ? protocol.TO_SERVER : protocol.TO_CLIENT;
        ByteBuf slice = ( this.slice ? in.retainedSlice() : in.copy() );
        Object packetTypeInfo = null;

        try {
            if (in.readableBytes() == 0 && !server)
                return;

            final int packetId = DefinedPacket.readVarInt(in);
            packetTypeInfo = packetId;

            final DefinedPacket packet = prot.createPacket(packetId, protocolVersion, supportsForge);
            if (packet != null) {
                packetTypeInfo = packet.getClass();
                doLengthSanityChecks(in, packet, prot.getDirection(), packetId);
                packet.read(in, protocol, prot.getDirection(), protocolVersion);

                if (in.isReadable()) {
                    if (!DEBUG)
                        throw PACKET_NOT_READ_TO_END;
                    throw new BadPacketException("Packet " + protocol + ":" + prot.getDirection() + "/" + packetId + " (" + packet.getClass().getSimpleName() + ") larger than expected, extra bytes: " + in.readableBytes());
                }
            } else
                in.skipBytes(in.readableBytes());

            out.add(new PacketWrapper(packet, slice, protocol));
            slice = null;
        } catch (Exception e) {
            if (!DEBUG)
                throw e;
            final String packetTypeStr = packetTypeInfo instanceof Integer ? "id " + Integer.toHexString((Integer) packetTypeInfo)
                    : packetTypeInfo != null ? "class " + ((Class<?>) packetTypeInfo).getSimpleName()
                    : "unknown";
            throw new FastDecoderException("Error decoding packet " + packetTypeStr + " with contents:\n" + ByteBufUtil.prettyHexDump(slice), e);
        } finally {
            if (slice != null)
                slice.release();
        }
    }

    public static final boolean DEBUG = Boolean.getBoolean("waterfall.packet-decode-logging");

    private static final CorruptedFrameException PACKET_LENGTH_OVERSIZED = new CorruptedFrameException("A packet could not be decoded because it was too large.");
    private static final CorruptedFrameException PACKET_LENGTH_UNDERSIZED = new CorruptedFrameException("A packet could not be decoded because it was smaller than allowed.");
    private static final BadPacketException PACKET_NOT_READ_TO_END = new BadPacketException("Couldn't read all bytes from a packet.");

    private void doLengthSanityChecks(ByteBuf buf, DefinedPacket packet, ProtocolConstants.Direction direction, int packetId) throws Exception {
        int expectedMinLen = packet.expectedMinLength(buf, direction, protocolVersion);
        int expectedMaxLen = packet.expectedMaxLength(buf, direction, protocolVersion);
        if (expectedMaxLen != -1 && buf.readableBytes() > expectedMaxLen)
            throw handleOverflow(packet, expectedMaxLen, buf.readableBytes(), packetId);
        if (buf.readableBytes() < expectedMinLen)
            throw handleUnderflow(packet, expectedMaxLen, buf.readableBytes(), packetId);
    }

    private Exception handleOverflow(DefinedPacket packet, int expected, int actual, int packetId) {
        return DEBUG ? new CorruptedFrameException("Packet " + packet.getClass() + " " + packetId + " Protocol " + protocolVersion + " was too big (expected " + expected + " bytes, got " + actual + " bytes)")
                : PACKET_LENGTH_OVERSIZED;
    }

    private Exception handleUnderflow(DefinedPacket packet, int expected, int actual, int packetId) {
        return DEBUG ? new CorruptedFrameException("Packet " + packet.getClass() + " " + packetId + " Protocol " + protocolVersion + " was too small (expected " + expected + " bytes, got " + actual + " bytes)")
                : PACKET_LENGTH_UNDERSIZED;
    }
}
