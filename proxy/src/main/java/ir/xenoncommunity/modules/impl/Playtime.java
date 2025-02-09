package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import lombok.Cleanup;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = true)
public class Playtime extends Command implements Listener {
    private final SQLManager sqlManager;

    public Playtime() {
        super("playtime", XenonCore.instance.getConfigData().getPlaytime().getPlaytimeperm(), "pt");
        sqlManager = new SQLManager(
                XenonCore.instance.getConfiguration().getSqlPlaytime(),
                "CREATE TABLE IF NOT EXISTS Players (" +
                        "username TEXT PRIMARY KEY," +
                        "lastjoin BIGINT," +
                        "totalplaytime BIGINT" +
                        ");");
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        if (!event.getPlayer().hasPermission(XenonCore.instance.getConfigData().getPlaytime().getPlaytimeperm())) {
            return;
        }

        XenonCore.instance.getTaskManager().add(() -> {
            try {
                final long currentTime = System.currentTimeMillis();
                @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                        "INSERT INTO Players (username, lastjoin, totalplaytime) " +
                                "VALUES (?, ?, ?) " +
                                "ON CONFLICT (username) DO UPDATE SET lastjoin = ?;");
                preparedStatement.setString(1, event.getPlayer().getName());
                preparedStatement.setLong(2, currentTime);
                preparedStatement.setLong(3, 0);
                preparedStatement.setLong(4, currentTime);
                sqlManager.updateDB(preparedStatement);
            } catch (Exception ex) {
                XenonCore.instance.getLogger().error(ex.getMessage());
            }
        });
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        if (!event.getPlayer().hasPermission(XenonCore.instance.getConfigData().getPlaytime().getPlaytimeperm())) {
            return;
        }

        XenonCore.instance.getTaskManager().add(() -> {
            try {
                final String username = event.getPlayer().getName();
                final long currentTime = System.currentTimeMillis();

                @Cleanup final PreparedStatement selectStatement = sqlManager.getConnection().prepareStatement(
                        "SELECT lastjoin, totalplaytime FROM Players WHERE username = ?;");
                selectStatement.setString(1, username);

                if (selectStatement.executeQuery().next())
                    updatePlaytime(username, currentTime);
            } catch (Exception ex) {
                XenonCore.instance.getLogger().error(ex.getMessage());
            }
        });
    }

    private void updatePlaytime(String username, long currentTime) throws Exception {
        @Cleanup final PreparedStatement updateStatement = sqlManager.getConnection().prepareStatement(
                "UPDATE Players SET totalplaytime = ?, lastjoin = ? WHERE username = ?;");
        updateStatement.setLong(1, (int) sqlManager.getData(username, "totalplaytime") +
                (currentTime - (long) sqlManager.getData(username, "lastjoin")));
        updateStatement.setLong(2, currentTime);
        updateStatement.setString(3, username);
        sqlManager.updateDB(updateStatement);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        XenonCore.instance.getTaskManager().add(() -> {
            try {
                sendPlaytimeMessage(sender, args.length == 0 ? sender.getName() : args[0]);
            } catch (Exception ex) {
                XenonCore.instance.getLogger().error(ex.getMessage());
            }
        });
    }

    private void sendPlaytimeMessage(CommandSender sender, String playerName) throws Exception {
        @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                "SELECT totalplaytime FROM Players WHERE username = ?;");
        preparedStatement.setString(1, playerName);

        if (preparedStatement.executeQuery().next()) {
            final long totalPlaytime = (long) sqlManager.getData(playerName, "totalplaytime");

            Message.send(sender, (sender.getName().equals(playerName))
                    ? XenonCore.instance.getConfigData().getPlaytime().getPlaytimemessage()
                    .replace("PLAYTIME", String.valueOf(totalPlaytime / 3600000))
                    : XenonCore.instance.getConfigData().getPlaytime().getPlaytimeothersmessage()
                    .replace("PLAYER", playerName)
                    .replace("PLAYTIME", String.valueOf(totalPlaytime / 3600000)), false);
        } else {
            Message.send(sender, "Player not found.", false);
        }
    }
}
