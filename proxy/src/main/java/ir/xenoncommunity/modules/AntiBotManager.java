package ir.xenoncommunity.modules;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Listener;
import org.reflections.Reflections;

import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Getter
public class AntiBotManager {
    private int violation;

    public static void init() {
        try{
            if (!XenonCore.instance.getConfiguration().getSqlDataBase().exists()) Files.createFile(XenonCore.instance.getConfiguration().getSqlDataBase().toPath());
            new Reflections("ir.xenoncommunity.modules.antibot").getTypesAnnotatedWith(ModuleListener.class).forEach(antibot -> {
                try {
                    XenonCore.instance.getBungeeInstance().getPluginManager().registerListener(null, (Listener) antibot.newInstance());
                    XenonCore.instance.getLogger().info(String.format("Module %s loaded.", antibot.getSimpleName()));
                } catch (Exception ignored){}
            });
            XenonCore.instance.getTaskManager().repeatingTask(() -> {
                XenonCore.instance.getBungeeInstance().getPlayers()
                        .forEach(p -> {
                            if (((int) XenonCore.instance.getSqlManager().getPlayerDataByIP(p.getAddress().toString(), "violationCount") >= 1)) {
                                XenonCore.instance.getSqlManager().updatePlayerByIP(p.getAddress().toString(), "lastBlacklist", System.currentTimeMillis());
                                p.disconnect();
                            }
                        });
            }, 0,200, TimeUnit.MILLISECONDS);

        } catch (Exception ignored) {}

    }

    public void fail(){

    }
}
