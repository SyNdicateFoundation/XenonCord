package net.md_5.bungee.chat;

import com.google.gson.*;
import net.md_5.bungee.api.chat.SelectorComponent;

import java.lang.reflect.Type;

public class SelectorComponentSerializer extends BaseComponentSerializer implements JsonSerializer<SelectorComponent>, JsonDeserializer<SelectorComponent>
{

    @Override
    public SelectorComponent deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject object = element.getAsJsonObject();
        JsonElement selector = object.get( "selector" );
        if ( selector == null )
        {
            throw new JsonParseException( "Could not parse JSON: missing 'selector' property" );
        }
        SelectorComponent component = new SelectorComponent( selector.getAsString() );

        JsonElement separator = object.get( "separator" );
        if ( separator != null )
        {
            component.setSeparator( ComponentSerializer.deserialize( separator.getAsString() ) );
        }

        deserialize( object, component, context );
        return component;
    }

    @Override
    public JsonElement serialize(SelectorComponent component, Type type, JsonSerializationContext context)
    {
        JsonObject object = new JsonObject();
        serialize( object, component, context );
        object.addProperty( "selector", component.getSelector() );

        if ( component.getSeparator() != null )
        {
            object.addProperty( "separator", ComponentSerializer.toString( component.getSeparator() ) );
        }
        return object;
    }
}
