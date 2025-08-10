package net.md_5.bungee.api.dialog;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.dialog.action.DialogAction;

@Data
@ToString
@EqualsAndHashCode
@Accessors(fluent = true)
public class DialogListDialog implements Dialog
{
    @NonNull
    @Accessors(fluent = false)
    private DialogBase base;
    private List<Dialog> dialogs;
    @SerializedName("exit_action")
    private DialogAction exitAction;
    private Integer columns;
    @SerializedName("button_width")
    private Integer buttonWidth;

    public DialogListDialog(@NonNull DialogBase base, Dialog... dialogs)
    {
        this( base, Arrays.asList( dialogs ), null, null, null );
    }

    public DialogListDialog(@NonNull DialogBase base, List<Dialog> dialogs, DialogAction exitAction, Integer columns, Integer buttonWidth)
    {
        this.base = base;
        this.dialogs = dialogs;
        this.exitAction = exitAction;
        this.columns = columns;
        this.buttonWidth = buttonWidth;
        columns( columns );
        buttonWidth( buttonWidth );
    }
    public DialogListDialog columns(Integer columns) {
        Preconditions.checkArgument(columns == null || columns > 0, "At least one column is required");
        this.columns = columns;
        return this;
    }

    public DialogListDialog buttonWidth(Integer buttonWidth)
    {
        Preconditions.checkArgument( buttonWidth == null || ( buttonWidth >= 1 && buttonWidth <= 1024 ), "buttonWidth must be between 1 and 1024" );
        this.buttonWidth = buttonWidth;
        return this;
    }
}
