package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import org.reflections.Reflections;

public class PunishManager implements Listener {
    public PunishManager(){
        new Reflections("ir.xenoncommunity.punishmanager").getSubTypesOf(Command.class).forEach(command ->{
            try {
                XenonCore.instance.logdebuginfo(String.format("Module %s loaded.", command.getSimpleName()));
                XenonCore.instance.getBungeeInstance().pluginManager.registerCommand(null, command.newInstance());
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }
}
