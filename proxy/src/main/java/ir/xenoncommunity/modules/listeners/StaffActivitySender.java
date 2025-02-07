package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.DiscordWebhook;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import lombok.Cleanup;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;

public class StaffActivitySender implements Listener {
    private final SQLManager sqlManager;

    public StaffActivitySender() {
        sqlManager = new SQLManager(
                XenonCore.instance.getConfiguration().getSqlStaffActivity(),
                "CREATE TABLE IF NOT EXISTS Players (" +
                        "username TEXT PRIMARY KEY," +
                        "lastjoin BIGINT," +
                        "totalplaytime BIGINT" +
                        ");");
        XenonCore.instance.getBungeeInstance().getPluginManager().registerListener(null, this);
        XenonCore.instance.getTaskManager().async(() -> {
            while(true) {
                LocalTime now = LocalTime.now();
                LocalTime sendTime = LocalTime.of(XenonCore.instance.getConfigData().getStaffactivity().getSendtime(), 0);

                if(now.isBefore(sendTime)) {
                    try {
                        Thread.sleep(Duration.between(now, sendTime).toMillis());
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    try {
                        Arrays.stream(XenonCore.instance.getConfigData().getStaffactivity().getStaffusernames()).forEach(element -> {
                            try {
                                @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                                        "SELECT totalplaytime FROM Players WHERE username = ?;");
                                preparedStatement.setString(1, element);
                                if (preparedStatement.executeQuery().next())
                                    new DiscordWebhook("https://discord.com/api/webhooks/1217899699957399603/LF28kD_6HdfaB7ZezX-RjYYXaz4rf2rKfpVdmRhZ_feups8Rh0skw3mze3YSv31i2rIw")
                                            .setContent(String.format("Staff %s | %sh", element, (((int) sqlManager.getData(element, "totalplaytime"))
                                            / 3600000))).execute();
                            } catch(Exception ignored) {}
                        });
                        Files.delete(XenonCore.instance.getConfiguration().getSqlStaffActivity().toPath());
                        new SQLManager(
                                XenonCore.instance.getConfiguration().getSqlStaffActivity(),
                                "CREATE TABLE IF NOT EXISTS Players (" +
                                        "username TEXT PRIMARY KEY," +
                                        "lastjoin BIGINT," +
                                        "totalplaytime BIGINT" +
                                        ");");
                    } catch (final Exception ex) {
                        XenonCore.instance.getLogger().error(ex.getMessage());
                    }
                }
            }
        });
    }

    @EventHandler
    public void onJoin(final PostLoginEvent event) {
        if(!Arrays.asList(XenonCore.instance.getConfigData().getStaffactivity().getStaffusernames()).contains(event.getPlayer().getName())) return;

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
        if(!Arrays.asList(XenonCore.instance.getConfigData().getStaffactivity().getStaffusernames()).contains(event.getPlayer().getName())) return;

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

/*
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
                                XenonCore.instance.getConfigData().getPlaytime().getPlaytimemessage()
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
                            XenonCore.instance.getConfigData().getPlaytime().getPlaytimeothersmessage()
                                    .replace("PLAYER", playerName)
                                    .replace("PLAYTIME",
                                            String.valueOf((((int) sqlManager.getData(playerName, "totalplaytime")) / 3600000))), false);



            } catch (final Exception ex) {
                XenonCore.instance.getLogger().error(ex.getMessage());
            }
        });
    }*/
}
