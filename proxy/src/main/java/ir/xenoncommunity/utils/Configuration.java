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
import java.util.Objects;

@Getter
public class Configuration {
    private final File configFile;
    private final Logger logger;

    public Configuration() {
        this.configFile = new File("XenonCord.yml");
        this.logger = XenonCore.instance.getLogger();
    }

    private void copyConfig() {
        try {
            Files.copy(Objects.requireNonNull(XenonCore.class.getResourceAsStream("/XenonCord.yml")), configFile.toPath());
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

            configData.setCannot_execute_as_console_message(Message.translateColor(configData.getCannot_execute_as_console_message()));
            configData.setUnknown_option_message(Message.translateColor(configData.getUnknown_option_message()));
            configData.setReload_message(Message.translateColor(configData.getReload_message()));
            configData.setReload_complete_message(Message.translateColor(configData.getReload_complete_message()));

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
        private String in_game_brandname, cannot_execute_as_console_message,
                unknown_option_message, xenoncord_permission, reload_permission, reload_message, reload_complete_message,
                whitelist_ip_mode;
        private boolean debug, use_gui, socket_backend;
        private long gui_refresh_rate;
        private String[] whitelisted_ips;
    }

}
