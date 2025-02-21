package net.md_5.bungee.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

@Data
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class PacketSendEvent extends Event implements Cancellable {
    private Object packet;
    private String playerIP;
    private boolean cancelled;

    public PacketSendEvent(Object packetIn, String playerIP) {
        this.packet = packetIn;
        this.playerIP = playerIP;
    }
}
