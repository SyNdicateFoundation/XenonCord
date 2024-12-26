package net.md_5.bungee;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionThrottle
{

    private final LoadingCache<InetAddress, AtomicInteger> throttle;
    private final int throttleLimit;

    public ConnectionThrottle(int throttleTime, int throttleLimit)
    {
        this( Ticker.systemTicker(), throttleTime, throttleLimit );
    }

    @VisibleForTesting
    ConnectionThrottle(Ticker ticker, int throttleTime, int throttleLimit)
    {
        this.throttle = CacheBuilder.newBuilder()
                .ticker( ticker )
                .concurrencyLevel( Runtime.getRuntime().availableProcessors() )
                .initialCapacity( 100 )
                .expireAfterWrite( throttleTime, TimeUnit.MILLISECONDS )
                .build( new CacheLoader<InetAddress, AtomicInteger>()
                {
                    @NotNull
                    @Override
                    public AtomicInteger load(InetAddress key) {
                        return new AtomicInteger();
                    }
                } );
        this.throttleLimit = throttleLimit;
    }

    public void unthrottle(SocketAddress socketAddress)
    {
        if ( !( socketAddress instanceof InetSocketAddress ) )
        {
            return;
        }

        AtomicInteger throttleCount = throttle.getIfPresent( ( (InetSocketAddress) socketAddress ).getAddress() );
        if ( throttleCount != null )
            throttleCount.decrementAndGet();
    }

    public boolean throttle(SocketAddress socketAddress)
    {
        if ( !( socketAddress instanceof InetSocketAddress ) )
        {
            return false;
        }

        int throttleCount = throttle.getUnchecked(  ( (InetSocketAddress) socketAddress ).getAddress() ).incrementAndGet();

        return throttleCount > throttleLimit;
    }
}
