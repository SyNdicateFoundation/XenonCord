package net.md_5.bungee.netty;

import com.google.common.base.Preconditions;
import io.github.waterfallmc.waterfall.event.ConnectionInitEvent;
import io.netty.buffer.PooledByteBufAllocator;
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
import java.util.logging.Logger;

public class PipelineUtils
{

    public static final AttributeKey<ListenerInfo> LISTENER = AttributeKey.newInstance( "ListerInfo" );
    public static final ChannelInitializer<Channel> SERVER_CHILD = new ChannelInitializer<Channel>()
    {
        @Override
        protected void initChannel(Channel ch) {
            SocketAddress remoteAddress = ( ch.remoteAddress() == null ) ? ch.parent().localAddress() : ch.remoteAddress();
            ListenerInfo listener = ch.attr( LISTENER ).get();
            ConnectionInitEvent connectionInitEvent = new ConnectionInitEvent(ch.remoteAddress(), listener, (result, throwable) -> { // Waterfall

           if (
           (BungeeCord.getInstance().getConnectionThrottle() != null && BungeeCord.getInstance().getConnectionThrottle().throttle( remoteAddress ))
           || ( BungeeCord.getInstance().getPluginManager().callEvent( new ClientConnectEvent( remoteAddress, listener ) ).isCancelled() )
           || (result.isCancelled()))
            {
                ch.close();
                return;
            }

            try {
                BASE.initChannel( ch );
            } catch (Exception e) {
                e.printStackTrace();
                ch.close();
                return;
            }
            ch.pipeline().addBefore( FRAME_DECODER, LEGACY_DECODER, new LegacyDecoder() );
            ch.pipeline().addAfter( FRAME_DECODER, PACKET_DECODER, new MinecraftDecoder( Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion() ) );
            ch.pipeline().addAfter( FRAME_PREPENDER, PACKET_ENCODER, new MinecraftEncoder( Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion() ) );
            ch.pipeline().addBefore( FRAME_PREPENDER, LEGACY_KICKER, legacyKicker );
            ch.pipeline().get( HandlerBoss.class ).setHandler( new InitialHandler( BungeeCord.getInstance(), listener ) );

            if ( listener.isProxyProtocol() )
                ch.pipeline().addFirst( new HAProxyMessageDecoder() );
            });

            BungeeCord.getInstance().getPluginManager().callEvent(connectionInitEvent);
        }
    };
    public static final Base BASE = new Base( false );
    public static final Base BASE_SERVERSIDE = new Base( true );
    private static final KickStringWriter legacyKicker = new KickStringWriter();
    private static final Varint21LengthFieldPrepender framePrepender = new Varint21LengthFieldPrepender();
    private static final Varint21LengthFieldExtraBufPrepender serverFramePrepender = new Varint21LengthFieldExtraBufPrepender();
    public static final String TIMEOUT_HANDLER = "timeout";
    public static final String PACKET_DECODER = "packet-decoder";
    public static final String PACKET_ENCODER = "packet-encoder";
    public static final String BOSS_HANDLER = "inbound-boss";
    public static final String ENCRYPT_HANDLER = "encrypt";
    public static final String DECRYPT_HANDLER = "decrypt";
    public static final String FRAME_DECODER = "frame-decoder";
    public static final String FRAME_PREPENDER = "frame-prepender";
    public static final String LEGACY_DECODER = "legacy-decoder";
    public static final String LEGACY_KICKER = "legacy-kick";

    private static boolean epoll;
    private static boolean io_uring;
    // Waterfall start: netty reflection -> factory
    private static final ChannelFactory<? extends ServerChannel> serverChannelFactory;
    private static final ChannelFactory<? extends ServerChannel> serverChannelDomainFactory;
    private static final ChannelFactory<? extends Channel> channelFactory;
    private static final ChannelFactory<? extends Channel> channelDomainFactory;
    // Waterfall end

    static {
        final Logger logger = ProxyServer.getInstance().getLogger();

        if (!PlatformDependent.isWindows()) {

            if (Boolean.parseBoolean(System.getProperty("bungee.io_uring", "false"))) {
                logger.info("Not on Windows, attempting to use enhanced IOUringEventLoopGroup");
                io_uring = IOUring.isAvailable();
                logger.log(io_uring ? Level.WARNING : Level.INFO, String.format("io_uring is %s", io_uring ? "enabled and working, utilising it! (experimental feature)." : "not working: {0}"), Util.exception(IOUring.unavailabilityCause()));
            }
            if (!io_uring && Boolean.parseBoolean(System.getProperty("bungee.epoll", "true"))) {
                logger.info("Not on Windows, attempting to use enhanced EpollEventLoop");
                epoll = Epoll.isAvailable();
                logger.log(epoll ? Level.INFO : Level.WARNING, String.format("Epoll is %s", epoll ? "working, utilising it!" : "not working, falling back to NIO: {0}"), Util.exception(Epoll.unavailabilityCause()));
            }
        }
        serverChannelFactory = epoll ? EpollServerSocketChannel::new : NioServerSocketChannel::new;
        serverChannelDomainFactory = epoll ? EpollServerDomainSocketChannel::new : null;
        channelFactory = epoll ? EpollSocketChannel::new : NioSocketChannel::new;
        channelDomainFactory = epoll ? EpollDomainSocketChannel::new : null;
    }


    public static EventLoopGroup newEventLoopGroup(int threads, ThreadFactory factory)
    {
        return io_uring ? new IOUringEventLoopGroup( threads, factory ) : epoll ? new EpollEventLoopGroup( threads, factory ) : new NioEventLoopGroup( threads, factory );
    }

    public static Class<? extends ServerChannel> getServerChannel(SocketAddress address)
    {
        if ( address instanceof DomainSocketAddress )
        {
            Preconditions.checkState( epoll, "Epoll required to have UNIX sockets" );

            return EpollServerDomainSocketChannel.class;
        }

        return io_uring ? IOUringServerSocketChannel.class : epoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    public static Class<? extends Channel> getChannel(SocketAddress address)
    {
        if ( address instanceof DomainSocketAddress )
        {
            Preconditions.checkState( epoll, "Epoll required to have UNIX sockets" );

            return EpollDomainSocketChannel.class;
        }

        return io_uring ? IOUringSocketChannel.class : epoll ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    // Waterfall start: netty reflection -> factory
    public static ChannelFactory<? extends ServerChannel> getServerChannelFactory(SocketAddress address)
    {
        if ( address instanceof DomainSocketAddress )
        {
            ChannelFactory<? extends ServerChannel> factory = PipelineUtils.serverChannelDomainFactory;
            Preconditions.checkState( factory != null, "Epoll required to have UNIX sockets" );

            return factory;
        }

        return serverChannelFactory;
    }

    public static ChannelFactory<? extends Channel> getChannelFactory(SocketAddress address)
    {
        if ( address instanceof DomainSocketAddress )
        {
            ChannelFactory<? extends Channel> factory = PipelineUtils.channelDomainFactory;
            Preconditions.checkState( factory != null, "Epoll required to have UNIX sockets" );

            return factory;
        }

        return channelFactory;
    }
    // Waterfall end

    public static Class<? extends DatagramChannel> getDatagramChannel()
    {
        return io_uring ? IOUringDatagramChannel.class : epoll ? EpollDatagramChannel.class : NioDatagramChannel.class;
    }

    private static final int LOW_MARK = Integer.getInteger( "net.md_5.bungee.low_mark", 2 << 18 ); // 0.5 mb
    private static final int HIGH_MARK = Integer.getInteger( "net.md_5.bungee.high_mark", 2 << 20 ); // 2 mb
    private static final WriteBufferWaterMark MARK = new WriteBufferWaterMark( LOW_MARK, HIGH_MARK );

    @NoArgsConstructor // for backwards compatibility
    @AllArgsConstructor
    public static final class Base extends ChannelInitializer<Channel>
    {

        private boolean toServer = false;

        @Override
        public void initChannel(Channel ch) throws Exception
        {
            try
            {
                ch.config().setOption( ChannelOption.IP_TOS, 0x18 );
            } catch ( ChannelException ex )
            {
                // IP_TOS is not supported (Windows XP / Windows Server 2003)
            }
            ch.config().setOption( ChannelOption.TCP_NODELAY, true );
            ch.config().setAllocator( PooledByteBufAllocator.DEFAULT );
            ch.config().setWriteBufferWaterMark( MARK );

            ch.pipeline().addLast( FRAME_DECODER, new Varint21FrameDecoder() );
            ch.pipeline().addLast( TIMEOUT_HANDLER, new ReadTimeoutHandler( BungeeCord.getInstance().config.getTimeout(), TimeUnit.MILLISECONDS ) );
            // No encryption bungee -> server, therefore use extra buffer to avoid copying everything for length prepending
            // Not used bungee -> client as header would need to be encrypted separately through expensive JNI call
            ch.pipeline().addLast( FRAME_PREPENDER, ( toServer ) ? serverFramePrepender : framePrepender );

            ch.pipeline().addLast( BOSS_HANDLER, new HandlerBoss() );
        }
    }
}
