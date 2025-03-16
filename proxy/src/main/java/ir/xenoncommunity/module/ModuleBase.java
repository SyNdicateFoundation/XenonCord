package ir.xenoncommunity.module;


import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Configuration;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.plugin.Listener;

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

}
