package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import ir.xenoncommunity.utils.Configuration;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.xenoncord.PostPlayerHandshakeEvent;
import net.md_5.bungee.event.EventHandler;

@SuppressWarnings("unused")
@AntibotCheck(name = "Not Verified Yet")
public class VerificationCheck extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    private final long cooldownThreshold;
    private final String disconnectVerify;

    public VerificationCheck(){
        final Configuration.AntiBotData config = XenonCore.instance.getConfigData().getAntibot();
        this.cooldownThreshold = config.getPlayer_specified_cooldown();
        this.disconnectVerify = config.getDisconnect_please_wait_until_verify();
    }

    // verify handler
    @EventHandler(priority = Byte.MAX_VALUE)
    public void onPostPlayerHandshake(PostPlayerHandshakeEvent event) {
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        final long currentTime = System.currentTimeMillis();

        cooldownMap.put(playerIp, currentTime);
        firstJoinTimestamps.putIfAbsent(playerIp, currentTime);

        if (currentTime - firstJoinTimestamps.get(playerIp) < cooldownThreshold)
            cancelPostHandshake(event, disconnectVerify);
    }
}
