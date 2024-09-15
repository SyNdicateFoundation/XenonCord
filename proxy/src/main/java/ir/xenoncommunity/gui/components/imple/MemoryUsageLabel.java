package ir.xenoncommunity.gui.components.imple;

import ir.xenoncommunity.XenonCore;

import javax.swing.*;

public class MemoryUsageLabel extends JLabel {
    public MemoryUsageLabel(){
        new Timer((int) XenonCore.instance.getConfigData().getGuirefreshrate(), e -> {
            this.setText(String.format("Memory Usage: %s",
                    (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory())
                            / (1024*1024)));
        }).start();
        this.setBounds(0, 0, 200, 10);

    }
}
