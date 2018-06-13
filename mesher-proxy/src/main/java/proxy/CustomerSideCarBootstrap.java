package proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.connect.ProtobufClientConnector;
import proxy.core.ClientConfig;
import proxy.core.ProxyClient;
import proxy.core.connect.channel.RequestChannel;
import proxy.handler.customer.ClientProxyHandler;
import proxy.registry.ETCDRegistry;
import proxy.registry.Endpoint;
import proxy.registry.IRegistry;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * launch proxy sidecar
 * Created by fzsens on 2018/5/30.
 */
public class CustomerSidecarBootstrap implements Closeable {

    private final Logger log = LoggerFactory.getLogger(CustomerSidecarBootstrap.class);
    /**
     * all channels created
     */
    private final ChannelGroup allChannels = new DefaultChannelGroup("mesher-proxy-client", GlobalEventExecutor.INSTANCE);

    private final InetSocketAddress bindAddress;

    private Map<Endpoint, RequestChannel> requestChannelMap = new ConcurrentHashMap<>();

    public CustomerSidecarBootstrap(InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    /**
     * Register a new {@link Channel} with this server, for later closing.
     *
     * @param channel nettyChannel
     */
    protected void registerChannel(Channel channel) {
        allChannels.add(channel);
    }

    void initRequestMap(List<Endpoint> endpoints) throws ExecutionException, InterruptedException {
        for (Endpoint endpoint : endpoints) {
            ClientConfig config = new ClientConfig(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
            ProxyClient client = new ProxyClient(config);
            RequestChannel clientChannel = client.connectAsync(new ProtobufClientConnector(config.getDefaultSocksProxyAddress())).get();
            this.requestChannelMap.put(endpoint, clientChannel);
        }

    }

    void doStart() throws Exception {

        IRegistry registry = new ETCDRegistry("http://127.0.0.1:2379");

        List<Endpoint> endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");

        initRequestMap(endpoints);

        ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
            protected void initChannel(Channel ch) throws Exception {
                new ClientProxyHandler(
                        ch.pipeline(), requestChannelMap);
            }
        };
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioServerSocketChannel.class)
                .childHandler(initializer);
        ChannelFuture future = serverBootstrap.bind(bindAddress)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future)
                            throws Exception {
                        if (future.isSuccess()) {
                            registerChannel(future.channel());
                        }
                    }
                }).awaitUninterruptibly();
        Throwable cause = future.cause();
        if (cause != null) {
            throw new RuntimeException(cause);
        }
        InetSocketAddress boundAddress = ((InetSocketAddress) future.channel().localAddress());
        log.info("Proxy started at address: " + boundAddress);
    }


    public static void main(String[] args) throws Exception {
        CustomerSidecarBootstrap bootstrap = new CustomerSidecarBootstrap(new InetSocketAddress("127.0.0.1", 20000));
        bootstrap.doStart();
    }

    @Override
    public void close() throws IOException {
        this.allChannels.close().awaitUninterruptibly();
    }
}
