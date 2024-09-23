package net.md_5.bungee.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.protocol.PacketWrapper;

@Data
@AllArgsConstructor
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class PacketReceiveEvent extends Event implements Cancellable {
    private PacketWrapper packet;
    private ProxiedPlayer player;
    private boolean cancelled;
}
