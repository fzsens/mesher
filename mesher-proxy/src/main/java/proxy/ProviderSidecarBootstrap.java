package proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.dubbo.DubboClientConnector;
import protocol.dubbo.protobuf.MesherProtoDubbo;
import proxy.core.ClientConfig;
import proxy.core.ProxyClient;
import proxy.core.connect.ClientConnector;
import proxy.core.connect.channel.ClientChannel;
import proxy.core.connect.channel.RequestChannel;
import proxy.handler.provider.ProviderProxyHandler;
import proxy.registry.ETCDRegistry;
import proxy.registry.IpHelper;

import java.io.Closeable;
import java.net.InetSocketAddress;

/**
 * Created by fzsens on 6/3/18.
 */
public class ProviderSidecarBootstrap implements Closeable {

    private Logger log = LoggerFactory.getLogger(ProviderSidecarBootstrap.class);
    /**
     * all channels created
     */
    private final ChannelGroup allChannels = new DefaultChannelGroup("mesher-server-proxy", GlobalEventExecutor.INSTANCE);

    private final InetSocketAddress bindAddress;

    public ProviderSidecarBootstrap(InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    /**
     * Register a new {@link Channel} with this server, for later closing.
     *
     * @param channel channel
     */
    protected void registerChannel(Channel channel) throws Exception {
        allChannels.add(channel);
        String etcdUrl= System.getProperty("etcd.url");
        String serviceName = "com.alibaba.dubbo.performance.demo.provider.IHelloService";
        String weight = System.getProperty("load.weight");
        // register to etcd
        ETCDRegistry etcdRegistry = new ETCDRegistry(etcdUrl);
        etcdRegistry.register(serviceName, this.bindAddress.getPort(), weight);
    }

    void doStart() throws Exception {

        int dubboPort = Integer.parseInt(System.getProperty("dubbo.protocol.port"));

        ClientConfig config = new ClientConfig(new InetSocketAddress(IpHelper.getHostIp(), dubboPort));
        ProxyClient client = new ProxyClient(config);
        ClientConnector<ClientChannel> defaultConnector =
                new DubboClientConnector(new InetSocketAddress(IpHelper.getHostIp(), dubboPort));
        RequestChannel channel = client.connectAsync(defaultConnector).get();

        ProviderProxyHandler handler = new ProviderProxyHandler(channel);
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(MesherProtoDubbo.Request.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast(handler);
                    }
                });
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

    @Override
    public void close() {
        this.allChannels.close().awaitUninterruptibly();
    }

    /**
     * -Dserver.port=30000 -Detcd.url=http://127.0.0.1:2379 -Ddubbo.protocol.port=20880  -Dload.weight=3
     */
    public static void main(String[] args) throws Exception {
        int serverPort= Integer.parseInt(System.getProperty("server.port"));
        ProviderSidecarBootstrap bootstrap = new ProviderSidecarBootstrap(new InetSocketAddress(IpHelper.getHostIp(), serverPort));
        bootstrap.doStart();
    }
}
