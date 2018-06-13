package proxy.registry;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Created by fzsens on 6/3/18.
 */
public class ETCDRegistry implements IRegistry {

    private Logger log = LoggerFactory.getLogger(ETCDRegistry.class);

    private final String rootPath = "dubbo-mesher";
    private Lease lease;
    private KV kv;
    private long leaseId;

    public ETCDRegistry(String registryAddress) {
        Client client = Client.builder().endpoints(registryAddress).build();
        this.lease = client.getLeaseClient();
        this.kv = client.getKVClient();
        try {
            this.leaseId = lease.grant(30).get().getID();
        } catch (Exception e) {
            e.printStackTrace();
        }
        keepAlive();
    }

    public void keepAlive() {
        Executors.newSingleThreadExecutor().submit(
                () -> {
                    try {
                        Lease.KeepAliveListener listener = lease.keepAlive(leaseId);
                        listener.listen();
                        log.info("KeepAlive lease:" + leaseId + "; Hex format:" + Long.toHexString(leaseId));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @Override
    public void register(String serviceName, int port) throws Exception {
        String strKey = MessageFormat.format("/{0}/{1}/{2}:{3}", rootPath, serviceName, "127.0.0.1", String.valueOf(port));
        ByteSequence key = ByteSequence.fromString(strKey);
        String weight = System.getProperty("lb.weight");
        ByteSequence val;
        if (weight == null) {
            weight = "1";
            val = ByteSequence.fromString(weight);
            log.warn("未设置provider权重，默认设置为1");
        } else {
            val = ByteSequence.fromString(weight);
        }
        kv.put(key, val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
        log.info("Register a new service at:{},weight:{}", strKey, weight);
    }

    @Override
    public List<Endpoint> find(String serviceName) throws Exception {
        String strKey = MessageFormat.format("/{0}/{1}", rootPath, serviceName);
        ByteSequence key = ByteSequence.fromString(strKey);
        GetResponse response = kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get();
        List<Endpoint> endpoints = new ArrayList<>();
        for (com.coreos.jetcd.data.KeyValue kv : response.getKvs()) {
            String s = kv.getKey().toStringUtf8();
            int index = s.lastIndexOf("/");
            String endpointStr = s.substring(index + 1, s.length());
            String host = endpointStr.split(":")[0];
            int port = Integer.valueOf(endpointStr.split(":")[1]);
            int weight = Integer.parseInt(kv.getValue().toStringUtf8());
            Endpoint endpoint = new Endpoint(host,port,weight);
            endpoints.add(endpoint);
        }
        log.info("endpoints size:{}",endpoints.size());
        log.info("endpoints contents:{}",endpoints.toString());
        return endpoints;
    }
}
