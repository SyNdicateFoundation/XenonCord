package net.md_5.bungee.connection;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import io.github.waterfallmc.waterfall.conf.WaterfallConfiguration;
import io.github.waterfallmc.waterfall.forwarding.ForwardingMode;
import io.github.waterfallmc.waterfall.forwarding.VelocityForwardingUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ir.xenoncommunity.XenonCore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.score.Scoreboard;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.forge.ForgeServerHandler;
import net.md_5.bungee.forge.ForgeUtils;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.*;
import net.md_5.bungee.protocol.packet.*;
import net.md_5.bungee.util.AddressUtil;
import net.md_5.bungee.util.BufUtil;
import net.md_5.bungee.util.QuietException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

@RequiredArgsConstructor
public class ServerConnector extends PacketHandler {

    private final ProxyServer bungee;
    private final UserConnection user;
    private final BungeeServerInfo target;
    private ChannelWrapper ch;
    private State thisState = State.LOGIN_SUCCESS;
    @Getter
    private ForgeServerHandler handshakeHandler;
    private boolean obsolete;

    private boolean didForwardInformation = false; // Waterfall: Forwarding rework

    public static void handleLogin(ProxyServer bungee, ChannelWrapper ch, UserConnection user, BungeeServerInfo target, ForgeServerHandler handshakeHandler, ServerConnection server, Login login) throws Exception {
        if (server.isForgeServer() && user.isForgeUser()) {
            ((net.md_5.bungee.protocol.MinecraftDecoder) server.getCh().getHandle().pipeline().get(net.md_5.bungee.netty.PipelineUtils.PACKET_DECODER)).setSupportsForge(true);
            ((net.md_5.bungee.protocol.MinecraftDecoder) user.getCh().getHandle().pipeline().get(net.md_5.bungee.netty.PipelineUtils.PACKET_DECODER)).setSupportsForge(true);
        }

        bungee.getPluginManager().callEvent(new ServerConnectedEvent(user, server));

        ch.write(BungeeCord.getInstance().registerChannels(user.getPendingConnection().getVersion()));
        final Queue<DefinedPacket> packetQueue = target.getPacketQueue();

        synchronized (packetQueue) {
            XenonCore.instance.getTaskManager().async(() -> {
                while (!packetQueue.isEmpty()) {
                    ch.write(packetQueue.poll());
                }
            });
        }

        final PluginMessage brandMessage = user.getPendingConnection().getBrandMessage();
        if (brandMessage != null) {
            ch.write(brandMessage);
        }

        Set<String> registeredChannels = user.getPendingConnection().getRegisteredChannels();
        if (!registeredChannels.isEmpty()) {
            ch.write(new PluginMessage(user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13 ? "minecraft:register" : "REGISTER", Joiner.on("\0").join(registeredChannels).getBytes(StandardCharsets.UTF_8), false));
        }

        // Something deeper is going wrong here.
        if (user.getSettings() != null && (!user.isDisableEntityMetadataRewrite() || user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_2)) {
            ch.write(user.getSettings());
        }

        if (user.getForgeClientHandler().getClientModList() == null && !user.getForgeClientHandler().isHandshakeComplete()) // Vanilla
        {
            user.getForgeClientHandler().setHandshakeComplete();
        }

        if (user.getServer() == null || user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_16) {
            // Once again, first connection
            user.setClientEntityId(login.getEntityId());
            user.setServerEntityId(login.getEntityId());

            // Set tab list size, TODO: what shall we do about packet mutability
            user.unsafe().sendPacket(new Login(login.getEntityId(), login.isHardcore(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(),
                    (byte) user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.getSimulationDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isLimitedCrafting(), login.isDebug(), login.isFlat(), login.getDeathLocation(),
                    login.getPortalCooldown(), login.getSeaLevel(), login.isSecureProfile()));

            if (user.getDimension() != null) {
                user.getTabListHandler().onServerChange();

                user.getServerSentScoreboard().clear();


                user.getSentBossBars().forEach(bossbar -> {
                    user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.BossBar(bossbar, 1));
                });
                user.getSentBossBars().clear();

                user.unsafe().sendPacket(new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), (byte) 0, login.getDeathLocation(),
                        login.getPortalCooldown(), login.getSeaLevel()));
            } else {
                user.unsafe().sendPacket(BungeeCord.getInstance().registerChannels(user.getPendingConnection().getVersion()));

                final ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
                DefinedPacket.writeString(bungee.getName() + " (" + bungee.getVersion() + ")", brand);
                user.unsafe().sendPacket(new PluginMessage(user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13 ? "minecraft:brand" : "MC|Brand", brand, handshakeHandler != null && handshakeHandler.isServerForge())); // Waterfall
                brand.release();
            }

            user.setDimension(login.getDimension());
        } else {
            user.getServer().setObsolete(true);
            user.getTabListHandler().onServerChange();

            final Scoreboard serverScoreboard = user.getServerSentScoreboard();
            if (!user.isDisableEntityMetadataRewrite()) { // Waterfall
                serverScoreboard.getObjectives().forEach(objective -> {
                    user.unsafe().sendPacket(new ScoreboardObjective(
                            objective.getName(),
                            (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13) ? Either.right(ComponentSerializer.deserialize(objective.getValue())) : Either.left(objective.getValue()),
                            ScoreboardObjective.HealthDisplay.fromString(objective.getType()),
                            (byte) 1, null)
                    );
                });
                serverScoreboard.getScores().forEach(score -> {
                    if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_3) {
                        user.unsafe().sendPacket(new ScoreboardScoreReset(score.getItemName(), null));
                    } else {
                        user.unsafe().sendPacket(new ScoreboardScore(score.getItemName(), (byte) 1, score.getScoreName(), score.getValue(), null, null));
                    }
                });
                serverScoreboard.getTeams().forEach(team -> user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.Team(team.getName())));
            } // Waterfall
            serverScoreboard.clear();

            user.getSentBossBars().forEach(bossbar -> user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.BossBar(bossbar, 1)));
            user.getSentBossBars().clear();

            user.unsafe().sendPacket(new EntityStatus(user.getClientEntityId(), login.isReducedDebugInfo() ? EntityStatus.DEBUG_INFO_REDUCED : EntityStatus.DEBUG_INFO_NORMAL));
            // And immediate respawn
            if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_15) {
                user.unsafe().sendPacket(new GameState(GameState.IMMEDIATE_RESPAWN, login.isNormalRespawn() ? 0 : 1));
            }

            user.setDimensionChange(true);
            if (!user.isDisableEntityMetadataRewrite() && login.getDimension() == user.getDimension()) // Waterfall - defer
                user.unsafe().sendPacket(new Respawn((Integer) login.getDimension() >= 0 ? -1 : 0, login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(),
                        (byte) 0, login.getDeathLocation(), login.getPortalCooldown(), login.getSeaLevel()));

            user.setServerEntityId(login.getEntityId());

            // Waterfall start
            if (user.isDisableEntityMetadataRewrite()) {
                // Ensure that we maintain consistency
                user.setClientEntityId(login.getEntityId());
                // Only send if we are not in the same dimension
                if (login.getDimension() != user.getDimension()) // Waterfall - defer
                {
                    user.unsafe().sendPacket(new Respawn((Integer) user.getDimension() >= 0 ? -1 : 0, login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), (byte) 0, login.getDeathLocation(), login.getPortalCooldown(), login.getSeaLevel()));
                }
                user.unsafe().sendPacket(new Login(login.getEntityId(), login.isHardcore(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(),
                        (byte) user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.getSimulationDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isLimitedCrafting(), login.isDebug(), login.isFlat(), login.getDeathLocation(),
                        login.getPortalCooldown(), login.getSeaLevel(), login.isSecureProfile()));
                // Only send if we're in the same dimension
                if (login.getDimension() == user.getDimension()) // Waterfall - defer
                {
                    user.unsafe().sendPacket(new Respawn((Integer) login.getDimension() >= 0 ? -1 : 0, login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), (byte) 0, login.getDeathLocation(), login.getPortalCooldown(), login.getSeaLevel()));
                }
            }
            // Waterfall end
            user.unsafe().sendPacket(new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(),
                    (byte) 0, login.getDeathLocation(), login.getPortalCooldown(), login.getSeaLevel()));
            if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_14) {
                user.unsafe().sendPacket(new ViewDistance(login.getViewDistance()));
            }
            user.setDimension(login.getDimension());
        }
    }

    @Override
    public void exception(Throwable t) throws Exception {
        if (obsolete) {
            return;
        }

        final String message = ChatColor.RED + "Exception Connecting: " + Util.exception(t);
        if (user.getServer() == null) {
            user.disconnect(message);
        } else {
            user.sendMessage(message);
        }
    }

    @Override
    public void connected(ChannelWrapper channel) throws Exception {
        this.ch = channel;

        this.handshakeHandler = new ForgeServerHandler(user, ch, target);
        final Handshake originalHandshake = user.getPendingConnection().getHandshake();
        final Handshake copiedHandshake = new Handshake(originalHandshake.getProtocolVersion(), originalHandshake.getHost(), originalHandshake.getPort(), 2);

        if (BungeeCord.getInstance().config.isIpForward() && user.getSocketAddress() instanceof InetSocketAddress) {
            String newHost = copiedHandshake.getHost() + "\00" + AddressUtil.sanitizeAddress(user.getAddress()) + "\00" + user.getUUID();

            final LoginResult profile = user.getPendingConnection().getLoginProfile();

            // Handle properties.
            net.md_5.bungee.protocol.Property[] properties = new net.md_5.bungee.protocol.Property[0];

            if (profile != null && profile.getProperties() != null && profile.getProperties().length > 0)
                properties = profile.getProperties();

            // Waterfall start: Forwarding rework
            if (BungeeCord.getInstance().config.getForwardingMode() == ForwardingMode.BUNGEEGUARD) {
                List<Property> temp = new ArrayList<>(Arrays.asList(properties));
                temp.add(new Property("bungeeguard-token", new String(
                        ((WaterfallConfiguration) BungeeCord.getInstance().config).getForwardingSecret(),
                        StandardCharsets.UTF_8
                ), null));
                properties = temp.toArray(new Property[0]);
            }
            // Waterfall end: Forwarding rework

            if (user.getForgeClientHandler().isFmlTokenInHandshake()) {
                // Get the current properties and copy them into a slightly bigger array.
                final net.md_5.bungee.protocol.Property[] newp = Arrays.copyOf(properties, properties.length + 2);

                // Add a new profile property that specifies that this user is a Forge user.
                newp[newp.length - 2] = new net.md_5.bungee.protocol.Property(ForgeConstants.FML_LOGIN_PROFILE, "true", null);

                // If we do not perform the replacement, then the IP Forwarding code in Spigot et. al. will try to split on this prematurely.
                newp[newp.length - 1] = new net.md_5.bungee.protocol.Property(ForgeConstants.EXTRA_DATA, user.getExtraDataInHandshake().replaceAll("\0", "\1"), "");

                // All done.
                properties = newp;
            }

            // If we touched any properties, then append them
            if (properties.length > 0) {
                newHost += "\00" + BungeeCord.getInstance().gson.toJson(properties);
            }

            copiedHandshake.setHost(newHost);
        } else if (!user.getExtraDataInHandshake().isEmpty()) {
            copiedHandshake.setHost(copiedHandshake.getHost() + user.getExtraDataInHandshake());
        }

        channel.write(copiedHandshake);

        channel.setProtocol(Protocol.LOGIN);
        channel.write(new LoginRequest(user.getName(), null, user.getRewriteId()));
    }

    @Override
    public void disconnected(ChannelWrapper channel) {
        user.getPendingConnects().remove(target);

        if (user.getServer() == null && !obsolete && user.getPendingConnects().isEmpty() && thisState == State.LOGIN_SUCCESS) {
            // this is called if we get disconnected but not have received any response after we send the handshake
            // in this case probably an exception was thrown because the handshake could not be read correctly
            // because of the extra ip forward data, also we skip the disconnect if another server is also in the
            // pendingConnects queue because we don't want to lose the player
            user.disconnect("Unexpected disconnect during server login, did you forget to enable BungeeCord / IP forwarding on your server?");
        }
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception {
        if (packet.packet == null) {
            throw new QuietException("Unexpected packet received during server login process!\n" + BufUtil.dump(packet.buf, 16));
        }
    }

    @Override
    public void handle(LoginSuccess loginSuccess) throws Exception {
        // Waterfall start: Forwarding rework
        if ( !didForwardInformation && BungeeCord.getInstance().config.isIpForward()
                && BungeeCord.getInstance().config.getForwardingMode() == ForwardingMode.VELOCITY_MODERN) {
            throw new QuietException(VelocityForwardingUtil.MODERN_IP_FORWARDING_FAILURE);
        }
        // Waterfall end: Forwarding rework
        Preconditions.checkState(thisState == State.LOGIN_SUCCESS, "Not expecting LOGIN_SUCCESS");
        if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_2) {
            cutThrough(new ServerConnection(ch, target));
        } else {
            ch.setProtocol(Protocol.GAME);
            thisState = State.LOGIN;
        }

        if (user.getServer() != null && user.getForgeClientHandler().isHandshakeComplete()
                && user.getServer().isForgeServer()) {
            user.getForgeClientHandler().resetHandshake();
        }

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(SetCompression setCompression) throws Exception {
        ch.setCompressionThreshold(setCompression.getThreshold());
    }

    @Override
    public void handle(CookieRequest cookieRequest) throws Exception {
        user.retrieveCookie(cookieRequest.getCookie()).thenAccept((cookie) -> ch.write(new CookieResponse(cookieRequest.getCookie(), cookie)));
    }

    @Override
    public void handle(Login login) throws Exception {
        Preconditions.checkState(thisState == State.LOGIN, "Not expecting LOGIN");

        final ServerConnection server = new ServerConnection(ch, target);
        handleLogin(bungee, ch, user, target, handshakeHandler, server, login);
        cutThrough(server);
    }

    private void cutThrough(ServerConnection server) {
        // TODO: Fix this?
        if (!user.isActive()) {
            server.disconnect("Quitting");
            bungee.getLogger().log(Level.WARNING, "[{0}] No client connected for pending server!", user);
            return;
        }

        if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_2) {
            if (user.getServer() != null) {
                // Begin config mode
                if (user.getCh().getEncodeProtocol() != Protocol.CONFIGURATION) {
                    user.unsafe().sendPacket(new StartConfiguration());
                }
            } else {
                final LoginResult loginProfile = user.getPendingConnection().getLoginProfile();
                user.unsafe().sendPacket(new LoginSuccess(user.getRewriteId(), user.getName(), (loginProfile == null) ? null : loginProfile.getProperties()));
                user.getCh().setEncodeProtocol(Protocol.CONFIGURATION);
            }
        }

        // Remove from old servers
        if (user.getServer() != null) {
            user.getServer().disconnect("Quitting");
        }

        ServerInfo from = (user.getServer() == null) ? null : user.getServer().getInfo();
        // Add to new serverwhat
        // TODO: Move this to the connected() method of DownstreamBridge
        target.addPlayer(user);
        user.getPendingConnects().remove(target);
        user.setServerJoinQueue(null);
        user.setDimensionChange(false);

        user.setServer(server);
        ch.getHandle().pipeline().get(HandlerBoss.class).setHandler(new DownstreamBridge(bungee, user, server));
        bungee.getPluginManager().callEvent(new ServerSwitchEvent(user, from));
        thisState = State.FINISHED;

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(EncryptionRequest encryptionRequest) throws Exception {
        throw new QuietException("Server is online mode!");
    }

    @Override
    public void handle(Kick kick) throws Exception {
        final ServerInfo def = user.updateAndGetNextServer(target);
        final ServerKickEvent event = new ServerKickEvent(user, target, new BaseComponent[]
                {
                        kick.getMessage()
                }, def, ServerKickEvent.State.CONNECTING, ServerKickEvent.Cause.SERVER);  // Waterfall );
        if (event.getKickReason().toLowerCase(Locale.ROOT).contains("outdated") && def != null) {
            // Pre cancel the event if we are going to try another server
            event.setCancelled(true);
        }
        bungee.getPluginManager().callEvent(event);
        if (event.isCancelled() && event.getCancelServer() != null) {
            obsolete = true;
            user.connect(event.getCancelServer(), ServerConnectEvent.Reason.KICK_REDIRECT);
            throw CancelSendSignal.INSTANCE;
        }

        String message = bungee.getTranslation("connect_kick", target.getName(), event.getKickReason());
        if (user.isDimensionChange()) {
            user.disconnect(message);
        } else {
            user.sendMessage(message);
        }

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception {
        if (BungeeCord.getInstance().config.isForgeSupport()) {
            if (pluginMessage.getTag().equals(ForgeConstants.FML_REGISTER)) {
                boolean isForgeServer = false;
                for (String channel : ForgeUtils.readRegisteredChannels(pluginMessage)) {
                    if (channel.equals(ForgeConstants.FML_HANDSHAKE_TAG)) {
                        // If we have a completed handshake and we have been asked to register a FML|HS
                        // packet, let's send the reset packet now. Then, we can continue the message sending.
                        // The handshake will not be complete if we reset this earlier.
                        if (user.getServer() != null && user.getForgeClientHandler().isHandshakeComplete()) {
                            user.getForgeClientHandler().resetHandshake();
                        }

                        isForgeServer = true;
                        break;
                    }
                }

                if (isForgeServer && !this.handshakeHandler.isServerForge()) {
                    // We now set the server-side handshake handler for the client to this.
                    handshakeHandler.setServerAsForgeServer();
                    user.setForgeServerHandler(handshakeHandler);
                }
            }

            if (pluginMessage.getTag().equals(ForgeConstants.FML_HANDSHAKE_TAG) || pluginMessage.getTag().equals(ForgeConstants.FORGE_REGISTER)) {
                this.handshakeHandler.handle(pluginMessage);

                // We send the message as part of the handler, so don't send it here.
                throw CancelSendSignal.INSTANCE;
            }
        }

        // We have to forward these to the user, especially with Forge as stuff might break
        // This includes any REGISTER messages we intercepted earlier.
        user.unsafe().sendPacket(pluginMessage);
    }

    @Override
    public void handle(LoginPayloadRequest loginPayloadRequest) {
        // Waterfall start: Forwarding rework
        if ( !didForwardInformation && BungeeCord.getInstance().config.isIpForward()
                && BungeeCord.getInstance().config.getForwardingMode() == ForwardingMode.VELOCITY_MODERN
                && loginPayloadRequest.getChannel().equals(VelocityForwardingUtil.VELOCITY_IP_FORWARDING_CHANNEL)) {

            byte[] forwardingData = VelocityForwardingUtil
                    .writeForwardingData(user.getAddress().getAddress().getHostAddress(),
                            user.getName(), user.getUniqueId(),
                            user.getPendingConnection().getLoginProfile().getProperties());
            ch.write(new LoginPayloadResponse(loginPayloadRequest.getId(), forwardingData));
            didForwardInformation = true;
            return;
        }
        // Waterfall end: Forwarding rework
        ch.write(new LoginPayloadResponse(loginPayloadRequest.getId(), null));
    }

    @Override
    public String toString() {
        return "[" + user.getName() + "|" + user.getAddress() + "] <-> ServerConnector [" + target.getName() + "]";
    }

    private enum State {

        LOGIN_SUCCESS, LOGIN, FINISHED;
    }
}
