package ir.xenoncommunity.punishmanager;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;

public class KickCommand extends Command {

    public KickCommand( SQLManager sqlManagerIn) {
        super("kick", XenonCore.instance.getConfigData().getPunishmanager().getKickperm());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Message.send(sender,
                    XenonCore.instance.getConfigData().getUnknownoptionmessage()
                            .replace("OPTIONS", "player, reason"), false);
            return;
        }

        if(args[1].toLowerCase().equals("console")) {
            Message.send(sender,
                    XenonCore.instance.getConfigData().getCannotexecasconsoleerrormessage(), false);
            return;
        }

        final String playerName = args[0];

        final StringBuilder sb = new StringBuilder();

        Arrays.stream(args).forEach(word -> sb.append(word).append(" "));

        final String reason = sb.toString().replace(args[0] , "").substring(1);

        XenonCore.instance.getTaskManager().add(() -> {
            try {

                XenonCore.instance.getBungeeInstance().getPlayers().forEach(player ->{
                    if(player.getName().equals(playerName)) player.disconnect(
                            new TextComponent(
                                    ChatColor.translateAlternateColorCodes(
                                            '&',
                                            XenonCore.instance.getConfigData().getPunishmanager().getKickdisconnectmessage()
                                                    .replace("PLAYER", playerName)
                                                    .replace("REASON", reason)
                                    )
                            )
                    );

                    Message.send(player, XenonCore.instance.getConfigData().getPunishmanager().getKickannouncemessage()
                            .replace("PLAYER1", sender.getName())
                            .replace("PLAYER2", playerName)
                            .replace("REASON", reason), false);
            });
                Message.send(XenonCore.instance.getConfigData().getPunishmanager().getKickannouncemessage()
                        .replace("PLAYER1", sender.getName())
                        .replace("PLAYER2", playerName)
                        .replace("REASON", reason));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
