package proxy.client;

import com.google.common.net.HostAndPort;
import io.netty.util.Timer;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public class ClientConfig {

    private final Map<String, Object> bootstrapOptions;
    private final SocketAddress defaultSocksProxyAddress;
    private final int workerThreadCount;

    public ClientConfig(Map<String, Object> bootstrapOptions, SocketAddress defaultSocksProxyAddress, int workerThreadCount) {
        this.bootstrapOptions = bootstrapOptions;
        this.defaultSocksProxyAddress = defaultSocksProxyAddress;
        this.workerThreadCount = workerThreadCount;
    }

    public Map<String, Object> getBootstrapOptions() {
        return bootstrapOptions;
    }

    public SocketAddress getDefaultSocksProxyAddress() {
        return defaultSocksProxyAddress;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }
}
