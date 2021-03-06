package proxy.core.connect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import proxy.core.ClientConfig;
import proxy.core.bootstrap.ClientBootstrap;
import proxy.core.connect.channel.RequestChannel;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public interface ClientConnector<T extends RequestChannel> {

    ChannelFuture connect(ClientBootstrap bootstrap);

    T newClientChannel(Channel channel, ClientConfig clientConfig);

    ChannelInitializer<Channel> newChannelInitializer(ClientConfig clientConfig);
}
