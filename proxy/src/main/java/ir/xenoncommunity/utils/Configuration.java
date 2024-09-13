package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.Objects;

public class Configuration {
    @Getter private File configFile;
    public Configuration(){
        this.configFile = new File("XenonCore.yml");
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
        private String prefix;
        private String loadingmessage;
        private ModulesData modules;
    }
    @Getter
    @Setter
    public static class ModulesData{
        private String motd;
        private String[] enables;
    }
}
