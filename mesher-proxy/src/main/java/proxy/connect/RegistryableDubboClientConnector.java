package proxy.connect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.dubbo.DubboClientConnector;
import proxy.core.ClientConfig;
import proxy.core.bootstrap.ClientBootstrap;
import proxy.core.connect.channel.ClientChannel;

import java.net.SocketAddress;

/**
 * Created by thierry.fu on 2018/6/13.
 */
public class RegistryableDubboClientConnector extends DubboClientConnector {

    private Logger log = LoggerFactory.getLogger(RegistryableDubboClientConnector.class);

    public RegistryableDubboClientConnector(SocketAddress address) {
        super(address);
    }

    @Override
    public ClientChannel newClientChannel(Channel nettyChannel, ClientConfig clientConfig) {
        RegistryableDubboClientChannel channel = new RegistryableDubboClientChannel(nettyChannel);
        channel.getNettyChannel().pipeline().addLast("dubbo", channel);
        return channel;
    }

    @Override
    public ChannelFuture connect(ClientBootstrap bootstrap) {
        return super.connect(bootstrap).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(!channelFuture.isSuccess()) {
                    log.info("need reconnect!");
                }
            }
        });
    }
}
