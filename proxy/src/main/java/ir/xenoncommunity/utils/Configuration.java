package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Getter
public class Configuration {
    private final File configFile;
    private final File sqlDataBase;
    public Configuration(){
        this.configFile = new File("XenonCore.yml");
        this.sqlDataBase = new File("XenonCore.db");
    }
    public ConfigData init(){
        XenonCore.instance.getLogger().info("Initializing Configuration...");
        try {
            if (!configFile.exists()) copyConfig();
            XenonCore.instance.getLogger().info("Successfully Initialized!");
            return getConfig();
        } catch (final Exception e) {
            XenonCore.instance.getLogger().error(e.getMessage());
        }
        return null;
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
            XenonCore.instance.getLogger().error(e.getMessage());
        }
    }
    public ConfigData getConfig() {
        try {
            @Cleanup final FileInputStream is = new FileInputStream(configFile);
            return new Yaml().loadAs(is, ConfigData.class);
        } catch (final Exception e) {
            XenonCore.instance.getLogger().error(e.getMessage());
            return null;
        }
    }
    @Getter
    @Setter
    public static class ConfigData{
        private String prefix, loadingmessage, ingamebrandname;
        private boolean usegui;
        private long guirefreshrate;
        private ModulesData modules;
        private CommandWhitelistData commandwhitelist;
    }
    @Getter
    @Setter
    public static class ModulesData{
        private String motd, spybypass, spyperm, spymessage, staffchatperm, staffchatmessage;
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
