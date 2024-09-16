package ir.xenoncommunity.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import ir.xenoncommunity.XenonCore;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

@UtilityClass
public class SwingManager {
    private JFrame mainWindow;
    public void initSwingGui(){
        if(!XenonCore.instance.getConfigData().isUsegui()) return;

        FlatDarculaLaf.setup();

        SwingUtilities.invokeLater(() -> {
            mainWindow = new JFrame();
            final Toolkit toolkit = Toolkit.getDefaultToolkit();
            final JList<String> playersList = new JList<>();
            final JScrollPane scrollPane = new JScrollPane(playersList);
            final JLabel[] labels = {new JLabel("Memory Usage:"),
                    new JLabel(String.format("Bound Address: %s", XenonCore.instance.getBungeeInstance().getConfig().getListeners().iterator().next().getSocketAddress().toString())),
                    new JLabel("Online Players:")
            };


            new Timer((int) XenonCore.instance.getConfigData().getGuirefreshrate(), e -> {
                labels[0].setText(String.format("Memory Usage: %s",
                        (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024)));
                labels[2].setText(String.format("Online Players: %s", XenonCore.instance.getBungeeInstance().getOnlineCount()));
                String[] players = XenonCore.instance.getPlayerNames().toArray(new String[0]);
                playersList.setListData(players);
            }).start();

            labels[0].setBounds(0,0, 200, 10);
            labels[1].setBounds(0,0, 200,40);
            labels[2].setBounds(0,0,200,70);
            scrollPane.setBounds(510, 10, 200, 300);

            mainWindow.setTitle("XenonCord Proxy");
            mainWindow.setSize(800, 400);
            mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            mainWindow.setLayout(null);
            mainWindow.setLocation((toolkit.getScreenSize().width - mainWindow.getSize().width) / 2, (toolkit.getScreenSize().height - mainWindow.getSize().height) / 2);
            mainWindow.setResizable(false);

            Arrays.stream(XenonCore.instance.getConfigData().getGuicomponents()).forEach(enabledGuis -> Arrays.stream(labels).filter(label -> label.getText().contains(enabledGuis)).forEach(mainWindow::add));
            if(Arrays.asList(XenonCore.instance.getConfigData().getGuicomponents()).contains("Players List"))
                mainWindow.add(scrollPane);

            mainWindow.setVisible(true);
        });
    }
}
