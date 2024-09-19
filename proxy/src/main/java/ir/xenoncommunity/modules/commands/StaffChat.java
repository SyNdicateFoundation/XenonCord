package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;

@SuppressWarnings("unused") public class StaffChat extends Command {
    public StaffChat() {
        super("staffchat", XenonCore.instance.getConfigData().getModules().getStaffchatperm(), "sc");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!(sender instanceof ProxiedPlayer) ||
        !sender.hasPermission(XenonCore.instance.getConfigData().getModules().getStaffchatperm())) return;

        XenonCore.instance.getTaskManager().add(() -> {
            final StringBuilder stringBuilder = new StringBuilder();
            Arrays.stream(args).forEach(string -> stringBuilder.append(string).append(" "));
            final String message = stringBuilder.toString();

            XenonCore.instance.getBungeeInstance().getPlayers().stream().filter(
                    proxiedPlayer -> proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getModules()
                            .getStaffchatperm())).forEach(
                                    proxiedPlayer -> proxiedPlayer.sendMessage(ChatColor.translateAlternateColorCodes(
                                            '&',
                                            XenonCore.instance.getConfigData().getModules().getStaffchatmessage()
                                                    .replace("PLAYER", sender.getName())
                                                    .replace("MESSAGE", message))));
        });
    }
}
