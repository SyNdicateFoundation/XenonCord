package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
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

public class Playtime extends Command implements Listener {
    private final SQLManager sqlManager;

    public Playtime() {
        super("playtime", XenonCore.instance.getConfigData().getModules().getPlaytimeperm(), "pt");
        sqlManager = new SQLManager(
                XenonCore.instance.getConfiguration().getSqlPlaytime(),
                "CREATE TABLE IF NOT EXISTS Players (" +
                        "username TEXT PRIMARY KEY," +
                        "lastjoin BIGINT," +
                        "totalplaytime BIGINT" +
                        ");");
        XenonCore.instance.getBungeeInstance().getPluginManager().registerListener(null, this);
    }

    @EventHandler
    public void onJoin(final PostLoginEvent event) {
        if (!event.getPlayer().hasPermission(XenonCore.instance.getConfigData().getModules().getPlaytimeperm())) return;

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
            } catch (final Exception ex) {
                XenonCore.instance.getLogger().error(ex.getMessage());
            }
        });
    }

    @EventHandler
    public void onLeave(final PlayerDisconnectEvent event) {
        if (!event.getPlayer().hasPermission(XenonCore.instance.getConfigData().getModules().getPlaytimeperm())) return;

        XenonCore.instance.getTaskManager().add(() -> {
            try {
                final String username = event.getPlayer().getName();
                final long currentTime = System.currentTimeMillis();

                @Cleanup final PreparedStatement selectStatement = sqlManager.getConnection().prepareStatement(
                        "SELECT lastjoin, totalplaytime FROM Players WHERE username = ?;");
                selectStatement.setString(1, username);

                if (selectStatement.executeQuery().next()) {
                    @Cleanup final PreparedStatement updateStatement = sqlManager.getConnection().prepareStatement(
                            "UPDATE Players SET totalplaytime = ?, lastjoin = ? WHERE username = ?;");
                    updateStatement.setLong(1, ((int) sqlManager.getData(username, "totalplaytime"))
                            + (currentTime - (long) sqlManager.getData(username, "lastjoin")));
                    updateStatement.setLong(2, currentTime);
                    updateStatement.setString(3, username);
                    sqlManager.updateDB(updateStatement);
                }
            } catch (final Exception ex) {
                XenonCore.instance.getLogger().error(ex.getMessage());
            }
        });
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        XenonCore.instance.getTaskManager().add(() -> {
            try {
                String playerName = sender.getName();
                @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                        "SELECT totalplaytime FROM Players WHERE username = ?;");
                if(args.length == 0){
                    preparedStatement.setString(1, playerName);
                    if (preparedStatement.executeQuery().next())
                        Message.send(sender,
                                XenonCore.instance.getConfigData().getModules().getPlaytimemessage()
                                        .replace("PLAYTIME",
                                                String.valueOf((((int) sqlManager.getData(playerName, "totalplaytime"))
                                                        / 3600000))),
                                false);
                    return;
                }
                playerName = args[0];

                preparedStatement.setString(1, playerName);
                if (preparedStatement.executeQuery().next())
                    Message.send(sender,
                            XenonCore.instance.getConfigData().getModules().getPlaytimemessage()
                                    .replace("PLAYER", playerName)
                                    .replace("PLAYTIME",
                                            String.valueOf((((int) sqlManager.getData(playerName, "totalplaytime")) / 3600000))), false);



            } catch (final Exception ex) {
                XenonCore.instance.getLogger().error(ex.getMessage());
            }
        });
    }
}
