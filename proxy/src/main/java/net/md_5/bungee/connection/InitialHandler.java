package net.md_5.bungee.connection;

import com.google.common.base.Preconditions;
import ir.xenoncommunity.XenonCore;
import lombok.*;
import net.md_5.bungee.*;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.http.HttpClient;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.netty.cipher.CipherDecoder;
import net.md_5.bungee.netty.cipher.CipherEncoder;
import net.md_5.bungee.protocol.*;
import net.md_5.bungee.protocol.packet.*;
import net.md_5.bungee.util.AllowedCharacters;
import net.md_5.bungee.util.BufUtil;
import net.md_5.bungee.util.QuietException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@RequiredArgsConstructor
public class InitialHandler extends PacketHandler implements PendingConnection
{

    private static final String MOJANG_AUTH_URL = System.getProperty("waterfall.auth.url", "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s%s");

    private final BungeeCord bungee;
    private ChannelWrapper ch;
    @Getter
    private final ListenerInfo listener;
    @Getter
    private Handshake handshake;
    @Getter
    private LoginRequest loginRequest;
    private EncryptionRequest request;
    @Getter
    private PluginMessage brandMessage;
    @Getter
    private final Set<String> registeredChannels = new HashSet<>();
    private State thisState = State.HANDSHAKE;
    private final Queue<CookieFuture> requestedCookies = new LinkedList<>();

    @Data
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class CookieFuture
    {

        private String cookie;
        private CompletableFuture<byte[]> future;
    }

    private final Unsafe unsafe = new Unsafe()
    {
        @Override
        public void sendPacket(DefinedPacket packet)
        {
            ch.write( packet );
        }
    };
    @Getter
    private boolean onlineMode = BungeeCord.getInstance().config.isOnlineMode();
    @Getter
    private InetSocketAddress virtualHost;
    private String name;
    @Getter
    private UUID uniqueId;
    @Getter
    private UUID offlineId;
    @Getter
    private UUID rewriteId;
    @Getter
    private LoginResult loginProfile;
    @Getter
    private boolean legacy;
    @Getter
    private String extraDataInHandshake = "";
    @Getter
    private boolean transferred;
    private UserConnection userCon;

    @Override
    public boolean shouldHandle(PacketWrapper packet) {
        return !ch.isClosing();
    }

    private enum State
    {

        HANDSHAKE, STATUS, PING, USERNAME, ENCRYPT, FINISHING
    }

    private boolean canSendKickMessage()
    {
        return thisState == State.USERNAME || thisState == State.ENCRYPT || thisState == State.FINISHING;
    }

    @Override
    public void connected(ChannelWrapper channel) throws Exception
    {
        this.ch = channel;
    }

    @Override
    public void exception(Throwable t) throws Exception
    {
        if (canSendKickMessage()) disconnect(ChatColor.RED + Util.exception(t));
        else ch.close();
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        if ( packet.packet == null )
            throw new QuietException( "Unexpected packet received during login process! " + BufUtil.dump( packet.buf, 16 ) );
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception
    {
        // Waterfall start
        try {
            this.relayMessage(pluginMessage);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new QuietException(ex.getMessage());
        }
        // Waterfall end
    }

    @Override
    public void handle(LegacyHandshake legacyHandshake) throws Exception
    {
        Preconditions.checkState( !this.legacy, "Not expecting LegacyHandshake" );
        this.legacy = true;
        ch.close( bungee.getTranslation( "outdated_client", bungee.getGameVersion() ) );
    }

    @Override
    public void handle(LegacyPing ping) throws Exception {
        Preconditions.checkState( !this.legacy, "Not expecting LegacyPing" );
        this.legacy = true;
        final ServerInfo forced = AbstractReconnectHandler.getForcedHost(this);
        final String motd = (forced != null) ? forced.getMotd() : listener.getMotd();
        final int protocol = bungee.getProtocolVersion();
        XenonCore.instance.getTaskManager().add(() -> {
            Callback<ServerPing> pingBack = (result, error) -> {
                if (error != null) {
                    result = getPingInfo(bungee.getTranslation("ping_cannot_connect"), protocol);
                    bungee.getLogger().log(Level.WARNING, "Error pinging remote server", error);
                }

                Callback<ProxyPingEvent> callback = (result1, error1) -> {
                    if (ch.isClosing()) return;

                    ServerPing legacy = result1.getResponse();
                    ch.close(ping.isV1_5()
                            ? ChatColor.DARK_BLUE + "\00" + 127
                            + '\00' + legacy.getVersion().getName()
                            + '\00' + getFirstLine(legacy.getDescription())
                            + '\00' + ((legacy.getPlayers() != null) ? legacy.getPlayers().getOnline() : "-1")
                            + '\00' + ((legacy.getPlayers() != null) ? legacy.getPlayers().getMax() : "-1")
                            : ChatColor.stripColor(getFirstLine(legacy.getDescription()))
                            + '\u00a7' + ((legacy.getPlayers() != null) ? legacy.getPlayers().getOnline() : "-1")
                            + '\u00a7' + ((legacy.getPlayers() != null) ? legacy.getPlayers().getMax() : "-1"));
                };

                bungee.getPluginManager().callEvent(new ProxyPingEvent(InitialHandler.this, result, callback));
            };

            if (forced != null && listener.isPingPassthrough()) ((BungeeServerInfo) forced).ping(pingBack, protocol);
            else pingBack.done(getPingInfo(motd, protocol), null);
        });
    }


    private static String getFirstLine(String str)
    {
        final int pos = str.indexOf( '\n' );
        return pos == -1 ? str : str.substring( 0, pos );
    }

    private ServerPing getPingInfo(String motd, int protocol)
    {
        return new ServerPing(
                new ServerPing.Protocol( bungee.getName() + " " + bungee.getGameVersion(), protocol ),
                new ServerPing.Players( listener.getMaxPlayers(), bungee.getOnlineCount(), null ),
                motd, BungeeCord.getInstance().config.getFaviconObject()
        );
    }

    @Override
    public void handle(StatusRequest statusRequest) throws Exception {
        Preconditions.checkState(thisState == State.STATUS, "Not expecting STATUS");

        final ServerInfo forced = AbstractReconnectHandler.getForcedHost(this);
        final String motd = (forced != null) ? forced.getMotd() : listener.getMotd();
        final int protocol = (ProtocolConstants.SUPPORTED_VERSION_IDS.contains(handshake.getProtocolVersion()))
                ? handshake.getProtocolVersion()
                : bungee.getProtocolVersion();

        XenonCore.instance.getTaskManager().add(() -> {
            Callback<ServerPing> pingBack = (result, error) -> {
                if (error != null) {
                    result = getPingInfo(bungee.getTranslation("ping_cannot_connect"), protocol);
                    bungee.getLogger().log(Level.WARNING, "Error pinging remote server", error);
                }

                Callback<ProxyPingEvent> callback = (pingResult, error1) -> {
                    unsafe.sendPacket(new StatusResponse(BungeeCord.getInstance().gson.toJson(pingResult.getResponse())));
                    if (bungee.getConnectionThrottle() != null) {
                        bungee.getConnectionThrottle().unthrottle(getSocketAddress());
                    }
                };

                bungee.getPluginManager().callEvent(new ProxyPingEvent(InitialHandler.this, result, callback));
            };

            if (forced != null && listener.isPingPassthrough()) {
                ((BungeeServerInfo) forced).ping(pingBack, handshake.getProtocolVersion());
            } else {
                pingBack.done(getPingInfo(motd, protocol), null);
            }
        });

        thisState = State.PING;
    }

    @Override
    public void handle(PingPacket ping) throws Exception
    {
        Preconditions.checkState(thisState == State.PING, "Not expecting PING");
        unsafe.sendPacket( ping );
        disconnect( "" );
    }

    @Override
    public void handle(Handshake handshake) throws Exception {
        Preconditions.checkState( thisState == State.HANDSHAKE && !this.legacy, "Not expecting HANDSHAKE" );
        this.handshake = handshake;
        ch.setVersion(handshake.getProtocolVersion());
        ch.getHandle().pipeline().remove(PipelineUtils.LEGACY_KICKER);
        final String host = handshake.getHost();

        if (host.indexOf("\0") != -1) {
            String[] split = handshake.getHost().split("\0", 2);
            handshake.setHost(split[0]);
            extraDataInHandshake = "\0" + split[1];
        } else if (host.endsWith("."))
            handshake.setHost(host.substring(0, host.length() - 1));

        this.virtualHost = InetSocketAddress.createUnresolved(handshake.getHost(), handshake.getPort());

        bungee.getPluginManager().callEvent(new PlayerHandshakeEvent(InitialHandler.this, handshake));
        if (ch.isClosing())
            return;

        switch (handshake.getRequestedProtocol()) {
            case 1:
                if (bungee.getConfig().isLogPings())
                    bungee.getLogger().log(Level.INFO, "{0} is pinging the server", this);
                thisState = State.STATUS;
                ch.setProtocol(Protocol.STATUS);
                break;
            case 2:
            case 3:
                transferred = handshake.getRequestedProtocol() == 3;
                if (BungeeCord.getInstance().getConfig().isLogInitialHandlerConnections())
                    bungee.getLogger().log(Level.INFO, "{0} is connected to the server", this);
                thisState = State.USERNAME;
                ch.setProtocol(Protocol.LOGIN);

                if (!ProtocolConstants.SUPPORTED_VERSION_IDS.contains(handshake.getProtocolVersion())) {
                    if (handshake.getProtocolVersion() > bungee.getProtocolVersion()) {
                        disconnect(bungee.getTranslation("outdated_server", bungee.getGameVersion()));
                    } else {
                        disconnect(bungee.getTranslation("outdated_client", bungee.getGameVersion()));
                    }
                    return;
                }

                if (transferred && bungee.config.isRejectTransfers()) {
                    disconnect(bungee.getTranslation("reject_transfer"));
                    return;
                }
                break;
            default:
                throw new QuietException("Cannot request protocol " + handshake.getRequestedProtocol());
        }
    }


    @Override
    public void handle(LoginRequest loginRequest) throws Exception {
        Preconditions.checkState( thisState == State.USERNAME && this.loginRequest == null, "Not expecting USERNAME" );

        if (!AllowedCharacters.isValidName(loginRequest.getData(), onlineMode)) {
            disconnect(bungee.getTranslation("name_invalid"));
            return;
        }

        if (BungeeCord.getInstance().config.isEnforceSecureProfile() && getVersion() < ProtocolConstants.MINECRAFT_1_19_3) {
            if (handshake.getProtocolVersion() < ProtocolConstants.MINECRAFT_1_19) {
                disconnect(bungee.getTranslation("secure_profile_unsupported"));
                return;
            }
            PlayerPublicKey publicKey = loginRequest.getPublicKey();
            if (publicKey == null) {
                disconnect(bungee.getTranslation("secure_profile_required"));
                return;
            }
            if (Instant.ofEpochMilli(publicKey.getExpiry()).isBefore(Instant.now())) {
                disconnect(bungee.getTranslation("secure_profile_expired"));
                return;
            }
            if (getVersion() < ProtocolConstants.MINECRAFT_1_19_1) {
                if (!EncryptionUtil.check(publicKey, null)) {
                    disconnect(bungee.getTranslation("secure_profile_invalid"));
                    return;
                }
            }
        }

        this.loginRequest = loginRequest;

        final int limit = BungeeCord.getInstance().config.getPlayerLimit();
        if (limit > 0 && bungee.getOnlineCount() >= limit) {
            disconnect(bungee.getTranslation("proxy_full"));
            return;
        }

        if (!isOnlineMode() && bungee.getPlayer(getUniqueId()) != null) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        bungee.getPluginManager().callEvent(new PreLoginEvent(InitialHandler.this, (result, error) -> {
            if (result.isCancelled()) {
                BaseComponent reason = result.getReason();
                disconnect((reason != null) ? reason : TextComponent.fromLegacy(bungee.getTranslation("kick_message")));
                return;
            }
            if (ch.isClosing()) return;
            if (onlineMode) {
                thisState = State.ENCRYPT;
                unsafe().sendPacket(EncryptionUtil.encryptRequest());
            } else {
                thisState = State.FINISHING;
                finish();
            }
        }));
    }

    @Override
    public void handle(final EncryptionResponse encryptResponse) throws Exception {
        Preconditions.checkState(thisState == State.ENCRYPT, "Not expecting ENCRYPT");
        Preconditions.checkState(EncryptionUtil.check(loginRequest.getPublicKey(), encryptResponse, request), "Invalid verification");

        thisState = State.FINISHING; // Move earlier - There is no verification of this later (and this is not API)

        SecretKey sharedKey = EncryptionUtil.getSecret(encryptResponse, request);

        if (sharedKey instanceof SecretKeySpec && sharedKey.getEncoded().length != 16) {
            ch.close();
            return;
        }

        ch.addBefore(PipelineUtils.FRAME_DECODER, PipelineUtils.DECRYPT_HANDLER, new CipherDecoder(EncryptionUtil.getCipher(false, sharedKey)));
        ch.addBefore(PipelineUtils.FRAME_PREPENDER, PipelineUtils.ENCRYPT_HANDLER, new CipherEncoder(EncryptionUtil.getCipher(true, sharedKey)));

        String encName = URLEncoder.encode(InitialHandler.this.getName(), "UTF-8");

        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        XenonCore.instance.getTaskManager().add(() -> {
            try{
                for (byte[] bit : new byte[][]{
                        request.getServerId().getBytes("ISO_8859_1"),
                        sharedKey.getEncoded(),
                        EncryptionUtil.keys.getPublic().getEncoded()
                }) {
                    sha.update(bit);
                }

                HttpClient.get(String.format(
                                MOJANG_AUTH_URL,
                                encName,
                                URLEncoder.encode(new BigInteger(sha.digest()).toString(16), "UTF-8"),
                                (BungeeCord.getInstance().config.isPreventProxyConnections() && getSocketAddress() instanceof InetSocketAddress)
                                        ? "&ip=" + URLEncoder.encode(getAddress().getAddress().getHostAddress(), "UTF-8")
                                        : "")
                        , ch.getHandle().eventLoop(),
                        (result, error) -> {
                            if (error == null) {
                                LoginResult obj = BungeeCord.getInstance().gson.fromJson(result, LoginResult.class);
                                if (obj != null && obj.getId() != null) {
                                    loginProfile = obj;
                                    name = obj.getName();
                                    uniqueId = Util.getUUID(obj.getId());
                                    finish();
                                } else
                                    disconnect(bungee.getTranslation("offline_mode_player"));
                            } else {
                                disconnect(bungee.getTranslation("mojang_fail"));
                                bungee.getLogger().log(Level.SEVERE, "Error authenticating " + getName() + " with minecraft.net", error);
                            }
                        });
            }catch(Exception e){
                e.printStackTrace();
            }
        });
    }

    private void finish() {
        offlineId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + getName()).getBytes(StandardCharsets.UTF_8));
        if (uniqueId == null)
            uniqueId = offlineId;

        rewriteId = (bungee.config.isIpForward()) ? uniqueId : offlineId;

        if (BungeeCord.getInstance().config.isEnforceSecureProfile()) {
            if (getVersion() >= ProtocolConstants.MINECRAFT_1_19_1 && getVersion() < ProtocolConstants.MINECRAFT_1_19_3) {
                try {
                    if (!EncryptionUtil.check(loginRequest.getPublicKey(), uniqueId)) {
                        disconnect(bungee.getTranslation("secure_profile_invalid"));
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }

        if ((isOnlineMode() && bungee.getPlayer(getName()) != null
                && bungee.getPlayer(getUniqueId()) != null)
                || bungee.getPlayer(getName()) != null) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        bungee.getPluginManager().callEvent(new LoginEvent(InitialHandler.this, (result, error) -> {
            if (result.isCancelled()) {
                BaseComponent reason = result.getReason();
                disconnect((reason != null) ? reason : TextComponent.fromLegacy(bungee.getTranslation("kick_message")));
                return;
            }
            if (ch.isClosing()) return;

            ch.getHandle().eventLoop().execute(() -> {
                if (!ch.isClosing()) {
                    userCon = new UserConnection(bungee, ch, getName(), InitialHandler.this);
                    userCon.setCompressionThreshold(BungeeCord.getInstance().config.getCompressionThreshold());

                    if (getVersion() < ProtocolConstants.MINECRAFT_1_20_2) {
                        unsafe().sendPacket(new LoginSuccess(getRewriteId(), getName(), (loginProfile == null) ? null : loginProfile.getProperties()));
                        ch.setProtocol(Protocol.GAME);
                    }
                    finish2();
                }
            });
        }, this.getLoginProfile())); // Waterfall: Parse LoginResult object to new constructor of LoginEvent
    }


    private void finish2() {
        if (!userCon.init()) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        ch.getHandle().pipeline().get(HandlerBoss.class).setHandler(new UpstreamBridge(bungee, userCon));

        ServerInfo initialServer = (bungee.getReconnectHandler() != null)
                ? bungee.getReconnectHandler().getServer(userCon)
                : AbstractReconnectHandler.getForcedHost(InitialHandler.this);

        if (initialServer == null)
            initialServer = bungee.getServerInfo(listener.getDefaultServer());

        bungee.getPluginManager().callEvent(new PostLoginEvent(userCon, initialServer, (result, error) -> {
            if (!ch.isClosing()) {
                userCon.connect(result.getTarget(), null, true, ServerConnectEvent.Reason.JOIN_PROXY);
            }
        }));
    }


    @Override
    public void handle(LoginPayloadResponse response) throws Exception
    {
        disconnect( "Unexpected custom LoginPayloadResponse" );
    }

    @Override
    public void handle(CookieResponse cookieResponse) {
        CookieFuture future;
        synchronized (requestedCookies) {
            future = requestedCookies.peek();
            if (future != null && future.cookie.equals(cookieResponse.getCookie())) {
                Preconditions.checkState(future == requestedCookies.poll(), "requestedCookies queue mismatch");
            } else {
                future = null; // leave for handling by backend
            }
        }

        if (future != null) {
            future.getFuture().complete(cookieResponse.getData());
            throw CancelSendSignal.INSTANCE;
        }
    }


    @Override
    public void disconnect(String reason)
    {
        if ( canSendKickMessage() )
        {
            disconnect( TextComponent.fromLegacy( reason ) );
        } else
        {
            ch.close();
        }
    }

    @Override
    public void disconnect(final BaseComponent... reason)
    {
        disconnect( TextComponent.fromArray( reason ) );
    }

    @Override
    public void disconnect(BaseComponent reason)
    {
        if ( canSendKickMessage() )
        {
            ch.delayedClose( new Kick( reason ) );
        } else
        {
            ch.close();
        }
    }

    @Override
    public String getName()
    {
        return ( name != null ) ? name : ( loginRequest == null ) ? null : loginRequest.getData();
    }

    @Override
    public int getVersion()
    {
        return ( handshake == null ) ? -1 : handshake.getProtocolVersion();
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
    public Unsafe unsafe()
    {
        return unsafe;
    }

    @Override
    public void setOnlineMode(boolean onlineMode)
    {
        Preconditions.checkState( thisState == State.USERNAME, "Can only set online mode status whilst state is username" );
        this.onlineMode = onlineMode;
    }

    @Override
    public void setUniqueId(UUID uuid)
    {
        Preconditions.checkState( thisState == State.USERNAME, "Can only set uuid while state is username" );
        Preconditions.checkState( !onlineMode, "Can only set uuid when online mode is false" );
        this.uniqueId = uuid;
    }

    @Override
    public String getUUID()
    {
        return io.github.waterfallmc.waterfall.utils.UUIDUtils.undash( uniqueId.toString() ); // Waterfall
    }

    @Override
    public String toString()
    {
        return "[" + getSocketAddress() + ( getName() != null ? "|" + getName() : "" ) + "] <-> InitialHandler";
    }

    @Override
    public boolean isConnected()
    {
        return !ch.isClosed();
    }

    public void relayMessage(PluginMessage input) {
        String content = new String( input.getData(), StandardCharsets.UTF_8 );
        XenonCore.instance.getTaskManager().async(() -> {
            Arrays.stream(content.split("\0")).forEach(id -> {
                // Waterfall start: Add configurable limits for plugin messaging
                if (input.getTag().equals("REGISTER") || input.getTag().equals("minecraft:register")){
                    Preconditions.checkState(!(registeredChannels.size() > bungee.getConfig().getPluginChannelLimit()), "Too many registered channels. This limit can be configured in the waterfall.yml");
                    Preconditions.checkArgument(!(id.length() > bungee.getConfig().getPluginChannelNameLimit()), "Channel name too long. This limit can be configured in the waterfall.yml");
                    // Waterfall end
                    registeredChannels.add(id);
                } else if ( input.getTag().equals( "UNREGISTER" ) || input.getTag().equals( "minecraft:unregister" )){
                    registeredChannels.remove( id );
                }else if ( input.getTag().equals( "MC|Brand" ) || input.getTag().equals( "minecraft:brand" )){
                    brandMessage = input;
                }
            });
        });
    }

    @Override
    public CompletableFuture<byte[]> retrieveCookie(String cookie)
    {
        Preconditions.checkState( getVersion() >= ProtocolConstants.MINECRAFT_1_20_5, "Cookies are only supported in 1.20.5 and above" );
        Preconditions.checkState( loginRequest != null, "Cannot retrieve cookies for status or legacy connections" );

        if ( cookie.indexOf( ':' ) == -1 )
        {
            // if we request an invalid resource location (no prefix) the client will respond with "minecraft:" prefix
            cookie = "minecraft:" + cookie;
        }

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        synchronized ( requestedCookies )
        {
            requestedCookies.add( new CookieFuture( cookie, future ) );
        }
        unsafe.sendPacket( new CookieRequest( cookie ) );

        return future;
    }
}
