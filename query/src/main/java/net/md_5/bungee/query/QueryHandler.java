package net.md_5.bungee.query;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.waterfallmc.waterfall.QueryResult;
import io.github.waterfallmc.waterfall.event.ProxyQueryEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class QueryHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final io.github.waterfallmc.waterfall.utils.FastException cachedNoSessionException = new io.github.waterfallmc.waterfall.utils.FastException("No Session!");
    private final ProxyServer bungee;
    private final ListenerInfo listener;
    /*========================================================================*/
    private final Random random = new Random();
    private final Cache<InetAddress, QuerySession> sessions = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    private void writeShort(ByteBuf buf, int s) {
        buf.writeShortLE(s);
    }

    private void writeNumber(ByteBuf buf, int i) {
        writeString(buf, Integer.toString(i));
    }

    private void writeString(ByteBuf buf, String s) {
        for (char c : s.toCharArray()) {
            buf.writeByte(c);
        }
        buf.writeByte(0x00);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        try {
            handleMessage(ctx, msg);
        } catch (Throwable t) {
            bungee.getLogger().log(Level.WARNING, "Error whilst handling query packet from " + msg.sender(), t);
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, DatagramPacket msg) {
        ByteBuf in = msg.content();
        if (in.readUnsignedByte() != 0xFE || in.readUnsignedByte() != 0xFD) {
            bungee.getLogger().log(Level.WARNING, "Query - Incorrect magic!: {0}", msg.sender());
            return;
        }

        ByteBuf out = ctx.alloc().buffer();
        AddressedEnvelope response = new DatagramPacket(out, msg.sender());

        byte type = in.readByte();
        int sessionId = in.readInt();

        if (type == 0x09) {
            out.writeByte(0x09);
            out.writeInt(sessionId);

            int challengeToken = random.nextInt();
            sessions.put(msg.sender().getAddress(), new QuerySession(challengeToken, System.currentTimeMillis()));

            writeNumber(out, challengeToken);
        }

        if (type == 0x00) {
            int challengeToken = in.readInt();
            QuerySession session = sessions.getIfPresent(msg.sender().getAddress());
            if (session == null || session.getToken() != challengeToken) {
                throw cachedNoSessionException; // Waterfall
            }

            // Waterfall start
            List<String> players = bungee.getPlayers().stream().map(ProxiedPlayer::getName).collect(Collectors.toList());

            String motd = listener.getDefault_bungee_motd();
            motd = ChatColor.translateAlternateColorCodes('&', motd);

            ProxyQueryEvent event = new ProxyQueryEvent(listener, new QueryResult(players, motd, "SMP",
                    "Waterfall_Proxy", bungee.getOnlineCount(), listener.getMaxPlayers(),
                    listener.getHost().getPort(), listener.getHost().getHostString(), "MINECRAFT", bungee.getGameVersion()));
            QueryResult result = bungee.getPluginManager().callEvent(event).getResult();
            // Waterfall end

            out.writeByte(0x00);
            out.writeInt(sessionId);

            if (in.readableBytes() == 0) {
                // Short response
                // Waterfall start
                writeString(out, result.getMotd()); // MOTD
                writeString(out, result.getGameType()); // Game Type
                writeString(out, result.getWorldName()); // World Name
                writeNumber(out, result.getOnlinePlayers()); // Online Count
                writeNumber(out, result.getMaxPlayers()); // Max Players
                writeShort(out, result.getPort()); // Port
                writeString(out, result.getAddress()); // IP
                // Waterfall end
            } else if (in.readableBytes() == 4) {
                // Long Response
                out.writeBytes(new byte[]
                        {
                                0x73, 0x70, 0x6C, 0x69, 0x74, 0x6E, 0x75, 0x6D, 0x00, (byte) 0x80, 0x00
                        });
                Map<String, String> data = new LinkedHashMap<>();

                // Waterfall start
                data.put("hostname", result.getMotd());
                data.put("gametype", result.getGameType());
                // Start Extra Info
                data.put("game_id", result.getGameId());
                data.put("version", result.getVersion());
                data.put("plugins", ""); // TODO: Allow population?
                // End Extra Info
                data.put("map", result.getWorldName());
                data.put("numplayers", Integer.toString(result.getOnlinePlayers()));
                data.put("maxplayers", Integer.toString(result.getMaxPlayers()));
                data.put("hostport", Integer.toString(result.getPort()));
                data.put("hostip", result.getAddress());
                // Waterfall end

                for (Map.Entry<String, String> entry : data.entrySet()) {
                    writeString(out, entry.getKey());
                    writeString(out, entry.getValue());
                }
                out.writeByte(0x00); // Null

                // Padding
                writeString(out, "\01player_\00");
                // Player List
                result.getPlayers().stream().forEach(p -> writeString(out, p)); // Waterfall
                out.writeByte(0x00); // Null
            } else {
                // Error!
                throw new IllegalStateException("Invalid data request packet");
            }
        }

        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        bungee.getLogger().log(Level.WARNING, "Error whilst handling query packet from " + ctx.channel().remoteAddress(), cause);
    }

    @Data
    private static class QuerySession {

        private final int token;
        private final long time;
    }
}
