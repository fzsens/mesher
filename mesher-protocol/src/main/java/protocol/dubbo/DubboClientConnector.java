package protocol.dubbo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import proxy.core.ClientConfig;
import proxy.core.connect.AbstractClientConnector;
import proxy.core.connect.channel.ClientChannel;

import java.net.SocketAddress;

/**
 * Created by fzsens on 6/10/18.
 */
public class DubboClientConnector extends AbstractClientConnector<ClientChannel> {

    public DubboClientConnector(SocketAddress address) {
        super(address);
    }

    @Override
    public ClientChannel newClientChannel(Channel nettyChannel, ClientConfig clientConfig) {
        DubboClientChannel channel = new DubboClientChannel(nettyChannel);
        channel.getNettyChannel().pipeline().addLast("dubbo", channel);
        return channel;
    }

    @Override
    public ChannelInitializer<Channel> newChannelInitializer(ClientConfig clientConfig) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                // init initializer
                channel.pipeline().addLast(new DubboRpcDecoder());
                channel.pipeline().addLast(new DubboRpcEncoder());
            }
        };
    }
}
