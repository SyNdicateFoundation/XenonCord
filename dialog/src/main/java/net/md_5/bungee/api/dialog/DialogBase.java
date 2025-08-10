package net.md_5.bungee.api.dialog;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.dialog.input.DialogInput;

@Data
@AllArgsConstructor
@Accessors(fluent = true)
public class DialogBase
{

    private BaseComponent title;
    @SerializedName("external_title")
    private BaseComponent externalTitle;
    /**
     * The inputs to the dialog.
     */
    private List<DialogInput> inputs;
    private List<?> body;
    @SerializedName("can_close_with_escape")
    private Boolean canCloseWithEscape;
    /**
     * Whether this dialog should pause the game in single-player mode (default:
     * true).
     */
    private Boolean pause;

    /**
     * Action to take after the a click or submit action is performed on the
     * dialog (default: close).
     */
    @SerializedName("after_action")
    private AfterAction afterAction;

    public DialogBase(@NonNull BaseComponent title)
    {
        this( title, null, null, null, null, null, null );
    }

    public enum AfterAction {
        /**
         * Close the dialog.
         */
        @SerializedName("close")
        CLOSE,
        /**
         * Do nothing.
         */
        @SerializedName("none")
        NONE,
        /**
         * Show a waiting for response screen.
         */
        @SerializedName("wait_for_response")
        WAIT_FOR_RESPONSE
    }
}
