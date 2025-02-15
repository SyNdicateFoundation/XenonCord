package ir.xenoncommunity.antibot;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Listener;
import org.reflections.Reflections;

import java.util.Arrays;

/*
 * Credits: _Bycz3Q
 * This source code is provided to SyNdicate Foundation to improve XenonCord's features.
 */
@Getter
public class AntibotManager {
    public void init() {
        new Reflections("ir.xenoncommunity.antibot.checks").getTypesAnnotatedWith(AntibotCheck.class).forEach(listener -> Arrays.stream(XenonCore.instance.getConfigData().getAntibot().getChecks()).filter(e -> e.equals(listener.getSimpleName())).forEach(check -> {
            try {
                XenonCore.instance.getBungeeInstance().getPluginManager().registerListener(null, (Listener) listener.newInstance());
                XenonCore.instance.logdebuginfo(String.format("Antibot check %s loaded.", check));
            } catch (Exception e) {
                XenonCore.instance.logdebugerror("Error while enabling antibot checks");
                e.printStackTrace();
            }
        }));
        //this.getProxy().getScheduler().schedule((Plugin)this, pingCheck::resetPings, (long)ConfigManager.getPingResetInterval(), (long)ConfigManager.getPingResetInterval(), TimeUnit.SECONDS);
        //this.getProxy().getScheduler().schedule((Plugin)this, handshakeCheck::resetHandshakes, (long)ConfigManager.getPingResetInterval(), (long)ConfigManager.getPingResetInterval(), TimeUnit.SECONDS);
    }
}
