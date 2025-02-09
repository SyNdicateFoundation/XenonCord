package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import lombok.Cleanup;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.reflections.Reflections;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.util.Arrays;

@SuppressWarnings("unused")
@ModuleListener(isExtended = false, isImplemented = true)
public class PunishManager implements Listener {
    private SQLManager sqlManager;

    public PunishManager() {
        XenonCore.instance.getTaskManager().async(this::initBackend);
        if (XenonCore.instance.getConfigData().getPunishmanager().getMode().equals("LiteBans")) {
            XenonCore.instance.getBungeeInstance().getPluginManager().unregisterListener(this);
            return;
        }

        sqlManager = new SQLManager(XenonCore.instance.getConfiguration().getSqlPunishments(),
                "CREATE TABLE IF NOT EXISTS Players (" +
                        "username TEXT PRIMARY KEY," +
                        "reason TEXT," +
                        "banduration BIGINT," +
                        "muteduration BIGINT," +
                        "lastpunish BIGINT," +
                        "punishadmin TEXT" +
                        ");");

        loadPunishCommands();
    }

    private void loadPunishCommands() {
        new Reflections("ir.xenoncommunity.punishmanager").getSubTypesOf(Command.class).forEach(command -> {
            try {
                Constructor<?> constructor = command.getConstructor(SQLManager.class);
                XenonCore.instance.logdebuginfo(String.format("CMD %s loaded.", command.getSimpleName()));
                XenonCore.instance.getBungeeInstance().pluginManager.registerCommand(null, (Command) constructor.newInstance(sqlManager));
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }

    @EventHandler
    public void onJoin(LoginEvent e) {
        if (XenonCore.instance.getConfigData().getPunishmanager().getMode().equals("LiteBans")) {
            return;
        }

        XenonCore.instance.getTaskManager().add(() -> handleJoinPunishment(e, e.getConnection().getName()));
    }

    private void handleJoinPunishment(LoginEvent e, String username) {
        try {
            final Integer banDuration = (Integer) sqlManager.getData(username, "banduration");
            final Integer lastPunish = (Integer) sqlManager.getData(username, "lastpunish");
            final Integer currentTime = (int) System.currentTimeMillis();
            final String punishAdmin = (String) sqlManager.getData(username, "punishadmin");

            if (banDuration == null || lastPunish == null || punishAdmin == null) {
                return;
            }

            if (banDuration > 0 && currentTime - lastPunish < banDuration) {
                e.setReason(new TextComponent(XenonCore.instance.getConfigData().getPunishmanager().getBandisconnectmessage()
                                .replace("PLAYER", username)
                                .replace("REASON", (String) sqlManager.getData(username, "reason"))
                                .replace("DURATION", String.valueOf(banDuration / 60000))));
                e.setCancelled(true);
                return;
            }

            unbanPlayer(username);
            Message.send(XenonCore.instance.getConfigData().getPunishmanager().getUnbanconsolelogmessage()
                    .replace("PLAYER1", e.getConnection().getName())
                    .replace("PLAYER2", punishAdmin));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void unbanPlayer(String username) throws Exception {
        @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                "DELETE FROM Players WHERE username = ?;");
        preparedStatement.setString(1, username);
        preparedStatement.executeUpdate();
        sqlManager.updateDB(preparedStatement);
    }

    @EventHandler
    public void onChat(ChatEvent e) {
        if (XenonCore.instance.getConfigData().getPunishmanager().getMode().equals("LiteBans")) {
            return;
        }

        if (e.getMessage().startsWith("/") &&
                Arrays.stream(XenonCore.instance.getConfigData().getPunishmanager().getMutecommands())
                        .noneMatch(element -> e.getMessage().split(" ")[0].equals(element))) {
            return;
        }

        final String username = ((CommandSender) e.getSender()).getName();
        try {
            final Integer muteDuration = (Integer) sqlManager.getData(username, "muteduration");
            final Integer lastPunish = (Integer) sqlManager.getData(username, "lastpunish");
            final Integer currentTime = (int) System.currentTimeMillis();
            final String punishAdmin = (String) sqlManager.getData(username, "punishadmin");

            if (muteDuration == null || lastPunish == null || punishAdmin == null) {
                return;
            }

            if (muteDuration > 0 && currentTime - lastPunish < muteDuration) {
                Message.send((CommandSender) e.getSender(), XenonCore.instance.getConfigData().getPunishmanager().getMuteblockmessage()
                                        .replace("PLAYER", username)
                                        .replace("REASON", (String) sqlManager.getData(username, "reason"))
                                        .replace("DURATION", String.valueOf(muteDuration / 60000)),
                        false);
                e.setCancelled(true);
                return;
            }

            unmutePlayer(username);
            Message.send(XenonCore.instance.getConfigData().getPunishmanager().getUnmuteconsolelogmessage()
                    .replace("PLAYER1", username)
                    .replace("PLAYER2", punishAdmin));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void unmutePlayer(String username) throws Exception {
        @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                "DELETE FROM Players WHERE username = ?;");
        preparedStatement.setString(1, username);
        preparedStatement.executeUpdate();
        sqlManager.updateDB(preparedStatement);
    }

    @SneakyThrows
    private void initBackend() {
        @Cleanup final ServerSocket serverSocket = new ServerSocket(20019, 50, InetAddress.getByName("127.0.0.1"));

        while (true) {
            @Cleanup final Socket socket = serverSocket.accept();
            @Cleanup final BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String req;
            while ((req = br.readLine()) != null) {
                XenonCore.instance.getBungeeInstance().getPluginManager().dispatchCommand(
                        XenonCore.instance.getBungeeInstance().getConsole(), req
                );
            }
        }
    }
}
