package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

@Getter
public class Configuration {
    private final File configFile;
    private final File sqlAntibot;
    private final File sqlPlaytime;
    private final File sqlPunishments;
    private final File sqlStaffActivity;
    private final Logger logger;

    public Configuration() {
        this.configFile = new File("XenonCore.yml");
        this.sqlAntibot = new File("AntiBot.db");
        this.sqlPlaytime = new File("Playtimes.db");
        this.sqlPunishments = new File("Punishments.db");
        this.sqlStaffActivity = new File("StaffActivity.db");
        this.logger = XenonCore.instance.getLogger();
    }

    private void copyConfig() {
        try {
            Files.copy(Objects.requireNonNull(XenonCore.class.getResourceAsStream("/XenonCore.yml")), configFile.toPath());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public ConfigData init() {
        logger.info("Initializing Configuration...");
        try {
            if (!configFile.exists()) copyConfig();

            @Cleanup final FileInputStream is = new FileInputStream(configFile);
            final ConfigData configData = new Yaml().loadAs(is, ConfigData.class);
            final String prefix = configData.prefix;

            configData.setLoadingmessage(Message.translateColor(configData.getLoadingmessage().replace("PREFIX", prefix)));
            configData.setCannotexecasconsoleerrormessage(Message.translateColor(configData.getCannotexecasconsoleerrormessage().replace("PREFIX", prefix)));
            configData.setUnknownoptionmessage(Message.translateColor(configData.getUnknownoptionmessage().replace("PREFIX", prefix)));
            configData.setReloadmessage(Message.translateColor(configData.getReloadmessage().replace("PREFIX", prefix)));
            configData.setReloadcompletemessage(Message.translateColor(configData.getReloadcompletemessage().replace("PREFIX", prefix)));
            configData.getMotdchanger().setMotd(Message.translateColor(configData.getMotdchanger().getMotd().replace("PREFIX", prefix)));
            configData.getMotdchanger().setMaintenancemotd(Message.translateColor(configData.getMotdchanger().getMaintenancemotd().replace("PREFIX", prefix)));
            configData.getCommandspy().setSpybypass(Message.translateColor(configData.getCommandspy().getSpybypass().replace("PREFIX", prefix)));
            configData.getCommandspy().setSpymessage(Message.translateColor(configData.getCommandspy().getSpymessage().replace("PREFIX", prefix)));
            configData.getCommandspy().setSpytogglemessage(Message.translateColor(configData.getCommandspy().getSpytogglemessage().replace("PREFIX", prefix)));
            configData.getStaffchat().setStaffchatmessage(Message.translateColor(configData.getStaffchat().getStaffchatmessage().replace("PREFIX", prefix)));
            configData.getStaffchat().setTogglemessage(Message.translateColor(configData.getStaffchat().getTogglemessage().replace("PREFIX", prefix)));
            configData.getAdminchat().setAdminchatmessage(Message.translateColor(configData.getAdminchat().getAdminchatmessage().replace("PREFIX", prefix)));
            configData.getAdminchat().setTogglemessage(Message.translateColor(configData.getAdminchat().getTogglemessage().replace("PREFIX", prefix)));
            configData.getMaintenance().setMaintenanceaddcommandmessage(Message.translateColor(configData.getMaintenance().getMaintenanceaddcommandmessage().replace("PREFIX", prefix)));
            configData.getMaintenance().setMaintenanceremovecommandmessage(Message.translateColor(configData.getMaintenance().getMaintenanceremovecommandmessage().replace("PREFIX", prefix)));
            configData.getMaintenance().setMaintenancedisconnectmessage(Message.translateColor(configData.getMaintenance().getMaintenancedisconnectmessage().replace("PREFIX", prefix)));
            configData.getPing().setPingmessage(Message.translateColor(configData.getPing().getPingmessage().replace("PREFIX", prefix)));
            configData.getPing().setPingothersmessage(Message.translateColor(configData.getPing().getPingothersmessage().replace("PREFIX", prefix)));
            configData.getBplugins().setPluginisloadingmessage(Message.translateColor(configData.getBplugins().getPluginisloadingmessage().replace("PREFIX", prefix)));
            configData.getBplugins().setPluginisunloadingmessage(Message.translateColor(configData.getBplugins().getPluginisunloadingmessage().replace("PREFIX", prefix)));
            configData.getBplugins().setPlugindoesntexisterrormessage(Message.translateColor(configData.getBplugins().getPlugindoesntexisterrormessage().replace("PREFIX", prefix)));
            configData.getPlaytime().setPlaytimemessage(Message.translateColor(configData.getPlaytime().getPlaytimemessage().replace("PREFIX", prefix)));
            configData.getPlaytime().setPlaytimeothersmessage(Message.translateColor(configData.getPlaytime().getPlaytimeothersmessage().replace("PREFIX", prefix)));
            configData.getCommandwhitelist().setBlockmessage(Message.translateColor(configData.getCommandwhitelist().getBlockmessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setBanannouncemessage(Message.translateColor(configData.getPunishmanager().getBanannouncemessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setBandisconnectmessage(Message.translateColor(configData.getPunishmanager().getBandisconnectmessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setMuteannouncemessage(Message.translateColor(configData.getPunishmanager().getMuteannouncemessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setMuteblockmessage(Message.translateColor(configData.getPunishmanager().getMuteblockmessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setKickannouncemessage(Message.translateColor(configData.getPunishmanager().getKickannouncemessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setKickdisconnectmessage(Message.translateColor(configData.getPunishmanager().getKickdisconnectmessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setUnbanconsolelogmessage(Message.translateColor(configData.getPunishmanager().getUnbanconsolelogmessage().replace("PREFIX", prefix)));
            configData.getPunishmanager().setUnmuteconsolelogmessage(Message.translateColor(configData.getPunishmanager().getUnmuteconsolelogmessage().replace("PREFIX", prefix)));
            configData.getAntibot().setDisconnect_cooldown(Message.translateColor(configData.getAntibot().getDisconnect_cooldown().replace("PREFIX", prefix)));
            configData.getAntibot().setDisconnect_invalidusername(Message.translateColor(configData.getAntibot().getDisconnect_invalidusername().replace("PREFIX", prefix)));
            configData.getAntibot().setDisconnect_slowconnection(Message.translateColor(configData.getAntibot().getDisconnect_slowconnection().replace("PREFIX", prefix)));


            logger.info("Successfully Initialized!");

            return configData;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Getter
    @Setter
    public static class ConfigData {
        private String prefix, loadingmessage, ingamebrandname, cannotexecasconsoleerrormessage,
                unknownoptionmessage, xenoncordperm, reloadperm, reloadmessage, reloadcompletemessage;
        private boolean debug, usegui;
        private long guirefreshrate;
        private ModulesData modules;
        private MotdChangerData motdchanger;
        private CommandSpyData commandspy;
        private StaffChatData staffchat;
        private AdminChatData adminchat;
        private MaintenanceData maintenance;
        private PingData ping;
        private BpluginsData bplugins;
        private PlaytimeData playtime;
        private PunishManagerData punishmanager;
        private StaffActivityData staffactivity;
        private CommandWhitelistData commandwhitelist;
        private AntiBotData antibot;
    }

    @Getter
    @Setter
    public static class ModulesData {
        private String[] enables;
    }

    @Getter
    @Setter
    public static class MotdChangerData {
        private String motd, maintenancemotd;
        private Boolean onemoreplayer;
    }

    @Getter
    @Setter
    public static class CommandSpyData {
        private String spymessage, spyperm, spybypass, spytogglemessage;
        private String[] spyexceptions;
    }

    @Getter
    @Setter
    public static class StaffChatData {
        private String staffchatperm, staffchatmessage, togglemessage;
    }

    @Getter
    @Setter
    public static class AdminChatData {
        private String adminchatperm, adminchatmessage, togglemessage;
    }

    @Getter
    @Setter
    public static class MaintenanceData {
        private String maintenanceperm, maintenancebypassperm,
                maintenanceaddcommandmessage, maintenanceremovecommandmessage,
                maintenancedisconnectmessage;
    }

    @Getter
    @Setter
    public static class PingData {
        private String pingperm, pingothersperm,
                pingmessage, pingothersmessage;
    }

    @Getter
    @Setter
    public static class BpluginsData {
        private String pluginisloadingmessage, pluginisunloadingmessage,
                plugindoesntexisterrormessage, pluginsperm, pluginstoggleperm;
    }

    @Getter
    @Setter
    public static class PlaytimeData {
        private String playtimemessage, playtimeothersmessage,
                playtimeperm, playtimeothersperm;
    }

    @Getter
    @Setter
    public static class PunishManagerData {
        private String mode, banperm, banannouncemessage, bandisconnectmessage,
                muteperm, muteannouncemessage, muteblockmessage,
                kickperm, kickannouncemessage, kickdisconnectmessage,
                clearchatperm, globalclearchatperm, unbanconsolelogmessage,
                unmuteconsolelogmessage;
        private String[] mutecommands;
    }

    @Getter
    @Setter
    public static class StaffActivityData {
        private String[] staffusernames;
        private int sendtime;
    }

    @Getter
    @Setter
    public static class CommandWhitelistData {
        private String bypass, blockmessage;
        private Map<String, GroupData> pergroup;
    }

    @Getter
    @Setter
    public static class GroupData {
        private String[] commands;
    }

    @Getter
    @Setter
    public static class AntiBotData {
        private int maxpings, blockdurationmillis, connectionintervalmillis, pingresetinterval, slowjointhreshold, playerspecifiedcooldown, fastjointhreshold;
        private String disconnect_cooldown, disconnect_invalidusername, disconnect_slowconnection;
        private String[] checks;
    }
}
