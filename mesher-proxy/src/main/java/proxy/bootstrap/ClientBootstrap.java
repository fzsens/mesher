package proxy.bootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by fzsens on 6/3/18.
 */
public interface ClientBootstrap
{
    ClientBootstrap setOption(String option, Object value);

    ClientBootstrap setOptions(Map<String, Object> options);

    ClientBootstrap handler(ChannelHandler handler);

    ClientBootstrap group(NioEventLoopGroup group);

    ChannelFuture connect(SocketAddress address);
}