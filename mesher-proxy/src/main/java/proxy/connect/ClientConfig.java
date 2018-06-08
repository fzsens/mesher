package proxy.connect;

import com.google.common.net.HostAndPort;
import io.netty.util.Timer;

import java.util.Map;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public class ClientConfig {

    private final Map<String, Object> bootstrapOptions;
    private final HostAndPort defaultSocksProxyAddress;
    private final Timer timer;
    private final int workerThreadCount;

    public ClientConfig(Map<String, Object> bootstrapOptions, HostAndPort defaultSocksProxyAddress, Timer timer, int workerThreadCount) {
        this.bootstrapOptions = bootstrapOptions;
        this.defaultSocksProxyAddress = defaultSocksProxyAddress;
        this.timer = timer;
        this.workerThreadCount = workerThreadCount;
    }

    public Map<String, Object> getBootstrapOptions() {
        return bootstrapOptions;
    }

    public HostAndPort getDefaultSocksProxyAddress() {
        return defaultSocksProxyAddress;
    }

    public Timer getTimer() {
        return timer;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }
}
