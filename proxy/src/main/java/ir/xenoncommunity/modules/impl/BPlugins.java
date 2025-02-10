package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import lombok.SneakyThrows;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Handler;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = false)
public class BPlugins extends Command {
    private final String prefix = XenonCore.instance.getConfigData().getPrefix();

    public BPlugins() {
        super("BPlugins", XenonCore.instance.getConfigData().getBplugins().getPluginsperm());
    }

    @Override
    @SneakyThrows
    public void execute(CommandSender sender, String[] args) {
        final PluginManager manager = XenonCore.instance.getBungeeInstance().getPluginManager();

        if (args.length == 0) {
            listPlugins(sender, manager);
            return;
        }

        if (!sender.hasPermission(XenonCore.instance.getConfigData().getBplugins().getPluginstoggleperm())) {
            return;
        }

        final File plFile = new File(String.format("plugins/%s", args[1]));
        if (!plFile.exists()) {
            Message.send(sender, XenonCore.instance.getConfigData().getBplugins().getPlugindoesntexisterrormessage(), false);
            return;
        }

        XenonCore.instance.getTaskManager().add(() -> handlePluginAction(args[0].toLowerCase(), plFile, sender, manager));
    }

    private void listPlugins(CommandSender sender, PluginManager manager) {
        StringBuilder sb = new StringBuilder();
        manager.getPlugins().forEach(plugin -> sb.append(plugin.getDescription().getName()).append(", "));
        Message.send(sender, String.format("%s &cPLUGINS: %s.", prefix, sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "None"), false);
    }

    private void handlePluginAction(String action, File plFile, CommandSender sender, PluginManager manager) {
        try {
            if (action.equalsIgnoreCase("load")) {
                loadPlugin(plFile, sender, manager);
            } else if (action.equalsIgnoreCase("unload")) {
                unloadPlugin(plFile, sender, manager);
            } else {
                Message.send(sender, XenonCore.instance.getConfigData().getUnknownoptionmessage().replace("OPTIONS", "load, unload, blank (to see plugins list)"), false);
            }
        } catch (Exception e) {
            XenonCore.instance.logdebugerror("Error while handing a plugin action");
            e.printStackTrace();
            Message.send(sender, String.format("%s &cAn error occurred while processing the plugin. Check the console for details.", prefix), true);
        }
    }

    private void loadPlugin(File plFile, CommandSender sender, PluginManager manager) throws Exception {
        final Field yamlField = PluginManager.class.getDeclaredField("yaml");
        final Field toLoadField = PluginManager.class.getDeclaredField("toLoad");
        final JarFile jar = new JarFile(plFile);

        yamlField.setAccessible(true);
        toLoadField.setAccessible(true);

        @SuppressWarnings("unchecked") final Map<String, PluginDescription> toLoad = Optional.ofNullable((Map<String, PluginDescription>) toLoadField.get(manager)).orElse(new HashMap<>());

        final PluginDescription desc = ((Yaml) yamlField.get(manager)).loadAs(
                jar.getInputStream(Optional.ofNullable(jar.getJarEntry("bungee.yml")).orElse(jar.getJarEntry("plugin.yml"))),
                PluginDescription.class);

        desc.setFile(plFile);
        toLoad.put(desc.getName(), desc);
        toLoadField.set(manager, toLoad);
        manager.loadPlugins();
        manager.getPlugin(desc.getName()).onEnable();

        Message.send(sender, XenonCore.instance.getConfigData().getBplugins().getPluginisloadingmessage().replace("PLUGIN", plFile.getName()), true);
    }

    private void unloadPlugin(File plFile, CommandSender sender, PluginManager manager) throws Exception {
        final Plugin plugin = manager.getPlugin(plFile.getName());
        if (plugin == null) {
            Message.send(sender, XenonCore.instance.getConfigData().getBplugins().getPlugindoesntexisterrormessage(), false);
            return;
        }

        plugin.onDisable();
        Arrays.stream(plugin.getLogger().getHandlers()).forEach(Handler::close);
        manager.unregisterCommands(plugin);
        manager.unregisterListeners(plugin);
        ProxyServer.getInstance().getScheduler().cancel(plugin);
        plugin.getExecutorService().shutdownNow();

        final Field pluginsField = PluginManager.class.getDeclaredField("plugins");
        pluginsField.setAccessible(true);
        @SuppressWarnings("unchecked") final Map<String, Plugin> plugins = (Map<String, Plugin>) pluginsField.get(manager);
        plugins.remove(plugin.getDescription().getName());

        final ClassLoader cl = plugin.getClass().getClassLoader();
        if (cl instanceof URLClassLoader) {
            final Field pluginField = cl.getClass().getDeclaredField("plugin");
            final Field pluginInitField = cl.getClass().getDeclaredField("desc");
            pluginField.setAccessible(true);
            pluginInitField.setAccessible(true);
            pluginField.set(cl, null);
            pluginInitField.set(cl, null);

            final Field allLoadersField = cl.getClass().getDeclaredField("allLoaders");
            allLoadersField.setAccessible(true);
            ((Set<?>) allLoadersField.get(cl)).remove(cl);
        }

        ((URLClassLoader) cl).close();

        System.gc();

        Message.send(sender, XenonCore.instance.getConfigData().getBplugins().getPluginisunloadingmessage().replace("PLUGIN", plFile.getName()), true);
    }
}
