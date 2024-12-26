package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf>
{

    @Getter
    @Setter
    private Protocol protocol;
    private final boolean server;
    @Setter
    private int protocolVersion;
    @Setter
    private boolean supportsForge = false;

    public MinecraftDecoder(Protocol protocol, boolean server, int protocolVersion) {
        this.protocol = protocol;
        this.server = server;
        this.protocolVersion = protocolVersion;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        // See Varint21FrameDecoder for the general reasoning. We add this here as ByteToMessageDecoder#handlerRemoved()
        // will fire any cumulated data through the pipeline, so we want to try and stop it here.
        if ( !ctx.channel().isActive() )
        {
            return;
        }

        Protocol.DirectionData prot = ( server ) ? protocol.TO_SERVER : protocol.TO_CLIENT;
        ByteBuf slice = in.copy(); // Can't slice this one due to EntityMap :(

        Object packetTypeInfo = null;
        try
        {
            // Waterfall start
            if (in.readableBytes() == 0 && !server) {
                return;
            }
            // Waterfall end

            int packetId = DefinedPacket.readVarInt( in );
            packetTypeInfo = packetId;

            DefinedPacket packet = prot.createPacket( packetId, protocolVersion, supportsForge );
            if ( packet != null )
            {
                packetTypeInfo = packet.getClass();
                doLengthSanityChecks(in, packet, prot.getDirection(), packetId); // Waterfall: Additional DoS mitigations
                packet.read( in, protocol, prot.getDirection(), protocolVersion );

                if ( in.isReadable() )
                {
                    // Waterfall start: Additional DoS mitigations
                    if(!DEBUG) {
                        throw PACKET_NOT_READ_TO_END;
                    }
                    // Waterfall end
                    throw new BadPacketException( "Packet " + protocol + ":" + prot.getDirection() + "/" + packetId + " (" + packet.getClass().getSimpleName() + ") larger than expected, extra bytes: " + in.readableBytes() );
                }
            } else
            {
                in.skipBytes( in.readableBytes() );
            }

            out.add( new PacketWrapper( packet, slice, protocol ) );
            slice = null;
        } catch (BadPacketException | IndexOutOfBoundsException e) {
            // Waterfall start: Additional DoS mitigations
            if(!DEBUG) {
                throw e;
            }
            // Waterfall end
            final String packetTypeStr;
            if (packetTypeInfo instanceof Integer) {
                packetTypeStr = "id " + Integer.toHexString((Integer) packetTypeInfo);
            } else if (packetTypeInfo instanceof Class) {
                packetTypeStr = "class " + ((Class) packetTypeInfo).getSimpleName();
            } else {
                packetTypeStr = "unknown";
            }
            throw new FastDecoderException("Error decoding packet " + packetTypeStr + " with contents:\n" + ByteBufUtil.prettyHexDump(slice), e); // Waterfall
            // Waterfall start
        } catch (Exception e) {
            if (!DEBUG) {
                throw e;
            }
            final String packetTypeStr;
            if (packetTypeInfo instanceof Integer) {
                packetTypeStr = "id " + Integer.toHexString((Integer) packetTypeInfo);
            } else if (packetTypeInfo instanceof Class) {
                packetTypeStr = "class " + ((Class) packetTypeInfo).getSimpleName();
            } else {
                packetTypeStr = "unknown";
            }
            throw new FastDecoderException("Error decoding packet " + packetTypeStr + " with contents:\n" + ByteBufUtil.prettyHexDump(slice), e); // Waterfall
            // Waterfall end
        } finally
        {
            if ( slice != null )
            {
                slice.release();
            }
        }
    }

    // Waterfall start: Additional DoS mitigations, courtesy of Velocity
    public static final boolean DEBUG = Boolean.getBoolean("waterfall.packet-decode-logging");

    // Cached Exceptions:
    private static final CorruptedFrameException PACKET_LENGTH_OVERSIZED =
            new CorruptedFrameException("A packet could not be decoded because it was too large. For more "
                    + "information, launch Waterfall with -Dwaterfall.packet-decode-logging=true");
    private static final CorruptedFrameException PACKET_LENGTH_UNDERSIZED =
            new CorruptedFrameException("A packet could not be decoded because it was smaller than allowed. For more "
                    + "information, launch Waterfall with -Dwaterfall.packet-decode-logging=true");
    private static final BadPacketException PACKET_NOT_READ_TO_END =
            new BadPacketException("Couldn't read all bytes from a packet. For more "
                    + "information, launch Waterfall with -Dwaterfall.packet-decode-logging=true");


    private void doLengthSanityChecks(ByteBuf buf, DefinedPacket packet,
                                      ProtocolConstants.Direction direction, int packetId) throws Exception {
        int expectedMinLen = packet.expectedMinLength(buf, direction, protocolVersion);
        int expectedMaxLen = packet.expectedMaxLength(buf, direction, protocolVersion);
        if (expectedMaxLen != -1 && buf.readableBytes() > expectedMaxLen) {
            throw handleOverflow(packet, expectedMaxLen, buf.readableBytes(), packetId);
        }
        if (buf.readableBytes() < expectedMinLen) {
            throw handleUnderflow(packet, expectedMaxLen, buf.readableBytes(), packetId);
        }
    }

    private Exception handleOverflow(DefinedPacket packet, int expected, int actual, int packetId) {
        if (DEBUG) {
            throw new CorruptedFrameException( "Packet " + packet.getClass() + " " + packetId
                    + " Protocol " + protocolVersion + " was too big (expected "
                    + expected + " bytes, got " + actual + " bytes)");
        } else {
            return PACKET_LENGTH_OVERSIZED;
        }
    }

    private Exception handleUnderflow(DefinedPacket packet, int expected, int actual, int packetId) {
        if (DEBUG) {
            throw new CorruptedFrameException( "Packet " + packet.getClass() + " " + packetId
                    + " Protocol " + protocolVersion + " was too small (expected "
                    + expected + " bytes, got " + actual + " bytes)");
        } else {
            return PACKET_LENGTH_UNDERSIZED;
        }
    }
    // Waterfall end
}
