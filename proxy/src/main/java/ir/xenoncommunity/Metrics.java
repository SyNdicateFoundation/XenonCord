package ir.xenoncommunity;

import org.bstats.MetricsBase;
import org.bstats.charts.SingleLineChart;
import org.bstats.config.MetricsConfig;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.bstats.json.JsonObjectBuilder;

public class Metrics {

    public Metrics(Logger logger, int id){
        MetricsConfig config;
        try {
            config = new MetricsConfig(new File("bstats", "bstats.txt"), true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        final MetricsBase metricsBase =  new MetricsBase("server-implementation",
                config.getServerUUID(),
                id,
                config.isEnabled(),
                (builder) -> {
                    builder.appendField("osName", System.getProperty("os.name"));
                    builder.appendField("osArch", System.getProperty("os.arch"));
                    builder.appendField("osVersion", System.getProperty("os.version"));
                    builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
                },
                jsonObjectBuilder -> {},
                null,
                () -> true,
                logger::warn,
                logger::info,
                config.isLogErrorsEnabled(),
                config.isLogSentDataEnabled(),
                config.isLogResponseStatusTextEnabled()
        );
        metricsBase.addCustomChart(
                new SingleLineChart("players", XenonCore.instance.getBungeeInstance().getPlayers()::size)
        );
    }
}
