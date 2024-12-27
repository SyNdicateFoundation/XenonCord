package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;
import java.util.Objects;

@Getter
public class Configuration {
    private final File configFile;
    private final File sqlDataBase;
    private final Logger logger = XenonCore.instance.getLogger();
    public Configuration(){
        this.configFile = new File("XenonCore.yml");
        this.sqlDataBase = new File("XenonCore.db");
    }
    private void copyConfig() {
        try {
            @NonNull @Cleanup final InputStream in = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("XenonCore.yml"));
            @Cleanup final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            @Cleanup final BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        } catch (final Exception e) {
            logger.error(e.getMessage());
        }
    }
    public ConfigData init(){
        logger.info("Initializing Configuration...");
        try {
            if (!configFile.exists()) copyConfig();
            final ConfigData configData = getConfig();
            final String prefix = configData.prefix;
            configData.setLoadingmessage(configData.getLoadingmessage().replace("PREFIX", prefix));
            configData.getModules().setSpymessage(configData.getModules().getSpymessage().replace("PREFIX", prefix));
            configData.getModules().setStaffchatmessage(configData.getModules().getStaffchatmessage().replace("PREFIX", prefix));
            configData.getCommandwhitelist().setBlockmessage(configData.getCommandwhitelist().getBlockmessage().replace("PREFIX", prefix));
            configData.getModules().setMaintenancedisconnectmessage(configData.getModules().getMaintenancedisconnectmessage().replace("PREFIX", prefix));
            logger.info("Successfully Initialized!");
            return configData;
        } catch (final Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }
    public ConfigData getConfig() {
        try {
            @Cleanup final FileInputStream is = new FileInputStream(configFile);
            return new Yaml().loadAs(is, ConfigData.class);
        } catch (final Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }
    // Config structures
    @Getter
    @Setter
    public static class ConfigData{
        private String prefix, loadingmessage, ingamebrandname;
        private boolean debug, usegui;
        private long guirefreshrate;
        private ModulesData modules;
        private CommandWhitelistData commandwhitelist;
    }
    @Getter
    @Setter
    public static class ModulesData{
        private String motd, spybypass, spyperm, spymessage,
                staffchatperm, staffchatmessage, maintenanceperm,
                maintenancebypassperm, maintenancedisconnectmessage,
                maintenancemotd, pingperm, pingothersperm, pingmessage,
                pingothersmessage, pluginsperm, pluginstoggleperm;
        private String[] spyexceptions, enables;
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
}
