package net.md_5.bungee;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.internal.PlatformDependent;
import ir.xenoncommunity.XenonCore;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.dialog.Dialog;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.score.Scoreboard;
import net.md_5.bungee.chat.VersionedComponentSerializer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.ServerConnector;
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
import java.util.logging.Level;

@RequiredArgsConstructor
public final class UserConnection implements ProxiedPlayer {

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
    @Getter
    private final Collection<ServerInfo> pendingConnects = new HashSet<>();
    /*========================================================================*/
    private final Collection<String> groups = new CaseInsensitiveSet();
    private final Collection<String> permissions = new CaseInsensitiveSet();
    @Getter
    private final Scoreboard serverSentScoreboard = new Scoreboard();
    @Getter
    private final Collection<UUID> sentBossBars = new HashSet<>();
    @Getter
    private VersionedComponentSerializer chatSerializer;
    // Waterfall start
    @Getter
    private final Multimap<Integer, Integer> potions = HashMultimap.create();
    /*========================================================================*/
    private final Queue<DefinedPacket> packetQueue = new ArrayDeque<>();
    /*========================================================================*/
    @Getter
    @Setter
    private ServerConnection server;    private final Unsafe unsafe = new Unsafe() {
        @Override
        public void sendPacket(DefinedPacket packet) {
            ch.write(packet);
        }

        @Override
        public void sendPacketQueued(DefinedPacket packet) {
            if (pendingConnection.getVersion() >= ProtocolConstants.MINECRAFT_1_20_2) {
                UserConnection.this.sendPacketQueued(packet);
            } else {
                sendPacket(packet);
            }
        }
    };
    @Getter
    @Setter
    private Object dimension;
    @Getter
    @Setter
    private boolean dimensionChange = true;
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
    @Getter
    @Setter
    private boolean bundling;

    public void toggleBundling()
    {
        bundling = !bundling;
    }
    /*========================================================================*/
    @Getter
    @Setter
    private int clientEntityId;
    @Getter
    @Setter
    private int serverEntityId;
    @Getter
    private ClientSettings settings;
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

    public boolean init() {
        this.entityRewrite = EntityMap.getEntityMap(getPendingConnection().getVersion());
        this.chatSerializer = ChatSerializer.forVersion( getPendingConnection().getVersion() );

        this.displayName = name;
        tabListHandler = new ServerUnique(this);


        forgeClientHandler = new ForgeClientHandler(this);

        if (this.getPendingConnection().getExtraDataInHandshake().contains(ForgeConstants.FML_HANDSHAKE_TOKEN))
            forgeClientHandler.setFmlTokenInHandshake(true);

        return BungeeCord.getInstance().addConnection(this);
    }

    public void sendPacket(PacketWrapper packet) {
        ch.write(packet);
    }

    public void sendPacketQueued(DefinedPacket packet) {
        ch.scheduleIfNecessary(() ->
        {
            if (ch.isClosed()) {
                return;
            }

            if (!ch.getEncodeProtocol().TO_CLIENT.hasPacket(packet.getClass(), getPendingConnection().getVersion())) {
                Preconditions.checkState(packetQueue.size() <= 4096, "too many queued packets");
                packetQueue.add(packet);
            } else {
                unsafe().sendPacket(packet);
            }
        });
    }

    public void sendQueuedPackets() {
        ch.scheduleIfNecessary(() ->
        {
            if (ch.isClosed())
                return;

            DefinedPacket packet;
            while ((packet = packetQueue.poll()) != null) {
                unsafe().sendPacket(packet);
            }
        });
    }

    @Deprecated
    public boolean isActive() {
        return !ch.isClosed();
    }

    @Override
    public void setDisplayName(String name) {
        Preconditions.checkNotNull(name, "displayName");
        displayName = name;
    }

    @Override
    public void connect(ServerInfo target) {
        connect(target, null, ServerConnectEvent.Reason.PLUGIN);
    }

    @Override
    public void connect(ServerInfo target, ServerConnectEvent.Reason reason) {
        connect(target, null, false, reason);
    }

    @Override
    public void connect(ServerInfo target, Callback<Boolean> callback) {
        connect(target, callback, false, ServerConnectEvent.Reason.PLUGIN);
    }

    @Override
    public void connect(ServerInfo target, Callback<Boolean> callback, ServerConnectEvent.Reason reason) {
        connect(target, callback, false, reason);
    }

    @Deprecated
    public void connectNow(ServerInfo target) {
        connectNow(target, ServerConnectEvent.Reason.UNKNOWN);
    }

    public void connectNow(ServerInfo target, ServerConnectEvent.Reason reason) {
        dimensionChange = true;
        connect(target, reason);
    }

    public ServerInfo updateAndGetNextServer(ServerInfo currentTarget) {
        if (serverJoinQueue == null)
            serverJoinQueue = new LinkedList<>(getPendingConnection().getListener().getServerPriority());

        while (!serverJoinQueue.isEmpty()) {
            final ServerInfo candidate = ProxyServer.getInstance().getServerInfo(serverJoinQueue.remove());
            if (!Objects.equals(currentTarget, candidate))
                return candidate;
        }

        return null;
    }

    public void connect(ServerInfo info, Callback<Boolean> callback, boolean retry) {
        connect(info, callback, retry, ServerConnectEvent.Reason.PLUGIN);
    }

    public void connect(ServerInfo info, Callback<Boolean> callback, boolean retry, ServerConnectEvent.Reason reason) {
        // Waterfall start
        connect(info, callback, retry, reason, bungee.getConfig().getServerConnectTimeout());
    }

    public void connect(ServerInfo info, Callback<Boolean> callback, boolean retry, int timeout) {
        connect(info, callback, retry, ServerConnectEvent.Reason.PLUGIN, timeout);
    }

    public void connect(ServerInfo info, Callback<Boolean> callback, boolean retry, ServerConnectEvent.Reason reason, int timeout) {
        this.connect(info, callback, retry, reason, timeout, true);
    }

    public void connect(ServerInfo info, Callback<Boolean> callback, boolean retry, ServerConnectEvent.Reason reason, int timeout, boolean sendFeedback) {
        // Waterfall end
        Preconditions.checkNotNull(info, "info");

        final ServerConnectRequest.Builder builder = ServerConnectRequest.builder().retry(retry).reason(reason).target(info).sendFeedback(sendFeedback); // Waterfall - feedback param
        builder.connectTimeout(timeout); // Waterfall
        if (callback != null)
            builder.callback((result, error) -> callback.done((result == ServerConnectRequest.Result.SUCCESS) ? Boolean.TRUE : Boolean.FALSE, error));

        connect(builder.build());
    }

    @Override
    public void connect(ServerConnectRequest request) {
        Preconditions.checkNotNull(request, "request");

        ch.getHandle().eventLoop().execute(() -> connect0(request));
    }

    private void connect0(ServerConnectRequest request) {
        final Callback<ServerConnectRequest.Result> callback = request.getCallback();
        final ServerConnectEvent event = new ServerConnectEvent(this, request.getTarget(), request.getReason(), request);

        if (bungee.getPluginManager().callEvent(event).isCancelled()) {
            if (callback != null)
                callback.done(ServerConnectRequest.Result.EVENT_CANCEL, null);
            return;
        }

        final BungeeServerInfo target = (BungeeServerInfo) event.getTarget(); // Update in case the event changed target

        if (getServer() != null && Objects.equals(getServer().getInfo(), target)) {
            if (callback != null)
                callback.done(ServerConnectRequest.Result.ALREADY_CONNECTED, null);

            if (request.isSendFeedback()) sendMessage(bungee.getTranslation("already_connected")); // Waterfall
            return;
        }
        if (pendingConnects.contains(target)) {
            if (callback != null)
                callback.done(ServerConnectRequest.Result.ALREADY_CONNECTING, null);

            if (request.isSendFeedback()) sendMessage(bungee.getTranslation("already_connecting")); // Waterfall
            return;
        }

        pendingConnects.add(target);

        ChannelInitializer initializer = new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                PipelineUtils.BASE_SERVERSIDE.initChannel(ch);
                ch.pipeline().addAfter(PipelineUtils.FRAME_DECODER, PipelineUtils.PACKET_DECODER, new MinecraftDecoder(Protocol.HANDSHAKE, false, getPendingConnection().getVersion()));
                ch.pipeline().addAfter(PipelineUtils.FRAME_PREPENDER, PipelineUtils.PACKET_ENCODER, new MinecraftEncoder(Protocol.HANDSHAKE, false, getPendingConnection().getVersion()));
                ch.pipeline().get(HandlerBoss.class).setHandler(new ServerConnector(bungee, UserConnection.this, target));
            }
        };
        ChannelFutureListener listener = future -> {
            if (callback != null)
                callback.done((future.isSuccess()) ? ServerConnectRequest.Result.SUCCESS : ServerConnectRequest.Result.FAIL, future.cause());

            if (!future.isSuccess()) {
                future.channel().close();
                pendingConnects.remove(target);

                final ServerInfo def = updateAndGetNextServer(target);
                if (request.isRetry() && def != null && (getServer() == null || !def.equals(getServer().getInfo()))) {
                    connect(def, (result, error) -> {
                        if (result && request.isSendFeedback()){
                            sendMessage( bungee.getTranslation( "fallback_lobby" ) );
                        }
                    }, true, ServerConnectEvent.Reason.LOBBY_FALLBACK);
                } else if (dimensionChange) {
                    disconnect(bungee.getTranslation("fallback_kick", connectionFailMessage(future.cause())));
                } else {
                    if (request.isSendFeedback())
                        sendMessage(bungee.getTranslation("fallback_kick", connectionFailMessage(future.cause())));
                }
            }
        };
        final Bootstrap b = new Bootstrap()
                .channelFactory(PipelineUtils.getChannelFactory(target.getAddress())) // Waterfall - netty reflection -> factory
                .group(ch.getHandle().eventLoop())
                .handler(initializer)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, request.getConnectTimeout())
                .remoteAddress(target.getAddress());
        // Windows is bugged, multi homed users will just have to live with random connecting IPs
        if (getPendingConnection().getListener().isSetLocalAddress() && !PlatformDependent.isWindows() && getPendingConnection().getListener().getSocketAddress() instanceof InetSocketAddress) {
            b.localAddress(getPendingConnection().getListener().getHost().getHostString(), 0);
        }
        b.connect().addListener(listener);
    }

    private String connectionFailMessage(Throwable cause) {
        bungee.getLogger().log(Level.WARNING, "Error occurred processing connection for " + this.name + " " + Util.exception(cause, false)); // Waterfall
        return ""; // Waterfall
    }

    @Override
    public void disconnect(String reason) {
        disconnect(TextComponent.fromLegacy(reason));
    }

    @Override
    public void disconnect(BaseComponent... reason) {
        disconnect(TextComponent.fromArray(reason));
    }

    @Override
    public void disconnect(BaseComponent reason) {
        disconnect0(reason);
    }

    public void disconnect0(BaseComponent reason) {
        if (ch.isClosing()) return;

        bungee.getLogger().log(Level.INFO, "[{0}] disconnected with: {1}", new Object[]
                {
                        getName(), BaseComponent.toLegacyText(reason)
                });

        ch.close(new Kick(reason));

        if (server != null) {
            server.setObsolete(true);
            server.disconnect("Quitting");
        }
    }

    @Override
    public void chat(String message) {
        Preconditions.checkState(server != null, "Not connected to server");
        if (getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_19) {
            throw new UnsupportedOperationException("Cannot spoof chat on this client version!");
        }
        server.getCh().write(new Chat(message));
    }

    @Override
    public void sendMessage(String message) {
        sendMessage(TextComponent.fromLegacy(message));
    }

    @Override
    public void sendMessages(String... messages) {
        XenonCore.instance.getTaskManager().async(() -> Arrays.stream(messages).forEach(this::sendMessage));
    }

    @Override
    public void sendMessage(BaseComponent... message) {
        sendMessage(ChatMessageType.SYSTEM, message);
    }

    @Override
    public void sendMessage(BaseComponent message) {
        sendMessage(ChatMessageType.SYSTEM, message);
    }

    @Override
    public void sendMessage(ChatMessageType position, BaseComponent... message) {
        sendMessage(position, null, TextComponent.fromArray(message));
    }

    @Override
    public void sendMessage(ChatMessageType position, BaseComponent message) {
        sendMessage(position, null, message);
    }

    @Override
    public void sendMessage(UUID sender, BaseComponent... message) {
        sendMessage(ChatMessageType.CHAT, sender, TextComponent.fromArray(message));
    }

    @Override
    public void sendMessage(UUID sender, BaseComponent message) {
        sendMessage(ChatMessageType.CHAT, sender, message);
    }

    private void sendMessage(ChatMessageType position, UUID sender, BaseComponent message) {
        message = ChatComponentTransformer.getInstance().transform(this, true, message);

        final int version = getPendingConnection().getVersion();

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
            sendPacketQueued( new Chat( chatSerializer.toString( message ), (byte) position.ordinal(), sender ) );
        }
    }

    @Override
    public void sendData(String channel, byte[] data) {
        sendPacketQueued(new PluginMessage(channel, data, forgeClientHandler.isForgeUser()));
    }

    @Override
    public InetSocketAddress getAddress() {
        return (InetSocketAddress) getSocketAddress();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return ch.getRemoteAddress();
    }

    @Override
    public Collection<String> getGroups() {
        return Collections.unmodifiableCollection(groups);
    }

    @Override
    public void addGroups(String... groups) {
        XenonCore.instance.getTaskManager().async(() -> this.groups.addAll(Arrays.asList(groups)));
    }

    @Override
    public void removeGroups(String... groups) {
        XenonCore.instance.getTaskManager().async(() -> this.groups.removeAll(Arrays.asList(groups)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return bungee.getPluginManager().callEvent(new PermissionCheckEvent(this, permission, permissions.contains(permission))).hasPermission();
    }

    @Override
    public void setPermission(String permission, boolean value) {
        if (value) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
    }

    @Override
    public Collection<String> getPermissions() {
        return Collections.unmodifiableCollection(permissions);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    @Override
    public String getUUID() {
        return getPendingConnection().getUUID();
    }

    @Override
    public UUID getUniqueId() {
        return getPendingConnection().getUniqueId();
    }

    public UUID getRewriteId() {
        return getPendingConnection().getRewriteId();
    }

    public void setSettings(ClientSettings settings) {
        this.settings = settings;
        this.locale = null;
    }

    @Override
    public Locale getLocale() {
        return (locale == null && settings != null) ? locale = Locale.forLanguageTag(settings.getLocale().replace('_', '-')) : locale;
    }

    @Override
    public byte getViewDistance() {
        return (settings != null) ? settings.getViewDistance() : 10;
    }

    @Override
    public ProxiedPlayer.ChatMode getChatMode() {
        if (settings == null) {
            return ProxiedPlayer.ChatMode.SHOWN;
        }

        switch (settings.getChatFlags()) {
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
    public boolean hasChatColors() {
        return settings == null || settings.isChatColours();
    }

    @Override
    public SkinConfiguration getSkinParts() {
        return (settings != null) ? new PlayerSkinConfiguration(settings.getSkinParts()) : PlayerSkinConfiguration.SKIN_SHOW_ALL;
    }

    @Override
    public ProxiedPlayer.MainHand getMainHand() {
        return (settings == null || settings.getMainHand() == 1) ? ProxiedPlayer.MainHand.RIGHT : ProxiedPlayer.MainHand.LEFT;
    }

    @Override
    public boolean isForgeUser() {
        return forgeClientHandler.isForgeUser();
    }

    @Override
    public Map<String, String> getModList() {
        if (forgeClientHandler.getClientModList() == null) {
            // Return an empty map, rather than a null, if the client hasn't got any mods,
            // or is yet to complete a handshake.
            return ImmutableMap.of();
        }

        return ImmutableMap.copyOf(forgeClientHandler.getClientModList());
    }

    @Override
    public void setTabHeader(BaseComponent header, BaseComponent footer) {
        header = ChatComponentTransformer.getInstance().transform(this, true, header);
        footer = ChatComponentTransformer.getInstance().transform(this, true, footer);

        sendPacketQueued(new PlayerListHeaderFooter(
                header,
                footer
        ));
    }

    @Override
    public void setTabHeader(BaseComponent[] header, BaseComponent[] footer) {
        setTabHeader(TextComponent.fromArray(header), TextComponent.fromArray(footer));
    }

    @Override
    public void resetTabHeader() {
        // Mojang did not add a way to remove the header / footer completely, we can only set it to empty
        setTabHeader((BaseComponent) null, null);
    }

    @Override
    public void sendTitle(Title title) {
        title.send(this);
    }

    public String getExtraDataInHandshake() {
        return this.getPendingConnection().getExtraDataInHandshake();
    }

    public String getClientBrand()
    {
        return getPendingConnection().getClientBrand();
    }

    public void setCompressionThreshold(int compressionThreshold) {
        if (!ch.isClosing() && this.compressionThreshold == -1 && compressionThreshold >= 0) {
            this.compressionThreshold = compressionThreshold;
            unsafe.sendPacket(new SetCompression(compressionThreshold));
            ch.setCompressionThreshold(compressionThreshold);
        }
    }

    @Override
    public boolean isConnected() {
        return !ch.isClosed();
    }

    @Override
    public Scoreboard getScoreboard() {
        return serverSentScoreboard;
    }

    @Override
    public CompletableFuture<byte[]> retrieveCookie(String cookie) {
        return pendingConnection.retrieveCookie(cookie);
    }

    @Override
    public void storeCookie(String cookie, byte[] data) {
        Preconditions.checkState(getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_5, "Cookies are only supported in 1.20.5 and above");

        unsafe().sendPacket(new StoreCookie(cookie, data));
    }

    @Override
    public void transfer(String host, int port) {
        Preconditions.checkState(getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_5, "Transfers are only supported in 1.20.5 and above");

        unsafe().sendPacket(new Transfer(host, port));
    }

    // Waterfall start
    public boolean isDisableEntityMetadataRewrite() {
        return entityRewrite == net.md_5.bungee.entitymap.EntityMap_Dummy.INSTANCE;
    }


    // Waterfall end

    @Override
    public void clearDialog()
    {
        Preconditions.checkState( getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_21_6, "Dialogs are only supported in 1.21.6 and above" );

        unsafe().sendPacket( new ClearDialog() );
    }

    @Override
    public void showDialog(Dialog dialog)
    {
        Preconditions.checkState( getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_21_6, "Dialogs are only supported in 1.21.6 and above" );

        if ( ch.getEncodeProtocol() == Protocol.CONFIGURATION )
        {
            unsafe.sendPacket( new ShowDialogDirect( dialog ) );
            return;
        }

        unsafe.sendPacket( new ShowDialog( Either.right( dialog ) ) );
    }
}
