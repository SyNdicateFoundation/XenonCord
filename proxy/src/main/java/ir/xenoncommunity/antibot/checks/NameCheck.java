package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
@AntibotCheck(name = "Invalid Name")
public class NameCheck extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    @EventHandler(priority = Byte.MAX_VALUE - 1)
    public void onPreLogin(PreLoginEvent event) {
        final String playerName = event.getConnection().getName();
        if (!(playerName.length() >= 3 && playerName.length() <= 16 && playerName.matches("^[a-zA-Z0-9_]+$"))) {
            blockPlayer(event, playerName, XenonCore.instance.getConfigData().getAntibot().getDisconnect_invalidusername());
        }
    }
}
