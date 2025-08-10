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
public class MultiActionDialog implements Dialog
{
    @NonNull
    @Accessors(fluent = false)
    private DialogBase base;
    @NonNull
    private List<DialogAction> actions;
    private Integer columns;
    @SerializedName("exit_action")
    private DialogAction exitAction;

    public MultiActionDialog(@NonNull DialogBase base, @NonNull DialogAction... actions)
    {
        this( base, Arrays.asList( actions ), null, null );
    }

    public MultiActionDialog(@NonNull DialogBase base, @NonNull List<DialogAction> actions, Integer columns, DialogAction exitAction)
    {
        Preconditions.checkArgument( !actions.isEmpty(), "At least one action must be provided" );
        this.base = base;
        this.actions = actions;
        columns( columns );
        this.exitAction = exitAction;
    }
    public MultiActionDialog columns(Integer columns)
    {
        Preconditions.checkArgument( columns == null || columns > 0, "At least one column is required" );
        this.columns = columns;
        return this;
    }
}
