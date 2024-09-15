package ir.xenoncommunity.gui.components.imple;

import ir.xenoncommunity.XenonCore;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import javax.swing.*;

public class CpuUsageLabel extends JLabel {
    public CpuUsageLabel() {
        new Timer((int) XenonCore.instance.getConfigData().getGuirefreshrate(), e -> {
            CentralProcessor processor = new SystemInfo().getHardware().getProcessor();
            this.setText(String.format("Cpu Usage: %s", processor.getSystemCpuLoadBetweenTicks(processor.getSystemCpuLoadTicks()) * 100));
        }).start();
        this.setBounds(0,0, 200,70);
    }


}
