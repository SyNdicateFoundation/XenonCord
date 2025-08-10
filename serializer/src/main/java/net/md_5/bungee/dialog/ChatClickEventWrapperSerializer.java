package net.md_5.bungee.dialog;


import com.google.gson.*;
import net.md_5.bungee.api.dialog.action.StaticAction;
import net.md_5.bungee.chat.ClickEventSerializer;

import java.lang.reflect.Type;

public class ChatClickEventWrapperSerializer implements JsonDeserializer<StaticAction.ChatClickEventWrapper>, JsonSerializer<StaticAction.ChatClickEventWrapper>
{
    @Override
    public StaticAction.ChatClickEventWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        return new StaticAction.ChatClickEventWrapper( ClickEventSerializer.DIALOG.deserialize( json.getAsJsonObject(), context ) );
    }

    @Override
    public JsonElement serialize(StaticAction.ChatClickEventWrapper src, Type typeOfSrc, JsonSerializationContext context)
    {
        return ClickEventSerializer.DIALOG.serialize( src.event(), context );
    }
}