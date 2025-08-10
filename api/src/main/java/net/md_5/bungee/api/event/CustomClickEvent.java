package net.md_5.bungee.api.event;

import java.util.Map;

import com.google.gson.JsonElement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;
import org.jetbrains.annotations.ApiStatus;

/**
 * Called after a {@link ProxiedPlayer} runs a custom action from a chat event
 * or form submission.
 */
@Data
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
@ApiStatus.Experimental
public class CustomClickEvent extends Event implements Cancellable
{

    /**
     * Map key containing the form action, if available.
     */
    public static final String ACTION_KEY = "action";
    //
    /**
     * Player who clicked.
     */
    private final ProxiedPlayer player;
    /**
     * Custom action ID.
     */
    private final String id;
    /**
     * The data as submitted.
     */
    private final JsonElement data;
    /**
     * Cancelled state.
     */
    private boolean cancelled;
}