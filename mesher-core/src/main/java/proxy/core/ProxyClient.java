package proxy.core;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.core.bootstrap.ClientBootstrap;
import proxy.core.bootstrap.DefaultClientBootstrap;
import proxy.core.connect.ClientConnector;
import proxy.core.connect.ClientFuture;
import proxy.core.connect.DefaultClientConnector;
import proxy.core.connect.channel.ClientChannel;


import java.io.Closeable;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * proxy client
 * Created by fzsens on 6/9/18.
 */
public class ProxyClient implements Closeable {

    private Logger log = LoggerFactory.getLogger(ProxyClient.class);

    private final ClientConfig clientConfig;
    private final SocketAddress defaultSocksProxyAddress;
    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final NioEventLoopGroup workerGroup;

    public ProxyClient(ClientConfig config) {
        this.clientConfig = config;
        this.defaultSocksProxyAddress = config.getDefaultSocksProxyAddress();
        int workerThreadCount = config.getWorkerThreadCount();
        this.workerGroup = new NioEventLoopGroup(workerThreadCount);
    }


    public ClientChannel connectSync(SocketAddress address) throws ExecutionException, InterruptedException {
        Preconditions.checkNotNull(address);
        ClientConnector<ClientChannel> connector = new DefaultClientConnector(address);
        AbstractFuture<ClientChannel> future = connectAsync(connector);
        return future.get();
    }

    public ClientFuture<ClientChannel> connectAsync() {
        ClientConnector<ClientChannel> connector = new DefaultClientConnector(defaultSocksProxyAddress);
        return connectAsync(connector);
    }

    public <T extends ClientChannel> ClientFuture<T> connectAsync(ClientConnector<T> clientConnector) {
        ClientBootstrap bootstrap = new DefaultClientBootstrap().group(workerGroup);
        bootstrap.setOptions(clientConfig.getBootstrapOptions());
        ChannelHandler handler = clientConnector.newChannelInitializer(clientConfig);
        bootstrap.handler(handler);
        ChannelFuture nettyChannelFuture = clientConnector.connect(bootstrap);
        nettyChannelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel channel = future.channel();
                if (future.isSuccess() && channel.isActive()) {
                    allChannels.add(channel);
                }
            }
        });
        return new ClientFuture<>(clientConnector, clientConfig, nettyChannelFuture);
    }

    @Override
    public void close() {
        try {
            try {
                boolean closed = this.allChannels.close().await(5, TimeUnit.SECONDS);
                if(!closed) {
                    log.warn("channel ");
                }
                Future<?> terminationFuture = workerGroup.shutdownGracefully();
                terminationFuture.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException | ExecutionException e) {
                log.warn("did not shutdown properly");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
