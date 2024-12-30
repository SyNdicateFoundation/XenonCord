package ir.xenoncommunity.punishmanager;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.commands.CommandSend;
import ir.xenoncommunity.utils.Message;
import ir.xenoncommunity.utils.SQLManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class ClearChatCommand extends Command {
    private final SQLManager sqlManager;
    public ClearChatCommand(SQLManager sqlManagerIn){
        super("clearchat", XenonCore.instance.getConfigData().getPunishmanager().getClearchatperm(), "cls");
        this.sqlManager = sqlManagerIn;;
    }
    @Override
    public void execute(CommandSender sender, String[] args){
        if(!sender.hasPermission(XenonCore.instance.getConfigData().getPunishmanager().getClearchatperm())) return;

        if(args.length == 0){
            for(int i = 0; i <= 100; i++){
                Message.send(sender, "", false);
            }
            return;
        }

        if(!args[0].equals("global"))
            Message.send(sender,
                    XenonCore.instance.getConfigData().getUnknownoptionmessage()
                            .replace("OPTIONS", "global, blank (for self use)"), false);

        XenonCore.instance.getTaskManager().add(() -> XenonCore.instance.getBungeeInstance().getPlayers().forEach(player -> {
            for(int i = 0; i <= 100; i++)
                Message.send(player, "", false);
        }));

    }
}
