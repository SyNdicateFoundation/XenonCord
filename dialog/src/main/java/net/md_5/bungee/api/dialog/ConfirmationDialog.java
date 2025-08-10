package net.md_5.bungee.api.dialog;

import lombok.*;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.dialog.action.DialogAction;

@Data
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@Accessors(fluent = true)
public class ConfirmationDialog implements Dialog
{
    @NonNull
    @Accessors(fluent = false)
    private DialogBase base;
    private DialogAction yes;
    private DialogAction no;

    public ConfirmationDialog(@NonNull DialogBase base)
    {
        this( base, null, null );
    }
}
