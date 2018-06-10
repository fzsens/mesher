package proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.handler.ClientProxyHandler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * launch proxy sidecar
 * Created by fzsens on 2018/5/30.
 */
public class ProxyBootstrap implements Closeable{

    private Logger log = LoggerFactory.getLogger(ProxyBootstrap.class);
    /**
     * all channels created
     */
    private final ChannelGroup allChannels = new DefaultChannelGroup("mesher-proxy-client", GlobalEventExecutor.INSTANCE);

    private final InetSocketAddress bindAddress;

    public ProxyBootstrap(InetSocketAddress bindAddress) {
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

    void doStart() {
        ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
            protected void initChannel(Channel ch) throws Exception {
                new ClientProxyHandler(
                        ch.pipeline());
            }
        };
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
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


    public static void main(String[] args) {
        ProxyBootstrap bootstrap = new ProxyBootstrap(new InetSocketAddress("127.0.0.1",20000));
        bootstrap.doStart();
    }

    @Override
    public void close() throws IOException {
        this.allChannels.close().awaitUninterruptibly();
    }
}
