package proxy.core.bootstrap;

import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * proxy to server bootstrap.
 * <p/>
 * Created by fzsens on 6/3/18.
 */
public class DefaultClientBootstrap implements ClientBootstrap {

    private final Bootstrap bootstrap;

    private ImmutableMap.Builder<ChannelOption, Object> bootstrapOptionsBuilder = ImmutableMap.builder();

    public DefaultClientBootstrap() {
        this.bootstrap = new Bootstrap();
        this.bootstrap.channel(NioSocketChannel.class);
    }

    @Override
    public DefaultClientBootstrap setOption(ChannelOption option, Object value) {
        this.bootstrapOptionsBuilder.put(option, value);
        return this;
    }

    @Override
    public DefaultClientBootstrap setOptions(Map<ChannelOption, Object> bootstrapOptions) {
        this.bootstrapOptionsBuilder = ImmutableMap.<ChannelOption, Object>builder().putAll(bootstrapOptions);
        return this;
    }

    @Override
    public ClientBootstrap handler(ChannelHandler handler) {
        bootstrap.handler(handler);
        return this;
    }

    @Override
    public ChannelFuture connect(SocketAddress address) {
        for (Map.Entry<ChannelOption, Object> option : bootstrapOptionsBuilder.build().entrySet()) {
            ChannelOption key = option.getKey();
            Object val = option.getValue();
            bootstrap.option(key,val);
        }
        return bootstrap.connect(address);
    }

    @Override
    public ClientBootstrap group(NioEventLoopGroup group) {
        bootstrap.group(group);
        return this;
    }
}