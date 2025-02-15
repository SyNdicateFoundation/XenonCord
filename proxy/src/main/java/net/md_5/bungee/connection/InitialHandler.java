package net.md_5.bungee.connection;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import io.netty.channel.EventLoop;
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
import net.md_5.bungee.api.xenoncord.PostPlayerHandshakeEvent;
import net.md_5.bungee.http.HttpClient;
import net.md_5.bungee.jni.cipher.BungeeCipher;
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
public class InitialHandler extends PacketHandler implements PendingConnection {

    private static final String MOJANG_AUTH_URL = System.getProperty("waterfall.auth.url", "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s%s");

    private final BungeeCord bungee;
    @Getter
    private final ListenerInfo listener;
    @Getter
    private final Set<String> registeredChannels = new HashSet<>();
    private final Map<Integer, CompletableFuture<byte[]>> requestedLoginPayloads = new HashMap<>();
    private final Queue<CookieFuture> requestedCookies = new LinkedList<>();
    private ChannelWrapper ch;
    private final Unsafe unsafe = new Unsafe() {
        @Override
        public void sendPacket(DefinedPacket packet) {
            ch.write(packet);
        }
    };
    @Getter
    private Handshake handshake;
    @Getter
    private LoginRequest loginRequest;
    private EncryptionRequest request;
    @Getter
    private PluginMessage brandMessage;
    private State thisState = State.HANDSHAKE;
    private int loginPayloadId;
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

    private static String getFirstLine(String str) {
        int pos = str.indexOf('\n');
        return pos == -1 ? str : str.substring(0, pos);
    }

    @Override
    public boolean shouldHandle(PacketWrapper packet) throws Exception {
        return !ch.isClosing();
    }

    private boolean canSendKickMessage() {
        return thisState == State.USERNAME || thisState == State.ENCRYPT || thisState == State.FINISHING;
    }

    @Override
    public void connected(ChannelWrapper channel) throws Exception {
        this.ch = channel;
    }

    @Override
    public void exception(Throwable t) throws Exception {
        if (canSendKickMessage()) disconnect(ChatColor.RED + Util.exception(t));
        else ch.close();
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception {
        if (packet.packet == null) {
            throw new QuietException("Unexpected packet received during login process! " + BufUtil.dump(packet.buf, 16));
        }
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception {
        // Waterfall start
        try {
            this.relayMessage(pluginMessage);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new QuietException(ex.getMessage());
        }
        // Waterfall end
    }

    @Override
    public void handle(LegacyHandshake legacyHandshake) throws Exception {
        Preconditions.checkState(!this.legacy, "Not expecting LegacyHandshake");
        this.legacy = true;
        ch.close(bungee.getTranslation("outdated_client", bungee.getGameVersion()));
    }

    @Override
    public void handle(LegacyPing ping) throws Exception {
        Preconditions.checkState(!this.legacy, "Not expecting LegacyPing");
        this.legacy = true;

        ServerInfo forced = AbstractReconnectHandler.getForcedHost(this);
        final String motd = (forced != null) ? forced.getMotd() : listener.getLoadmessage();
        final int protocol = bungee.getProtocolVersion();

        XenonCore.instance.getTaskManager().add(() -> {
            Callback<ServerPing> pingBack = (result, error) -> {
                if (error != null) {
                    result = getPingInfo(bungee.getTranslation("ping_cannot_connect"), protocol);
                    bungee.getLogger().log(Level.WARNING, "Error pinging remote server", error);
                }

                Callback<ProxyPingEvent> callback = (result1, error1) -> {
                    final ServerPing legacy = result1.getResponse();
                    if(legacy == null) return;
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

                bungee.getPluginManager().callEvent(new ProxyPingEvent(InitialHandler.this, result, eventLoopCallback(callback)));
            };

            if (forced != null && listener.isPingPassthrough()) ((BungeeServerInfo) forced).ping(pingBack, protocol);
            else pingBack.done(getPingInfo(motd, protocol), null);
        });
    }

    private ServerPing getPingInfo(String motd, int protocol) {
        return new ServerPing(
                new ServerPing.Protocol(bungee.getName() + " " + bungee.getGameVersion(), protocol),
                new ServerPing.Players(listener.getMaxPlayers(), bungee.getOnlineCount(), null),
                motd, BungeeCord.getInstance().config.getFaviconObject()
        );
    }

    @Override
    public void handle(StatusRequest statusRequest) throws Exception {
        Preconditions.checkState(thisState == State.STATUS, "Not expecting STATUS");

        ServerInfo forced = AbstractReconnectHandler.getForcedHost(this);
        final String motd = (forced != null) ? forced.getMotd() : listener.getLoadmessage();
        final int protocol = (ProtocolConstants.SUPPORTED_VERSION_IDS.contains(handshake.getProtocolVersion())) ? handshake.getProtocolVersion() : bungee.getProtocolVersion();

        Callback<ServerPing> pingBack = new Callback<ServerPing>() {
            @Override
            public void done(ServerPing result, Throwable error) {
                if (error != null) {
                    result = getPingInfo(bungee.getTranslation("ping_cannot_connect"), protocol);
                    bungee.getLogger().log(Level.WARNING, "Error pinging remote server", error);
                }

                Callback<ProxyPingEvent> callback = new Callback<ProxyPingEvent>() {
                    @Override
                    public void done(ProxyPingEvent pingResult, Throwable error) {
                        Gson gson = BungeeCord.getInstance().gson;
                        unsafe.sendPacket(new StatusResponse(gson.toJson(pingResult.getResponse())));
                        if (bungee.getConnectionThrottle() != null) {
                            bungee.getConnectionThrottle().unthrottle(getSocketAddress());
                        }
                    }
                };
                bungee.getPluginManager().callEvent(new ProxyPingEvent(InitialHandler.this, result, eventLoopCallback(callback)));
            }
        };

        if (forced != null && listener.isPingPassthrough()) {
            ((BungeeServerInfo) forced).ping(pingBack, handshake.getProtocolVersion());
        } else {
            pingBack.done(getPingInfo(motd, protocol), null);
        }

        thisState = State.PING;
    }

    @Override
    public void handle(PingPacket ping) throws Exception {
        Preconditions.checkState(thisState == State.PING, "Not expecting PING");
        unsafe.sendPacket(ping);
        disconnect("");
    }

    @Override
    public void handle(Handshake handshake) throws Exception {
        Preconditions.checkState(thisState == State.HANDSHAKE && !this.legacy, "Not expecting HANDSHAKE");
        this.handshake = handshake;
        ch.setVersion(handshake.getProtocolVersion());
        ch.getHandle().pipeline().remove(PipelineUtils.LEGACY_KICKER);

        // Starting with FML 1.8, a "\0FML\0" token is appended to the handshake. This interferes
        // with Bungee's IP forwarding, so we detect it, and remove it from the host string, for now.
        // We know FML appends \00FML\00. However, we need to also consider that other systems might
        // add their own data to the end of the string. So, we just take everything from the \0 character
        // and save it for later.
        if (handshake.getHost().contains("\0")) {
            String[] split = handshake.getHost().split("\0", 2);
            handshake.setHost(split[0]);
            extraDataInHandshake = "\0" + split[1];
        }

        // SRV records can end with a . depending on DNS / client.
        if (handshake.getHost().endsWith(".")) {
            handshake.setHost(handshake.getHost().substring(0, handshake.getHost().length() - 1));
        }

        this.virtualHost = InetSocketAddress.createUnresolved(handshake.getHost(), handshake.getPort());

        final PlayerHandshakeEvent event =  new PlayerHandshakeEvent(InitialHandler.this, handshake);
        bungee.getPluginManager().callEvent(event);

        try {
            final String host = event.getConnection().getVirtualHost().getHostString();
            if(!(host != null && host.length() <= 255 && host.matches("[a-zA-Z0-9.-]+")))
                event.setCancelled(true);
        } catch (Exception var3) {
            XenonCore.instance.logdebugerror("Error while handling pre-login event");
            event.setCancelled(true);
        }

        if (ch.isClosing()) {
            return;
        }else if (event.isCancelled()) {
            ch.close();
            return;
        }

        switch (handshake.getRequestedProtocol()) {
            case 1:
                // Ping
                if (bungee.getConfig().isLogPings()) {
                    bungee.getLogger().log(Level.INFO, "{0} has pinged", this);
                }
                thisState = State.STATUS;
                ch.setProtocol(Protocol.STATUS);
                break;
            case 2:
            case 3:
                transferred = handshake.getRequestedProtocol() == 3;
                // Login
                if (BungeeCord.getInstance().getConfig().isLogInitialHandlerConnections()) // Waterfall
                {
                    bungee.getLogger().log(Level.INFO, "{0} has connected", this);
                }
                thisState = State.USERNAME;
                ch.setProtocol(Protocol.LOGIN);

                final PostPlayerHandshakeEvent postEvent =  new PostPlayerHandshakeEvent(InitialHandler.this, handshake);
                bungee.getPluginManager().callEvent(postEvent);
                if(postEvent.isCancelled()){
                    disconnect(postEvent.getReason());
                    return;
                }

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
        Preconditions.checkState(thisState == State.USERNAME && this.loginRequest == null, "Not expecting USERNAME");

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

        int limit = BungeeCord.getInstance().config.getPlayerLimit();
        if (limit > 0 && bungee.getOnlineCount() >= limit) {
            disconnect(bungee.getTranslation("proxy_full"));
            return;
        }

        // If offline mode and they are already on, don't allow connect
        // We can just check by UUID here as names are based on UUID
        if (!isOnlineMode() && bungee.getPlayer(getUniqueId()) != null) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        Callback<PreLoginEvent> callback = new Callback<PreLoginEvent>() {

            @Override
            public void done(PreLoginEvent result, Throwable error) {
                if (result.isCancelled()) {
                    BaseComponent reason = result.getReason();
                    disconnect((reason != null) ? reason : TextComponent.fromLegacy(bungee.getTranslation("kick_message")));
                    return;
                }

                if (onlineMode) {
                    thisState = State.ENCRYPT;
                    unsafe().sendPacket(request = EncryptionUtil.encryptRequest());
                } else {
                    thisState = State.FINISHING;
                    finish();
                }
            }
        };

        // fire pre login event
        bungee.getPluginManager().callEvent(new PreLoginEvent(InitialHandler.this, eventLoopCallback(callback)));
    }

    @Override
    public void handle(EncryptionResponse encryptResponse) throws Exception {
        Preconditions.checkState(thisState == State.ENCRYPT, "Not expecting ENCRYPT");
        Preconditions.checkState(EncryptionUtil.check(loginRequest.getPublicKey(), encryptResponse, request), "Invalid verification");
        thisState = State.FINISHING; // Waterfall - move earlier - There is no verification of this later (and this is not API)

        SecretKey sharedKey = EncryptionUtil.getSecret(encryptResponse, request);
        // Waterfall start
        if (sharedKey instanceof SecretKeySpec) {
            if (sharedKey.getEncoded().length != 16) {
                this.ch.close();
                return;
            }
        }
        // Waterfall end
        BungeeCipher decrypt = EncryptionUtil.getCipher(false, sharedKey);
        ch.addBefore(PipelineUtils.FRAME_DECODER, PipelineUtils.DECRYPT_HANDLER, new CipherDecoder(decrypt));
        BungeeCipher encrypt = EncryptionUtil.getCipher(true, sharedKey);
        ch.addBefore(PipelineUtils.FRAME_PREPENDER, PipelineUtils.ENCRYPT_HANDLER, new CipherEncoder(encrypt));
        // disable use of composite buffers if we use natives
        ch.updateComposite();

        String encName = URLEncoder.encode(InitialHandler.this.getName(), "UTF-8");

        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        for (byte[] bit : new byte[][]
                {
                        request.getServerId().getBytes("ISO_8859_1"), sharedKey.getEncoded(), EncryptionUtil.keys.getPublic().getEncoded()
                }) {
            sha.update(bit);
        }
        String encodedHash = URLEncoder.encode(new BigInteger(sha.digest()).toString(16), "UTF-8");

        String preventProxy = (BungeeCord.getInstance().config.isPreventProxyConnections() && getSocketAddress() instanceof InetSocketAddress) ? "&ip=" + URLEncoder.encode(getAddress().getAddress().getHostAddress(), "UTF-8") : "";
        String authURL = String.format(MOJANG_AUTH_URL, encName, encodedHash, preventProxy);

        Callback<String> handler = new Callback<String>() {
            @Override
            public void done(String result, Throwable error) {
                if (error == null) {
                    LoginResult obj = BungeeCord.getInstance().gson.fromJson(result, LoginResult.class);
                    if (obj != null && obj.getId() != null) {
                        loginProfile = obj;
                        name = obj.getName();
                        uniqueId = Util.getUUID(obj.getId());
                        finish();
                        return;
                    }
                    disconnect(bungee.getTranslation("offline_mode_player"));
                } else {
                    disconnect(bungee.getTranslation("mojang_fail"));
                    bungee.getLogger().log(Level.SEVERE, "Error authenticating " + getName() + " with minecraft.net", error);
                }
            }
        };
        //thisState = State.FINISHING; // Waterfall - move earlier
        HttpClient.get(authURL, ch.getHandle().eventLoop(), handler);
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
                } catch (Exception ignored) {
                }
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
                if (result.isCancelled()) {
                    BaseComponent reason = result.getReason();
                    disconnect((reason != null) ? reason : TextComponent.fromLegacy(bungee.getTranslation("kick_message")));
                    return;
                }

                userCon = new UserConnection(bungee, ch, getName(), InitialHandler.this);
                userCon.setCompressionThreshold(BungeeCord.getInstance().config.getCompressionThreshold());

                if (getVersion() < ProtocolConstants.MINECRAFT_1_20_2) {
                    unsafe.sendPacket(new LoginSuccess(getRewriteId(), getName(), (loginProfile == null) ? null : loginProfile.getProperties()));
                    ch.setProtocol(Protocol.GAME);
                }
                finish2();
            });
        }, this.getLoginProfile())); // Waterfall: Parse LoginResult object to new constructor of LoginEvent
    }

    private void finish2() {
        if (!userCon.init()) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        ch.getHandle().pipeline().get(HandlerBoss.class).setHandler(new UpstreamBridge(bungee, userCon));

        ServerInfo initialServer;
        if (bungee.getReconnectHandler() != null) {
            initialServer = bungee.getReconnectHandler().getServer(userCon);
        } else {
            initialServer = AbstractReconnectHandler.getForcedHost(InitialHandler.this);
        }
        if (initialServer == null) {
            initialServer = bungee.getServerInfo(listener.getDefaultServer());
        }

        Callback<PostLoginEvent> complete = new Callback<PostLoginEvent>() {
            @Override
            public void done(PostLoginEvent result, Throwable error) {
                userCon.connect(result.getTarget(), null, true, ServerConnectEvent.Reason.JOIN_PROXY);
            }
        };
        bungee.getPluginManager().callEvent(new PostLoginEvent(userCon, initialServer, eventLoopCallback(complete)));
    }

    @Override
    public void handle(LoginPayloadResponse response) throws Exception {
        CompletableFuture<byte[]> future;
        synchronized (requestedLoginPayloads) {
            future = requestedLoginPayloads.remove(response.getId());
        }
        Preconditions.checkState(future != null, "Unexpected custom LoginPayloadResponse");
        XenonCore.instance.getTaskManager().async(() -> future.complete(response.getData()));
        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(LoginAcknowledged loginAcknowledged) throws Exception {
        // this packet should only be sent after the login success (it should be handled in the UpstreamBridge)
        disconnect("Unexpected LoginAcknowledged");
    }

    @Override
    public void handle(CookieResponse cookieResponse) {
        // be careful, backend server could also make the client send a cookie response
        CookieFuture future;
        synchronized (requestedCookies) {
            future = requestedCookies.peek();
            if (future != null) {
                if (future.cookie.equals(cookieResponse.getCookie())) {
                    Preconditions.checkState(future == requestedCookies.poll(), "requestedCookies queue mismatch");
                } else {
                    future = null; // leave for handling by backend
                }
            }
        }

        if (future != null) {
            future.getFuture().complete(cookieResponse.getData());

            throw CancelSendSignal.INSTANCE;
        }

        // if there is no userCon we can't have a connection to a backend server that could have requested this cookie
        // which means that this cookie is invalid as the proxy also has not requested it
        Preconditions.checkState(userCon != null, "not requested cookie received");
    }

    @Override
    public void disconnect(String reason) {
        if (canSendKickMessage()) {
            disconnect(TextComponent.fromLegacy(reason));
        } else {
            ch.close();
        }
    }

    @Override
    public void disconnect(BaseComponent... reason) {
        disconnect(TextComponent.fromArray(reason));
    }

    @Override
    public void disconnect(BaseComponent reason) {
        if (canSendKickMessage()) {
            ch.delayedClose(new Kick(reason));
        } else {
            ch.close();
        }
    }

    @Override
    public String getName() {
        return (name != null) ? name : (loginRequest == null) ? null : loginRequest.getData();
    }

    @Override
    public int getVersion() {
        return (handshake == null) ? -1 : handshake.getProtocolVersion();
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
    public Unsafe unsafe() {
        return unsafe;
    }

    @Override
    public void setOnlineMode(boolean onlineMode) {
        Preconditions.checkState(thisState == State.USERNAME, "Can only set online mode status whilst state is username");
        this.onlineMode = onlineMode;
    }

    @Override
    public void setUniqueId(UUID uuid) {
        Preconditions.checkState(thisState == State.USERNAME, "Can only set uuid while state is username");
        Preconditions.checkState(!onlineMode, "Can only set uuid when online mode is false");
        this.uniqueId = uuid;
    }

    @Override
    public String getUUID() {
        return io.github.waterfallmc.waterfall.utils.UUIDUtils.undash(uniqueId.toString()); // Waterfall
    }

    @Override
    public String toString() {
        return "[" + getSocketAddress() + (getName() != null ? "|" + getName() : "") + "] <-> InitialHandler";
    }

    @Override
    public boolean isConnected() {
        return !ch.isClosed();
    }

    public void relayMessage(PluginMessage input) throws Exception {
        if (input.getTag().equals("REGISTER") || input.getTag().equals("minecraft:register")) {
            String content = new String(input.getData(), StandardCharsets.UTF_8);

            for (String id : content.split("\0")) {
                // Waterfall start: Add configurable limits for plugin messaging
                Preconditions.checkState(!(registeredChannels.size() > bungee.getConfig().getPluginChannelLimit()), "Too many registered channels. This limit can be configured in the waterfall.yml");
                Preconditions.checkArgument(!(id.length() > bungee.getConfig().getPluginChannelNameLimit()), "Channel name too long. This limit can be configured in the waterfall.yml");
                // Waterfall end
                registeredChannels.add(id);
            }
        } else if (input.getTag().equals("UNREGISTER") || input.getTag().equals("minecraft:unregister")) {
            String content = new String(input.getData(), StandardCharsets.UTF_8);

            for (String id : content.split("\0")) {
                registeredChannels.remove(id);
            }
        } else if (input.getTag().equals("MC|Brand") || input.getTag().equals("minecraft:brand")) {
            brandMessage = input;
        }
    }

    @Override
    public CompletableFuture<byte[]> retrieveCookie(String cookie) {
        Preconditions.checkState(getVersion() >= ProtocolConstants.MINECRAFT_1_20_5, "Cookies are only supported in 1.20.5 and above");
        Preconditions.checkState(loginRequest != null, "Cannot retrieve cookies for status or legacy connections");

        if (cookie.indexOf(':') == -1) {
            // if we request an invalid resource location (no prefix) the client will respond with "minecraft:" prefix
            cookie = "minecraft:" + cookie;
        }

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        synchronized (requestedCookies) {
            requestedCookies.add(new CookieFuture(cookie, future));
        }
        unsafe.sendPacket(new CookieRequest(cookie));

        return future;
    }

    @Override
    public CompletableFuture<byte[]> sendData(String channel, byte[] data) {
        Preconditions.checkState(getVersion() >= ProtocolConstants.MINECRAFT_1_13, "LoginPayloads are only supported in 1.13 and above");
        Preconditions.checkState(ch.getEncodeProtocol() == Protocol.LOGIN, "LoginPayloads are only supported in the login phase");

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        final int id;
        synchronized (requestedLoginPayloads) {
            // thread safe loginPayloadId
            id = loginPayloadId++;
            requestedLoginPayloads.put(id, future);
        }
        unsafe.sendPacket(new LoginPayloadRequest(id, channel, data));
        return future;
    }

    private <T> Callback<T> eventLoopCallback(Callback<T> callback) {
        return (result, error) ->
        {
            final EventLoop eventLoop = ch.getHandle().eventLoop();
            if (eventLoop.inEventLoop()) {
                if (ch.isClosing()) return;

                callback.done(result, error);
                return;
            }
            eventLoop.execute(() ->
            {
                if (!ch.isClosing()) {
                    callback.done(result, error);
                }
            });
        };
    }

    private enum State {

        HANDSHAKE, STATUS, PING, USERNAME, ENCRYPT, FINISHING
    }

    @Data
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class CookieFuture {

        private String cookie;
        private CompletableFuture<byte[]> future;
    }
}
