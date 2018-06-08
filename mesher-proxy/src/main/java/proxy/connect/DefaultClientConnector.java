package proxy.connect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import proxy.connect.channel.DefaultClientChannel;

import java.net.SocketAddress;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public class DefaultClientConnector extends AbstractClientConnector<DefaultClientChannel> {

    public DefaultClientConnector(SocketAddress address) {
        super(address);
    }

    @Override
    public DefaultClientChannel newClientChannel(Channel nettyChannel, ClientConfig clientConfig) {
        DefaultClientChannel channel = new DefaultClientChannel(nettyChannel, clientConfig.getTimer());
        channel.getNettyChannel().pipeline().addLast("default", channel);
        return channel;
    }

    @Override
    public ChannelInitializer<Channel> newChannelInitializer(ClientConfig clientConfig) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                
            }
        };
    }
}
