package ir.xenoncommunity.modules;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import org.reflections.Reflections;

import java.util.Arrays;

public class ModuleManager {

    public void init() {
        XenonCore.instance.getLogger().info("Initializing modules....");

        new Reflections("ir.xenoncommunity.modules.impl").getTypesAnnotatedWith(ModuleListener.class).forEach(listener -> Arrays.stream(XenonCore.instance.getConfigData().getModules().getEnables()).filter(module -> module.equals(listener.getSimpleName())).forEach(module -> {
            try {
                if (listener.getAnnotation(ModuleListener.class).isExtended() && !listener.getAnnotation(ModuleListener.class).isImplemented()) {
                    XenonCore.instance.getBungeeInstance().getPluginManager().registerCommand(null, (Command) listener.newInstance());
                    XenonCore.instance.logdebuginfo(String.format("Module %s loaded as a command.", module));
                } else if (!listener.getAnnotation(ModuleListener.class).isExtended() && listener.getAnnotation(ModuleListener.class).isImplemented()) {
                    XenonCore.instance.getBungeeInstance().getPluginManager().registerListener(null, (Listener) listener.newInstance());
                    XenonCore.instance.logdebuginfo(String.format("Module %s loaded as a listener.", module));
                } else if (listener.getAnnotation(ModuleListener.class).isExtended() && listener.getAnnotation(ModuleListener.class).isImplemented()) {
                    XenonCore.instance.getBungeeInstance().getPluginManager().registerListenerAndCommand(null, listener);
                    XenonCore.instance.logdebuginfo(String.format("Module %s loaded as a both.", module));
                }
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        }));

        XenonCore.instance.getLogger().info("Successfully Initialized!");
    }
}
