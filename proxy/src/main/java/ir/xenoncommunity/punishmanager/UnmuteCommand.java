package ir.xenoncommunity.punishmanager;

import ir.xenoncommunity.utils.SQLManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class UnmuteCommand extends Command {
    private final SQLManager sqlManager;
    public UnmuteCommand(SQLManager sqlManagerIn){
        super("unmute", "");
        this.sqlManager = sqlManagerIn;;
    }
    @Override
    public void execute(CommandSender sender, String[] args){

    }
}
