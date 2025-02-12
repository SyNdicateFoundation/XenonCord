package net.md_5.bungee.protocol.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PluginMessageTest {

    @Test
    public void testModerniseChannel() {
        assertEquals( PluginMessage.BUNGEE_CHANNEL_MODERN, PluginMessage.MODERNISE.apply( PluginMessage.BUNGEE_CHANNEL_LEGACY ) );
        assertEquals( PluginMessage.BUNGEE_CHANNEL_LEGACY, PluginMessage.MODERNISE.apply( PluginMessage.BUNGEE_CHANNEL_MODERN ) );
        assertEquals("legacy:foo", PluginMessage.MODERNISE.apply("FoO"));
        assertEquals("foo:bar", PluginMessage.MODERNISE.apply("foo:bar"));
    }
}
