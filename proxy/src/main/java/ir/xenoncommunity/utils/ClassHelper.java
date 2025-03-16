package ir.xenoncommunity.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleInfo;
import ir.xenoncommunity.module.ModuleBase;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;

@UtilityClass
public class ClassHelper {
    public void registerModules() {
        new Reflections("ir.xenoncommunity.module.impl").getTypesAnnotatedWith(ModuleInfo.class).forEach(clz -> {
            try {
                ModuleInfo annotation = clz.getAnnotation(ModuleInfo.class);
                XenonCore.instance.getLogger().info(Colorize.console(String.format("&c[&6Module&c] &fLoaded &b%s&f - Version %s", annotation.name(), annotation.version())));
                ModuleBase moduleBase = (ModuleBase) clz.getConstructor().newInstance();
                moduleBase.onInit();
            }catch(Exception e){
                e.printStackTrace();
            }
        });
    }
}
