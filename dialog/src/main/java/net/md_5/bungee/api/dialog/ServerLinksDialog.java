package net.md_5.bungee.api.dialog;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.dialog.action.DialogAction;

@Data
@ToString
@EqualsAndHashCode
@Accessors(fluent = true)
public class ServerLinksDialog implements Dialog
{
    @NonNull
    @Accessors(fluent = false)
    private DialogBase base;
    @SerializedName("action")
    private DialogAction action;
    @SerializedName("exit_action")
    private DialogAction exitAction;
    private Integer columns;
    @SerializedName("button_width")
    private Integer buttonWidth;

    public ServerLinksDialog(@NonNull DialogBase base)
    {
        this( base, null, null, null );
    }

    public ServerLinksDialog(@NonNull DialogBase base, DialogAction action, Integer columns, Integer buttonWidth)
    {
        this.base = base;
        this.action = action;
        columns( columns );
        buttonWidth( buttonWidth );
    }

    public ServerLinksDialog columns(Integer columns)
    {
        Preconditions.checkArgument( columns == null || columns > 0, "At least one column is required" );
        this.columns = columns;
        return this;
    }

    public ServerLinksDialog buttonWidth(Integer buttonWidth)
    {
        Preconditions.checkArgument( buttonWidth == null || ( buttonWidth >= 1 && buttonWidth <= 1024 ), "buttonWidth must be between 1 and 1024" );
        this.buttonWidth = buttonWidth;
        return this;

    }
}
