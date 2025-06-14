package net.md_5.bungee.netty;

import com.google.common.base.Preconditions;
import io.github.waterfallmc.waterfall.event.ConnectionInitEvent;
import io.netty.channel.*;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.incubator.channel.uring.*;
import io.netty.util.AttributeKey;
import io.netty.util.internal.PlatformDependent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.ClientConnectEvent;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.protocol.*;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PipelineUtils {
    public static final AttributeKey<ListenerInfo> LISTENER = AttributeKey.newInstance("ListerInfo");
    public static final Base BASE = new Base(false);
    public static final Base BASE_SERVERSIDE = new Base(true);
    public static final String TIMEOUT_HANDLER = "timeout";
    public static final String WRITE_TIMEOUT_HANDLER = "write-timeout";
    public static final String PACKET_DECODER = "packet-decoder";
    public static final String PACKET_ENCODER = "packet-encoder";
    public static final String BOSS_HANDLER = "inbound-boss";
    public static final String ENCRYPT_HANDLER = "encrypt";
    public static final String DECRYPT_HANDLER = "decrypt";
    public static final String FRAME_DECODER = "frame-decoder";
    public static final String FRAME_PREPENDER = "frame-prepender";
    public static final String LEGACY_KICKER = "legacy-kick";
    public static final String FLUSH_CONSOLIDATION = "flush-consolidation";
    public static final String FLUSH_SIGNALING = "flush-signaling";
    private static final KickStringWriter legacyKicker = new KickStringWriter();
    private static final Varint21LengthFieldExtraBufPrepender serverFramePrepender = new Varint21LengthFieldExtraBufPrepender();
    private static final ChannelFactory<? extends ServerChannel> serverChannelFactory;
    private static final ChannelFactory<? extends ServerChannel> serverChannelDomainFactory;
    private static final ChannelFactory<? extends Channel> channelFactory;
    private static final ChannelFactory<? extends Channel> channelDomainFactory;
    private static final int LOW_MARK = Integer.getInteger("net.md_5.bungee.low_mark", 2 << 18);
    private static final int HIGH_MARK = Integer.getInteger("net.md_5.bungee.high_mark", 2 << 20);
    private static final WriteBufferWaterMark MARK = new WriteBufferWaterMark(LOW_MARK, HIGH_MARK);
    public static final ChannelInitializer<Channel> SERVER_CHILD = new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            SocketAddress remoteAddress = (ch.remoteAddress() == null) ? ch.parent().localAddress() : ch.remoteAddress();
            if (BungeeCord.getInstance().getConnectionThrottle() != null && BungeeCord.getInstance().getConnectionThrottle().throttle(remoteAddress)) {
                ch.close();
                return;
            }
            ListenerInfo listener = ch.attr(LISTENER).get();
            if (BungeeCord.getInstance().getPluginManager().callEvent(new ClientConnectEvent(remoteAddress, listener)).isCancelled()) {
                ch.close();
                return;
            }
            ConnectionInitEvent connectionInitEvent = new ConnectionInitEvent(ch.remoteAddress(), listener, (result, throwable) -> {
                if (result.isCancelled()) {
                    ch.close();
                    return;
                }
                try {
                    BASE.initChannel(ch);
                } catch (Exception e) {
                    e.printStackTrace();
                    ch.close();
                    return;
                }
                //ch.pipeline().addBefore(FRAME_DECODER, LEGACY_DECODER, new LegacyDecoder());
                ch.pipeline().addAfter(FRAME_DECODER, PACKET_DECODER, new MinecraftDecoder(Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion()));
                ch.pipeline().addAfter(FRAME_PREPENDER, PACKET_ENCODER, new MinecraftEncoder(Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion()));
                ch.pipeline().addBefore(FRAME_PREPENDER, LEGACY_KICKER, legacyKicker);
                ch.pipeline().get(HandlerBoss.class).setHandler(new InitialHandler(BungeeCord.getInstance(), listener));
                if (listener.isProxyProtocol()) {
                    ch.pipeline().addFirst(new HAProxyMessageDecoder());
                }
            });
            BungeeCord.getInstance().getPluginManager().callEvent(connectionInitEvent);
        }
    };
    private static boolean epoll;
    private static boolean io_uring;

    static {
        if (!PlatformDependent.isWindows()) {
            if (Boolean.parseBoolean(System.getProperty("bungee.io_uring", "false"))) {
                ProxyServer.getInstance().getLogger().info("Not on Windows, attempting to use enhanced IOUringEventLoopGroup");
                if (io_uring = IOUring.isAvailable()) {
                    ProxyServer.getInstance().getLogger().log(Level.WARNING, "io_uring is enabled and working, utilising it! (experimental feature)");
                } else {
                    ProxyServer.getInstance().getLogger().log(Level.WARNING, "io_uring is not working: {0}", Util.exception(IOUring.unavailabilityCause()));
                }
            }

            if (!io_uring && Boolean.parseBoolean(System.getProperty("bungee.epoll", "true"))) {
                ProxyServer.getInstance().getLogger().info("Not on Windows, attempting to use enhanced EpollEventLoop");
                if (epoll = Epoll.isAvailable()) {
                    ProxyServer.getInstance().getLogger().info("Epoll is working, utilising it!");
                } else {
                    ProxyServer.getInstance().getLogger().log(Level.WARNING, "Epoll is not working, falling back to NIO: {0}", Util.exception(Epoll.unavailabilityCause()));
                }
            }
        }
        serverChannelFactory = io_uring ? IOUringServerSocketChannel::new : epoll ? EpollServerSocketChannel::new : NioServerSocketChannel::new;
        serverChannelDomainFactory = io_uring ? IOUringServerSocketChannel::new : epoll ? EpollServerDomainSocketChannel::new : null;
        channelFactory = io_uring ? IOUringSocketChannel::new : epoll ? EpollSocketChannel::new : NioSocketChannel::new;
        channelDomainFactory = io_uring ? IOUringSocketChannel::new : epoll ? EpollDomainSocketChannel::new : null;
    }

    public static EventLoopGroup newEventLoopGroup(int threads, ThreadFactory factory) {
        return io_uring ? new IOUringEventLoopGroup(threads, factory) : epoll ? new EpollEventLoopGroup(threads, factory) : new NioEventLoopGroup(threads, factory);
    }

    public static Class<? extends ServerChannel> getServerChannel(SocketAddress address) {
        if (address instanceof DomainSocketAddress) {
            Preconditions.checkState(epoll, "Epoll required to have UNIX sockets");
            return EpollServerDomainSocketChannel.class;
        }
        return io_uring ? IOUringServerSocketChannel.class : epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    public static Class<? extends Channel> getChannel(SocketAddress address) {
        if (address instanceof DomainSocketAddress) {
            Preconditions.checkState(epoll, "Epoll required to have UNIX sockets");
            return EpollDomainSocketChannel.class;
        }
        return io_uring ? IOUringSocketChannel.class : epoll ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    public static ChannelFactory<? extends ServerChannel> getServerChannelFactory(SocketAddress address) {
        if (address instanceof DomainSocketAddress) {
            ChannelFactory<? extends ServerChannel> factory = serverChannelDomainFactory;
            Preconditions.checkState(factory != null, "Epoll required to have UNIX sockets");
            return factory;
        }
        return serverChannelFactory;
    }

    public static ChannelFactory<? extends Channel> getChannelFactory(SocketAddress address) {
        if (address instanceof DomainSocketAddress) {
            ChannelFactory<? extends Channel> factory = channelDomainFactory;
            Preconditions.checkState(factory != null, "Epoll required to have UNIX sockets");
            return factory;
        }
        return channelFactory;
    }

    public static Class<? extends DatagramChannel> getDatagramChannel() {
        return io_uring ? IOUringDatagramChannel.class : epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Base extends ChannelInitializer<Channel> {

        private boolean toServer = false;

        @Override
        public void initChannel(Channel ch) throws Exception {
            try {
                ch.config().setOption(ChannelOption.IP_TOS, 0x18);
            } catch (ChannelException ex) {
            }
            ch.config().setOption(ChannelOption.TCP_NODELAY, true);
            ch.config().setWriteBufferWaterMark(MARK);

            ch.pipeline().addLast(FRAME_DECODER, new Varint21FrameDecoder());
            ch.pipeline().addLast(TIMEOUT_HANDLER, new ReadTimeoutHandler(BungeeCord.getInstance().config.getTimeout(), TimeUnit.MILLISECONDS));
            ch.pipeline().addLast( WRITE_TIMEOUT_HANDLER, new WriteTimeoutHandler( BungeeCord.getInstance().config.getTimeout(), TimeUnit.MILLISECONDS ) );
            ch.pipeline().addLast(FRAME_PREPENDER, (toServer) ? serverFramePrepender : new Varint21LengthFieldPrepender());
            ch.pipeline().addLast(BOSS_HANDLER, new HandlerBoss());
        }
    }
}
