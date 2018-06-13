package proxy.registry;

import java.net.InetAddress;

/**
 * Created by fzsens on 6/13/18.
 */
public class IpHelper {

    public static String getHostIp() throws Exception {

        String ip = InetAddress.getLocalHost().getHostAddress();
        return ip;
    }
}

