package ir.xenoncommunity.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class XenonCord extends Command {

    public XenonCord(){
        super("xenoncord", XenonCore.instance.getConfigData().getXenoncordperm());
    }

    public void execute(CommandSender sender, String[] args){
        if(!sender.hasPermission(XenonCore.instance.getConfigData().getXenoncordperm())) return;

        if(args.length == 0){
            Message.send(sender, "XenonCord made by RealStresser", false);
            return;
        }

        switch (args[0]){
            case "reload":
                if(!sender.hasPermission(XenonCore.instance.getConfigData().getReloadperm())) return;
                Message.send(sender,
                        XenonCore.instance.getConfigData().getReloadmessage(),
                        true);
                XenonCore.instance.setConfigData(XenonCore.instance.getConfiguration().init());
                Message.send(sender,
                        XenonCore.instance.getConfigData().getReloadcompletemessage(),
                        true);
                break;
        }


    }
}
