/*
 * The original file is licensed under the following license:
 *
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * ---
 *
 * This file is based on the io/netty/handler/flush/FlushConsolidationHandler.java file from Netty (v4.1).
 * It was modified to fit to bungee's use of forwarded connections.
 * All modifications are licensed under the "Modified BSD 3-Clause License" to be found at
 * https://github.com/SpigotMC/BungeeCord/blob/master/LICENSE
 */
package net.md_5.bungee.netty.flush;

import io.netty.channel.*;
import io.netty.util.internal.ObjectUtil;

import java.util.concurrent.Future;

/**
 * {@link ChannelDuplexHandler} which consolidates {@link Channel#flush()} / {@link ChannelHandlerContext#flush()}
 * operations (which also includes
 * {@link Channel#writeAndFlush(Object)} / {@link Channel#writeAndFlush(Object, ChannelPromise)} and
 * {@link ChannelOutboundInvoker#writeAndFlush(Object)} /
 * {@link ChannelOutboundInvoker#writeAndFlush(Object, ChannelPromise)}).
 * <p>
 * Flush operations are generally speaking expensive as these may trigger a syscall on the transport level. Thus it is
 * in most cases (where write latency can be traded with throughput) a good idea to try to minimize flush operations
 * as much as possible.
 * <p>
 * If a {@link FlushSignalingHandler} signalises a read loop is currently ongoing,
 * {@link #flush(ChannelHandlerContext)} will not be passed on to the next {@link ChannelOutboundHandler} in the
 * {@link ChannelPipeline}, as it will pick up any pending flushes when
 * {@link #channelReadComplete(ChannelHandlerContext)} is triggered.
 * If no read loop is ongoing, the behavior depends on the {@code consolidateWhenNoReadInProgress} constructor argument:
 * <ul>
 *     <li>if {@code false}, flushes are passed on to the next handler directly;</li>
 *     <li>if {@code true}, the invocation of the next handler is submitted as a separate task on the event loop. Under
 *     high throughput, this gives the opportunity to process other flushes before the task gets executed, thus
 *     batching multiple flushes into one.</li>
 * </ul>
 * If {@code explicitFlushAfterFlushes} is reached the flush will be forwarded as well (whether while in a read loop, or
 * while batching outside of a read loop).
 * <p>
 * If the {@link Channel} becomes non-writable it will also try to execute any pending flush operations.
 * <p>
 * The {@link BungeeFlushConsolidationHandler} should be put as first {@link ChannelHandler} in the
 * {@link ChannelPipeline} to have the best effect.
 */
public final class BungeeFlushConsolidationHandler extends ChannelDuplexHandler
{
    private final int explicitFlushAfterFlushes;
    private final boolean consolidateWhenNoReadInProgress;
    private final Runnable flushTask;
    private int flushPendingCount;
    boolean readInProgress;
    ChannelHandlerContext ctx;
    private Future<?> nextScheduledFlush;

    /**
     * The default number of flushes after which a flush will be forwarded to downstream handlers (whether while in a
     * read loop, or while batching outside of a read loop).
     */
    public static final int DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES = 256;

    /**
     * Creates a new instance with bungee's default values
     *
     * @param toClient whether this handler is for a bungee-client connection
     * @return a new instance of BungeeFlushConsolidationHandler
     */
    public static BungeeFlushConsolidationHandler newInstance(boolean toClient)
    {
        // Currently the toClient boolean is ignored. It is present in case we find different parameters neccessary
        // for client and server connections.
        return new BungeeFlushConsolidationHandler( 20, false );
    }

    /**
     * Create new instance which explicit flush after {@value DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES} pending flush
     * operations at the latest.
     */
    private BungeeFlushConsolidationHandler()
    {
        this( DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, false );
    }

    /**
     * Create new instance which doesn't consolidate flushes when no read is in progress.
     *
     * @param explicitFlushAfterFlushes the number of flushes after which an explicit flush will be done.
     */
    private BungeeFlushConsolidationHandler(int explicitFlushAfterFlushes)
    {
        this( explicitFlushAfterFlushes, false );
    }

    /**
     * Create new instance.
     *
     * @param explicitFlushAfterFlushes the number of flushes after which an explicit flush will be done.
     * @param consolidateWhenNoReadInProgress whether to consolidate flushes even when no read loop is currently
     * ongoing.
     */
    private BungeeFlushConsolidationHandler(int explicitFlushAfterFlushes, boolean consolidateWhenNoReadInProgress)
    {
        this.explicitFlushAfterFlushes = ObjectUtil.checkPositive( explicitFlushAfterFlushes, "explicitFlushAfterFlushes" );
        this.consolidateWhenNoReadInProgress = consolidateWhenNoReadInProgress;
        this.flushTask = consolidateWhenNoReadInProgress ? new Runnable()
        {
            @Override
            public void run()
            {
                if ( flushPendingCount > 0 && !readInProgress )
                {
                    flushPendingCount = 0;
                    nextScheduledFlush = null;
                    ctx.flush();
                } // else we'll flush when the read completes
            }
        } : null;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception
    {
        this.ctx = ctx;
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception
    {
        if ( readInProgress )
        {
            // If there is still a read in progress we are sure we will see a channelReadComplete(...) call. Thus
            // we only need to flush if we reach the explicitFlushAfterFlushes limit.
            if ( ++flushPendingCount == explicitFlushAfterFlushes )
            {
                flushNow( ctx );
            }
        } else if ( consolidateWhenNoReadInProgress )
        {
            // Flush immediately if we reach the threshold, otherwise schedule
            if ( ++flushPendingCount == explicitFlushAfterFlushes )
            {
                flushNow( ctx );
            } else
            {
                scheduleFlush( ctx );
            }
        } else
        {
            // Always flush directly
            flushNow( ctx );
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        // To ensure we not miss to flush anything, do it now.
        resetReadAndFlushIfNeeded( ctx );
        ctx.fireExceptionCaught( cause );
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
        // Try to flush one last time if flushes are pending before disconnect the channel.
        resetReadAndFlushIfNeeded( ctx );
        ctx.disconnect( promise );
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
    {
        // Try to flush one last time if flushes are pending before close the channel.
        resetReadAndFlushIfNeeded( ctx );
        ctx.close( promise );
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
    {
        if ( !ctx.channel().isWritable() )
        {
            // The writability of the channel changed to false, so flush all consolidated flushes now to free up memory.
            flushIfNeeded( ctx );
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
    {
        flushIfNeeded( ctx );
    }

    void resetReadAndFlushIfNeeded(ChannelHandlerContext ctx)
    {
        readInProgress = false;
        flushIfNeeded( ctx );
    }

    void flushIfNeeded(ChannelHandlerContext ctx)
    {
        if ( flushPendingCount > 0 )
        {
            flushNow( ctx );
        }
    }

    private void flushNow(ChannelHandlerContext ctx)
    {
        cancelScheduledFlush();
        flushPendingCount = 0;
        ctx.flush();
    }

    private void scheduleFlush(final ChannelHandlerContext ctx)
    {
        if ( nextScheduledFlush == null )
        {
            // Run as soon as possible, but still yield to give a chance for additional writes to enqueue.
            nextScheduledFlush = ctx.channel().eventLoop().submit( flushTask );
        }
    }

    private void cancelScheduledFlush()
    {
        if ( nextScheduledFlush != null )
        {
            nextScheduledFlush.cancel( false );
            nextScheduledFlush = null;
        }
    }
}