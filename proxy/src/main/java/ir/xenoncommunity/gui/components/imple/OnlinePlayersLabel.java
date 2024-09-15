package ir.xenoncommunity.gui.components.imple;

import ir.xenoncommunity.XenonCore;

import javax.swing.*;

public class OnlinePlayersLabel extends JLabel {
    public OnlinePlayersLabel() {
        new Timer((int) XenonCore.instance.getConfigData().getGuirefreshrate(), e -> {
            this.setText(String.format("Online Players: %s", XenonCore.instance.getBungeeInstance().getOnlineCount()));
        }).start();
        this.setBounds(0, 0, 200, 50);
    }
}
