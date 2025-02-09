package ir.xenoncommunity.punishmanager;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import lombok.Cleanup;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.util.Arrays;

@SuppressWarnings("unused")
public class BanCommand extends Command {
    private final SQLManager sqlManager;

    public BanCommand(SQLManager sqlManagerIn) {
        super("ban", XenonCore.instance.getConfigData().getPunishmanager().getBanperm());
        this.sqlManager = sqlManagerIn;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Message.send(sender,
                    XenonCore.instance.getConfigData().getUnknownoptionmessage()
                            .replace("OPTIONS", "player, reason, duration in minute"), false);
            return;
        }

        if (args[1].toLowerCase().equals("console")) {
            Message.send(sender,
                    XenonCore.instance.getConfigData().getCannotexecasconsoleerrormessage(), false);
            return;
        }

        final String playerName = args[0];
        final int banDuration = Integer.parseInt(args[1]) == 0 ?
                60000 : Integer.parseInt(args[1]) * 60000;

        final StringBuilder sb = new StringBuilder();

        Arrays.stream(args).forEach(word -> sb.append(word).append(" "));

        final String reason = sb.toString().replace(args[0], "").replace(args[1], "").substring(2);

        XenonCore.instance.getTaskManager().add(() -> {
            try {
                @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                        "INSERT INTO Players (username, reason, banduration, muteduration, lastpunish, punishadmin) " +
                                "VALUES (?, ?, ?, 0, ?, ?) " +
                                "ON CONFLICT (username) DO UPDATE SET banduration = EXCLUDED.banduration," +
                                "reason = EXCLUDED.reason," +
                                "lastpunish = EXCLUDED.lastpunish," +
                                "punishadmin = EXCLUDED.punishadmin;");
                preparedStatement.setString(1, playerName);
                preparedStatement.setString(2, reason);
                preparedStatement.setInt(3, banDuration);
                preparedStatement.setInt(4, (int) System.currentTimeMillis());
                preparedStatement.setString(5, sender.getName());
                preparedStatement.executeUpdate();

                XenonCore.instance.getBungeeInstance().getPlayers().forEach(player -> {
                    if (player.getName().equals(playerName)) player.disconnect(
                            ChatColor.translateAlternateColorCodes('&',
                                    XenonCore.instance.getConfigData().getPunishmanager().getBandisconnectmessage()
                                            .replace("PLAYER", playerName)
                                            .replace("REASON", reason)
                                            .replace("DURATION", String.valueOf(banDuration / 60000))));
                    Message.send(player, XenonCore.instance.getConfigData().getPunishmanager().getBanannouncemessage()
                            .replace("PLAYER1", sender.getName())
                            .replace("PLAYER2", playerName)
                            .replace("REASON", reason)
                            .replace("DURATION", String.valueOf(banDuration / 60000)), false);
                });
                Message.send(XenonCore.instance.getConfigData().getPunishmanager().getBanannouncemessage()
                        .replace("PLAYER1", sender.getName())
                        .replace("PLAYER2", playerName)
                        .replace("REASON", reason)
                        .replace("DURATION", String.valueOf(banDuration / 60000)));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
