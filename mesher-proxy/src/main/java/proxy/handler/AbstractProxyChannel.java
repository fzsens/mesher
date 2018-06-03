package proxy.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * abstract proxy channel
 * Created by fzsens on 2018/5/31.
 */
public abstract class AbstractProxyChannel extends ChannelDuplexHandler {

    static final int CONTENT_LENGTH_D = 1048576;

    static final int IDLE_TIMEOUT = 10;

    /**
     * netty channel for read or write
     */
    protected volatile Channel channel;
    /**
     * relate handler context
     */
    protected volatile ChannelHandlerContext ctx;

    /**
     * trigger read event
     *
     * @param object if this is {@link ClientToProxyChannel} object is request from client otherwise
     *               this is {@link ProxyToServerChannel} and object is response from server
     */
    abstract void doRead(Object object);

    /**
     * @param msg msg
     * @return channelFuture
     */
    protected ChannelFuture doWrite(final Object msg) {
        return channel.writeAndFlush(msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        doRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        try {
            this.ctx = ctx;
            this.channel = ctx.channel();
        } finally {
            super.channelRegistered(ctx);
        }
    }

    /**
     * 使用IdleStateHandler对超时进行管理
     */
    @Override
    public final void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        try {
            if (evt instanceof IdleStateEvent) {
                timedOut();
            }
        } finally {
            super.userEventTriggered(ctx, evt);
        }
    }

    protected void timedOut() {
        disconnect();
    }

    /**
     * 断开Channel连接
     */
    Future<Void> disconnect() {
        if (channel == null) {
            return null;
        } else {
            final Promise<Void> promise = channel.newPromise();
            doWrite(Unpooled.EMPTY_BUFFER).addListener(
                    future -> closeChannel(promise));
            return promise;
        }
    }

    private void closeChannel(final Promise<Void> promise) {
        channel.close().addListener(
                future -> {
                    if (future
                            .isSuccess()) {
                        promise.setSuccess(null);
                    } else {
                        promise.setFailure(future
                                .cause());
                    }
                });
    }


    public boolean isConnected() {
        return this.channel != null && this.channel.isActive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        disconnect();
        cause.printStackTrace();
    }
}
