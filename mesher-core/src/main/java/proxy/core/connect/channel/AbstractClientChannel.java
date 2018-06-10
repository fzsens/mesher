package proxy.core.connect.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by thierry.fu on 2018/6/8.
 */
@SuppressWarnings("ALL")
public abstract class AbstractClientChannel extends ChannelDuplexHandler implements ClientChannel {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientChannel.class);

    private final Channel nettyChannel;

    private final Map<Integer, Request> requestMap = new HashMap<>();

    private volatile Exception channelError;


    protected AbstractClientChannel(Channel nettyChannel) {
        this.nettyChannel = nettyChannel;
    }

    @Override
    public Channel getNettyChannel() {
        return nettyChannel;
    }

    protected abstract ByteBuf extractResponse(Object message) throws Exception;

    protected abstract int extractSequenceId(Object messageBuffer) throws Exception;

    protected abstract ChannelFuture writeRequest(Object request);

    public void close() {
        getNettyChannel().close();
    }

    @Override
    public boolean hasError() {
        return channelError != null;
    }

    @Override
    public Exception getError() {
        return channelError;
    }


    @Override
    public void executeInIoThread(Runnable runnable) {
        NioSocketChannel nioSocketChannel = (NioSocketChannel) getNettyChannel();
        nioSocketChannel.eventLoop().execute(runnable);
    }

    @Override
    public void sendAsyncRequest(final Object message,
                                 final Listener listener) throws Exception {
        final int sequenceId = extractSequenceId(message);
        executeInIoThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Request request = makeRequest(sequenceId, listener);
                    // TODO TimeOut
                    if (!getNettyChannel().isActive()) {
                        fireChannelErrorCallback(listener, new Exception("Channel Closed!"));
                        return;
                    }
                    if (hasError()) {
                        fireChannelErrorCallback(listener, new Exception("has some error before."));
                        return;
                    }
                    ChannelFuture sendFuture = writeRequest(message);
                    getNettyChannel().flush();
                    sendFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            messageSent(future, request);
                        }
                    });
                } catch (Exception e) {
                    onError(e);
                }
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ByteBuf response = extractResponse(msg);
            if (response != null) {
                int sequenceId = extractSequenceId(response);
                onResponseReceived(sequenceId, response);
            } else {
                super.channelRead(ctx, msg);
            }
        } catch (Exception t) {
            onError(t);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!requestMap.isEmpty()) {
            onError(new Exception("Client was disconnected by server"));
        }
    }

    private void onResponseReceived(int sequenceId, ByteBuf response) {
        Request request = requestMap.remove(sequenceId);
        if (request == null) {
            onError(new Exception("Bad sequence id in response: " + sequenceId));
        } else {
            fireResponseReceivedCallback(request.getListener(), response);
        }
    }

    private void messageSent(ChannelFuture future, Request request) {
        try {
            if (future.isSuccess()) {
                fireRequestSentCallback(request.getListener());
            } else {
                onError(new Exception("send Failed!"));
            }
        } catch (Exception ex) {
            onError(ex);
        }
    }

    private Request makeRequest(int sequenceId, Listener listener) {
        Request request = new Request(listener);
        requestMap.put(sequenceId, request);
        return request;
    }

    protected void onError(Exception ex) {
        if (channelError == null) {
            channelError = ex;
        }
        Collection<Request> requests = new ArrayList<>();
        requests.addAll(requestMap.values());
        requestMap.clear();
        for (Request request : requests) {
            fireChannelErrorCallback(request.getListener(), ex);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        onError(new Exception(cause));
        ctx.channel().close();
    }

    private static class Request {

        private final Listener listener;

        private Timeout sendTimeout;

        private Timeout receiveTimeout;

        private volatile Timeout readTimeout;

        public Request(Listener listener) {
            this.listener = listener;
        }

        public Listener getListener() {
            return listener;
        }

        public Timeout getReceiveTimeout() {
            return receiveTimeout;
        }

        public void setReceiveTimeout(Timeout receiveTimeout) {
            this.receiveTimeout = receiveTimeout;
        }

        public Timeout getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Timeout readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Timeout getSendTimeout() {
            return sendTimeout;
        }

        public void setSendTimeout(Timeout sendTimeout) {
            this.sendTimeout = sendTimeout;
        }
    }

    private void fireRequestSentCallback(Listener listener) {
        try {
            listener.onRequestSent();
        } catch (Throwable t) {
            LOG.warn("Request sent listener callback triggered an exception: {}", t);
        }
    }

    private void fireResponseReceivedCallback(Listener listener, ByteBuf response) {
        try {
            listener.onResponseReceived(response);
        } catch (Throwable t) {
            LOG.warn("Response received listener callback triggered an exception: {}", t);
        }
    }

    private void fireChannelErrorCallback(Listener listener, Exception exception) {
        try {
            listener.onError(exception);
        } catch (Throwable t) {
            LOG.warn("Channel error listener callback triggered an exception: {}", t);
        }
    }
}
