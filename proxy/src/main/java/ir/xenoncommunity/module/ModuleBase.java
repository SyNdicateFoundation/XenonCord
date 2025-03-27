package ir.xenoncommunity.module;


import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Configuration;
import ir.xenoncommunity.utils.TaskManager;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.plugin.Listener;
import org.apache.logging.log4j.Logger;

public abstract class ModuleBase implements Listener {

    public void onInit() {
    }

    public void onShutdown() {
    }

    public BungeeCord getServer() {
        return XenonCore.instance.getBungeeInstance();
    }

    public Configuration.ConfigData getConfig() {
        return XenonCore.instance.getConfigData();
    }

    public Logger getLogger() {
        return XenonCore.instance.getLogger();
    }

    public TaskManager getTaskManager() {
        return XenonCore.instance.getTaskManager();
    }

}
