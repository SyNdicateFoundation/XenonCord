package net.md_5.bungee.api.dialog;

import lombok.*;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.dialog.action.DialogAction;

@Data
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Accessors(fluent = true)
public class NoticeDialog implements Dialog
{

    @Accessors(fluent = false)
    @NonNull
    private DialogBase base;
    private DialogAction action;

    public NoticeDialog(DialogBase base)
    {
        this( base, null );
    }
}
