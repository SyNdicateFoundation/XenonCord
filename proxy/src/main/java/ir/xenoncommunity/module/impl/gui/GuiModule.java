/*

CREDITS: KiloSheet
THANKS FOR SUPPORTING XENON COMMUNITY.

 */

package ir.xenoncommunity.module.impl.gui;

import com.sun.management.OperatingSystemMXBean;
import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.management.ManagementFactory;

@ModuleInfo(name = "GUI-Interface", version = 1.0, description = "UI Module for server analytics")
public class GuiModule extends ModuleBase {
    private JLabel onlinePlayersLabel;
    private JLabel memoryUsageLabel;
    private JLabel cpuUsageLabel;
    private JTextArea playerListArea;
    private OperatingSystemMXBean osBean;


    @Override
    public void onInit() {
        if (!getConfig().getModules().getGui_module().isEnabled())
            return;
        createAndShowGUI();
    }

    public void createAndShowGUI() {
        osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        JFrame frame = new JFrame("XenonCord Proxy");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        frame.add(createMainPanel());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Timer timer = new Timer((int) XenonCore.instance.getConfigData().getGui_refresh_rate(), e -> {
            onlinePlayersLabel.setText("Online Players: " + XenonCore.instance.getBungeeInstance().getOnlineCount());
            memoryUsageLabel.setText(getMemoryUsageText());
            cpuUsageLabel.setText(getCPUUsageText());
            updatePlayerList();
        });
        timer.start();
    }

    private JPanel createMainPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(30, 30, 30));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        onlinePlayersLabel = createLabel("Online Players: ", 24, new Color(0, 255, 255));
        panel.add(onlinePlayersLabel);

        memoryUsageLabel = createLabel(getMemoryUsageText(), 20, new Color(255, 105, 180));
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(memoryUsageLabel);

        cpuUsageLabel = createLabel(getCPUUsageText(), 20, new Color(135, 206, 250));
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(cpuUsageLabel);

        playerListArea = new JTextArea();
        playerListArea.setEditable(false);
        playerListArea.setBackground(new Color(40, 40, 40));
        playerListArea.setForeground(Color.WHITE);
        playerListArea.setFont(new Font("Roboto", Font.PLAIN, 18));
        playerListArea.setLineWrap(true);
        playerListArea.setWrapStyleWord(true);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(createRoundedPanel(new JScrollPane(playerListArea)));

        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(createCloseButton());

        return panel;
    }

    private JLabel createLabel(String text, int fontSize, Color color) {
        final JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(color);
        label.setFont(new Font("Roboto", Font.BOLD, fontSize));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private JButton createCloseButton() {
        final JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Arial", Font.PLAIN, 16));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(new Color(220, 20, 60));
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setOpaque(true);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> closeButton.getTopLevelAncestor().setVisible(false));
        return closeButton;
    }

    private JPanel createRoundedPanel(JComponent component) {
        final JPanel roundedPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0, 255, 255));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }
        };
        roundedPanel.setLayout(new BorderLayout());
        roundedPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        roundedPanel.setBackground(new Color(30, 30, 30));
        roundedPanel.add(component, BorderLayout.CENTER);
        return roundedPanel;
    }

    private void updatePlayerList() {
        final StringBuilder playerNames = new StringBuilder();
        XenonCore.instance.getPlayerNames().forEach(playerName -> playerNames.append(playerName).append("\n"));
        playerListArea.setText(playerNames.toString());
    }

    public String getCPUUsageText() {
        return String.format("CPU usage: %.2f%%", osBean.getSystemCpuLoad() * 100);
    }

    public String getMemoryUsageText() {
        return "Memory usage: " + (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + "MB / " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB";
    }
}
