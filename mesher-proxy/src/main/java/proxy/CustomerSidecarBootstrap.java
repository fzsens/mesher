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
import proxy.handler.customer.CustomerProxyHandler;
import proxy.registry.ETCDRegistry;
import proxy.registry.Endpoint;
import proxy.registry.IRegistry;
import proxy.registry.IpHelper;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
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

    void initRequestMap() throws Exception {
        String etcdUrl = System.getProperty("etcd.url");
        IRegistry registry = new ETCDRegistry(etcdUrl);
        List<Endpoint> endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");

        for (Endpoint endpoint : endpoints) {
            ClientConfig config = new ClientConfig(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
            ProxyClient client = new ProxyClient(config);
            RequestChannel clientChannel = client.connectAsync(new ProtobufClientConnector(config.getDefaultSocksProxyAddress())).get();
            this.requestChannelMap.put(endpoint, clientChannel);
        }

    }

    void doStart() throws Exception {

        initRequestMap();
        ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
            protected void initChannel(Channel ch) throws Exception {
                new CustomerProxyHandler(
                        ch.pipeline(), requestChannelMap);
            }
        };
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
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


    /**
     * -Dserver.port=20000 -Detcd.url=http://127.0.0.1:2379
     */
    public static void main(String[] args) throws Exception {
        int serverPort = Integer.parseInt(System.getProperty("server.port"));
        CustomerSidecarBootstrap bootstrap = new CustomerSidecarBootstrap(new InetSocketAddress(IpHelper.getHostIp(), serverPort));
        bootstrap.doStart();
    }

    @Override
    public void close() throws IOException {
        this.allChannels.close().awaitUninterruptibly();
    }
}
