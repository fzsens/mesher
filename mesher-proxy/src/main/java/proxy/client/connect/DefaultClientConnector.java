package proxy.client.connect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import proxy.client.ClientConfig;
import proxy.client.connect.channel.ClientChannel;
import proxy.client.connect.channel.DefaultClientChannel;

import java.net.SocketAddress;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public class DefaultClientConnector extends AbstractClientConnector<ClientChannel> {

    public DefaultClientConnector(SocketAddress address) {
        super(address);
    }

    @Override
    public ClientChannel newClientChannel(Channel nettyChannel, ClientConfig clientConfig) {
        DefaultClientChannel channel = new DefaultClientChannel(nettyChannel);
        channel.getNettyChannel().pipeline().addLast("default", channel);
        return channel;
    }

    @Override
    public ChannelInitializer<Channel> newChannelInitializer(ClientConfig clientConfig) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                // init initializer
            }
        };
    }
}
