package proxy.connect;

import io.netty.channel.ChannelFuture;
import proxy.bootstrap.ClientBootstrap;
import proxy.connect.channel.ClientChannel;

import java.net.SocketAddress;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public abstract class AbstractClientConnector<T extends ClientChannel> implements ClientConnector<T> {

    private final SocketAddress address;

    public AbstractClientConnector(SocketAddress address) {
        this.address = address;
    }

    @Override
    public ChannelFuture connect(ClientBootstrap bootstrap) {
        return bootstrap.connect(address);
    }

}
