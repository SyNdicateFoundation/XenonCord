package ir.xenoncommunity.punishmanager;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import lombok.Cleanup;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.util.Arrays;

public class MuteCommand extends Command {
    private final SQLManager sqlManager;

    public MuteCommand(final SQLManager sqlManagerIn) {
        super("mute", XenonCore.instance.getConfigData().getPunishmanager().getMuteperm());
        this.sqlManager = sqlManagerIn;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(XenonCore.instance.getConfigData().getPunishmanager().getMuteperm())) return;

        if (args.length < 3) {
            Message.send(sender,
                    XenonCore.instance.getConfigData().getUnknownoptionmessage()
                            .replace("OPTIONS", "player, reason, duration in minute"), false);
            return;
        }

        if(args[1].toLowerCase().equals("console")) {
            Message.send(sender,
                    XenonCore.instance.getConfigData().getCannotexecasconsoleerrormessage(), false);
            return;
        }

        final String playerName = args[0];
        final int muteDuration = Integer.parseInt(args[1]) == 0 ?
                60000 : Integer.parseInt(args[1]) * 60000;

        final StringBuilder sb = new StringBuilder();

        Arrays.stream(args).forEach(word -> sb.append(word).append(" "));

        final String reason = sb.toString().replace(args[0] , "").replace(args[1], "").substring(2);

        XenonCore.instance.getTaskManager().add(() -> {
            try {
                @Cleanup final PreparedStatement preparedStatement = sqlManager.getConnection().prepareStatement(
                        "INSERT INTO Players (username, reason, banduration, muteduration, lastpunish) " +
                                "VALUES (?, ?, 0, ?, ?) " +
                                "ON CONFLICT (username) DO UPDATE SET muteduration = EXCLUDED.banduration," +
                                "reason = EXCLUDED.reason," +
                                "lastpunish = EXCLUDED.lastpunish;");
                preparedStatement.setString(1, playerName);
                preparedStatement.setString(2, reason);
                preparedStatement.setInt(3, muteDuration);
                preparedStatement.setInt(4, (int) System.currentTimeMillis());
                preparedStatement.executeUpdate();

                XenonCore.instance.getBungeeInstance().getPlayers().forEach(player ->
                        Message.send(player, XenonCore.instance.getConfigData().getPunishmanager().getMuteannouncemessage()
                        .replace("PLAYER1", sender.getName())
                        .replace("PLAYER2", playerName)
                        .replace("REASON", reason)
                        .replace("DURATION", String.valueOf(muteDuration / 60000)), true));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
