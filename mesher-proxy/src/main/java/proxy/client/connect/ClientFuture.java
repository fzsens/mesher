package proxy.client.connect;

/**
 * Created by fzsens on 6/9/18.
 */

import com.google.common.util.concurrent.AbstractFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import proxy.client.ClientConfig;
import proxy.client.connect.channel.ClientChannel;

public class ClientFuture<T extends ClientChannel> extends AbstractFuture<T> {
    public ClientFuture(final ClientConnector<T> connector, ClientConfig config, final ChannelFuture future) {
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                try {
                    if (future.isSuccess()) {
                        Channel nettyChannel = future.channel();
                        T channel = connector.newClientChannel(nettyChannel, config);
                        set(channel);
                    } else if (future.isCancelled()) {
                        cancel(true);
                    } else {
                        throw future.cause();
                    }
                } catch (Throwable t) {
                    setException(t);
                }
            }
        });
    }
}