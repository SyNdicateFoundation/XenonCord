package ir.xenoncommunity;

import org.bstats.MetricsBase;
import org.bstats.config.MetricsConfig;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Logger;

public class Metrics {

    public Metrics(Logger logger, int id){
        MetricsConfig config;
        try {
            config = new MetricsConfig(new File("bstats", "bstats.txt"), true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        new MetricsBase("server-implementation",
                XenonCore.instance.getBungeeInstance().getConfig().getUuid(),
                id,
                config.isEnabled(),
                null,
                null,
                null,
                () -> true,
                logger::warn,
                logger::info,
                config.isLogErrorsEnabled(),
                config.isLogSentDataEnabled(),
                config.isLogResponseStatusTextEnabled()
        );
    }
///25130
}
