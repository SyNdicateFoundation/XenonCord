package net.md_5.bungee;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
import ir.xenoncommunity.XenonCore;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.score.Scoreboard;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.entitymap.EntityMap;
import net.md_5.bungee.forge.ForgeClientHandler;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.forge.ForgeServerHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.*;
import net.md_5.bungee.protocol.packet.*;
import net.md_5.bungee.tab.ServerUnique;
import net.md_5.bungee.tab.TabList;
import net.md_5.bungee.util.CaseInsensitiveSet;
import net.md_5.bungee.util.ChatComponentTransformer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class UserConnection implements ProxiedPlayer
{

    /*========================================================================*/
    @NonNull
    private final ProxyServer bungee;
    @Getter
    @NonNull
    private final ChannelWrapper ch;
    @Getter
    @NonNull
    private final String name;
    @Getter
    private final InitialHandler pendingConnection;
    /*========================================================================*/
    @Getter
    @Setter
    private ServerConnection server;
    @Getter
    @Setter
    private Object dimension;
    @Getter
    @Setter
    private boolean dimensionChange = true;
    @Getter
    private final Collection<ServerInfo> pendingConnects = new HashSet<>();
    /*========================================================================*/
    @Getter
    @Setter
    private int ping = 100;
    @Getter
    @Setter
    private ServerInfo reconnectServer;
    @Getter
    private TabList tabListHandler;
    @Getter
    @Setter
    private int gamemode;
    @Getter
    private int compressionThreshold = -1;
    // Used for trying multiple servers in order
    @Setter
    private Queue<String> serverJoinQueue;
    /*========================================================================*/
    private final Collection<String> groups = new CaseInsensitiveSet();
    private final Collection<String> permissions = new CaseInsensitiveSet();
    /*========================================================================*/
    @Getter
    @Setter
    private int clientEntityId;
    @Getter
    @Setter
    private int serverEntityId;
    @Getter
    private ClientSettings settings;
    @Getter
    private final Scoreboard serverSentScoreboard = new Scoreboard();
    @Getter
    private final Collection<UUID> sentBossBars = new HashSet<>();
    // Waterfall start
    @Getter
    private final Multimap<Integer, Integer> potions = HashMultimap.create();
    // Waterfall end
    @Getter
    @Setter
    private String lastCommandTabbed;
    /*========================================================================*/
    @Getter
    private String displayName;
    @Getter
    private EntityMap entityRewrite;
    private Locale locale;
    /*========================================================================*/
    @Getter
    @Setter
    private ForgeClientHandler forgeClientHandler;
    @Getter
    @Setter
    private ForgeServerHandler forgeServerHandler;
    private static final Map<Integer, EntityMap> entityMapCache = new ConcurrentHashMap<>();

    /*========================================================================*/
    private final Queue<DefinedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private static EntityMap getCachedEntityMap(int version) {
        return entityMapCache.computeIfAbsent(version, EntityMap::getEntityMap);
    }
    private final Unsafe unsafe = new Unsafe()
    {
        @Override
        public void sendPacket(DefinedPacket packet)
        {
            //.instance.getTaskManager().add(() -> {
            ch.write( packet );
            // });
        }
    };

    public boolean init() {
        final ConfigurationAdapter configAdapter = bungee.getConfigurationAdapter();

        Set<String> nameGroups = new HashSet<>(configAdapter.getGroups(name));
        Set<String> uuidGroups = new HashSet<>(configAdapter.getGroups(getUniqueId().toString()));
        nameGroups.addAll(uuidGroups);

        nameGroups.forEach(this::addGroups);

        this.entityRewrite = getCachedEntityMap(this.getPendingConnection().getVersion());

        this.displayName = name;
        this.tabListHandler = new ServerUnique(this);

        InitialHandler pendingConnection = this.getPendingConnection();
        this.forgeClientHandler = new ForgeClientHandler(this);
        if (pendingConnection.getExtraDataInHandshake().contains(ForgeConstants.FML_HANDSHAKE_TOKEN)) {
            this.forgeClientHandler.setFmlTokenInHandshake(true);
        }

        return BungeeCord.getInstance().addConnection(this);
    }

    public void sendPacket(PacketWrapper packet)
    {
        //XenonCore.instance.getTaskManager().add(() -> {
        ch.write(packet);
        //});
    }

    public void sendPacketQueued(DefinedPacket packet)
    {
        if ( ch.getEncodeProtocol().TO_CLIENT.hasPacket( packet.getClass(), getPendingConnection().getVersion() ) ) {
            unsafe().sendPacket( packet );
            return;
        }
        packetQueue.add(packet);
    }

    public void sendQueuedPackets() {
        AtomicReference<DefinedPacket> packet = new AtomicReference<>();
        XenonCore.instance.getTaskManager().add(() -> {
            packet.set(packetQueue.poll());
            while (packet.get() != null) {
                unsafe().sendPacket(packet.get());
                packet.set(packetQueue.poll());
            }
        });
    }

    @Deprecated
    public boolean isActive()
    {
        return !ch.isClosed();
    }

    @Override
    public void setDisplayName(String name)
    {
        Preconditions.checkNotNull( name, "displayName" );
        displayName = name;
    }

    @Override
    public void connect(ServerInfo target)
    {
        connect( target, null, ServerConnectEvent.Reason.PLUGIN );
    }

    @Override
    public void connect(ServerInfo target, ServerConnectEvent.Reason reason)
    {
        connect( target, null, false, reason );
    }

    @Override
    public void connect(ServerInfo target, Callback<Boolean> callback)
    {
        connect( target, callback, false, ServerConnectEvent.Reason.PLUGIN );
    }

    @Override
    public void connect(ServerInfo target, Callback<Boolean> callback, ServerConnectEvent.Reason reason)
    {
        connect( target, callback, false, reason );
    }

    @Deprecated
    public void connectNow(ServerInfo target)
    {
        connectNow( target, ServerConnectEvent.Reason.UNKNOWN );
    }

    public void connectNow(ServerInfo target, ServerConnectEvent.Reason reason)
    {
        dimensionChange = true;
        connect( target, reason );
    }

    public void updateAndGetNextServer(ServerInfo currentTarget, Callback<ServerInfo> callback) {
        if (serverJoinQueue == null) {
            serverJoinQueue = new LinkedList<>(getPendingConnection().getListener().getServerPriority());
        }
        AtomicReference<ServerInfo> next = null;
        XenonCore.instance.getTaskManager().add(() -> {
            while (!serverJoinQueue.isEmpty()) {
                ServerInfo candidate = ProxyServer.getInstance().getServerInfo(serverJoinQueue.remove());
                if (!Objects.equals(currentTarget, candidate)) {
                    next.set(candidate);
                    break;
                }
                // test
                if(next == null) next.set(candidate);
            }
            if (callback != null)
                XenonCore.instance.getTaskManager().add(() -> callback.done(next.get(), null));
        });
    }

    public void connect(ServerInfo info, final Callback<Boolean> callback, final boolean retry)
    {
        connect( info, callback, retry, ServerConnectEvent.Reason.PLUGIN );
    }

    public void connect(ServerInfo info, final Callback<Boolean> callback, final boolean retry, ServerConnectEvent.Reason reason)
    {
        // Waterfall start
        connect(info, callback, retry, reason, bungee.getConfig().getServerConnectTimeout());
    }
    public void connect(ServerInfo info, final Callback<Boolean> callback, final boolean retry, int timeout) {
        connect(info, callback, retry, ServerConnectEvent.Reason.PLUGIN, timeout);
    }

    public void connect(ServerInfo info, final Callback<Boolean> callback, final boolean retry, ServerConnectEvent.Reason reason, final int timeout) {
        this.connect(info, callback, retry, reason, timeout, true);
    }

    public void connect(ServerInfo info, final Callback<Boolean> callback, final boolean retry, ServerConnectEvent.Reason reason, final int timeout, boolean sendFeedback)
    {
        Preconditions.checkNotNull( info, "info" );
        ServerConnectRequest.Builder builder = ServerConnectRequest.builder().retry( retry ).reason( reason ).target( info ).sendFeedback(sendFeedback); // Waterfall - feedback param
        builder.connectTimeout(timeout); // Waterfall
        if ( callback != null )
            builder.callback((result, error) -> callback.done( ( result == ServerConnectRequest.Result.SUCCESS ) ? Boolean.TRUE : Boolean.FALSE, error ));

        connect( builder.build() );
    }

    @Override
    public void connect(final ServerConnectRequest request) {
        Preconditions.checkNotNull(request, "request");
        XenonCore.instance.getTaskManager().add(() -> {
            final Callback<ServerConnectRequest.Result> callback = request.getCallback();
            final ServerConnectEvent event = new ServerConnectEvent(this, request.getTarget(), request.getReason(), request);

            if (bungee.getPluginManager().callEvent(event).isCancelled()) {
                if (callback != null)
                    callback.done(ServerConnectRequest.Result.EVENT_CANCEL, null);

                if (getServer() == null && !ch.isClosing())
                    throw new IllegalStateException("Cancelled ServerConnectEvent with no server or disconnect.");
                return;
            }

            final BungeeServerInfo target = (BungeeServerInfo) event.getTarget();

            if (getServer() != null && Objects.equals(getServer().getInfo(), target)) {
                if (callback != null)
                    callback.done(ServerConnectRequest.Result.ALREADY_CONNECTED, null);
                if (request.isSendFeedback())
                    sendMessage(bungee.getTranslation("already_connected"));
                return;
            }

            if (pendingConnects.contains(target)) {
                if (callback != null)
                    callback.done(ServerConnectRequest.Result.ALREADY_CONNECTING, null);
                if (request.isSendFeedback())
                    sendMessage(bungee.getTranslation("already_connecting"));
                return;
            }

            pendingConnects.add(target);

            ChannelInitializer initializer = new ChannelInitializer() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    PipelineUtils.BASE_SERVERSIDE.initChannel(ch);
                    ch.pipeline().addAfter(PipelineUtils.FRAME_DECODER, PipelineUtils.PACKET_DECODER,
                            new MinecraftDecoder(Protocol.HANDSHAKE, false, getPendingConnection().getVersion()));
                    ch.pipeline().addAfter(PipelineUtils.FRAME_PREPENDER, PipelineUtils.PACKET_ENCODER,
                            new MinecraftEncoder(Protocol.HANDSHAKE, false, getPendingConnection().getVersion()));
                    ch.pipeline().get(HandlerBoss.class).setHandler(new ServerConnector(bungee, UserConnection.this, target));
                }
            };

            ChannelFutureListener listener = future -> {
                if (callback != null)
                    callback.done(future.isSuccess() ? ServerConnectRequest.Result.SUCCESS : ServerConnectRequest.Result.FAIL, future.cause());

                if (!future.isSuccess()) {
                    future.channel().close();
                    pendingConnects.remove(target);

                    updateAndGetNextServer(target, (nextServer, error) -> {
                        if (error != null) {
                            System.err.println("Failed to update and get the next server: " + error.getMessage());
                            disconnect(bungee.getTranslation("fallback_kick", connectionFailMessage(future.cause())));
                            return;
                        }

                        if (request.isRetry() && nextServer != null && (getServer() == null || !nextServer.equals(getServer().getInfo()))) {
                            if (request.isSendFeedback())
                                sendMessage(bungee.getTranslation("fallback_lobby"));
                            connect(nextServer, null, true, ServerConnectEvent.Reason.LOBBY_FALLBACK, request.getConnectTimeout(), request.isSendFeedback());
                        } else if (dimensionChange)
                            disconnect(bungee.getTranslation("fallback_kick", connectionFailMessage(future.cause())));
                        else if (request.isSendFeedback())
                            sendMessage(bungee.getTranslation("fallback_kick", connectionFailMessage(future.cause())));
                    });
                }
            };

            Bootstrap bootstrap = new Bootstrap()
                    .channelFactory(PipelineUtils.getChannelFactory(target.getAddress()))
                    .group(ch.getHandle().eventLoop())
                    .handler(initializer)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, request.getConnectTimeout())
                    .remoteAddress(target.getAddress());

            if (getPendingConnection().getListener().isSetLocalAddress() &&
                    !PlatformDependent.isWindows() &&
                    getPendingConnection().getListener().getSocketAddress() instanceof InetSocketAddress) {
                bootstrap.localAddress(getPendingConnection().getListener().getHost().getHostString(), 0);
            }

            bootstrap.connect().addListener(listener);
        });
    }


    private String connectionFailMessage(Throwable cause)
    {
        bungee.getLogger().log(Level.WARNING, "Error occurred processing connection for " + this.name + " " + Util.exception( cause, false )); // Waterfall
        return ""; // Waterfall
    }

    @Override
    public void disconnect(String reason)
    {
        disconnect( TextComponent.fromLegacy( reason ) );
    }

    @Override
    public void disconnect(BaseComponent... reason)
    {
        disconnect( TextComponent.fromArray( reason ) );
    }

    @Override
    public void disconnect(BaseComponent reason)
    {
        disconnect0( reason );
    }

    public void disconnect0(final BaseComponent reason)
    {
        if ( ch.isClosing() ) return;

        bungee.getLogger().log( Level.INFO, "[{0}] disconnected with: {1}", new Object[]
                {
                        getName(), BaseComponent.toLegacyText( reason )
                } );

        ch.close( new Kick( reason ) );

        if ( server != null )
        {
            server.setObsolete( true );
            server.disconnect( "Quitting" );
        }
    }

    @Override
    public void chat(String message)
    {
        Preconditions.checkState( server != null, "Not connected to server" );

        if ( getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_19 )
            throw new UnsupportedOperationException( "Cannot spoof chat on this client version!" );

        server.getCh().write( new Chat( message ) );
    }

    @Override
    public void sendMessage(String message)
    {
        sendMessage( TextComponent.fromLegacy( message ) );
    }

    @Override
    public void sendMessages(String... messages)
    {
        XenonCore.instance.getTaskManager().add(() -> Arrays.stream(messages).forEach(this::sendMessage));
    }

    @Override
    public void sendMessage(BaseComponent... message)
    {
        sendMessage( ChatMessageType.SYSTEM, message );
    }

    @Override
    public void sendMessage(BaseComponent message)
    {
        sendMessage( ChatMessageType.SYSTEM, message );
    }

    @Override
    public void sendMessage(ChatMessageType position, BaseComponent... message)
    {
        sendMessage( position, null, TextComponent.fromArray( message ) );
    }

    @Override
    public void sendMessage(ChatMessageType position, BaseComponent message)
    {
        sendMessage( position, null, message );
    }

    @Override
    public void sendMessage(UUID sender, BaseComponent... message)
    {
        sendMessage( ChatMessageType.CHAT, sender, TextComponent.fromArray( message ) );
    }

    @Override
    public void sendMessage(UUID sender, BaseComponent message)
    {
        sendMessage( ChatMessageType.CHAT, sender, message );
    }

    private void sendMessage(ChatMessageType position, UUID sender, BaseComponent message) {
        message = ChatComponentTransformer.getInstance().transform(this, true, message);

        int version = getPendingConnection().getVersion();

        if (position == ChatMessageType.ACTION_BAR && version < ProtocolConstants.MINECRAFT_1_17) {
            if (version <= ProtocolConstants.MINECRAFT_1_10)
                message = new TextComponent(BaseComponent.toLegacyText(message));
         else {
                net.md_5.bungee.protocol.packet.Title title = new net.md_5.bungee.protocol.packet.Title();
                title.setAction(net.md_5.bungee.protocol.packet.Title.Action.ACTIONBAR);
                title.setText(message);
                sendPacketQueued(title);
                return;
            }
        }

        if (version >= ProtocolConstants.MINECRAFT_1_19) {
            if (position == ChatMessageType.CHAT)
                position = ChatMessageType.SYSTEM;
            sendPacketQueued(new SystemChat(message, position.ordinal()));
        } else {
            sendPacketQueued(new Chat(ComponentSerializer.toString(message), (byte) position.ordinal(), sender));
        }
    }


    @Override
    public void sendData(String channel, byte[] data)
    {
        sendPacketQueued( new PluginMessage( channel, data, forgeClientHandler.isForgeUser() ) );
    }

    @Override
    public InetSocketAddress getAddress()
    {
        return (InetSocketAddress) getSocketAddress();
    }

    @Override
    public SocketAddress getSocketAddress()
    {
        return ch.getRemoteAddress();
    }

    @Override
    public Collection<String> getGroups()
    {
        return Collections.unmodifiableCollection( groups );
    }

    @Override
    public void addGroups(String... groups)
    {
        XenonCore.instance.getTaskManager().add(() -> Arrays.stream(groups).forEach(group -> {
            this.groups.add(group);
            bungee.getConfigurationAdapter().getPermissions(group).forEach(perm -> setPermission(perm, true));
        }));
    }

    @Override
    public void removeGroups(String... groups)
    {
        XenonCore.instance.getTaskManager().add(() -> Arrays.stream(groups).forEach(group -> {
            bungee.getConfigurationAdapter().getPermissions(group).forEach(perm -> setPermission(perm, false));
            this.groups.remove(group);
        }));
    }

    @Override
    public boolean hasPermission(String permission)
    {
        return bungee.getPluginManager().callEvent( new PermissionCheckEvent( this, permission, permissions.contains( permission ) ) ).hasPermission();
    }

    @Override
    public void setPermission(String permission, boolean value)
    {
        if ( value )
            permissions.add( permission );
        else
            permissions.remove( permission );

    }

    @Override
    public Collection<String> getPermissions()
    {
        return Collections.unmodifiableCollection( permissions );
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public Unsafe unsafe()
    {
        return unsafe;
    }

    @Override
    public String getUUID()
    {
        return getPendingConnection().getUUID();
    }

    @Override
    public UUID getUniqueId()
    {
        return getPendingConnection().getUniqueId();
    }

    public UUID getRewriteId()
    {
        return getPendingConnection().getRewriteId();
    }

    public void setSettings(ClientSettings settings)
    {
        this.settings = settings;
        this.locale = null;
    }

    @Override
    public Locale getLocale()
    {
        return ( locale == null && settings != null ) ? locale = Locale.forLanguageTag( settings.getLocale().replace( '_', '-' ) ) : locale;
    }

    @Override
    public byte getViewDistance()
    {
        return ( settings != null ) ? settings.getViewDistance() : 10;
    }

    @Override
    public ProxiedPlayer.ChatMode getChatMode()
    {
        if ( settings == null )
            return ProxiedPlayer.ChatMode.SHOWN;

        switch ( settings.getChatFlags() )
        {
            default:
            case 0:
                return ProxiedPlayer.ChatMode.SHOWN;
            case 1:
                return ProxiedPlayer.ChatMode.COMMANDS_ONLY;
            case 2:
                return ProxiedPlayer.ChatMode.HIDDEN;
        }
    }

    @Override
    public boolean hasChatColors()
    {
        return settings == null || settings.isChatColours();
    }

    @Override
    public SkinConfiguration getSkinParts()
    {
        return ( settings != null ) ? new PlayerSkinConfiguration( settings.getSkinParts() ) : PlayerSkinConfiguration.SKIN_SHOW_ALL;
    }

    @Override
    public ProxiedPlayer.MainHand getMainHand()
    {
        return ( settings == null || settings.getMainHand() == 1 ) ? ProxiedPlayer.MainHand.RIGHT : ProxiedPlayer.MainHand.LEFT;
    }

    @Override
    public boolean isForgeUser()
    {
        return forgeClientHandler.isForgeUser();
    }

    @Override
    public Map<String, String> getModList()
    {
        if ( forgeClientHandler.getClientModList() == null )
            return ImmutableMap.of();

        return ImmutableMap.copyOf( forgeClientHandler.getClientModList() );
    }

    @Override
    public void setTabHeader(BaseComponent header, BaseComponent footer)
    {
        header = ChatComponentTransformer.getInstance().transform( this, true, header );
        footer = ChatComponentTransformer.getInstance().transform( this, true, footer );

        sendPacketQueued( new PlayerListHeaderFooter(
                header,
                footer
        ) );
    }

    @Override
    public void setTabHeader(BaseComponent[] header, BaseComponent[] footer)
    {
        setTabHeader( TextComponent.fromArray( header ), TextComponent.fromArray( footer ) );
    }

    @Override
    public void resetTabHeader()
    {
        // Mojang did not add a way to remove the header / footer completely, we can only set it to empty
        setTabHeader( (BaseComponent) null, null );
    }

    @Override
    public void sendTitle(Title title)
    {
        title.send( this );
    }

    public String getExtraDataInHandshake()
    {
        return this.getPendingConnection().getExtraDataInHandshake();
    }

    public void setCompressionThreshold(int compressionThreshold)
    {
        if ( !ch.isClosing() && this.compressionThreshold == -1 && compressionThreshold >= 0 )
        {
            this.compressionThreshold = compressionThreshold;
            unsafe.sendPacket( new SetCompression( compressionThreshold ) );
            ch.setCompressionThreshold( compressionThreshold );
        }
    }

    @Override
    public boolean isConnected()
    {
        return !ch.isClosed();
    }

    @Override
    public Scoreboard getScoreboard()
    {
        return serverSentScoreboard;
    }

    @Override
    public CompletableFuture<byte[]> retrieveCookie(String cookie)
    {
        return pendingConnection.retrieveCookie( cookie );
    }

    @Override
    public void storeCookie(String cookie, byte[] data)
    {
        Preconditions.checkState( getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_5, "Cookies are only supported in 1.20.5 and above" );

        unsafe().sendPacket( new StoreCookie( cookie, data ) );
    }

    @Override
    public void transfer(String host, int port)
    {
        Preconditions.checkState( getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_5, "Transfers are only supported in 1.20.5 and above" );

        unsafe().sendPacket( new Transfer( host, port ) );
    }
    // Waterfall start
    public boolean isDisableEntityMetadataRewrite() {
        return entityRewrite == net.md_5.bungee.entitymap.EntityMap_Dummy.INSTANCE;
    }
    // Waterfall end
}