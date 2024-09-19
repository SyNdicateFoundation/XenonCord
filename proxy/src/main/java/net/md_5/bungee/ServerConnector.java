package net.md_5.bungee;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ir.xenoncommunity.XenonCore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.score.Objective;
import net.md_5.bungee.api.score.Score;
import net.md_5.bungee.api.score.Scoreboard;
import net.md_5.bungee.api.score.Team;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.CancelSendSignal;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.connection.LoginResult;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;

@RequiredArgsConstructor
public class ServerConnector extends PacketHandler
{

    private final ProxyServer bungee;
    private ChannelWrapper ch;
    private final UserConnection user;
    private final BungeeServerInfo target;
    private State thisState = State.LOGIN_SUCCESS;
    @Getter
    private ForgeServerHandler handshakeHandler;
    private boolean obsolete;

    private enum State
    {

        LOGIN_SUCCESS, LOGIN, FINISHED
    }

    @Override
    public void exception(Throwable t) throws Exception
    {
        if ( obsolete )
            return;

        String message = ChatColor.RED + "Exception Connecting: " + Util.exception( t );
        if ( user.getServer() == null ) user.disconnect( message );
        else user.sendMessage( message );
    }

    @Override
    public void connected(ChannelWrapper channel) throws Exception {
        this.ch = channel;
        this.handshakeHandler = new ForgeServerHandler(user, ch, target);

        final Handshake originalHandshake = user.getPendingConnection().getHandshake();
        final Handshake copiedHandshake = new Handshake(
                originalHandshake.getProtocolVersion(),
                originalHandshake.getHost(),
                originalHandshake.getPort(),
                2
        );;

        if (BungeeCord.getInstance().config.isIpForward() && user.getSocketAddress() instanceof InetSocketAddress) {
            final StringBuilder newHost = new StringBuilder()
                    .append(copiedHandshake.getHost())
                    .append("\00")
                    .append(AddressUtil.sanitizeAddress(user.getAddress()))
                    .append("\00")
                    .append(user.getUUID());

            final LoginResult profile = user.getPendingConnection().getLoginProfile();
            net.md_5.bungee.protocol.Property[] properties = (profile != null && profile.getProperties() != null) ? profile.getProperties() : new net.md_5.bungee.protocol.Property[0];

            if (user.getForgeClientHandler().isFmlTokenInHandshake()) {
                properties = Arrays.copyOf(properties, properties.length + 2);
                properties[properties.length - 2] = new net.md_5.bungee.protocol.Property(ForgeConstants.FML_LOGIN_PROFILE, "true", null);
                properties[properties.length - 1] = new net.md_5.bungee.protocol.Property(ForgeConstants.EXTRA_DATA, user.getExtraDataInHandshake().replace("\0", "\1"), "");
            }

            if (properties.length > 0)
                newHost.append("\00").append(BungeeCord.getInstance().gson.toJson(properties));

            copiedHandshake.setHost(newHost.toString());
        } else if (!user.getExtraDataInHandshake().isEmpty())
            copiedHandshake.setHost(copiedHandshake.getHost() + user.getExtraDataInHandshake());

        channel.write(copiedHandshake);
        channel.setProtocol(Protocol.LOGIN);
        channel.write(new LoginRequest(user.getName(), null, user.getRewriteId()));
    }


    @Override
    public void disconnected(ChannelWrapper channel) throws Exception
    {
        user.getPendingConnects().remove( target );
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        if ( packet.packet == null )
            throw new QuietException( "Unexpected packet received during server login process!\n" + BufUtil.dump( packet.buf, 16 ) );
    }

    @Override
    public void handle(LoginSuccess loginSuccess) throws Exception
    {
        Preconditions.checkState( thisState == State.LOGIN_SUCCESS, "Not expecting LOGIN_SUCCESS" );
        if ( user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_2 )
        {
            cutThrough(  new ServerConnection( ch, target ) );
        } else
        {
            ch.setProtocol( Protocol.GAME );
            thisState = State.LOGIN;
        }

        if ( user.getServer() != null && user.getForgeClientHandler().isHandshakeComplete()
                && user.getServer().isForgeServer() )
            user.getForgeClientHandler().resetHandshake();

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(SetCompression setCompression) throws Exception
    {
        ch.setCompressionThreshold( setCompression.getThreshold() );
    }

    @Override
    public void handle(CookieRequest cookieRequest) throws Exception
    {
        user.retrieveCookie( cookieRequest.getCookie() ).thenAccept( (cookie) -> ch.write( new CookieResponse( cookieRequest.getCookie(), cookie ) ) );
    }

    @Override
    public void handle(Login login) throws Exception
    {
        Preconditions.checkState( thisState == State.LOGIN, "Not expecting LOGIN" );

        ServerConnection server = new ServerConnection( ch, target );
        handleLogin( bungee, ch, user, target, handshakeHandler, server, login );
        cutThrough( server );
    }

    public static void handleLogin(ProxyServer bungee, ChannelWrapper ch, UserConnection user, BungeeServerInfo target, ForgeServerHandler handshakeHandler, ServerConnection server, Login login) throws Exception {
        ServerConnectedEvent event = new ServerConnectedEvent(user, server);

        if (server.isForgeServer() && user.isForgeUser()) {
            ((net.md_5.bungee.protocol.MinecraftDecoder) server.getCh().getHandle().pipeline().get(net.md_5.bungee.netty.PipelineUtils.PACKET_DECODER)).setSupportsForge(true);
            ((net.md_5.bungee.protocol.MinecraftDecoder) user.getCh().getHandle().pipeline().get(net.md_5.bungee.netty.PipelineUtils.PACKET_DECODER)).setSupportsForge(true);
        }

        bungee.getPluginManager().callEvent(event);
        ch.write(BungeeCord.getInstance().registerChannels(user.getPendingConnection().getVersion()));

        Queue<DefinedPacket> packetQueue = target.getPacketQueue();
        synchronized (packetQueue) {
            XenonCore.instance.getTaskManager().async(() -> {
                while (!packetQueue.isEmpty()) {
                    ch.write(packetQueue.poll());
                }
            });
        }

        PluginMessage brandMessage = user.getPendingConnection().getBrandMessage();
        if (brandMessage != null)
            ch.write(brandMessage);

        Set<String> registeredChannels = user.getPendingConnection().getRegisteredChannels();
        if (!registeredChannels.isEmpty())
            ch.write(new PluginMessage(user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13 ? "minecraft:register" : "REGISTER", Joiner.on("\0").join(registeredChannels).getBytes(StandardCharsets.UTF_8), false));

        if (user.getSettings() != null && (!user.isDisableEntityMetadataRewrite() || user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_2))
            ch.write(user.getSettings());

        if (user.getForgeClientHandler().getClientModList() == null && !user.getForgeClientHandler().isHandshakeComplete())
            user.getForgeClientHandler().setHandshakeComplete();

        if (user.getServer() == null || user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_16) {
            user.setClientEntityId(login.getEntityId());
            user.setServerEntityId(login.getEntityId());

            Login modLogin = new Login(login.getEntityId(), login.isHardcore(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(),
                    (byte) user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.getSimulationDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isLimitedCrafting(), login.isDebug(), login.isFlat(), login.getDeathLocation(),
                    login.getPortalCooldown(), login.isSecureProfile());

            user.unsafe().sendPacket(modLogin);
            if (user.getDimension() != null) {
                user.getTabListHandler().onServerChange();
                user.getServerSentScoreboard().clear();

                user.getSentBossBars().forEach(bossbar -> user.unsafe().sendPacket(new BossBar(bossbar, 1)));
                user.getSentBossBars().clear();

                user.unsafe().sendPacket(new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), (byte) 0, login.getDeathLocation(),
                        login.getPortalCooldown()));
            } else {
                user.unsafe().sendPacket(BungeeCord.getInstance().registerChannels(user.getPendingConnection().getVersion()));

                ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
                DefinedPacket.writeString(bungee.getName() + " (" + bungee.getVersion() + ")", brand);
                user.unsafe().sendPacket(new PluginMessage(user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13 ? "minecraft:brand" : "MC|Brand", brand, handshakeHandler != null && handshakeHandler.isServerForge()));
                brand.release();
            }
        } else {
            user.getServer().setObsolete(true);
            user.getTabListHandler().onServerChange();
            Scoreboard serverScoreboard = user.getServerSentScoreboard();

            XenonCore.instance.getTaskManager().async(() -> {
                if (!user.isDisableEntityMetadataRewrite()) {
                    serverScoreboard.getObjectives().forEach(objective -> user.unsafe().sendPacket(new ScoreboardObjective(
                            objective.getName(),
                            (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13)
                                    ? Either.right(ComponentSerializer.deserialize(objective.getValue()))
                                    : Either.left(objective.getValue()),
                            ScoreboardObjective.HealthDisplay.fromString(objective.getType()),
                            (byte) 1, null
                    )));

                    serverScoreboard.getScores().forEach(score -> {
                        if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_3) {
                            user.unsafe().sendPacket(new ScoreboardScoreReset(score.getItemName(), null));
                        } else {
                            user.unsafe().sendPacket(new ScoreboardScore(
                                    score.getItemName(),
                                    (byte) 1,
                                    score.getScoreName(),
                                    score.getValue(),
                                    null,
                                    null
                            ));
                        }
                    });

                    serverScoreboard.getTeams().forEach(team -> user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.Team(team.getName())));
                }

                serverScoreboard.clear();

                user.getSentBossBars().forEach(bossbar -> user.unsafe().sendPacket(new BossBar(bossbar, 1)));

                user.getSentBossBars().clear();
            });



            user.unsafe().sendPacket(new EntityStatus(user.getClientEntityId(), login.isReducedDebugInfo() ? EntityStatus.DEBUG_INFO_REDUCED : EntityStatus.DEBUG_INFO_NORMAL));

            if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_15)
                user.unsafe().sendPacket(new GameState(GameState.IMMEDIATE_RESPAWN, login.isNormalRespawn() ? 0 : 1));

            user.setDimensionChange(true);

            if (!user.isDisableEntityMetadataRewrite() && login.getDimension().equals(user.getDimension()))
                user.unsafe().sendPacket(new Respawn((Integer) login.getDimension() >= 0 ? -1 : 0, login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(),
                        (byte) 0, login.getDeathLocation(), login.getPortalCooldown()));

            user.setServerEntityId(login.getEntityId());

            if (user.isDisableEntityMetadataRewrite()) {
                user.setClientEntityId(login.getEntityId());

                if (!login.getDimension().equals(user.getDimension()))
                    user.unsafe().sendPacket(new Respawn((Integer) user.getDimension() >= 0 ? -1 : 0, login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), (byte) 0, login.getDeathLocation(), login.getPortalCooldown()));

                Login modLogin = new Login(login.getEntityId(), login.isHardcore(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(),
                        (byte) user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.getSimulationDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isLimitedCrafting(), login.isDebug(), login.isFlat(), login.getDeathLocation(),
                        login.getPortalCooldown(), login.isSecureProfile());
                user.unsafe().sendPacket(modLogin);

                if (login.getDimension().equals(user.getDimension()))
                    user.unsafe().sendPacket(new Respawn((Integer) login.getDimension() >= 0 ? -1 : 0, login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), (byte) 0, login.getDeathLocation(), login.getPortalCooldown()));
            }

            user.unsafe().sendPacket(new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), (byte) 0, login.getDeathLocation(), login.getPortalCooldown()));

            if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_14)
                user.unsafe().sendPacket(new ViewDistance(login.getViewDistance()));

        }
        user.setDimension(login.getDimension());
    }


    private void cutThrough(ServerConnection server)
    {
        if ( !user.isActive() )
        {
            server.disconnect( "Quitting" );
            return;
        }

        if ( user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_20_2 )
        {
            if ( user.getServer() != null )
            {
                // Begin config mode
                user.unsafe().sendPacket( new StartConfiguration() );
            } else
            {
                LoginResult loginProfile = user.getPendingConnection().getLoginProfile();
                user.unsafe().sendPacket( new LoginSuccess( user.getRewriteId(), user.getName(), ( loginProfile == null ) ? null : loginProfile.getProperties() ) );
                user.getCh().setEncodeProtocol( Protocol.CONFIGURATION );
            }
        }

        // Remove from old servers
        if ( user.getServer() != null )
        {
            user.getServer().setObsolete( true );
            user.getServer().disconnect( "Quitting" );
        }

        // Add to new server
        // TODO: Move this to the connected() method of DownstreamBridge
        target.addPlayer( user );
        user.getPendingConnects().remove( target );
        user.setServerJoinQueue( null );
        user.setDimensionChange( false );

        ServerInfo from = ( user.getServer() == null ) ? null : user.getServer().getInfo();
        user.setServer( server );
        ch.getHandle().pipeline().get( HandlerBoss.class ).setHandler( new DownstreamBridge( bungee, user, server ) );

        bungee.getPluginManager().callEvent( new ServerSwitchEvent( user, from ) );

        thisState = State.FINISHED;

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(EncryptionRequest encryptionRequest) throws Exception
    {
        throw new QuietException( "Server is online mode!" );
    }

    @Override
    public void handle(Kick kick) throws Exception {
        // XenonCore.instance.getTaskManager().add(() -> {
        ServerInfo nextServer;
        try {
            final Future<ServerInfo> future = new CompletableFuture<>();
            user.updateAndGetNextServer(target, (result, error) -> {
                if (error != null) {
                    System.err.println("Error while updating and getting next server: " + error.getMessage());
                    ((CompletableFuture<ServerInfo>) future).completeExceptionally(error);
                } else {
                    ((CompletableFuture<ServerInfo>) future).complete(result);
                }
            });

            nextServer = future.get();

        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        ServerKickEvent event = new ServerKickEvent(user, target, new BaseComponent[] { kick.getMessage() }, nextServer, ServerKickEvent.State.CONNECTING, ServerKickEvent.Cause.SERVER);

        if (event.getKickReason().toLowerCase(Locale.ROOT).contains("outdated") && nextServer != null)
            event.setCancelled(true);

        bungee.getPluginManager().callEvent(event);

        if (event.isCancelled() && event.getCancelServer() != null) {
            obsolete = true;
            user.connect(event.getCancelServer(), ServerConnectEvent.Reason.KICK_REDIRECT);
            throw CancelSendSignal.INSTANCE;
        }

        final String message = bungee.getTranslation("connect_kick", target.getName(), event.getKickReason());
        if (user.isDimensionChange())
            user.disconnect(message);
        else
            user.sendMessage(message);


        throw CancelSendSignal.INSTANCE;
        //     });
    }
    @Override
    public void handle(PluginMessage pluginMessage) throws Exception
    {
        if ( BungeeCord.getInstance().config.isForgeSupport() )
        {
            if ( pluginMessage.getTag().equals( ForgeConstants.FML_REGISTER ) )
            {
                Set<String> channels = ForgeUtils.readRegisteredChannels( pluginMessage );
                XenonCore.instance.getTaskManager().async(() -> {
                    boolean isForgeServer = false;
                    for ( String channel : channels )
                    {
                        if ( channel.equals( ForgeConstants.FML_HANDSHAKE_TAG ) )
                        {
                            // If we have a completed handshake and we have been asked to register a FML|HS
                            // packet, let's send the reset packet now. Then, we can continue the message sending.
                            // The handshake will not be complete if we reset this earlier.
                            if ( user.getServer() != null && user.getForgeClientHandler().isHandshakeComplete() )
                                user.getForgeClientHandler().resetHandshake();

                            isForgeServer = true;
                            break;
                        }
                    }

                    if ( isForgeServer && !this.handshakeHandler.isServerForge() )
                    {
                        // We now set the server-side handshake gui for the client to this.
                        handshakeHandler.setServerAsForgeServer();
                        user.setForgeServerHandler( handshakeHandler );
                    }
                });
            }

            if ( pluginMessage.getTag().equals( ForgeConstants.FML_HANDSHAKE_TAG ) || pluginMessage.getTag().equals( ForgeConstants.FORGE_REGISTER ) )
            {
                this.handshakeHandler.handle( pluginMessage );

                // We send the message as part of the gui, so don't send it here.
                throw CancelSendSignal.INSTANCE;
            }
        }

        // We have to forward these to the user, especially with Forge as stuff might break
        // This includes any REGISTER messages we intercepted earlier.
        user.unsafe().sendPacket( pluginMessage );
    }

    @Override
    public void handle(LoginPayloadRequest loginPayloadRequest)
    {
        ch.write( new LoginPayloadResponse( loginPayloadRequest.getId(), null ) );
    }

    @Override
    public String toString()
    {
        return "[" + user.getName() + "|" + user.getAddress() + "] <-> ServerConnector [" + target.getName() + "]";
    }
}