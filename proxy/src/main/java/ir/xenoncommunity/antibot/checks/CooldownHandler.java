package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
@AntibotCheck(name = "Still On Cooldown")
public class CooldownHandler extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPreLogin(PreLoginEvent event) {
        final String playerName = event.getConnection().getName();
        final Long lastAttemptTime = cooldownMap.get(playerName);
        if (lastAttemptTime != null && (System.currentTimeMillis() - lastAttemptTime) < XenonCore.instance.getConfigData().getAntibot().getPlayerspecifiedcooldown()/*config player specified*/) {
            cancelPreLogin(event, XenonCore.instance.getConfigData().getAntibot().getDisconnect_cooldown());
            return;
        }
        resetPlayerData(playerName, System.currentTimeMillis());
    }
}
