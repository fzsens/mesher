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
import proxy.connect.RegistryableDubboClientConnector;
import proxy.core.ClientConfig;
import proxy.core.ProxyClient;
import proxy.core.connect.ClientConnector;
import proxy.core.connect.channel.ClientChannel;
import proxy.core.connect.channel.RequestChannel;
import proxy.handler.provider.SrvHandler;
import proxy.registry.ETCDRegistry;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

/**
 * Created by fzsens on 6/3/18.
 */
public class ProviderSidecarBootstrap {

    private Logger log = LoggerFactory.getLogger(ProviderSidecarBootstrap.class);
    /**
     * all channels created
     */
    private final ChannelGroup allChannels = new DefaultChannelGroup("mesher-server-proxy", GlobalEventExecutor.INSTANCE);

    private final InetSocketAddress bindAddress;

    private final ETCDRegistry etcdRegistry = new ETCDRegistry("http://127.0.0.1:2379");

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
        // registry etcd
        String serivceName = "com.alibaba.dubbo.performance.demo.provider.IHelloService";
        etcdRegistry.register(serivceName,this.bindAddress.getPort());
    }

    void doStart() throws ExecutionException, InterruptedException {

        ClientConfig config = new ClientConfig(new InetSocketAddress("127.0.0.1", 20880));
        ProxyClient client = new ProxyClient(config);
        ClientConnector<ClientChannel> defaultConnector = new RegistryableDubboClientConnector(new InetSocketAddress("127.0.0.1", 20880));
        RequestChannel channel = client.connectAsync(defaultConnector).get();

        SrvHandler handler = new SrvHandler(channel);

        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
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

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ProviderSidecarBootstrap bootstrap = new ProviderSidecarBootstrap(new InetSocketAddress("127.0.0.1", 21001));
        bootstrap.doStart();
    }
}
