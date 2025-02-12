package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.xenoncord.PostPlayerHandshakeEvent;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
@AntibotCheck(name = "Joining & Leaving Contantly")
public class FastJoin extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPreLogin(PostPlayerHandshakeEvent event) {
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        final Long lastAttemptTime = cooldownMap.get(playerIp);
        if(lastAttemptTime == null) return;

        if((System.currentTimeMillis() - lastAttemptTime) < XenonCore.instance.getConfigData().getAntibot().getFastjointhreshold()){
            blockPlayer(event, playerIp, XenonCore.instance.getConfigData().getAntibot().getDisconnect_cooldown());
        }
    }
}
