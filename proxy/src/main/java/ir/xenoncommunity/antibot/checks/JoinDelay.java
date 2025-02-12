package ir.xenoncommunity.antibot.checks;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.AntibotCheck;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@AntibotCheck(name = "Joining Took A Long Time")
public class JoinDelay extends ir.xenoncommunity.antibot.AntibotCheck implements Listener {
    private final int slowJoinThreshold;

    public JoinDelay() {
        this.slowJoinThreshold = XenonCore.instance.getConfigData().getAntibot().getSlowjointhreshold();
        XenonCore.instance.getTaskManager().repeatingTask(() ->
                        joinTimeMap.entrySet().removeIf
                                (entry -> System.currentTimeMillis() - entry.getValue() > slowJoinThreshold)
                , 10, 10, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        final String playerIp = event.getConnection().getAddress().getAddress().getHostAddress();
        final Long joinTime = joinTimeMap.get(playerIp);

        if ((joinTime != null && (System.currentTimeMillis() - joinTime) > slowJoinThreshold)) {
            blockPlayer(event, playerIp, XenonCore.instance.getConfigData().getAntibot().getDisconnect_slowconnection());
        }
    }
}
