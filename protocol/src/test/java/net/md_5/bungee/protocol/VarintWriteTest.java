package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class VarintWriteTest
{
    private final int[] ints = new Random( 8132264708911581L ).ints( 4096 ).toArray();

    private static void originalVarintWrite(int value, ByteBuf output)
    {
        int part;
        while ( true )
        {
            part = value & 0x7F;

            value >>>= 7;
            if ( value != 0 )
            {
                part |= 0x80;
            }

            output.writeByte( part );

            if ( value == 0 )
            {
                break;
            }
        }
    }

    @Test
    public void testWriteVarint()
    {
        for ( int i : ints )
        {
            ByteBuf expected = Unpooled.buffer( 5 );
            originalVarintWrite( i, expected );

            ByteBuf actual = Unpooled.buffer( 5 );
            DefinedPacket.writeVarInt( i, actual );

            Assertions.assertArrayEquals( expected.array(), actual.array() , "Number " + i);

            expected.release();
            actual.release();
        }
    }

    @Test
    public void testWriteVarintWithLen()
    {
        for ( int i : ints )
        {
            ByteBuf expected = Unpooled.buffer( 5 );
            originalVarintWrite( i, expected );

            ByteBuf actual = Unpooled.buffer( 5 );
            DefinedPacket.writeVarInt( i, actual, Varint21LengthFieldPrepender.varintSize( i ) );

            Assertions.assertArrayEquals( expected.array(), actual.array(),  "Number " + i );

            expected.release();
            actual.release();
        }
    }
}