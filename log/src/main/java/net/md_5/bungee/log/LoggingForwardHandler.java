package net.md_5.bungee.log;

import lombok.RequiredArgsConstructor;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class LoggingForwardHandler extends Handler
{

    private final Logger logger;

    @Override
    public void publish(LogRecord record)
    {
        logger.log( record );
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void close() throws SecurityException
    {
    }
}
