package proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.registry.IpHelper;

import java.net.InetSocketAddress;

/**
 * Created by fzsens on 6/15/18.
 */
public class ProxyMain {

    private static Logger LOG = LoggerFactory.getLogger(ProxyMain.class);
    public static void main(String[] args) throws Exception {
        int serverPort= Integer.parseInt(System.getProperty("server.port"));

        String type = System.getProperty("type");   // 获取type参数
        if ("provider".equals(type)) {
            LOG.info("provider start.");
            ProviderSidecarBootstrap bootstrap = new ProviderSidecarBootstrap(new InetSocketAddress(IpHelper.getHostIp(), serverPort));
            bootstrap.doStart();
        }
        if ("consumer".equals(type)) {
            LOG.info("consumer start.");
            CustomerSidecarBootstrap bootstrap = new CustomerSidecarBootstrap(new InetSocketAddress(IpHelper.getHostIp(), serverPort));
            bootstrap.doStart();
        }
    }
}
