package net.md_5.bungee.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

/**
 * Represents a player getting kicked from a server.
 */
@Data
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class ServerKickEvent extends Event implements Cancellable {

    /**
     * Player being kicked.
     */
    private final ProxiedPlayer player;
    /**
     * The server the player was kicked from, should be used in preference to
     * {@link ProxiedPlayer#getServer()}.
     */
    private final ServerInfo kickedFrom;
    /**
     * Cancelled status.
     */
    private boolean cancelled;
    /**
     * Kick reason.
     */
    private BaseComponent reason;
    /**
     * Server to send player to if this event is cancelled.
     */
    private ServerInfo cancelServer;
    /**
     * State in which the kick occured.
     */
    private State state;
    // Waterfall start
    /**
     * Circumstances which led to the kick.
     */
    private Cause cause;
    // Waterfall end

    @Deprecated
    public ServerKickEvent(ProxiedPlayer player, BaseComponent[] kickReasonComponent, ServerInfo cancelServer) {
        this(player, kickReasonComponent, cancelServer, State.UNKNOWN);
    }

    @Deprecated
    public ServerKickEvent(ProxiedPlayer player, BaseComponent[] kickReasonComponent, ServerInfo cancelServer, State state) {
        this(player, player.getServer().getInfo(), kickReasonComponent, cancelServer, state);
    }
    // Waterfall end

    @Deprecated
    public ServerKickEvent(ProxiedPlayer player, ServerInfo kickedFrom, BaseComponent[] kickReasonComponent, ServerInfo cancelServer, State state) {
        this(player, kickedFrom, TextComponent.fromArray(kickReasonComponent), cancelServer, state);
    }

    @Deprecated
    public ServerKickEvent(ProxiedPlayer player, ServerInfo kickedFrom, BaseComponent kickReasonComponent, ServerInfo cancelServer, State state) {
        this(player, kickedFrom, kickReasonComponent, cancelServer, state, Cause.UNKNOWN);
    }

    public ServerKickEvent(ProxiedPlayer player, ServerInfo kickedFrom, BaseComponent[] reason, ServerInfo cancelServer, State state, Cause cause) {
        this(player, kickedFrom, TextComponent.fromArray(reason), cancelServer, state, cause);
    }

    // Waterfall start

    public ServerKickEvent(ProxiedPlayer player, ServerInfo kickedFrom, BaseComponent reason, ServerInfo cancelServer, State state, Cause cause) {
        this.player = player;
        this.kickedFrom = kickedFrom;
        this.reason = reason;
        this.cancelServer = cancelServer;
        this.state = state;
        this.cause = cause;
    }

    /**
     * @return the kick reason
     * @deprecated use component methods instead
     */
    @Deprecated
    public String getKickReason() {
        return BaseComponent.toLegacyText(getReason());
    }

    /**
     * @param reason the kick reason
     * @deprecated use component methods instead
     */
    @Deprecated
    public void setKickReason(String reason) {
        this.setReason(TextComponent.fromLegacy(reason));
    }
    // Waterfall end

    /**
     * @return the kick reason
     * @deprecated use single component methods instead
     */
    @Deprecated
    public BaseComponent[] getKickReasonComponent() {
        return new BaseComponent[]
                {
                        getReason()
                };
    }

    /**
     * @param kickReasonComponent the kick reason
     * @deprecated use single component methods instead
     */
    @Deprecated
    public void setKickReasonComponent(BaseComponent[] kickReasonComponent) {
        this.setReason(TextComponent.fromArray(kickReasonComponent));
    }

    public enum State {

        CONNECTING, CONNECTED, UNKNOWN
    }

    // Waterfall start
    public enum Cause {
        SERVER, LOST_CONNECTION, EXCEPTION, UNKNOWN
    }
}
