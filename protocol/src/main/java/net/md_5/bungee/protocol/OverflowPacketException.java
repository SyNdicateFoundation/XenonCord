package net.md_5.bungee.protocol;

public class OverflowPacketException extends RuntimeException
{
    public OverflowPacketException(String message)
    {
        super( message );
    }

    // Waterfall start
    @Override
    public Throwable initCause(Throwable cause)
    {
        if (DefinedPacket.PROCESS_TRACES) {
            return super.initCause(cause);
        }
        return this;
    }

    @Override
    public Throwable fillInStackTrace()
    {
        if (DefinedPacket.PROCESS_TRACES) {
            return super.fillInStackTrace();
        }
        return this;
    }
    // Waterfall end
}
