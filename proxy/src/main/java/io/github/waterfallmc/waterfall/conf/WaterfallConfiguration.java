package io.github.waterfallmc.waterfall.conf;

import com.google.common.base.Joiner;
import io.github.waterfallmc.waterfall.forwarding.ForwardingMode;
import ir.xenoncommunity.XenonCore;
import lombok.Getter;
import net.md_5.bungee.Util;
import net.md_5.bungee.conf.Configuration;
import net.md_5.bungee.conf.YamlConfig;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class WaterfallConfiguration extends Configuration {

    /**
     * The supported versions displayed to the client
     * <p>Default is a comma separated list of supported versions. For example 1.8.x, 1.9.x, 1.10.x</p>
     */
    private String gameVersion;

    /**
     * Whether we use Netty's async DNS resolver for the HttpClient.
     * <p>Default is true (use Netty's async DNS resolver)</p>
     */
    private boolean useNettyDnsResolver = true;

    /*
     * Throttling options
     * Helps prevent players from overloading the servers behind us
     */

    /**
     * How often players are allowed to send tab throttle.
     * Value in milliseconds.
     * <p/>
     * Default is one packet per second.
     */
    private int tabThrottle = 1000;
    private boolean disableModernTabLimiter = true;

    private boolean disableEntityMetadataRewrite = false;
    private boolean disableTabListRewrite = true;

    private ForwardingMode forwardingMode = ForwardingMode.BUNGEECORD_LEGACY;
    @Getter
    private byte[] forwardingSecret = Util.randomAlphanumericSequence(12);

    /*
     * Plugin Message limiting options
     * Allows for more control over server-client communication
     */

    /**
     * How many channels there can be between server and player,
     * typically used by mods or some plugins.
     */
    private int pluginChannelLimit = 128;

    /**
     * How long the maximum channel name can be,
     * only reason to change it would be broken mods.
     */
    private int pluginChannelNameLimit = 128;

    @Override
    public void load() {
        super.load();
        YamlConfig config = new YamlConfig(new File("waterfall.yml"));
        config.load(); // Load, but no permissions
        gameVersion = Joiner.on(", ").join(ProtocolConstants.SUPPORTED_VERSIONS);
        useNettyDnsResolver = config.getBoolean("use_netty_dns_resolver", useNettyDnsResolver);
        // Throttling options
        tabThrottle = config.getInt("throttling.tab_complete", tabThrottle);
        disableModernTabLimiter = config.getBoolean("disable_modern_tab_limiter", disableModernTabLimiter);
        disableEntityMetadataRewrite = config.getBoolean("disable_entity_metadata_rewrite", disableEntityMetadataRewrite);
        disableTabListRewrite = config.getBoolean("disable_tab_list_rewrite", disableTabListRewrite);

        forwardingMode = ForwardingMode.valueOf(config.getString("forwarding_mode", ForwardingMode.BUNGEECORD_LEGACY.toString()).toUpperCase());
        final Logger logger = XenonCore.instance.getLogger();
        if (super.isIpForward()) {
            switch (forwardingMode) {
                case BUNGEECORD_LEGACY:
                    logger.info("Forwarding mode is set to Bungeecord/Legacy forwarding. " +
                            "It is recommended to use another forwarding method to mitigate information spoofing attacks.");
                    break;
                case BUNGEEGUARD:
                    logger.info("Forwarding mode is set to BungeeGuard forwarding. " +
                            "Please ensure all connected servers make use of BungeeGuard for optimal security.");
                    break;
                case VELOCITY_MODERN:
                    logger.info("Forwarding mode is set to modern/Velocity forwarding. " +
                            "If you need to use versions older than 1.13 please use another forwarding type.");
                    break;
            }
        } else {
            logger.warn("Information forwarding (ip-forwarding) is disabled. " +
                    "Player UUIDs may not be consistent across the servers. " +
                    "For the optimal experience please enable ip_forward in the config.yml and " +
                    "configure forwarding and on your servers.");
        }

        if (config.getString("forwarding_secret", "").isEmpty()) {
            config.regenerateForwardingSecret();
            logger.warn("A new forwarding secret has been generated. If this was the " +
                    "first start of the proxy please configure forwarding for your network.");
        }
        forwardingSecret = config.getString("forwarding_secret", "").getBytes(StandardCharsets.UTF_8);

        pluginChannelLimit = config.getInt("registered_plugin_channels_limit", pluginChannelLimit);
        pluginChannelNameLimit = config.getInt("plugin_channel_name_limit", pluginChannelNameLimit);
    }

    @Override
    public String getGameVersion() {
        return gameVersion;
    }

    @Override
    public boolean isUseNettyDnsResolver() {
        return useNettyDnsResolver;
    }

    @Override
    public int getTabThrottle() {
        return tabThrottle;
    }

    @Override
    public boolean isDisableModernTabLimiter() {
        return disableModernTabLimiter;
    }

    @Override
    public boolean isDisableEntityMetadataRewrite() {
        return disableEntityMetadataRewrite;
    }

    @Override
    public boolean isDisableTabListRewrite() {
        return disableTabListRewrite;
    }

    @Override
    public int getPluginChannelLimit() {
        return pluginChannelLimit;
    }

    @Override
    public int getPluginChannelNameLimit() {
        return pluginChannelNameLimit;
    }

    @Override
    public ForwardingMode getForwardingMode() {
        return forwardingMode;
    }
}
