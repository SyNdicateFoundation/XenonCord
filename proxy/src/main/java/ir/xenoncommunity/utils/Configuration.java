package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
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
    private final File sqlAntiBot;
    private final Logger logger;

    public Configuration() {
        this.configFile = new File("XenonCore.yml");
        this.sqlAntibot = new File("AntiBot.db");
        this.sqlPlaytime = new File("Playtimes.db");
        this.sqlPunishments = new File("Punishments.db");
        this.sqlStaffActivity = new File("StaffActivity.db");
        this.sqlAntiBot = new File("AntiBot.db");
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

            Thread.currentThread().setContextClassLoader(ConfigData.class.getClassLoader());

            @Cleanup final FileInputStream is = new FileInputStream(configFile);
            final ConfigData configData = new Yaml().loadAs(is, ConfigData.class);
            final String prefix = configData.prefix;

            configData.setCannot_execute_as_console_message(Message.translateColor(configData.getCannot_execute_as_console_message().replace("PREFIX", prefix)));
            configData.setUnknown_option_message(Message.translateColor(configData.getUnknown_option_message().replace("PREFIX", prefix)));
            configData.setReload_message(Message.translateColor(configData.getReload_message().replace("PREFIX", prefix)));
            configData.setReload_complete_message(Message.translateColor(configData.getReload_complete_message().replace("PREFIX", prefix)));
            configData.getMotd_changer().setMotd(Message.translateColor(configData.getMotd_changer().getMotd().replace("PREFIX", prefix)));
            configData.getMotd_changer().setMaintenance_motd(Message.translateColor(configData.getMotd_changer().getMaintenance_motd().replace("PREFIX", prefix)));
            configData.getCommand_spy().setSpy_bypass(Message.translateColor(configData.getCommand_spy().getSpy_bypass().replace("PREFIX", prefix)));
            configData.getCommand_spy().setSpy_message(Message.translateColor(configData.getCommand_spy().getSpy_message().replace("PREFIX", prefix)));
            configData.getCommand_spy().setSpy_toggle_message(Message.translateColor(configData.getCommand_spy().getSpy_toggle_message().replace("PREFIX", prefix)));
            configData.getStaff_chat().setStaff_chat_message(Message.translateColor(configData.getStaff_chat().getStaff_chat_message().replace("PREFIX", prefix)));
            configData.getStaff_chat().setToggle_message(Message.translateColor(configData.getStaff_chat().getToggle_message().replace("PREFIX", prefix)));
            configData.getAdmin_chat().setAdmin_chat_message(Message.translateColor(configData.getAdmin_chat().getAdmin_chat_message().replace("PREFIX", prefix)));
            configData.getAdmin_chat().setToggle_message(Message.translateColor(configData.getAdmin_chat().getToggle_message().replace("PREFIX", prefix)));
            configData.getMaintenance().setMaintenance_add_command_message(Message.translateColor(configData.getMaintenance().getMaintenance_add_command_message().replace("PREFIX", prefix)));
            configData.getMaintenance().setMaintenance_remove_command_message(Message.translateColor(configData.getMaintenance().getMaintenance_remove_command_message().replace("PREFIX", prefix)));
            configData.getMaintenance().setMaintenance_disconnect_message(Message.translateColor(configData.getMaintenance().getMaintenance_disconnect_message().replace("PREFIX", prefix)));
            configData.getPing().setPing_message(Message.translateColor(configData.getPing().getPing_message().replace("PREFIX", prefix)));
            configData.getPing().setPing_others_message(Message.translateColor(configData.getPing().getPing_others_message().replace("PREFIX", prefix)));
            configData.getBplugins().setPlugin_is_loading_message(Message.translateColor(configData.getBplugins().getPlugin_is_loading_message().replace("PREFIX", prefix)));
            configData.getBplugins().setPlugin_is_unloading_message(Message.translateColor(configData.getBplugins().getPlugin_is_unloading_message().replace("PREFIX", prefix)));
            configData.getBplugins().setPlugin_does_not_exist_error_message(Message.translateColor(configData.getBplugins().getPlugin_does_not_exist_error_message().replace("PREFIX", prefix)));
            configData.getPlaytime().setPlaytime_message(Message.translateColor(configData.getPlaytime().getPlaytime_message().replace("PREFIX", prefix)));
            configData.getPlaytime().setPlaytime_others_message(Message.translateColor(configData.getPlaytime().getPlaytime_others_message().replace("PREFIX", prefix)));
            configData.getCommand_whitelist().setBlock_message(Message.translateColor(configData.getCommand_whitelist().getBlock_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setBan_announce_message(Message.translateColor(configData.getPunish_manager().getBan_announce_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setBan_disconnect_message(Message.translateColor(configData.getPunish_manager().getBan_disconnect_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setMute_announce_message(Message.translateColor(configData.getPunish_manager().getMute_announce_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setMute_block_message(Message.translateColor(configData.getPunish_manager().getMute_block_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setKick_announce_message(Message.translateColor(configData.getPunish_manager().getKick_announce_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setKick_disconnect_message(Message.translateColor(configData.getPunish_manager().getKick_disconnect_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setUnban_console_log_message(Message.translateColor(configData.getPunish_manager().getUnban_console_log_message().replace("PREFIX", prefix)));
            configData.getPunish_manager().setUnmute_console_log_message(Message.translateColor(configData.getPunish_manager().getUnmute_console_log_message().replace("PREFIX", prefix)));
            configData.getAntibot().setDisconnect_cooldown(Message.translateColor(configData.getAntibot().getDisconnect_cooldown().replace("PREFIX", prefix)));
            configData.getAntibot().setDisconnect_please_wait_until_verify(Message.translateColor(configData.getAntibot().getDisconnect_please_wait_until_verify().replace("PREFIX", prefix)));
            configData.getAntibot().setDisconnect_invalid_username(Message.translateColor(configData.getAntibot().getDisconnect_invalid_username().replace("PREFIX", prefix)));


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
        private String prefix, in_game_brandname, cannot_execute_as_console_message,
                unknown_option_message, xenoncord_permission, reload_permission, reload_message, reload_complete_message,
                whitelist_ip_mode;
        private boolean debug, use_gui;
        private long gui_refresh_rate;
        private String[] whitelisted_ips;
        private ModulesData modules;
        private MotdChangerData motd_changer;
        private CommandSpyData command_spy;
        private StaffChatData staff_chat;
        private AdminChatData admin_chat;
        private MaintenanceData maintenance;
        private PingData ping;
        private BpluginsData bplugins;
        private PlaytimeData playtime;
        private PunishManagerData punish_manager;
        private StaffActivityData staff_activity;
        private CommandWhitelistData command_whitelist;
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
        private String motd, maintenance_motd;
        private Boolean one_more_player;
    }

    @Getter
    @Setter
    public static class CommandSpyData {
        private String spy_message, spy_perm, spy_bypass, spy_toggle_message;
        private String[] spy_exceptions;
    }

    @Getter
    @Setter
    public static class StaffChatData {
        private String staff_chat_perm, staff_chat_message, toggle_message;
    }

    @Getter
    @Setter
    public static class AdminChatData {
        private String admin_chat_perm, admin_chat_message, toggle_message;
    }

    @Getter
    @Setter
    public static class MaintenanceData {
        private String maintenance_perm, maintenance_bypass_perm,
                maintenance_add_command_message, maintenance_remove_command_message,
                maintenance_disconnect_message;
    }

    @Getter
    @Setter
    public static class PingData {
        private String ping_perm, ping_others_perm,
                ping_message, ping_others_message;
    }

    @Getter
    @Setter
    public static class BpluginsData {
        private String plugin_is_loading_message, plugin_is_unloading_message,
                plugin_does_not_exist_error_message, plugins_perm, plugins_toggle_perm;
    }

    @Getter
    @Setter
    public static class PlaytimeData {
        private String playtime_message, playtime_others_message,
                playtime_perm, playtime_others_perm;
    }

    @Getter
    @Setter
    public static class PunishManagerData {
        private String mode, ban_perm, ban_announce_message, ban_disconnect_message,
                mute_perm, mute_announce_message, mute_block_message,
                kick_perm, kick_announce_message, kick_disconnect_message,
                clear_chat_perm, global_clear_chat_perm, unban_console_log_message,
                unmute_console_log_message;
        private String[] mute_commands;
    }

    @Getter
    @Setter
    public static class StaffActivityData {
        private String[] staff_usernames;
        private int send_time;
    }

    @Getter
    @Setter
    public static class CommandWhitelistData {
        private String bypass, block_message;
        private Map<String, GroupData> per_group;
    }

    @Getter
    @Setter
    public static class GroupData {
        private String[] commands;
    }

    @Getter
    @Setter
    public static class AntiBotData {
        private int block_duration_millis,
                player_specified_cooldown,
                fast_join_threshold;
        private String disconnect_cooldown, disconnect_please_wait_until_verify, disconnect_invalid_username;
        private String[] checks;
    }

}
