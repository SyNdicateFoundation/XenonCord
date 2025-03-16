package ir.xenoncommunity.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClassHelper {
    public void registerModules() {
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().scan()) {
            for (ClassInfo clz : scanResult.getClassesWithAnnotation(ModuleInfo.class)) {
                Class<?> loadClass = clz.loadClass();
                ModuleInfo annotation = loadClass.getAnnotation(ModuleInfo.class);
                XenonCore.instance.getLogger().info(Colorize.console(String.format("&c[&6Module&c] &fLoaded &b%s&f - Version %s", annotation.name(), annotation.version())));
                ModuleBase moduleBase = (ModuleBase) loadClass.getConstructor().newInstance();
                moduleBase.onInit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
