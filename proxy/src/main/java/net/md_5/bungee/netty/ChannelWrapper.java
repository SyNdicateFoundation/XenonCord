package net.md_5.bungee.netty;

import com.google.common.base.Preconditions;
import io.netty.channel.*;
import ir.xenoncommunity.XenonCore;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.compress.PacketCompressor;
import net.md_5.bungee.compress.PacketDecompressor;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.MinecraftDecoder;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.Kick;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class ChannelWrapper {

    private final Channel ch;
    @Getter
    @Setter
    private SocketAddress remoteAddress;
    @Getter
    private volatile boolean closed;
    @Getter
    private volatile boolean closing;

    public ChannelWrapper(ChannelHandlerContext ctx) {
        this.ch = ctx.channel();
        this.remoteAddress = (this.ch.remoteAddress() == null) ? this.ch.parent().localAddress() : this.ch.remoteAddress();
    }

    public Protocol getDecodeProtocol() {
        return ch.pipeline().get(MinecraftDecoder.class).getProtocol();
    }

    public void setDecodeProtocol(Protocol protocol) {
        ch.pipeline().get(MinecraftDecoder.class).setProtocol(protocol);
    }

    public Protocol getEncodeProtocol() {
        return ch.pipeline().get(MinecraftEncoder.class).getProtocol();
    }

    public void setEncodeProtocol(Protocol protocol) {
        ch.pipeline().get(MinecraftEncoder.class).setProtocol(protocol);
    }

    public void setProtocol(Protocol protocol) {
        setDecodeProtocol(protocol);
        setEncodeProtocol(protocol);
    }

    public void setVersion(int protocol) {
        ch.pipeline().get(MinecraftDecoder.class).setProtocolVersion(protocol);
        ch.pipeline().get(MinecraftEncoder.class).setProtocolVersion(protocol);
    }

    public int getEncodeVersion() {
        return ch.pipeline().get(MinecraftEncoder.class).getProtocolVersion();
    }

    public void write(Object packet) {
        if (!closed) {
            DefinedPacket defined = null;
            if (packet instanceof PacketWrapper) {
                PacketWrapper wrapper = (PacketWrapper) packet;
                wrapper.setReleased(true);
                XenonCore.instance.getTaskManager().async(() -> ch.writeAndFlush(wrapper.buf, ch.voidPromise()));
                defined = wrapper.packet;
            } else {
                XenonCore.instance.getTaskManager().async(() -> ch.writeAndFlush(packet, ch.voidPromise()));
                if (packet instanceof DefinedPacket)
                    defined = (DefinedPacket) packet;
            }

            if (defined != null) {
                Protocol nextProtocol = defined.nextProtocol();
                if (nextProtocol != null)
                    setEncodeProtocol(nextProtocol);
            }
        }
    }

    public void markClosed() {
        closed = closing = true;
    }

    public void close() {
        close(null);
    }

    public void close(Object packet) {
        if (!closed) {
            closed = closing = true;
            if (packet != null && ch.isActive())
                ch.writeAndFlush(packet).addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, ChannelFutureListener.CLOSE);
            else {
                ch.flush();
                ch.close();
            }
        }
    }

    public void delayedClose(final Kick kick) {
        if (!closing) {
            closing = true;

            ch.eventLoop().schedule(() -> close(kick), 250, TimeUnit.MILLISECONDS);
        }
    }

    public void addBefore(String baseName, String name, ChannelHandler handler) {
        Preconditions.checkState(ch.eventLoop().inEventLoop(), "cannot add handler outside of event loop");
        XenonCore.instance.getTaskManager().async(() -> {
            ch.pipeline().flush();
            ch.pipeline().addBefore(baseName, name, handler);
        });
    }

    public Channel getHandle() {
        return ch;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        if (compressionThreshold >= 0) {
            if (ch.pipeline().get(PacketCompressor.class) == null)
                addBefore(PipelineUtils.PACKET_ENCODER, "compress", new PacketCompressor());
            ch.pipeline().get(PacketCompressor.class).setThreshold(compressionThreshold);
        } else
            ch.pipeline().remove("compress");

        if (ch.pipeline().get(PacketDecompressor.class) == null && compressionThreshold >= 0)
            addBefore(PipelineUtils.PACKET_DECODER, "decompress", new PacketDecompressor(compressionThreshold));

        if (compressionThreshold < 0)
            ch.pipeline().remove("decompress");
    }
}
