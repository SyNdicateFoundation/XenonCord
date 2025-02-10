package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@AntibotCheck(name = "Joining & Leaving Contantly")
public class FastJoin extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    @EventHandler(priority = Byte.MAX_VALUE - 2)
    public void onPreLogin(PreLoginEvent event) {
        final Long lastAttemptTime = cooldownMap.get(event.getConnection().getName());
        if(lastAttemptTime == null) return;

        if((System.currentTimeMillis() - lastAttemptTime) < XenonCore.instance.getConfigData().getAntibot().getFastjointhreshold()){
            blockPlayer(event, event.getConnection().getName(), "You are joining too quickly.");
        }
    }
}
