package net.md_5.bungee.connection;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.unix.DomainSocketAddress;
import ir.xenoncommunity.XenonCore;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.ServerConnection.KeepAliveData;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.score.Team;
import net.md_5.bungee.api.score.*;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.entitymap.EntityMap;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.*;
import net.md_5.bungee.tab.TabList;

import java.io.DataInput;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@SuppressWarnings({"deprecation", "rawtypes"})
public class DownstreamBridge extends PacketHandler
{

    // #3246: Recent versions of MinecraftForge alter Vanilla behaviour and require a command so that the executable flag is set
    // If the flag is not set, then the command will appear and successfully tab complete, but cannot be successfully executed

    private static final com.mojang.brigadier.Command DUMMY_COMMAND = (context) ->
            0;
    //
    private final ProxyServer bungee;
    private final UserConnection con;
    private final ServerConnection server;
    private boolean receivedLogin;

    @Override
    public void exception(Throwable t) throws Exception {
        if (server.isObsolete()) return;

        ServerInfo nextServer;
        try {
            CompletableFuture<ServerInfo> future = new CompletableFuture<>();
            con.updateAndGetNextServer(server.getInfo(), (result, error) -> {
                if (error != null) {
                    System.err.println("Error while updating and getting the next server: " + error.getMessage());
                    future.completeExceptionally(error);
                } else {
                    future.complete(result);
                }
            });

            nextServer = future.get();

        } catch (Exception e) {
            e.printStackTrace();
            nextServer = null;
        }

        ServerKickEvent event = new ServerKickEvent(
                con,
                server.getInfo(),
                TextComponent.fromLegacyText(bungee.getTranslation("server_went_down")),
                nextServer,
                ServerKickEvent.State.CONNECTED,
                ServerKickEvent.Cause.EXCEPTION
        );

        bungee.getPluginManager().callEvent(event);

        if (event.isCancelled() && event.getCancelServer() != null) {
            server.setObsolete(true);
            con.connectNow(event.getCancelServer(), ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
        } else {
            if (nextServer != null) {
                server.setObsolete(true);
                con.connectNow(nextServer, ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
                con.sendMessage(bungee.getTranslation("server_went_down", nextServer.getName()));
            } else {
                con.disconnect0(event.getReason());
            }
        }

    }


    @Override
    public void disconnected(ChannelWrapper channel) {
        server.getInfo().removePlayer(con);

        if (bungee.getReconnectHandler() != null) {
            bungee.getReconnectHandler().setServer(con);
        }

        ServerDisconnectEvent serverDisconnectEvent = new ServerDisconnectEvent(con, server.getInfo());
        bungee.getPluginManager().callEvent(serverDisconnectEvent);

        if (server.isObsolete())
            return;

        ServerInfo nextServer;
        try {
            CompletableFuture<ServerInfo> future = new CompletableFuture<>();
            con.updateAndGetNextServer(server.getInfo(), (result, error) -> {
                if (error != null) {
                    System.err.println("Error while updating and getting the next server: " + error.getMessage());
                    future.completeExceptionally(error);
                } else {
                    future.complete(result);
                }
            });

            nextServer = future.get();

        } catch (Exception e) {
            e.printStackTrace();
            nextServer = null;
        }

        ServerKickEvent event = new ServerKickEvent(
                con,
                server.getInfo(),
                TextComponent.fromLegacyText(bungee.getTranslation("lost_connection")),
                nextServer,
                ServerKickEvent.State.CONNECTED,
                ServerKickEvent.Cause.LOST_CONNECTION
        );

        bungee.getPluginManager().callEvent(event);

        if (event.isCancelled() && event.getCancelServer() != null) {
            server.setObsolete(true);
            con.connectNow(event.getCancelServer());
        } else {
            if (nextServer != null) {
                server.setObsolete(true);
                con.connectNow(nextServer, ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
                con.sendMessage(bungee.getTranslation("server_went_down", nextServer.getName()));
            } else {
                con.disconnect0(event.getReason());
            }
        }
    }


    @Override
    public boolean shouldHandle(PacketWrapper packet) {
        return !server.isObsolete();
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        EntityMap rewrite = con.getEntityRewrite();
        if ( rewrite != null && con.getCh().getEncodeProtocol() == Protocol.GAME )
            rewrite.rewriteClientbound( packet.buf, con.getServerEntityId(), con.getClientEntityId(), con.getPendingConnection().getVersion() );
        con.sendPacket( packet );
    }

    @Override
    public void handle(KeepAlive alive) throws Exception
    {
        int timeout = bungee.getConfig().getTimeout();
        if ( timeout <= 0 || server.getKeepAlives().size() < timeout / 50 ) // Some people disable timeout, otherwise allow a theoretical maximum of 1 keepalive per tick
            server.getKeepAlives().add( new KeepAliveData( alive.getRandomId(), System.currentTimeMillis() ) );
    }

    @Override
    public void handle(PlayerListItem playerList) throws Exception
    {
        //Waterfall start
        boolean skipRewrites = bungee.getConfig().isDisableTabListRewrite();
        con.getTabListHandler().onUpdate( skipRewrites ? playerList : TabList.rewrite( playerList ) );
        if ( !skipRewrites )
            throw CancelSendSignal.INSTANCE; // Only throw if profile rewriting is enabled
        // Waterfall end
    }

    @Override
    public void handle(PlayerListItemRemove playerList) throws Exception
    {
        con.getTabListHandler().onUpdate( TabList.rewrite( playerList ) );
        throw CancelSendSignal.INSTANCE; // Always throw because of profile rewriting
    }

    @Override
    public void handle(PlayerListItemUpdate playerList) throws Exception
    {
        con.getTabListHandler().onUpdate( TabList.rewrite( playerList ) );
        throw CancelSendSignal.INSTANCE; // Always throw because of profile rewriting
    }

    @Override
    public void handle(ScoreboardObjective objective) throws Exception
    {
        Scoreboard serverScoreboard = con.getServerSentScoreboard();
        switch ( objective.getAction() )
        {
            case 0:
                serverScoreboard.addObjective( new Objective( objective.getName(), ( objective.getValue().isLeft() ) ? objective.getValue().getLeft() : ComponentSerializer.toString( objective.getValue().getRight() ), objective.getType().toString() ) );
                break;
            case 1:
                serverScoreboard.removeObjective( objective.getName() );
                break;
            case 2:
                Objective oldObjective = serverScoreboard.getObjective( objective.getName() );
                if ( oldObjective != null )
                {
                    oldObjective.setValue( ( objective.getValue().isLeft() ) ? objective.getValue().getLeft() : ComponentSerializer.toString( objective.getValue().getRight() ) );
                    oldObjective.setType( objective.getType().toString() );
                }
                break;
            default:
                throw new IllegalArgumentException( "Unknown objective action: " + objective.getAction() );
        }
    }

    @Override
    public void handle(ScoreboardScore score) throws Exception
    {
        Scoreboard serverScoreboard = con.getServerSentScoreboard();
        switch ( score.getAction() )
        {
            case 0:
                Score s = new Score( score.getItemName(), score.getScoreName(), score.getValue() );
                serverScoreboard.removeScore( score.getItemName() );
                serverScoreboard.addScore( s );
                break;
            case 1:
                serverScoreboard.removeScore( score.getItemName() );
                break;
            default:
                throw new IllegalArgumentException( "Unknown scoreboard action: " + score.getAction() );
        }
    }

    @Override
    public void handle(ScoreboardScoreReset scoreboardScoreReset) throws Exception
    {
        Scoreboard serverScoreboard = con.getServerSentScoreboard();

        // TODO: Expand score API to handle objective values. Shouldn't matter currently as only used for removing score entries.
        if ( scoreboardScoreReset.getScoreName() == null )
            serverScoreboard.removeScore( scoreboardScoreReset.getItemName() );
    }

    @Override
    public void handle(ScoreboardDisplay displayScoreboard) throws Exception
    {
        Scoreboard serverScoreboard = con.getServerSentScoreboard();
        serverScoreboard.setName( displayScoreboard.getName() );
        serverScoreboard.setPosition( Position.values()[displayScoreboard.getPosition()] );
    }

    @Override
    public void handle(net.md_5.bungee.protocol.packet.Team team) throws Exception {
        final Scoreboard serverScoreboard = con.getServerSentScoreboard();
        final String teamName = team.getName();

        if (team.getMode() == 1) {
            serverScoreboard.removeTeam(teamName);
            return;
        }

        Team t = (team.getMode() == 0) ? new Team(teamName) : serverScoreboard.getTeam(teamName);

        if (team.getMode() == 0)
            serverScoreboard.addTeam(t);

        if (t == null) return;

        if (team.getMode() == 0 || team.getMode() == 2) {
            t.setDisplayName(team.getDisplayName().getLeftOrCompute(ComponentSerializer::toString));
            t.setPrefix(team.getPrefix().getLeftOrCompute(ComponentSerializer::toString));
            t.setSuffix(team.getSuffix().getLeftOrCompute(ComponentSerializer::toString));
            t.setFriendlyFire(team.getFriendlyFire());
            t.setNameTagVisibility(team.getNameTagVisibility());
            t.setCollisionRule(team.getCollisionRule());
            t.setColor(team.getColor());
        }

        if (team.getPlayers() == null) return;

        XenonCore.instance.getTaskManager().add(() -> {
            Arrays.stream(team.getPlayers()).forEach(s -> {
                if (team.getMode() == 0 || team.getMode() == 3)
                    t.addPlayer(s);
                else if (team.getMode() == 4)
                    t.removePlayer(s);
            });
        });
    }

    @Override
    @SuppressWarnings("checkstyle:avoidnestedblocks")
    public void handle(PluginMessage pluginMessage) throws Exception {
        final PluginMessageEvent event = new PluginMessageEvent(server, con, pluginMessage.getTag(), pluginMessage.getData().clone());
        final String tag = pluginMessage.getTag();
        final int protocolVersion = con.getPendingConnection().getVersion();
        final DataInput in = pluginMessage.getStream();
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        final String subChannel = in.readUTF();
        final String channel = in.readUTF();
        final short len = in.readShort();
        final byte[] data = new byte[len];
        XenonCore.instance.getTaskManager().async(() -> {
            try {
                if (bungee.getPluginManager().callEvent(event).isCancelled())
                    throw CancelSendSignal.INSTANCE;

                if (tag.equals(protocolVersion >= ProtocolConstants.MINECRAFT_1_13 ? "minecraft:brand" : "MC|Brand")) {
                    final ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
                    DefinedPacket.writeString(XenonCore.instance.getConfigData().getIngamebrandname(), brand);
                    pluginMessage.setData(brand);
                    brand.release();
                    con.unsafe().sendPacket(pluginMessage);
                    throw CancelSendSignal.INSTANCE;
                }

                if (tag.equals("BungeeCord")) {

                    switch (subChannel) {
                        case "ForwardToPlayer": {
                            ProxiedPlayer target = bungee.getPlayer(in.readUTF());
                            if (target != null) {
                                in.readFully(data);
                                out.writeUTF(channel);
                                out.writeShort(len);
                                out.write(data);
                                byte[] payload = out.toByteArray();

                                target.getServer().sendData("BungeeCord", payload);
                            }
                            break;
                        }
                        case "Forward": {
                            String target = in.readUTF();
                            in.readFully(data);
                            out.writeUTF(channel);
                            out.writeShort(len);
                            out.write(data);
                            byte[] payload = out.toByteArray();

                            switch (target) {
                                case "ALL":
                                case "ONLINE":
                                    boolean online = target.equals("ONLINE");
                                    for (ServerInfo server : bungee.getServers().values()) {
                                        if (server != this.server.getInfo()) {
                                            server.sendData("BungeeCord", payload, online);
                                        }
                                    }
                                    break;
                                default:
                                    ServerInfo server = bungee.getServerInfo(target);
                                    if (server != null) {
                                        server.sendData("BungeeCord", payload);
                                    }
                                    break;
                            }
                            break;
                        }
                        case "Connect": {
                            ServerInfo server = bungee.getServerInfo(in.readUTF());
                            if (server != null) {
                                con.connect(server, ServerConnectEvent.Reason.PLUGIN_MESSAGE);
                            }
                            break;
                        }
                        case "ConnectOther": {
                            ProxiedPlayer player = bungee.getPlayer(in.readUTF());
                            if (player != null) {
                                ServerInfo server = bungee.getServerInfo(in.readUTF());
                                if (server != null) {
                                    player.connect(server);
                                }
                            }
                            break;
                        }
                        case "GetPlayerServer": {
                            String name = in.readUTF();
                            ProxiedPlayer player = bungee.getPlayer(name);
                            out.writeUTF("GetPlayerServer");
                            out.writeUTF(name);
                            out.writeUTF(player == null || player.getServer() == null ? "" : player.getServer().getInfo().getName());
                            break;
                        }
                        case "IP": {
                            out.writeUTF("IP");
                            if (con.getSocketAddress() instanceof InetSocketAddress) {
                                out.writeUTF(con.getAddress().getHostString());
                                out.writeInt(con.getAddress().getPort());
                            } else {
                                out.writeUTF("unix://" + ((DomainSocketAddress) con.getSocketAddress()).path());
                                out.writeInt(0);
                            }
                            break;
                        }
                        case "IPOther": {
                            ProxiedPlayer player = bungee.getPlayer(in.readUTF());
                            if (player != null) {
                                out.writeUTF("IPOther");
                                out.writeUTF(player.getName());
                                if (player.getSocketAddress() instanceof InetSocketAddress) {
                                    InetSocketAddress address = (InetSocketAddress) player.getSocketAddress();
                                    out.writeUTF(address.getHostString());
                                    out.writeInt(address.getPort());
                                } else {
                                    out.writeUTF("unix://" + ((DomainSocketAddress) player.getSocketAddress()).path());
                                    out.writeInt(0);
                                }
                            }
                            break;
                        }
                        case "PlayerCount": {
                            String target = in.readUTF();
                            out.writeUTF("PlayerCount");
                            if (target.equals("ALL")) {
                                out.writeUTF("ALL");
                                out.writeInt(bungee.getOnlineCount());
                            } else {
                                ServerInfo server = bungee.getServerInfo(target);
                                if (server != null) {
                                    out.writeUTF(server.getName());
                                    out.writeInt(server.getPlayers().size());
                                }
                            }
                            break;
                        }
                        case "PlayerList": {
                            String target = in.readUTF();
                            out.writeUTF("PlayerList");
                            if (target.equals("ALL")) {
                                out.writeUTF("ALL");
                                out.writeUTF(Util.csv(bungee.getPlayers()));
                            } else {
                                ServerInfo server = bungee.getServerInfo(target);
                                if (server != null) {
                                    out.writeUTF(server.getName());
                                    out.writeUTF(Util.csv(server.getPlayers()));
                                }
                            }
                            break;
                        }
                        case "GetServers": {
                            out.writeUTF("GetServers");
                            out.writeUTF(Util.csv(bungee.getServers().keySet()));
                            break;
                        }
                        case "Message": {
                            String target = in.readUTF();
                            String message = in.readUTF();
                            if (target.equals("ALL")) {
                                for (ProxiedPlayer player : bungee.getPlayers()) {
                                    player.sendMessage(message);
                                }
                            } else {
                                ProxiedPlayer player = bungee.getPlayer(target);
                                if (player != null) {
                                    player.sendMessage(message);
                                }
                            }
                            break;
                        }
                        case "MessageRaw": {
                            String target = in.readUTF();
                            BaseComponent[] message = ComponentSerializer.parse(in.readUTF());
                            if (target.equals("ALL")) {
                                for (ProxiedPlayer player : bungee.getPlayers()) {
                                    player.sendMessage(message);
                                }
                            } else {
                                ProxiedPlayer player = bungee.getPlayer(target);
                                if (player != null) {
                                    player.sendMessage(message);
                                }
                            }
                            break;
                        }
                        case "GetServer": {
                            out.writeUTF("GetServer");
                            out.writeUTF(server.getInfo().getName());
                            break;
                        }
                        case "UUID": {
                            out.writeUTF("UUID");
                            out.writeUTF(con.getUUID());
                            break;
                        }
                        case "UUIDOther": {
                            ProxiedPlayer player = bungee.getPlayer(in.readUTF());
                            if (player != null) {
                                out.writeUTF("UUIDOther");
                                out.writeUTF(player.getName());
                                out.writeUTF(player.getUUID());
                            }
                            break;
                        }
                        case "ServerIP": {
                            ServerInfo info = bungee.getServerInfo(in.readUTF());
                            if (info != null && !info.getAddress().isUnresolved()) {
                                out.writeUTF("ServerIP");
                                out.writeUTF(info.getName());
                                out.writeUTF(info.getAddress().getAddress().getHostAddress());
                                out.writeShort(info.getAddress().getPort());
                            }
                            break;
                        }
                        case "KickPlayer": {
                            ProxiedPlayer player = bungee.getPlayer(in.readUTF());
                            if (player != null) {
                                String kickReason = in.readUTF();
                                player.disconnect(new TextComponent(kickReason));
                            }
                            break;
                        }
                        case "KickPlayerRaw": {
                            ProxiedPlayer player = bungee.getPlayer(in.readUTF());
                            if (player != null) {
                                BaseComponent[] kickReason = ComponentSerializer.parse(in.readUTF());
                                player.disconnect(kickReason);
                            }
                            break;
                        }
                    }

                    byte[] response = out.toByteArray();
                    if (response.length != 0) {
                        server.sendData("BungeeCord", response);
                    }
                }
            } catch(Exception ignored) {
            }
        });
        throw CancelSendSignal.INSTANCE;
    }



    @Override
    public void handle(Kick kick) throws Exception {
        //  XenonCore.instance.getTaskManager().add(() -> {
        ServerInfo nextServer;
        try {
            Future<ServerInfo> future = new CompletableFuture<>();
            con.updateAndGetNextServer(server.getInfo(), (result, error) -> {
                if (error != null) {
                    System.err.println("Error while updating and getting the next server: " + error.getMessage());
                    ((CompletableFuture<ServerInfo>) future).completeExceptionally(error);
                } else {
                    ((CompletableFuture<ServerInfo>) future).complete(result);
                }
            });

            nextServer = future.get();
            if (server.getInfo().equals(nextServer)) {
                nextServer = null;
            }

            ServerKickEvent event = new ServerKickEvent(
                    con,
                    server.getInfo(),
                    new BaseComponent[]{kick.getMessage()},
                    nextServer,
                    ServerKickEvent.State.CONNECTED,
                    ServerKickEvent.Cause.SERVER
            );

            bungee.getPluginManager().callEvent(event);

            if (event.isCancelled() && event.getCancelServer() != null) {
                con.connectNow(event.getCancelServer(), ServerConnectEvent.Reason.KICK_REDIRECT);
            } else {
                con.disconnect(event.getKickReasonComponent());
            }

            server.setObsolete(true);

        } catch (Exception e) {
            e.printStackTrace();
            con.disconnect(new BaseComponent[]{kick.getMessage()});
        }
        // });

        throw CancelSendSignal.INSTANCE;
    }



    @Override
    public void handle(SetCompression setCompression) throws Exception
    {
        server.getCh().setCompressionThreshold( setCompression.getThreshold() );
    }

    @Override
    public void handle(TabCompleteResponse tabCompleteResponse) throws Exception {
        List<String> commands = tabCompleteResponse.getCommands() != null
                ? tabCompleteResponse.getCommands()
                : tabCompleteResponse.getSuggestions().getList().stream()
                .map(Suggestion::getText)
                .collect(Collectors.toList());

        String last = con.getLastCommandTabbed();
        if (last != null) {
            String commandName = last.toLowerCase(Locale.ROOT);

            List<String> matchingCommands = bungee.getPluginManager().getCommands().stream()
                    .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).startsWith(commandName)
                            && entry.getValue().hasPermission(con)
                            && !bungee.getDisabledCommands().contains(entry.getKey().toLowerCase(Locale.ROOT)))
                    .map(entry -> '/' + entry.getKey())
                    .sorted()
                    .collect(Collectors.toList());

            commands.addAll(matchingCommands);
            con.setLastCommandTabbed(null);
        }

        TabCompleteResponseEvent tabCompleteResponseEvent = new TabCompleteResponseEvent(server, con, new ArrayList<>(commands));
        if (!bungee.getPluginManager().callEvent(tabCompleteResponseEvent).isCancelled()) {
            List<String> newSuggestions = tabCompleteResponseEvent.getSuggestions();

            if (!commands.equals(newSuggestions)) {
                if (tabCompleteResponse.getCommands() != null) {
                    tabCompleteResponse.setCommands(newSuggestions);
                } else {
                    StringRange range = tabCompleteResponse.getSuggestions().getRange();
                    List<Suggestion> suggestions = newSuggestions.stream()
                            .map(input -> new Suggestion(range, input))
                            .collect(Collectors.toList());
                    tabCompleteResponse.setSuggestions(new Suggestions(range, suggestions));
                }
            }

            con.unsafe().sendPacket(tabCompleteResponse);
        }

        throw CancelSendSignal.INSTANCE;
    }


    @Override
    public void handle(BossBar bossBar)
    {
        if(bossBar.getAction() == 0)
            con.getSentBossBars().add(bossBar.getUuid());
        else
            con.getSentBossBars().remove( bossBar.getUuid() );
    }

    // Waterfall start
    @Override
    public void handle(net.md_5.bungee.protocol.packet.EntityEffect entityEffect) throws Exception
    {
        if (con.isDisableEntityMetadataRewrite()) return;
        if (this.con.getForgeClientHandler().isForgeUser() && !this.con.getForgeClientHandler().isHandshakeComplete())
            throw CancelSendSignal.INSTANCE;
        con.getPotions().put(rewriteEntityId(entityEffect.getEntityId()), entityEffect.getEffectId());
    }

    @Override
    public void handle(net.md_5.bungee.protocol.packet.EntityRemoveEffect removeEffect) throws Exception
    {
        if (con.isDisableEntityMetadataRewrite()) return;
        con.getPotions().remove(rewriteEntityId(removeEffect.getEntityId()), removeEffect.getEffectId());
    }

    private int rewriteEntityId(int entityId) {
        if (entityId == con.getServerEntityId())
            return con.getClientEntityId();
        return entityId;
    }
    // Waterfall end

    @Override
    public void handle(Respawn respawn)
    {
        con.setDimension( respawn.getDimension() );
    }

    @Override
    public void handle(Commands commands) throws Exception
    {

        // Waterfall start
        Map<String, Command> commandMap = new java.util.HashMap<>();
        XenonCore.instance.getTaskManager().async(() -> {
            boolean modified = false;

            bungee.getPluginManager().getCommands().forEach((commandEntry) -> {
                if (!bungee.getDisabledCommands().contains(commandEntry.getKey())
                        && commands.getRoot().getChild(commandEntry.getKey()) == null
                        && commandEntry.getValue().hasPermission(this.con)) {
                    commandMap.put(commandEntry.getKey(), commandEntry.getValue());
                }
            });


            io.github.waterfallmc.waterfall.event.ProxyDefineCommandsEvent event = new io.github.waterfallmc.waterfall.event.ProxyDefineCommandsEvent( this.server, this.con, commandMap );

            bungee.getPluginManager().callEvent( event );

            for (Map.Entry<String, Command> command : event.getCommands().entrySet()) {
                CommandNode dummy = LiteralArgumentBuilder.literal(command.getKey()).executes(DUMMY_COMMAND)
                        .then(RequiredArgumentBuilder.argument("args", StringArgumentType.greedyString())
                                .suggests(Commands.SuggestionRegistry.ASK_SERVER).executes(DUMMY_COMMAND))
                        .build();
                commands.getRoot().addChild(dummy);

                modified = true;
            }

            if ( !modified ) return;

            con.unsafe().sendPacket( commands );
        });
        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(ServerData serverData) throws Exception
    {
        // 1.19.4 doesn't allow empty MOTD and we probably don't want to simulate a ping event to get the "correct" one
        // serverData.setMotd( null );
        // serverData.setIcon( null );
        // con.unsafe().sendPacket( serverData );
        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(Login login) throws Exception
    {
        Preconditions.checkState( !receivedLogin, "Not expecting login" );

        receivedLogin = true;
        ServerConnector.handleLogin( bungee, server.getCh(), con, server.getInfo(), null, server, login );

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public String toString()
    {
        return "[" + con.getAddress() + "|" + con.getName() + "] <-> DownstreamBridge <-> [" + server.getInfo().getName() + "]";
    }
}