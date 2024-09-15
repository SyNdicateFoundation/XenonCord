package ir.xenoncommunity.gui;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.abstracts.ModuleListener;
import ir.xenoncommunity.gui.components.MainGui;
import net.md_5.bungee.api.plugin.Listener;
import org.reflections.Reflections;

import javax.swing.*;
import java.util.Arrays;

public class SwingManager {
    public static void initSwingGuis(){
        new Reflections("ir.xenoncommunity.gui.components").getSubTypesOf(JFrame.class).forEach(gui -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    MainGui main = new MainGui();
                    new Reflections("ir.xenoncommunity.gui.components.imple").getSubTypesOf(JLabel.class).forEach(label -> {
                        try {
                            main.add(label.newInstance());
                        } catch (Exception e) {
                            XenonCore.instance.getLogger().error(e.getMessage());
                        }
                    });
                });
            } catch (Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }
}
