package ir.xenoncommunity.abstracts;

import ir.xenoncommunity.XenonCore;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import org.reflections.Reflections;

import java.util.Arrays;

public abstract class ModuleListener {
    public static void init(){
        XenonCore.instance.getLogger().info("Initializing modules....");
        new Reflections("ir.xenoncommunity.modules.listeners").getSubTypesOf(ModuleListener.class).forEach(listener -> Arrays.stream(XenonCore.instance.getConfigData().getModules().getEnables()).filter(module -> module.equals(listener.getSimpleName())).forEach(module -> {
            try {
                XenonCore.instance.getLogger().info(String.format("Module %s loaded.", module));
                XenonCore.instance.getBungeeInstance().pluginManager.registerListener(null, (Listener) listener.newInstance());
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        }));
        new Reflections("ir.xenoncommunity.modules.commands").getSubTypesOf(Command.class).forEach(command -> Arrays.stream(XenonCore.instance.getConfigData().getModules().getEnables()).filter(module -> module.equals(command.getSimpleName())).forEach(commandModule -> {
            try {
                XenonCore.instance.getLogger().info(String.format("Module %s loaded.", commandModule));
                XenonCore.instance.getBungeeInstance().pluginManager.registerCommand(null, command.newInstance());
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        }));
        XenonCore.instance.getLogger().info("Successfully Initialized!");
    }
}
