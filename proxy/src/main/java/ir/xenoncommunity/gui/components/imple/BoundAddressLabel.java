package ir.xenoncommunity.gui.components.imple;

import ir.xenoncommunity.XenonCore;

import javax.swing.*;

public class BoundAddressLabel extends JLabel {
    public BoundAddressLabel(){
        this.setText(String.format("Bound Address: %s",
                XenonCore.instance.getBungeeInstance().getConfig().getListeners().iterator().next().getHost()));
        this.setBounds(0,0, 200,30);
    }
}
