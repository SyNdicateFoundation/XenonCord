package ir.xenoncommunity.modules.antibot;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.modules.AntiBotManager;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@ModuleListener
public class PlayerInserter extends AntiBotManager implements Listener {
    public PlayerInserter(){
        super(0);
    }
    @EventHandler public void preLoginEvent(PreLoginEvent e){
        XenonCore.instance.getTaskManager().add(() ->
                XenonCore.instance.getSqlManager().addPlayer(
                e.getConnection().getSocketAddress().toString(),
                e.getConnection().getName(),
                false,
                0,
                "-1"));
    }

}
