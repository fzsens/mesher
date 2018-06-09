package proxy.client.bootstrap;

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
 *
 * Created by fzsens on 6/3/18.
 */
public class DefaultClientBootstrap implements ClientBootstrap {

    private final Bootstrap bootstrap;

    private ImmutableMap.Builder<String, Object> bootstrapOptionsBuilder = ImmutableMap.builder();

    public DefaultClientBootstrap() {
        this.bootstrap = new Bootstrap();
        this.bootstrap.channel(NioSocketChannel.class);
    }

    @Override
    public DefaultClientBootstrap setOption(String option, Object value) {
        this.bootstrapOptionsBuilder.put(option, value);
        return this;
    }

    @Override
    public DefaultClientBootstrap setOptions(Map<String, Object> bootstrapOptions) {
        this.bootstrapOptionsBuilder = ImmutableMap.<String, Object>builder().putAll(bootstrapOptions);
        return this;
    }

    @Override
    public ClientBootstrap handler(ChannelHandler handler) {
        bootstrap.handler(handler);
        return this;
    }

    @Override
    public ChannelFuture connect(SocketAddress address) {
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelConfig config = ch.config();
                for (Map.Entry<String, Object> option : bootstrapOptionsBuilder.build().entrySet()) {
                    String optionName = option.getKey();
                    final String setterMethodName = "set" + optionName.substring(0, 1).toUpperCase() + optionName.substring(1);
                    Method method = findMethod(config.getClass(), setterMethodName);
                    method.invoke(config, option.getValue());
                }
            }
        });
        return bootstrap.connect(address);
    }

    private Method findMethod(Class configClass, String setterMethodName) throws NoSuchMethodException {
        checkNotNull(configClass);
        checkNotNull(setterMethodName);

        while (configClass != null && configClass != Object.class) {
            for (Method method : configClass.getDeclaredMethods()) {
                if (method.getName().equals(setterMethodName)) {
                    return method;
                }
            }
            configClass = configClass.getSuperclass();
        }

        throw new NoSuchMethodException(setterMethodName);
    }

    @Override
    public ClientBootstrap group(NioEventLoopGroup group) {
        bootstrap.group(group);
        return this;
    }
}