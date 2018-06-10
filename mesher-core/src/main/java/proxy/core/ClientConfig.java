package proxy.core;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fzsens on 2018/6/8.
 */
public class ClientConfig {

    private final Map<ChannelOption, Object> bootstrapOptions;
    private final SocketAddress defaultSocksProxyAddress;
    private final int workerThreadCount;

    public ClientConfig(SocketAddress defaultSocksProxyAddress) {
        bootstrapOptions = new HashMap<>();
        bootstrapOptions.put(ChannelOption.SO_KEEPALIVE, true);
        bootstrapOptions.put(ChannelOption.TCP_NODELAY, true);
        bootstrapOptions.put(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        workerThreadCount = Runtime.getRuntime().availableProcessors();
        this.defaultSocksProxyAddress = defaultSocksProxyAddress;
    }

    public ClientConfig(Map<ChannelOption, Object> bootstrapOptions, SocketAddress defaultSocksProxyAddress, int workerThreadCount) {
        this.bootstrapOptions = bootstrapOptions;
        this.defaultSocksProxyAddress = defaultSocksProxyAddress;
        this.workerThreadCount = workerThreadCount;
    }

    public Map<ChannelOption, Object> getBootstrapOptions() {
        return bootstrapOptions;
    }

    public SocketAddress getDefaultSocksProxyAddress() {
        return defaultSocksProxyAddress;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }
}
