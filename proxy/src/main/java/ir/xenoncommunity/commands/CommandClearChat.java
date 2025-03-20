package ir.xenoncommunity.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

@SuppressWarnings("unused")
public class CommandClearChat extends Command {

    public CommandClearChat() {
        super("clearchat", "xenoncord.clearchat", "cls");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            for (int i = 0; i <= 100; i++) {
                Message.send(sender, "", false);
            }
            return;
        }

        if (!sender.hasPermission("xenoncord.clearchat.global")) return;

        if (!args[0].equals("global"))
            Message.send(sender,
                    XenonCore.instance.getConfigData().getUnknown_option_message()
                            .replace("OPTIONS", "global, blank (for self use)"), false);

        XenonCore.instance.getTaskManager().add(() -> XenonCore.instance.getBungeeInstance().getPlayers().forEach(player -> {
            for (int i = 0; i <= 100; i++)
                Message.send(player, "", false);
        }));

    }
}
